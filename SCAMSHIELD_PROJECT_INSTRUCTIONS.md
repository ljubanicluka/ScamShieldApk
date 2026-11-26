# ScamShield — persistent project instructions and handoff

Read this file before making any future ScamShield change.

## Permanent maintenance rule
- Only extend this file. Never delete, rewrite, shorten, reorder, or replace existing instructions.
- Append new dated requirements to the Change log. Preserve all previous entries verbatim.
- Mark features as planned, in progress, implemented or blocked. A written requirement is not proof of implementation.
- Work on a feature branch and open a pull request. Do not push directly to main or merge without explicit user approval.
- Do not confuse this native Android Kotlin/WebView repository with the user's older .NET MAUI project.

## Repository architecture
- Repository: ljubanicluka/ScamShieldApk.
- UI source of truth: web/scamshield-app.html.
- Packaged Android WebView UI: app/src/main/assets/index.html. These two files must remain byte-identical.
- Native Android code is Kotlin. Local HTML/JS state is not a live backend and does not sync across devices.
- Never claim a real antivirus, full-device scan, universal app lock, VPN or cross-device community unless the required Android APIs, permissions and server services are implemented and tested.
- QR/link content must never open automatically. Show the destination and require explicit Proceed/Cancel confirmation.

## Community sample dataset
- Required preview seed: exactly 124 fictional accounts and exactly 467 fictional posts.
- Sample content must be disclosed as preview/example data. Never count sample accounts, posts, votes, reports, recommendations, comments or statistics as genuine activity or real scam reports.
- Use varied handles, cities, account ages, vote totals and writing styles. Some accounts should have no posts. Avoid repetitive, obviously generated text.
- Keep the seed deterministic for repeatable tests.
- Sample reputation and reports must not affect real verdicts, moderation or production statistics. Local infinite scrolling is not a live feed.

## Verification and reputation — keep three systems distinct
1. Verified badge: free email verification. Users may hide/show the public indicator without changing the actual email-verification state. No subscription requirement.
2. Community levels are automatic net-vote thresholds (upvotes minus downvotes): Level 1 = 40, Level 2 = 80, Level 3 = 120, Level 4 = 160, Level 5 = 200.
3. Administrator verification is a separate human approval, visibly distinguished with a thicker yellow outline/glow. Never grant it merely for votes or level.
- At 250 net votes, grant the automatic community Verified badge and show a congratulatory notification. Administrator verification remains separate.
- Queue award notifications when offline and show them on next launch; avoid duplicate awards.
- Level 5 should explain that additional verification can be requested. Account age is calculated from the server-created timestamp, not user-entered data.
- Minimum age/activity eligibility may reduce manipulation, but must not silently change the numeric thresholds.
- Endorse/recommend an author once per account, with undo. It must not upvote posts. Show recommended authors on the user's profile; moderator/admin access follows role permissions.

## Community behaviour and presentation
- Separate upvote and downvote actions. Include report, comment, view-all-comments, repost and a distinct one-word author endorsement (current English copy: Endorse).
- Following/unfollowing is supported.
- Posts/comments may be edited; show an Edited marker and retain a suitable audit history.
- Support text, images and links. Before opening an external link, show the destination and require Proceed or Cancel.
- Unauthenticated visitors can browse. Interactions require a verified account, except any deliberately designed anonymous-voting flow.
- Profile-search privacy must not prevent authorized moderation.
- Anonymous votes may be public, while attribution for abuse prevention is server-side and access-controlled for moderators/admins. Avoid unnecessary local storage or APK growth.
- Maintain smooth transitions, swipe navigation, configurable page order and remembered start page.

## Profiles and onboarding
- Usernames are unique. If taken, say username is currently occupied and suggest alternatives.
- Provide built-in avatars and upload from camera, gallery and Android file picker with correct permission handling.
- Users can request account and personal-data deletion. Explain legitimate security/legal retention.
- Ask age during onboarding and apply age-appropriate filters; collect no unnecessary sensitive information.
- Tutorial appears on first registration and can be replayed in Settings. Optional customizations have Skip controls.
- Top-right profile/settings menu includes account switching, appearance/theme, settings, privacy and tutorial replay.

