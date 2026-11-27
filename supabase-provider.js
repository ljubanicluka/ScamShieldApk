/* ScamShield Supabase auth adapter.
 * Public publishable key only. No service-role key belongs in this file.
 */
(() => {
  'use strict';
  const CONFIG = Object.freeze({
    url: 'https://zrobvuscdgbisaqvliil.supabase.co',
    publishableKey: 'sb_publishable__B4Y_zNvkwBXZht4mUyyKQ_qHM89uEO'
  });
  const STORAGE_KEY = 'scamshield.supabase.session.v1';
  let session = null;
  const read = () => { try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null'); } catch (_) { return null; } };
  const write = value => { session = value; try { if (value) localStorage.setItem(STORAGE_KEY, JSON.stringify(value)); else localStorage.removeItem(STORAGE_KEY); } catch (_) {} };
  const headers = (token) => ({
    'apikey': CONFIG.publishableKey,
    'Authorization': 'Bearer ' + (token || CONFIG.publishableKey),
    'Content-Type': 'application/json'
  });
  async function request(path, options = {}, token) {
    const response = await fetch(CONFIG.url + path, {
      ...options,
      headers: { ...headers(token), ...(options.headers || {}) }
    });
    const raw = await response.text();
    let data = null;
    try { data = raw ? JSON.parse(raw) : null; } catch (_) { data = { message: raw }; }
    if (!response.ok) {
      const error = new Error(data?.msg || data?.message || data?.error_description || 'Supabase request failed (' + response.status + ')');
      error.status = response.status;
      throw error;
    }
    return data;
  }
  function normalizeUser(user) {
    if (!user) return null;
    return {
      id: user.id,
      uid: user.id,
      email: user.email || '',
      emailVerified: !!(user.email_confirmed_at || user.confirmed_at)
    };
  }
  async function refreshIfNeeded() {
    if (!session) session = read();
    if (!session?.refresh_token) return null;
    const expiry = Number(session.expires_at || 0);
    if (expiry > Math.floor(Date.now()/1000) + 60) return session;
    const next = await request('/auth/v1/token?grant_type=refresh_token', {
      method: 'POST', body: JSON.stringify({ refresh_token: session.refresh_token })
    });
    write({ ...next, expires_at: Math.floor(Date.now()/1000) + Number(next.expires_in || 3600) });
    return session;
  }
  async function currentUser() {
    const s = await refreshIfNeeded();
    if (!s?.access_token) return null;
    try {
      const user = await request('/auth/v1/user', { method: 'GET' }, s.access_token);
      return normalizeUser(user);
    } catch (error) {
      if (error.status === 401) write(null);
      throw error;
    }
  }
  const provider = {
    async signIn({ email, password }) {
      const data = await request('/auth/v1/token?grant_type=password', {
        method: 'POST', body: JSON.stringify({ email, password })
      });
      write({ ...data, expires_at: Math.floor(Date.now()/1000) + Number(data.expires_in || 3600) });
      return { user: normalizeUser(data.user) };
    },
    async register({ email, password, username }) {
      const data = await request('/auth/v1/signup', {
        method: 'POST',
        body: JSON.stringify({ email, password, data: { username: String(username || '').toLowerCase() } })
      });
      if (data.access_token && data.refresh_token) {
        write({ ...data, expires_at: Math.floor(Date.now()/1000) + Number(data.expires_in || 3600) });
        return { user: normalizeUser(data.user) };
      }
      return { user: null, needsVerification: true };
    },
    async resetPassword({ email }) {
      return request('/auth/v1/recover', { method: 'POST', body: JSON.stringify({ email }) });
    },
    async resendVerification({ email }) {
      return request('/auth/v1/resend', { method: 'POST', body: JSON.stringify({ type: 'signup', email }) });
    },
    async signOut() {
      const s = await refreshIfNeeded();
      try { if (s?.access_token) await request('/auth/v1/logout', { method: 'POST' }, s.access_token); }
      finally { write(null); }
    },
    async restoreSession() { return currentUser(); }
  };
  window.ScamShieldAuth = provider;
  // Re-render account controls after the provider is available, then restore
  // a previously authenticated session without exposing credentials to the UI.
  if (typeof window.R === 'function') window.R();
  provider.restoreSession().then(user => {
    if (user && window.S) {
      window.S.account = { uid: user.id, email: user.email, verified: user.emailVerified };
      window.S.guest = 0;
      if (typeof window.sv === 'function') window.sv();
      if (typeof window.R === 'function') window.R();
    }
  }).catch(() => {});
})();
