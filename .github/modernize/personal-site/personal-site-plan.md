# Personal Website and Financial Data Lab Plan

## Purpose

Evolve this repository into the personal website published at
`https://chiragjraval.github.io/`, with Financial Data Lab as one section, while
preserving the existing stock-screener pipeline and daily data publishing.
Prioritize a coherent, high-density compact UI across pages before adding more
content or splitting repositories.

This plan is the resumable source of truth for scope, decisions, execution,
validation, and manual GitHub actions. Update its phase status and progress log
after each work session. Do not rename the repository or publish a route change
until the rollout gates below are satisfied.

## Decisions and constraints

- **Repository strategy:** Rename this repository to `chiragjraval.github.io`.
  Keep the website, screener, data, and pipeline together for now; revisit
  splitting them only if it becomes a priority.
- **UX priority:** Establish a shared design system and navigation shell for the
  personal site and Financial Data Lab. Favor a compact, information-dense
  layout inspired by modern GenAI tools.
- **Daily work:** Preserve the existing scheduled data-only refresh. A daily
  data update must not trigger a full frontend rebuild unless the workflow
  design proves that unavoidable.
- **Preserve behavior:** Keep the Java/Maven pipeline, JSON contract, test-data
  workflow, existing screener features, and explicit production deployment
  safeguards intact.
- **Content scope:** Do not invent personal-site sections or copy. Initially
  support the known destinations (Home, Financial Data Lab, Stock Screeners);
  confirm any additional content and destinations with the user before adding
  them.
- **No premature rename:** Repository rename, Pages configuration changes, and
  first publication to the new root are manual rollout actions after local
  verification and explicit user approval.
- **Worktree:** The user-provided untracked
  `.github/modernize/repo-restructure/repo-restructure-prompt.md` is unrelated
  input and must remain untouched.

## Target routes

| Page/data | Target URL path |
|---|---|
| Personal website home | `/` |
| Financial Data Lab landing page | `/financial-data-lab/` |
| Stock Screeners | `/financial-data-lab/stock-screeners/` |
| Screener production JSON | `/data/stock-screener/prod/` |

Preserve the current project-site URL
`https://chiragjraval.github.io/daily_custom_stock_screener/` as a migration
consideration. Decide and verify a redirect or transition page before cutover;
do not assume repository rename redirects the old Pages URL.

## Current baseline

- The current Pages URL is the project site
  `https://chiragjraval.github.io/daily_custom_stock_screener/`.
- `public/index.html` and `public/portal.js` provide a small static portal.
  `shared/config/site.json` lists Stock Screeners at `stock-screeners/`.
- The React 18/Vite app is under `apps/stock-screener/`; its build base and
  production data URL currently include `/daily_custom_stock_screener/`.
- Shared visual tokens are in `shared/theme/tokens.css`; the portal currently
  uses a spacious card layout. A shared, persistent site navigation does not
  yet exist.
- Java 25/Maven pipeline sources live under `pipelines/stock-screener/` and
  production JSON is under `data/stock-screener/prod/`.
- `.github/workflows/stock-screener.yml` builds the frontend for site changes
  and separately builds/publishes production data. The scheduled job does not
  build the frontend. `.github/workflows/deploy.yml` overlays selected
  artifacts onto `gh-pages`; its current cleanup scope must be reviewed before
  it owns a broader personal-site root.
- The production gate is the repository Actions variable
  `PAGES_DEPLOYMENT_ENABLED`; manual production publishing uses the explicit
  `publish=true` input.

Relevant existing references:

- [Repository restructure plan](../repo-restructure/repo-restructure-plan.md)
- [Architecture](../../../docs/architecture.md)
- [Deployment](../../../docs/deployment.md)
- [Adding apps and data](../../../docs/adding-apps-and-data.md)

## UX and design acceptance criteria

1. A consistent global shell frames Home, Financial Data Lab, and Stock
   Screeners; navigation clearly identifies the active destination.
2. Desktop uses a persistent left navigation and a readable main-content pane.
   The navigation can collapse or resize if this improves usable content width.
3. Small screens use an accessible collapsible/drawer navigation; the main
   content remains usable without horizontal page overflow.
4. Compact density is deliberate and consistent: restrained page padding,
   compact navigation rows, clear type hierarchy, and reusable spacing and
   surface tokens. Do not reduce chart, table, or control readability.
