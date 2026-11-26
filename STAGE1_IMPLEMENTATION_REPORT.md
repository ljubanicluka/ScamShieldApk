# ScamShield — Stage 1 implementation report

Date: 2026-11-24

## Implemented in this source package

- Six persistent main destinations: Device Security, Home, Community, QR Scanner, I Got Scammed, and Family.
- Bottom navigation and horizontal swipe navigation.
- Profile/settings panel with a persistent startup-page choice and reorder controls for the six destinations.
- Local demo community with 100 synthetic accounts and 1,200 synthetic posts. The feed is explicitly labelled DEMO; it is not represented as real users or real incident reports.
- Demo posts include varied short-form writing styles, occasional lower-case text, missing punctuation and minor typos while remaining readable.
- Local demo upvote/downvote toggles and author recommendations/follows. Net reputation is calculated as total upvotes minus total downvotes. Thresholds: 40, 80, 120, 160 and 200 net votes for Levels 1–5. A score of 100 is Level 2.
- A separate administrator-glow flag. It can be granted or revoked for any account through the demo Admin console and does not depend on the account's level or net score. Email Verified display is independently toggleable and does not alter the underlying email-verification state.
- The 250-net-vote milestone displays eligibility for an administrator review request; it does not silently turn the separate admin-glow badge on.
- Demo Admin console can search accounts, remove demo accounts/posts and grant/revoke administrator glow.
- Private demo profiles are masked in the public feed. Moderator account lists exclude hidden profiles; Admin and Owner lists can see them. Demo account age and separate upvote/downvote totals are displayed in the Admin account view.
- Demo role switching is exposed only as a clearly labelled local test feature. Moderators can remove demo content but cannot inspect hidden profiles, remove accounts or grant Admin glow. Admin/Owner can manage demo accounts and glow. It is not authentication or a real security boundary.
- Existing QR camera/gallery flow, local QR decoding, URL heuristics, native confirmation before opening external HTTP(S) links, and official advisory integration are retained.
- The app asset and source web asset are synchronised so the CI workflow no longer overwrites the updated UI with an older copy.
- Added a regression script for demo account/post counts, net-vote levels, independent admin glow, private-account visibility and admin controls.

## Validation run

- ZIP integrity: PASS.
- `scripts/check-web.js`: PASS.
- `scripts/check-kotlin.js`: PASS (static regression checks only; not an Android compile).
- `scripts/test-phone-norm.js`: PASS (9 tests).
- `scripts/test-qr-analyze.js`: PASS (20 tests).
- `scripts/test-qr-decode.js`: PASS (11 fixtures).
- `scripts/test-verdict.js`: PASS.
- `scripts/test-demo-community.js`: PASS.
- Android Gradle build: NOT RUN locally; Gradle/Android SDK are not installed in this environment.
- Physical device test: NOT RUN.

## Not yet implemented / requires a later stage

- Supabase project, real authentication, email verification, Google sign-in, database, RLS, server-side community, cross-device sync and real moderation.
- Real admin/owner identity and permission enforcement. The role switch in this stage is a demo-only UI simulation.
- Server-enforced page/action restrictions, ban expiry, appeals and moderation audit logs.
- Persistent real account privacy, deletion requests and real vote attribution.
- Real map data and verified location-based notifications.
- Real-time threat-intelligence reputation for URLs, phone numbers, files and images. Current URL checks remain local heuristics; QR images are decoded locally, not classified as safe/unsafe images.
- Device-wide antivirus, call blocking, universal app-lock enforcement or VPN. These cannot be honestly delivered by a normal Android app without platform/third-party support.
- Full multilingual localization beyond the existing Serbian Latin/Cyrillic and English interface.

## Important demo-data rule

Synthetic users, posts, votes, cities, account ages and reputation values are test fixtures only. They must never be mixed into real user totals, real incident counts, scam verdict thresholds, map pins or public evidence. A future backend must keep demo fixtures in a separate development/demo dataset.
