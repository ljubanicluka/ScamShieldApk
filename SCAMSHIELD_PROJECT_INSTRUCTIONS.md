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

## 2026-11-26 — Detailed questionnaire answers and product direction

### Authority
- The user has now answered the 308-question design questionnaire. These answers supersede earlier unanswered-question assumptions. The full numbered questions and recorded answers are in `SCAMSHIELD_QUESTIONNAIRE_AND_ANSWERS.md`; the merged context and copy-ready Claude prompt are in `SCAMSHIELD_CLAUDE_MASTER_CONTEXT.md` and `SCAMSHIELD_CLAUDE_PROMPT.md`.
- Preserve this file append-only. Do not delete or rewrite previous content. Do not ask again for already answered requirements. Keep all ambiguities below visible.

### Product identity and appearance
- Professional cybersecurity application combined with X/Reddit-inspired community. QR/link checking and community are central.
- Community is detailed/information-rich. QR and Family are minimal/clean.
- Rounded cards; fully rounded pill-shaped rectangular buttons (not circular); simple distinctive icons for every section; ScamShield logo in sticky top bar; sticky bottom navigation; subtle borders; restrained shadows mainly in onboarding/tutorial. Slight card press animation. No generic page-opening loading animation. Silent by default; haptics are optional pending evaluation.
- Light theme white/blue/dark text; dark theme near-black/yellow/white. Follow Android theme by default, allow custom accent/appearance, remember per account. Compact but readable UI; user text-size controls, Android accessibility font scaling, tablet-specific layout. Red for important security warnings.
- Six bottom destinations always visible: Device Security, Home, Community, QR Scanner, I've Been Scammed, Family. Selected destination gets subtle accent-tinted background.
- Swipe follows the configured navigation order and user's finger, horizontal slide, target 150 ms. Nested horizontal gesture handling should be configurable and must not break galleries/carousels. Page order drag-and-drop editable, reset layout, starting-page selection in onboarding and Settings. Profiles open as separate sliding screens. Use normal Android Back-stack behaviour. Persist selected page; test scroll restoration.

### Guest access, authentication, onboarding
- Guest users can preview/browse, but community interactions are unavailable until registered and eligible.
- Email/password and Google sign-in. Email verification immediately after email/password registration; use provider verification for Google.
- Ask age, name, username and sex. Username changes have a 7-day cooldown. Display name is optional; if offered it can be changed independently. Profile picture and username can be changed after registration.
- Tutorial must be an interactive walkthrough of the real UI with coach-mark overlays, illustrations, brief animations and screenshots. Not static slides. Skip individual sections, but no global one-tap skip. Show completion percentage; allow replay in Settings.
- Explain all six sections, Settings, QR/link, reports, rules, badges/reputation, location and Family permissions. Request notification permission during onboarding after rationale; request other permissions contextually after explaining why. Do not request unsupported/unnecessary permissions.
- User reports that current app has no working sign-in, tutorial or Rules page. Do not claim otherwise.

### Profiles, community and reputation
- Each of the 124 sample accounts needs a distinct complete fictional profile; every username/avatar is clickable. Tap opens a compact overview, then tap overview opens full profile.
- Full profile has four tabs/icons under name: Posts, Comments, Reposts, About. Optional bio omitted when empty. Country can be hidden. Support banner, profile-photo crop/rotate/zoom, and hideable account creation date.
- Settings include login history, active sessions and sign-out of all other devices. Deletion requires confirmation, seven-day waiting period and admin review/processing.
- Popular/Personal feed by default; Latest, Trending, Following views; search, infinite scroll, bookmarks, sharing, multi-image/document evidence, detailed post/thread view, threaded comments, edit/delete with Edited marker and moderator-visible edit history, repost/quote-repost, user-chosen scam tags, content warnings and comment reports. No automatic tagging; provide more user-selectable scam categories. No post character limit.
- Image/evidence viewer should support zoom, rotate, copy and download where applicable. Posts/images/links are reviewed by moderators before public display; admin-verified users may bypass review. Enforce on backend.
- Endorse action wording: `Recommend`; separate from post votes and reversible. Notification history is deletable. Follow notifications only appear for actual activity; comment/reply notifications optional.
- Keep email verification, community level and administrator verification internally distinct. Net vote thresholds remain 40/80/120/160/200 for Levels 1–5; requested visual names Bronze, Iron, Gold, Diamond, Emerald. Automatic community Verified at 250 net votes remains separate from manual admin verification (yellow outline/glow). User wants badge explanation, progress, award animation and notifications; negative votes immediately reduce progress, levels can be lost, automatic Verified not retained below threshold.
- Q76 later says not to show automatic and admin verification separately on profiles; this conflicts with prior rules. Do not merge underlying states; confirm public display preference later.
- User skipped visual inspiration questions 287–291 because provided references looked bad. Q292–297 mostly unanswered; explicit design direction above is sufficient to proceed.