5. Keyboard navigation, visible focus, semantic landmarks, active-page
   indication, and appropriate accessible labels are included.
6. Existing screener search/filtering, company details, charts, and data loading
   continue to work within the new shell.
7. Portal and screener use the same brand, typography, colors, navigation
   treatment, and core interactive states. App-specific controls remain
   app-specific.
8. Avoid a new frontend framework or dependency unless a documented prototype
   shows it is needed for the shared shell.

## Execution phases

### Phase 0 — Baseline and safeguards

**Status:** Complete — GitHub settings remain a cutover prerequisite

- [x] Confirm current branch/worktree and preserve unrelated user changes.
- [x] Record the current production URL, representative screener interactions,
  summary endpoint, company count, and workflow status.
- [x] Inventory hard-coded project-site path prefixes in frontend, workflows,
  docs, and deployment scripts.
- [x] Record Pages source/branch settings and whether a repository named
  `chiragjraval.github.io` already exists or is otherwise unavailable.
- [x] Capture current data-only schedule and deployment gate behavior.

**Exit gate:** Existing site and data behavior are recorded; no production
scrape, route cutover, or repository rename has occurred.

**Baseline evidence — 2026-11-22**

- Local branch is `main` at `476656cfa`. The only pre-existing worktree item
  was the untracked user-provided restructure prompt; it was not modified.
- The project-site portal and `/stock-screeners/` route loaded. The production
  summary endpoint returned 481 companies with `generatedAt`
  `2026-11-22T12:27:23.896513822`. One initial screener snapshot showed zero;
  a subsequent load showed all 481. The cause was not isolated. Keep this as a
  loading-state observation to recheck during the UX work, not as evidence that
  the endpoint is currently missing.
