# ScamShield — assistant project notes

Last updated: 2026-11-27

## Repository
- Repository: `ljubanicluka/ScamShieldApk`
- Working branch: `supabase-backend-integration`
- Keep work on this branch unless the project owner asks otherwise.
- Do not merge pull requests or change the default branch without explicit approval.

## Supabase
- Project ref: `zrobvuscdgbisaqvliil`
- Project URL: `https://zrobvuscdgbisaqvliil.supabase.co`
- The initial schema migration at `supabase/migrations/202611270001_initial_schema.sql` was successfully run by the project owner in Supabase SQL Editor on 2026-11-27 (reported: “Success. No rows returned”).
- The app contains a client-side publishable key. Never store or request secret/service-role keys. If a secret is exposed, advise the owner to revoke/rotate it.
- Next: configure Supabase Auth URL/email settings, test signup/email confirmation/login/session restore, then implement remote community data access with RLS-safe vote aggregates and report workflows.

## Current implementation status
- `web/supabase-provider.js` supplies email/password signup, login, verification resend, password recovery, signout and session restoration.
- `web/scamshield-app.html` loads the auth provider and listens for restored sessions.
- Android assets include matching web UI/provider copies.
- Community feed, comments, votes, reposts and scam reports still use local/demo state; cross-device sync is NOT implemented.
- Do not describe the app as production-ready or claim cross-device sync works until implemented and tested.
- The database schema includes profiles, posts, comments, private vote rows, reposts, scam reports, rule acceptances and moderation audit records with RLS enabled.

## Working rules
- Preserve the existing ScamShield product requirements and UI behaviour unless asked to change them.
- Keep the web HTML and Android asset copies synchronized.
- Do not invent test results. Report build/device/backend checks as untested until actually run.
- Build workflow run #65 failed only at the UI regression assertion for the password field. The app enables the field when the Supabase auth adapter is loaded; the old test incorrectly expected it to remain disabled even with the adapter. The regression test is being corrected to verify both provider-absent (disabled) and provider-present (enabled) states. This is a test correction, not a claim that live Supabase authentication has been tested.
- Keep this file updated when repository structure, branches, implementation status, setup steps or test results change.
- This file is an assistant handoff note; do not add Claude-specific workflow instructions or secrets here.