### QR, links and Device Security
- QR landing page shows Scan, Upload Image and Enter Link choices. Camera preview in upper half, not full screen. Include scan frame/animation, detection feedback, available-only flashlight, zoom. Gallery and Android file picker; auto-decode; multiple codes per image. No barcode support needed.
- Recognise Wi-Fi, contacts, phone, email, payment, websites, text, app-store/download links and locations. Show complete scrollable preview before action.
- Never automatically open links, connect to Wi-Fi, save contacts, call, email, launch apps, navigate or pay. External links always require a full destination preview and Proceed/Cancel.
- Analyse domains using available threat data. Safe/Suspicious/Likely Scam only with evidence and explanation; domain age only if reliable. Offline decoding is required; distinguish it from unavailable online reputation checks. Include copy, failed-scan guidance, first-use mini-tutorial and complete Serbian Latin/Cyrillic/English translations. No QR history or dedicated QR-report action in current scope.
- Device Security: overall status, Scan, user-selected files/folders, scan history, exclusions, detailed findings, SHA-256 display/copy/compare, banking checklist, sourced App Lock help. SHA-256 is a fingerprint, not a malware verdict.
- Android cannot grant an ordinary app unrestricted access to all private files or other apps' sandboxes. Use permitted Android APIs/system file/folder pickers and explain limits. No false full-device antivirus claims. Quarantine/delete only after confirmation. No automatic scan at app launch or fixed recurring schedule; optional configurable reminder notifications only.
- App Lock lookup should offer live suggestions, common manufacturers, official sourced instructions, last-checked dates and user feedback.

### Map, Family and reporting
- A real interactive scam map is required (user says it is currently missing): current city if permission granted, manual city search, city following, grouped markers, scam-type colours, recent/historical labels and report lists from markers. Protect precise locations; location alerts opt-in.
- Reports can be submitted as guest. Guest reports are anonymous; signed-in reports are associated privately with account for abuse prevention. User can view status and success confirmation.
- Q193 public review is unanswered; recommended default is moderation review before public display.
- Family should work across devices and focus on security alerts, suspicious notifications and potentially unsafe downloads, not screen-time surveillance. Require approval/consent, disclose sharing, per-category controls and disconnect on either side. Android notification/download visibility is limited and sensitive; only implement supported permissioned features. No stealth monitoring, private-message capture or unrelated data collection.
- Q207 answered “no” to sharing transparency. This cannot mean hidden monitoring: transparency and user control are mandatory. Explain the conflict in product UI/spec.

### Rules, moderation, backend
- Build 200+ distinct numbered rules and about 30 subtle Easter eggs naturally mixed into the text. No separate joke chapter and do not label them as Easter eggs. Humour never overrides enforceable policy.
- Expandable categories, titles/numbers, examples, search, three languages. Rules readable by guests; acceptance required before posting/commenting, not at guest preview/first browse. Important changes require acceptance; simple clickable update notice, not a full public changelog. User does not want per-rule moderator-action explanation.
- Roles: User → Moderator → Admin → Owner. Owner is highest authority. Separate dashboards, reports overview, sort by all useful priority criteria, user lookup, logged admin attribution access for anonymous votes, audit log, appeals/status, restriction reasons/scopes/durations, permanent-ban warning and moderator request queue.
- Ten independent reports may raise priority but never prove wrongdoing or auto-ban. Backend-enforce all roles/actions. Owner can do anything within the app; critical actions still need logging and secure enforcement.
- User says no personal-data export in current scope, no standalone permission dashboard and no manual revoke-permission control. Briefly explain permissions; Android Settings manages revocation. Include public-search visibility, online status, block list, mute list and clear-local-data controls. Do not add per-user controls for who can follow/comment.
- User approves real backend for auth, posts, votes, comments, reports, moderation and multi-device sync; multiple concurrent devices, automated tests and physical-device testing for major releases. No offline action queue; do provide offline indicator and retry.
- Q283 is contradictory: user answered yes to remote source with local cache but also wrote “all data on phone”. Confirm architecture preference before final backend/storage decision. Cross-device sync requires remote source of truth.
- User is open to replacing Kotlin/WebView if it materially fixes persistent problems; do not rewrite without technical evidence and preserve working functionality.

