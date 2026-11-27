# ScamShield — Supabase setup (foundation)

Project reference: zrobvuscdgbisaqvliil

## What this migration creates
- Auth-linked profiles with private/public visibility.
- Community posts and comments.
- One vote per account per post.
- Reposts and one-per-account author endorsements.
- Scam reports storing a one-way SHA-256 target fingerprint and masked display value, not the raw target.
- Row Level Security policies and database-side email-verification synchronization.

## Apply the migration
1. Open the ScamShield Supabase project.
2. Select SQL Editor → New query.
3. Open supabase/migrations/202611270001_initial_schema.sql from this repository.
4. Paste the complete file into SQL Editor and run it once.
5. Check Table Editor. Expected tables: profiles, posts, comments, post_votes, post_reposts, author_endorsements, scam_reports.

This migration is designed to be re-runnable for policies and triggers. Do not run it against a production database containing important data without taking a backup first.

## Authentication settings
In Authentication → URL Configuration, set the Site URL and allowed redirect URLs to the actual app/web origin you will use. Do not use a placeholder production URL.

In Authentication → Providers → Email, enable email/password and email confirmation. Test signup and password reset with a real mailbox. Supabase's default email sender is rate-limited; configure custom SMTP before a public launch.

## Client configuration
- Project URL: https://zrobvuscdgbisaqvliil.supabase.co
- Use the publishable key in the client.
- Never put a secret / service_role key in the app, web assets, GitHub, or a public build.
- The publishable key is not a password. RLS is the actual data-access boundary.

## Implementation status
This is the database foundation only. The existing app still uses its local sample community and local state until a separate, tested client integration replaces those paths. Do not label the community as live or cross-device yet.

Not yet implemented: production moderation, trusted admin role assignment, spam/rate limiting, aggregate scam-verdict RPCs, cross-account report deduplication, attachment upload policies, and abuse review. The SQL protects role and verification columns from client-side updates. Admin provisioning must use a trusted server-side process, never a value supplied by the app.

For scam-report hashes, the client must normalize the target consistently and hash it with SHA-256. Do not submit raw phone numbers, email addresses, or URLs. A later migration should add a secure aggregate function that counts distinct verified accounts without disclosing reporter identities.

## Next integration stage
1. Add Supabase JS v2 initialization using the project URL and publishable key.
2. Connect auth and restore sessions on app launch.
3. Fetch and update the signed-in user's profile.
4. Add live posts, comments and votes incrementally.
5. Add safe report submission and a privacy-preserving aggregate verdict function.
6. Test RLS with anonymous, normal user, private-profile owner and moderator/admin contexts.