## Age-rated scam education
- Adult-oriented scam categories may cover gambling scams or adult-content impersonation, but explanations remain non-graphic and focused on fraud prevention.
- Apply age gating and category filters. Do not link minors to adult content, gambling services or explicit material.

## Moderation hierarchy and restrictions
- Role escalation: User → Moderator → Admin → Owner. Users can request moderator review; moderators escalate to admins; admins escalate owner-level matters.
- Owner has full role, security and data-policy control. Admin handles account decisions, administrator verification, high-level restrictions and escalations. Moderators handle reports and permitted content restrictions.
- Backend-enforce all roles. A client-side role selector/local storage is never real authorization.
- Admins need flexible independent restrictions with manually entered durations (e.g. 12 hours, 7 days, 45 days), affected permissions, reason, start and exact expiry.
- Cooldown restrictions govern when posting/commenting/voting/uploading resumes. The user explicitly excluded a separate incident-expiry timer.
- Provide appeals and an audit trail. Server-side restrictions expire automatically.

## Admin dashboard and prioritisation
- Search, filter and sort users, posts and reports. Show account age and activity.
- Authorized admins can inspect identity, posts, comments, reposts, votes, recommendations, submitted reports and moderation history.
- Prioritisation may consider report count/velocity, net votes, account age, activity, recommendations, verification and moderation history.
- A threshold such as ten independent reports may raise queue priority; it is not proof of wrongdoing and must not auto-ban.
- Around 250 net votes may improve positive reputation visibility; it must not be represented as a danger score.
- Distinguish reports against an account from reports submitted by that account. Sample reports never trigger production moderation.

## App Lock help and device security
- Start with common Android manufacturers/models. If a model is not listed, update suggestions automatically on each keystroke; no Generate/Search button.
- Prefer official manufacturer documentation. Show source and last-checked date; do not invent model-specific instructions.
- Unlisted models may use safe web lookup with source attribution.
- A banking security checklist may inspect only settings exposed through supported Android APIs/intents or ask the user to confirm. Do not claim access to protected settings Android does not expose.
- Scanner reminders are opt-in and can be skipped during onboarding.

## Location and city alerts
- Incident reports work without location permission; users can select a city/place manually.
- For physical incidents, offer Use my location and request permission only then. Online scams need no physical location.
- Users may follow cities for alerts even when not physically present.
- Do not add a separate incident-expiry timer. Older reports should be labelled historical according to a transparent policy.

## Rulebook and Easter eggs
- Build a rulebook with at least 200 distinct numbered rules and approximately 30 humorous Easter-egg rules.
- Serious sections: accounts, privacy, posts, comments, voting, endorsements, external links, scam reports/evidence, device security, moderation, appeals, roles, retention, third parties and disclaimers.
- Humour is a minority. Include a few light Emperor Damjan/inside-joke references, less prominently than normal rules.
- Jokes must be clearly marked as humour and cannot override safety, privacy, moderation or legal requirements.
- Do not pad with duplicate rules. Keep Serbian and English versions aligned and include version/date/acceptance text.

## Testing and release
- Keep the two HTML files identical.
- Test exact sample counts, unique usernames, net-vote thresholds, verification separation, report/endorsement actions and feed behaviour.
- Run existing Node static/QR/verdict tests.
- Android Gradle build and physical-device validation are separate; never claim success unless run.
- Never commit API keys, signing keys, service-role credentials or private tokens.
- Inspect the diff, test, push the feature branch and open a PR. Do not merge without approval.

## Current implementation status — 2026-11-26
- This branch update seeds 124 fictional accounts and 467 fictional posts, broadens scam topic coverage, distinguishes the automatic 250-net Verified badge from administrator approval, and adds local demo report/all-comments affordances.
- This remains local preview functionality, not a server-backed community. Admin roles, reports, recommendations, votes and restrictions are not production-secured.
- The full 200+ rulebook, real backend, complete admin sorting/restrictions, real phone-model lookup, cross-session notification queue and Android device validation remain follow-up work.

## Change log
### 2026-11-26 — Initial persistent handoff
- User requested 124 accounts, 467 posts, a persistent assistant/developer instruction file and an append-only policy.
- Preserve this file verbatim and append future instructions below.