### Reported current bugs/state
- Bottom navigation works; scope of swipe failure is unclear, so test all six pages and nested horizontal components.
- QR camera opens; image upload reportedly works.
- Language change currently updates only after restart; make translations update immediately.
- Tutorial, Rules page and functional sign-in are currently absent.
- Tapping sample username opens a compact overview; build out correct full individual profile and four tabs.
- User says transitions are absent; implement and test.
- No other broken buttons noticed.
- Q34/Q35 Back/scroll-restoration answers were partly garbled; use normal Back stack and test persistence. Q49 recurring scan setup was not explicitly answered; reminders only, no mandatory schedule. Q78 follower visibility unclear. Q298 swipe scope unclear. Q302 choose translation diagnosis after code inspection; Q303 confirms restart currently needed. Q207/Q283/Q76 conflicts remain unresolved as noted.

### Required next work
1. Audit current repository/branch/PR, source, tests and CI. Reproduce current bugs before editing.
2. Fix design foundation, immediate translations, page swipe/transitions, back-stack and responsive navigation.
3. Implement guest preview, authentication and interactive onboarding.
4. Complete unique sample profiles and detailed community/thread screens.
5. Complete QR/link UX and translations.
6. Implement Device Security within Android storage limits.
7. Build interactive map and transparent, consent-based Family features.
8. Implement rulebook and backend-enforced moderation.
9. Run automated checks, build APK and physically test on Android. Report device testing separately from compilation.
- Never claim an item works merely because a button/mock exists. Keep the two HTML files byte-identical. Never commit secrets. Work on a feature branch and PR; do not merge without explicit user approval.

### 2026-11-26 handoff filenames
- Full 308-question record: `SCAMSHIELD_QUESTIONNAIRE_AND_ANSWERS.md`.
- Merged old context + latest answers: `SCAMSHIELD_CLAUDE_MASTER_CONTEXT.md`.
- Copy-ready prompt: `SCAMSHIELD_CLAUDE_PROMPT.md`.


## 2026-11-26 — Claude Free-plan handoff and current workflow

- The user currently uses Claude Free and has separate Claude and GitHub accounts. Different email addresses/accounts are not inherently a problem; when authorising any GitHub integration, use the GitHub account that owns or has access to `ljubanicluka/ScamShieldApk`.
- Do not assume Claude Code / direct repository access is available on the user's current plan. The previous Claude attempt did not access the repository: its workspace was empty, its GitHub connection was not configured, and no files were changed or built.
- Until repository-connected coding is available, use the ZIP-and-upload workflow with Claude's regular chat: download the required branch ZIP, upload it to a Claude conversation, and include this project-instructions file plus the relevant context/questionnaire/prompt documents. Ask Claude to inspect first and report its findings before editing.
- When requesting edits through file uploads, require Claude to return the complete changed files (or a clearly structured patch) and identify every path changed. Do not accept claims that GitHub was updated unless a commit/PR link and commit SHA are provided.
- Before replacing repository files, compare against the latest branch to avoid overwriting newer work. Keep `web/scamshield-app.html` and `app/src/main/assets/index.html` byte-identical. Do not upload secrets, API keys, signing material or access tokens.
- Current handoff state: branch `community-feed-badges-2026-11-26`; PR #1 remains unmerged. Do not merge it without the user's explicit approval. No new application-code changes were made during the failed Claude repository-access attempt.
- Suggested immediate sequence: (1) download the branch ZIP; (2) upload the ZIP and this file to Claude; (3) ask for a read-only repository audit with findings and test plan; (4) review the audit before authorising code edits; (5) have edits returned as files/patches; (6) run checks and build through GitHub Actions or a local Android build; (7) review the diff and open/update a PR without merging.


