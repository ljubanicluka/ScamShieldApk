# Supabase setup for ScamShield

This branch adds the first production backend layer: Supabase email/password authentication, persistent sessions, profile creation, and a secured database schema for community data. It does not yet replace the existing local demo feed with remote feed queries; that is a separate integration step and must be completed before claiming cross-device community sync.

## 1. Apply the database migration

1. Open the Supabase project for ScamShield.
2. Open **SQL Editor** and create a new query.
3. Copy the complete contents of `supabase/migrations/202611270001_initial_schema.sql`.
4. Run it once and confirm it completes without errors.

The migration creates profiles, posts, comments, private vote rows, reposts, scam reports, rule acceptances, and a moderation audit table. RLS is enabled on every table. It does not grant clients access to moderation actions or other users' individual vote rows.

## 2. Configure email authentication

In Supabase **Authentication → URL Configuration**, set the Site URL to the public URL you use for the project/app testing. In **Authentication → Providers → Email**, keep email/password enabled and require email confirmation for new accounts. Set allowed redirect URLs to the actual app/web origins you control. Do not add arbitrary wildcard redirects.

Supabase's built-in email sender is rate-limited and intended for testing. For a public launch, configure an email provider you control and verify its sender/domain.

## 3. App configuration

The publishable key is embedded in `supabase-provider.js`; it is intended to be public. RLS is the security boundary. Never put a secret or service-role key in Android assets, JavaScript, GitHub, or an APK.

## 4. Current scope and limitations

- Sign in, registration, verification resend, password recovery, sign out, and session restoration use Supabase Auth.
- New auth users receive a profile row from a database trigger.
- The community posts/comments/votes/reports tables are ready for the next remote-data integration, but the existing UI still uses local demo state until that integration is completed.
- No real user data is migrated from local storage.
- Test email verification and password reset on a real device before release.
- Do not use seeded demo profiles/posts as if they were real reports.