- Latest known Stock Screener workflow run
  [45019853339](https://github.com/chiragjraval/daily_custom_stock_screener/actions/runs/45019853339)
  completed successfully. The scheduled job is configured for 18:00 UTC and
  remains gated by `PAGES_DEPLOYMENT_ENABLED`; manual production publishing
  still requires `publish=true`.
- Project-prefix references occur in `apps/stock-screener/vite.config.js`,
  `.github/workflows/stock-screener.yml`, `docs/deployment.md`, and
  `docs/repo-restructure-contracts.md`. Maven artifact IDs also include the
  former repository name, but are not URL prefixes and should only change if
  needed for a separate build reason.
- GitHub repository search found no public repository named
  `chiragjraval.github.io`; an unauthenticated URL check returned 404. This
  cannot rule out a private repository or another account-level constraint.
  The connected tools cannot read the current Pages source/branch settings.
  Verify target-name availability and Pages settings manually before Phase 6.
- No production scrape, workflow dispatch, or live deployment was run.

### Phase 1 — Navigation, information architecture, and design contract

**Status:** Complete — first-release scope and implementation boundary agreed

- [x] Confirm the first-release navigation destinations and their labels.
  Baseline proposal: Home, Financial Data Lab, Stock Screeners.
- [x] Confirm what content belongs on the personal home page; leave
  unconfirmed sections out rather than filling them with invented content.
- [x] Define desktop and mobile navigation behavior, density, typography,
  spacing, color, focus, active, hover, and collapsed states.
- [x] Decide whether the global shell is implemented as shared framework-neutral
  assets/components or whether the portal and screener should share a build
  framework. Evaluate reuse and deployment complexity before choosing.
- [x] Record the chosen shell boundary and route model in this plan before
  implementation.

**Exit gate:** User-approved first-release page inventory and responsive UX
contract; no new dependencies without a demonstrated need.

**Decision record — 2026-11-22**

- **Navigation:** Home (`/`), Financial Data Lab (`/financial-data-lab/`), and
  Stock Screeners (`/financial-data-lab/stock-screeners/`) only for the first
  release.
- **Home content:** A concise personal introduction and links to Financial Data
  Lab. The user supplied
  [LinkedIn](https://www.linkedin.com/in/chiragjraval/) as a source for personal
  information and will provide or approve final copy later. Until then, use
  only a short provisional introduction, the user's name, and a LinkedIn link;
  do not infer or publish detailed employment or education history.
- **Global shell:** Keep the existing static portal and React/Vite screener
  build boundaries. Implement one lightweight, framework-neutral Web Component
  for shared navigation and page framing, with route metadata in one shared
  config module. This avoids framework migration and a duplicated nav
  implementation; no dependency is needed.
- **Desktop density:** Persistent left sidebar, initially about 15rem wide,
  compact navigation rows, restrained content padding, and readable page
  hierarchy. Keep a collapse option if needed to preserve table/chart width.
- **Mobile:** Use a keyboard-operable menu button that expands/collapses the
  navigation drawer; expose its state and controlled region accessibly and
  close the drawer after selecting a route.
- **Visual language:** Reuse the current light brand palette and shared CSS
  tokens; introduce compact spacing and component states. Keep screener tables,
  filters, charts, and touch targets legible.
- **Existing transient zero-count snapshot:** Add explicit loading and error
  states to the screener so an empty array is not presented as a successful
  zero-company result while the request is pending or fails.

### Phase 2 — Build the shared compact site shell

**Status:** Complete — local desktop and mobile behavior verified

- [x] Implement the shared navigation and content layout using repository
  conventions and the approved Phase 1 design contract.
- [x] Extend/refine `shared/theme/` tokens for compact spacing, typography,
  surfaces, borders, and interaction states.
- [x] Ensure the shell can render both static portal content and the React
  screener without duplicated or conflicting navigation.
- [x] Verify keyboard operation, semantic landmarks, focus visibility, and
  mobile menu behavior.
- [x] Add focused verification for navigation behavior/accessibility. There is
  no existing frontend test harness; validate the custom-element shell through
  browser checks instead.

**Exit gate:** Both a representative portal page and screener can use the same
shell locally at desktop and mobile sizes.

**Implementation evidence**

- Added the no-dependency `fdl-site-shell` Web Component and shared route
  config; static pages and the React screener now use the same shell.
- Reworked shared tokens for compact light surfaces, restrained content
  spacing, and consistent cards. The mobile drawer exposes `aria-expanded`,
  supports Escape, and returns focus to its menu button.
- Browser checks confirmed the three navigation entries, active route,
  visible shell, working mobile drawer, Escape close/focus return, and no
  horizontal page overflow at a 390px viewport.

### Phase 3 — Integrate pages and migrate routes

**Status:** Complete — target pages and local route transitions verified

- [x] Turn `/` into the personal home page using only confirmed content.
- [x] Move Financial Data Lab landing content to `/financial-data-lab/`.
- [x] Move Stock Screeners to
  `/financial-data-lab/stock-screeners/` and include it in global navigation.
- [x] Update Vite base path, app links, registry paths, asset references, and
  production JSON URL to match the user-site root.
- [x] Verify local development/test-data URLs still work independently of
  production paths.
- [x] Decide how the previous project-site URL is kept usable or redirected;
  document any unavoidable limitation.

**Exit gate:** The three target routes render locally with shared navigation;
the screener loads the test dataset, and no existing screener interaction has
regressed.

**Implementation evidence**

- Added a provisional personal home, Financial Data Lab landing page, and the
  first-release route hierarchy. Detailed personal-profile copy remains for the
  user to provide or approve.
- Moved the app's build base and production data URL to user-site-root paths.
  Added static transitions from the former project-site and screener routes;
  former direct data URLs are not duplicated.
- Added explicit stock-data loading/error states so a pending or failed request
  cannot look like a successful zero-company result.
- Local browser checks loaded all three pages, followed the legacy transitions,
  loaded 25 test companies, filtered/reset, and opened company details/charts.

### Phase 4 — Adapt GitHub Actions and Pages overlay

**Status:** Complete locally — no live publish or Actions dispatch

- [x] Update path-aware site builds and production URL/base-path settings for a
  user site served at `/`.
- [x] Keep daily scheduled scraping data-only and retain the current deployment
  gate and manual approval input.
- [x] Review and tighten the `gh-pages` overlay so it replaces only
  repository-owned paths and does not erase future personal-site content.
- [x] Define the artifact layout for the personal home, Financial Data Lab,
  screener, shared assets, and data.
- [x] Add deployment preflight checks for required route entry points and the
  production summary JSON shape before any Pages mutation.
- [x] Validate path filters, artifacts, and overlay behavior locally; do not
  publish as part of this phase.

**Exit gate:** Workflow simulations show site and data-only updates preserve
unrelated Pages content; no production scrape or publishing has occurred.

**Implementation evidence**

- The site artifact now includes the personal pages, shared shell/config/theme,
  and nested screener build. Production builds use
  `/financial-data-lab/stock-screeners/` and
  `/data/stock-screener/prod/`.
- Added `publish_site=true` for a manual site-only publish; `publish=true`
  retains explicit production-scrape approval. Scheduled data refreshes remain
  data-only and gated by `PAGES_DEPLOYMENT_ENABLED`.
- The deploy overlay checks required files, replaces only owned routes/shared
  files, and retains the production dataset during site-only publication.
- A local overlay simulation preserved `.nojekyll`, an unrelated app, nested
  Financial Data Lab content, and existing production JSON. No GitHub workflow
  was dispatched; workflow syntax was manually inspected because no local
  workflow YAML parser is installed.

### Phase 5 — Pre-cutover validation and documentation

**Status:** In progress — local validation complete; cutover approval remains

- [x] Run Java tests and safe test-mode scrape.
- [x] Build the frontend and run available frontend tests/type checks.
- [x] Test Home, Financial Data Lab, screener data loading, filtering, details,
  charts, navigation, keyboard interactions, and mobile layout.
- [x] Validate all generated links/assets/data URLs against the target paths.
- [x] Update README, architecture, deployment, and app onboarding docs for the
  user-site layout and its data-only schedule.
- [x] Prepare a rollback/transition checklist and record exact manual GitHub
  actions for approval.

**Exit gate:** Local validation passes and the user explicitly approves the
rename and first publication.

**Validation evidence**

- `mvn -B test` passed all four Java tests and completed its safe test-mode
  pipeline run.
- `npm run build` succeeded with the production route and JSON URL embedded.
  The existing Vite large-bundle warning remains; no frontend test/lint
  scripts are defined.
- Local browser checks confirmed the Home, Financial Data Lab, and Stock
  Screeners routes; legacy route redirects; 25 test companies; filter/reset;
  detail charts; explicit loading and HTTP 503 error states; mobile navigation
  keyboard behavior; and no horizontal overflow at 390px.
- Production build inspection confirmed the nested app base and
  `/data/stock-screener/prod/` URL. Production data and the live site were not
  changed.

### Phase 6 — Manual repository rename and live rollout

**Status:** Not started — requires user action/approval

- [ ] User renames the GitHub repository to `chiragjraval.github.io` in
  **Settings → General → Repository name**. Verify the name is available first.
- [ ] Before the first publish, create a backup branch from the current
  `gh-pages` branch (for example, `gh-pages-before-personal-site`) and confirm
  the value of `PAGES_DEPLOYMENT_ENABLED`.
- [ ] Confirm GitHub Pages publishes the expected branch/root and Actions
  permissions and branch protection allow the publisher.
- [ ] Update local `origin` remote if needed after the rename.
- [ ] Predict whether pushing the prepared changes will publish automatically:
  with `PAGES_DEPLOYMENT_ENABLED=true`, a site-path push publishes the site;
  otherwise, manually dispatch with `publish_site=true`.
- [ ] Publish the approved site artifact to the user-site root. Do not run a
  production scrape unless separately approved; use the existing production
  data and verify it is present at `/data/stock-screener/prod/`.
- [ ] Verify the root, Financial Data Lab, screener, data endpoint, refresh
  workflow, and old project-site transition behavior in a browser.
- [ ] Enable or retain `PAGES_DEPLOYMENT_ENABLED` according to the user's
  preference; the rename itself must not silently enable scheduled publishing.

**Exit gate:** Live target URLs and production data work; rollback instructions
and final workflow status are recorded.

### Phase 7 — Handoff and future separation decision

**Status:** Not started

- [ ] Record delivered routes, shell implementation, validation evidence,
  deployment run, and remaining manual actions.
- [ ] Update this plan's progress log and final handoff.
- [ ] Revisit repository separation only if a concrete operational or ownership
  need arises; it is not part of this release.

**Exit gate:** The user can resume or safely extend the site without repeating
completed investigation.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Repository rename changes project-site and asset URLs | Keep rename behind Phase 5 approval; validate target base paths and make an old-URL transition plan. |
| Deployment overlay deletes unrelated content at the user-site root | Define owned paths explicitly; test overlay against a fixture containing unrelated files. |
| Shared design drifts between static portal and React app | Use one shared shell/design source and validate both entry points in the same checks. |
| Compact design harms chart/table readability or accessibility | Treat density as spacing/navigation, retain readable data surfaces, and test keyboard/mobile behavior. |
| Unspecified personal content expands scope | Include only approved destinations and content; add sections in a later scoped change. |
| Daily refresh accidentally rebuilds the frontend or bypasses approval | Preserve and test data-only scheduled job, `PAGES_DEPLOYMENT_ENABLED`, and manual `publish` gate. |
| Old project Pages URL stops resolving as expected | Test the old URL after rename; provide a transition route where feasible and record limitations before cutover. |

## Manual actions and checkpoints

- The assistant can edit this repository, update workflows, run local checks,
  and inspect available GitHub Actions workflow/run information.
- Repository rename and GitHub Pages settings changes must be performed by the
  user in GitHub Settings; the connected GitHub tools do not expose a rename
  operation.
- The user must approve any production scrape or first live cutover. Do not
  change `PAGES_DEPLOYMENT_ENABLED` without explicit instruction.
- No local command is required from the user unless a later validation depends
  on a GitHub setting or workflow run that cannot be accessed from this
  workspace.
- **Rollback:** Preserve the existing `gh-pages` state on a backup branch before
  publication. If the new routes fail, restore that snapshot to `gh-pages`
  without running a production scrape. Renaming back to the old repository
  name is a secondary manual recovery option only if that name is still
  available and GitHub Pages settings are restored.

## Progress log

| Date | Phase/item | Status | Change/evidence | Validation | Manual action/blocker |
|---|---|---|---|---|---|
| 2026-11-22 | Initial plan | Complete | Recorded single-repository strategy, target routes, shared compact UX priority, phases, gates, and existing workflow constraints. | Compared plan with current README, architecture/deployment docs, Vite paths, portal registry, and workflow definitions. | No action yet. |
| 2026-11-22 | Phase 0 — Baseline and safeguards | Complete | Verified live portal, screener and 481-company summary; inventoried URL prefixes and workflow behavior. Recorded target-repository and Pages-settings access limitations. | Browser showed 481 on a subsequent load; latest known run `45019853339` succeeded. | Before cutover, user must confirm target repository availability and Pages source/settings. |
| 2026-11-22 | Phase 1 — UX and information architecture | Complete | User confirmed three navigation destinations, provisional home content, and LinkedIn as the source for later-approved personal copy. Chose a no-dependency shared Web Component. | Recorded responsive compact design contract and target routes. | Final personal copy can be supplied later. |
| 2026-11-22 | Phases 2–3 — Shell and routes | Complete locally | Added shared shell, personal home and Financial Data Lab pages, nested screener route, old-route transitions, and explicit data loading/error states. | Browser verified routes, 25-company test data, filtering, details/charts, mobile menu/Escape/focus return, and no 390px horizontal overflow. | Source changes are local and not pushed; cutover remains gated. |
| 2026-11-22 | Phase 4 — Workflows and overlay | Complete locally | Added root-route artifacts, scoped Pages overlay, deployment preflights, and manual `publish_site` dispatch so site-only rollout does not scrape data. | Production build paths checked; overlay simulation preserved `.nojekyll`, other apps, nested content, and production JSON. No GitHub workflow was run. | Confirm deployment variable/settings before cutover. |
| 2026-11-22 | Phase 5 — Local validation and docs | Complete locally | Updated README and architecture/deployment/onboarding contracts; added rollback steps. | `mvn -B test`: four tests passed and safe test scrape completed; `npm run build` succeeded; browser and overlay checks passed. | User approval and GitHub settings verification required before rename/publication. |

## Current handoff

- **Overall status:** Phases 0–5 are complete locally. The shared compact shell,
  target routes, workflow changes, and documentation are implemented but not
  pushed.
- **Next step:** Review the local changes, then proceed to Phase 6 only after
  the user confirms repository-name availability, Pages settings, deployment
  variable state, and first-live-publish approval.
- **Repository rename:** Deferred until local changes and deployment artifacts
  are validated and the user approves the cutover.
- **Phase 0 cutover checks:** Target repository availability and Pages source/
  branch settings could not be confirmed through connected tools; verify them
  manually before Phase 6.
- **Production publishing:** No production scrape, workflow dispatch, or live
  deployment was performed during this execution session. A transient zero
  count during initial live navigation was followed by the expected 481 rows;
  the local UI now shows a loading state instead of a premature zero count.
- **Worktree:** The user-provided untracked restructure prompt remains
  untouched.