## 2026-11-26 — Stage 1 (navigation foundation) — ZIP-based session, no GitHub access
- Work done on the uploaded ZIP of branch `community-feed-badges-2026-11-26`; no git history, PR or CI was reachable, so nothing was pushed. Apply the returned files to the branch and open/update PR #1 yourself. Do not merge without explicit approval.
- Root causes found (all in `web/scamshield-app.html`): (1) swipe listener was bound to `#app`, which is only as tall as its content, so short pages such as QR left most of the screen outside the swipe area; (2) `popstate` always jumped to Home, so Back/gesture-back never walked a stack; (3) native language was synced only when the camera was launched, so gallery/native QR messages could use a stale language; (4) tapping any non-button area inside the settings panel counted as a backdrop tap and closed it; (5) `go()` was defined twice; (6) QR title contained the awkward "(ne otvara ništa)" filler.
- Changes: document-level finger-following swipe with 150 ms settle/enter animation, guarded for inputs, settings panel and (optionally) horizontally scrolling elements; two Settings toggles (swipe on/off, nested-list guard); real history back stack (nav taps push, swipes replace, settings panel has its own entry); `syncNativeLang()` on every render; filler text removed; duplicate `go()` removed.
- Added `tests/ui/ui-regression.js` (headless Chrome, emulated touch). Rulebook, tutorial and sign-in are reported there as TODO, not as passing.
- NOT done / still open: sign-in, interactive tutorial, rulebook (200+), per-account profile overview/full profile with four tabs, interactive map, drag-and-drop page order (move up/down buttons still exist), QR landing redesign with in-page camera preview, Device Security, Family, backend. No APK was built (no Android SDK in the session) and nothing was tested on a physical device.


## 2026-11-27 — Stage 2 visual redesign (submitted for automated validation)

### UI changes
- Rebuilt the top bar with a shield/check brand mark, ScamShield wordmark, profile/settings access, language/script controls and theme control. Existing action identifiers and handlers are retained.
- Rebuilt the six-item bottom navigation with consistent vector line icons, clearer selected state and accessible current-page semantics. Existing navigation order and page IDs are retained.
- Added a consistent visual system: white/blue light mode, near-black/yellow/white dark mode, responsive card surfaces, typography hierarchy, inputs, button states, focus treatment and reduced-motion support.
- Reworked the Home screen into a security-focused dashboard with primary Check and QR actions, local-only overview counters and the existing shortcut grid.
- Reworked the visual hierarchy for Community, QR Scanner, Device Security, help after a scam, and Family. Existing content and actions are retained; the redesign does not create backend, authentication, antivirus, map or cross-device capabilities.
- Added a clear QR/link scanner introduction and graphic while retaining the current camera, gallery, link-analysis and user-confirmation flow.
- The synthetic Community preview disclosure and DEMO MODE label remain visible. No synthetic activity is represented as genuine reports or users.
- Added responsive layouts for narrow phones and wider screens and subtle press/focus transitions.

### Validation status
- Source UI was updated first, then mirrored into the packaged Android HTML asset. CI will check that the files remain byte-identical.
- Playwright tests now capture all six main screens in light/dark themes and Serbian Latin, Serbian Cyrillic and English (36 screenshots at a 390 x 780 viewport), and assert the new app shell and per-page layout.
- Automated tests, the Android debug build and screenshot review are pending for this change. CI output does not replace physical-device testing.
- Stage 2 is not marked complete until automated checks pass and screenshots have been reviewed.

- Follow-up validation correction: repository has a later active `R()` renderer that overrides the earlier declaration. The page-introduction/home-hero logic was moved into the active renderer; the next CI run verifies the actual rendered six-page layout.

- Follow-up visual review: removed redundant duplicate headings/descriptions from Home and QR Scanner; kept the QR scan action card and original camera/gallery actions. Updated the swipe regression case to avoid starting inside the Family screen's protected text input; gestures inside controls remain intentionally blocked.


## 2026-11-27 — Stage 2 visual redesign validation result

- Status: **automated validation passed** on UI/build revision `6ded7b0be2c7f68f32cbc0a4c82cc2bac6ca59cb`.
- GitHub Actions run: `46024865835` — successful.
- The run passed Playwright UI regression, all web/Kotlin and functional source scripts, screenshot capture, Android unit tests, debug APK assembly, APK existence validation and artifact upload.
- The screenshot matrix contains 36 renders: all six main pages, light and dark themes, and Serbian Latin, Serbian Cyrillic and English.
- The packaged APK was inspected: it is a valid Android package, and its `assets/index.html` exactly matches the redesigned source HTML.
- The source handoff ZIP passed ZIP integrity verification; its editable web UI and Android WebView copies are byte-identical.
- Stage 2 visual redesign is complete from the source/automated-build perspective. A physical Android installation and interaction pass is still required before calling the UI device-verified.
- PR #1 remains open and unmerged. Continue working on the feature branch; do not merge without the user's explicit approval.
