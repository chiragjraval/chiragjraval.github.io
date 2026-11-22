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

**Status:** In progress — first-release content inventory needs user input

- [ ] Confirm the first-release navigation destinations and their labels.
  Baseline proposal: Home, Financial Data Lab, Stock Screeners.
- [ ] Confirm what content belongs on the personal home page; leave
  unconfirmed sections out rather than filling them with invented content.
- [ ] Define desktop and mobile navigation behavior, density, typography,
  spacing, color, focus, active, hover, and collapsed states.
- [ ] Decide whether the global shell is implemented as shared framework-neutral
  assets/components or whether the portal and screener should share a build
  framework. Evaluate reuse and deployment complexity before choosing.
- [ ] Record the chosen shell boundary and route model in this plan before
  implementation.

**Exit gate:** User-approved first-release page inventory and responsive UX
contract; no new dependencies without a demonstrated need.

### Phase 2 — Build the shared compact site shell

**Status:** Not started

- [ ] Implement the shared navigation and content layout using repository
  conventions and the approved Phase 1 design contract.
- [ ] Extend/refine `shared/theme/` tokens for compact spacing, typography,
  surfaces, borders, and interaction states.
- [ ] Ensure the shell can render both static portal content and the React
  screener without duplicated or conflicting navigation.
- [ ] Verify keyboard operation, semantic landmarks, focus visibility, and
  mobile menu behavior.
- [ ] Add focused tests for navigation behavior or accessibility requirements
  where the chosen implementation supports them.

**Exit gate:** Both a representative portal page and screener can use the same
shell locally at desktop and mobile sizes.

### Phase 3 — Integrate pages and migrate routes

**Status:** Not started

- [ ] Turn `/` into the personal home page using only confirmed content.
- [ ] Move Financial Data Lab landing content to `/financial-data-lab/`.
- [ ] Move Stock Screeners to
  `/financial-data-lab/stock-screeners/` and include it in global navigation.
- [ ] Update Vite base path, app links, registry paths, asset references, and
  production JSON URL to match the user-site root.
- [ ] Verify local development/test-data URLs still work independently of
  production paths.
- [ ] Decide how the previous project-site URL is kept usable or redirected;
  document any unavoidable limitation.

**Exit gate:** The three target routes render locally with shared navigation;
the screener loads the test dataset, and no existing screener interaction has
regressed.

### Phase 4 — Adapt GitHub Actions and Pages overlay

**Status:** Not started

- [ ] Update path-aware site builds and production URL/base-path settings for a
  user site served at `/`.
- [ ] Keep daily scheduled scraping data-only and retain the current deployment
  gate and manual approval input.
- [ ] Review and tighten the `gh-pages` overlay so it replaces only
  repository-owned paths and does not erase future personal-site content.
- [ ] Define the artifact layout for the personal home, Financial Data Lab,
  screener, shared assets, and data.
- [ ] Add deployment preflight checks for required route entry points and the
  production summary JSON shape before any Pages mutation.
- [ ] Validate path filters, artifacts, and overlay behavior locally; do not
  publish as part of this phase.

**Exit gate:** Workflow simulations show site and data-only updates preserve
unrelated Pages content; no production scrape or publishing has occurred.

### Phase 5 — Pre-cutover validation and documentation

**Status:** Not started

- [ ] Run Java tests and safe test-mode scrape.
- [ ] Build the frontend and run available frontend tests/type checks.
- [ ] Test Home, Financial Data Lab, screener data loading, filtering, details,
  charts, navigation, keyboard interactions, and mobile layout.
- [ ] Validate all generated links/assets/data URLs against the target paths.
- [ ] Update README, architecture, deployment, and app onboarding docs for the
  user-site layout and its data-only schedule.
- [ ] Prepare a rollback/transition checklist and record exact manual GitHub
  actions for approval.

**Exit gate:** Local validation passes and the user explicitly approves the
rename and first publication.

### Phase 6 — Manual repository rename and live rollout

**Status:** Not started — requires user action/approval

- [ ] User renames the GitHub repository to `chiragjraval.github.io` in
  **Settings → General → Repository name**. Verify the name is available first.
- [ ] Confirm GitHub Pages publishes the expected branch/root and Actions
  permissions and branch protection allow the publisher.
- [ ] Update local `origin` remote if needed after the rename.
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

## Progress log

| Date | Phase/item | Status | Change/evidence | Validation | Manual action/blocker |
|---|---|---|---|---|---|
| 2026-11-22 | Initial plan | Complete | Recorded single-repository strategy, target routes, shared compact UX priority, phases, gates, and existing workflow constraints. | Compared plan with current README, architecture/deployment docs, Vite paths, portal registry, and workflow definitions. | No action yet. |
| 2026-11-22 | Phase 0 — Baseline and safeguards | Complete | Verified live portal, screener and 481-company summary; inventoried URL prefixes and workflow behavior. Recorded target-repository and Pages-settings access limitations. | Browser showed 481 on a subsequent load; latest known run `45019853339` succeeded. | Before cutover, user must confirm target repository availability and Pages source/settings. |

## Current handoff

- **Overall status:** Execution started; Phase 0 complete and Phase 1 in
  progress. No application code has been changed.
- **Next step:** Complete the initial page inventory and design contract in
  Phase 1. Confirm what the personal home page should contain and whether any
  additional destinations belong in the first release.
- **Repository rename:** Deferred until local changes and deployment artifacts
  are validated and the user approves the cutover.
- **Phase 0 cutover checks:** Target repository availability and Pages source/
  branch settings could not be confirmed through connected tools; verify them
  manually before Phase 6.
- **Production publishing:** No scrape, workflow dispatch, or live deployment
  was performed during this execution session.
