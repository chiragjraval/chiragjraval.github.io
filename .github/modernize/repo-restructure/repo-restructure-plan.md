# Financial Data Lab Repository Restructure Plan

## Purpose and source of truth

This plan turns the proposal in [repo-restructure-prompt.md](./repo-restructure-prompt.md) into ordered, verifiable implementation steps. The existing stock screener must remain usable throughout the migration.

Current technical baseline:

- Java 25 and Maven scraper; scraper execution and frontend build are both coupled to Maven phases in the root `pom.xml`.
- React/Vite frontend is under `src/main/resources/app`; it has a fixed Vite base path and reads `prod` JSON paths directly.
- Scraper output is under `src/main/resources/screener-data/{prod,test}`.
- The scheduled workflow runs `mvn package`, combines the frontend build and data, then publishes to `gh-pages`.
- Production mode enables Git operations in the Java scraper as well as the workflow's Pages deployment.

## Proposed target layout

```text
apps/
  stock-screener/             # Existing React/Vite application
data/
  stock-screener/
    prod/
    test/
pipelines/
  stock-screener/             # Java scraper, Maven build, samples and README
public/
  index.html                  # Static application directory/landing page
shared/
  ui/
  components/
  charts/
  utils/
  theme/                      # Shared colors, typography, spacing, branding
docs/
  architecture.md
  deployment.md
  adding-apps-and-data.md
.github/
  workflows/
    stock-screener.yml
    deploy.yml                # Reusable, serialized gh-pages publisher
```

Keep the root `pom.xml` as a Maven aggregator if practical, with the scraper as a module. The frontend remains independently buildable with npm. Generated data, application assets, and pipeline sources must have separate ownership and output paths.

## Operating rules

- Progress starts at **Not started**. Update the status, evidence, validation results, and any manual action after each phase.
- Work in small, reviewable changes. Do not commit or push source changes unless requested.
- Do not run a production scrape or publish to `gh-pages` until the local parity checks pass and the user approves the deployment checkpoint.
- Use the test environment for local scraper verification. Preserve generated JSON shape and current stock-screener behavior.
- Make the existing project root a portal and move the screener to `/stock-screeners/`. The root URL will no longer open the screener directly; the portal must provide a prominent link. This route change was confirmed by the user on 2026-11-22.
- Do not add a future app's code or data merely to demonstrate the architecture.

## Execution checklist

### Phase 0 — Baseline and safeguards

**Status:** Complete — 2026-11-22

- [x] Inspect current branch/worktree and retain unrelated user changes.
- [x] Record the current Pages URL and verify the site and representative company-detail view.
- [x] Capture the summary/detail JSON contract and a test-environment scrape result.
- [x] Record current scheduled workflow timing, workflow permissions, and `gh-pages` deployment layout.
- [x] Record the route strategy: make the existing project root a portal and move the screener to `/stock-screeners/`; user confirmed this on 2026-11-22.

**Baseline evidence**

- Local branch is `main`; remote `origin` has `main` and `gh-pages`. The `gh-pages` branch currently has `.nojekyll`, `assets/`, `index.html`, and `screener-data/` at its root.
- Live URL: `https://chiragjraval.github.io/daily_custom_stock_screener/`. It loaded 481 stocks, and expanding a company row loaded its score details. The live summary endpoint also reports 481 companies.
- The user confirmed that this root will become the portal and the screener will move to `https://chiragjraval.github.io/daily_custom_stock_screener/stock-screeners/`; preserving the old root as a direct screener route is not required.
- A safe `mvn -Denvironment=test test` completed successfully: 25 companies were written to the test summary. Summary keys are `totalCompanies`, `generatedAt`, and `companies`; a company detail includes `metadata`, `attributes`, `companyScore`, `quarterlyResults`, `technicalHistory`, `latestQuarterlyResult`, and `latestTechnicalAttributes`.
- The test detail directory contained 52 JSON files after the run, 27 of which were not referenced by its 25-company summary. The pre-run count was not captured; these extras were preserved pending a cleanup/retention decision.
- The production summary in the local checkout had 481 companies and was not modified. The test invocation did not push or publish.
- Workflow timing is `18:00 UTC` daily (`23:30 IST`) plus manual dispatch; workflow-level permission is `contents: write`. Published assets and data are combined at the `gh-pages` branch root.
- Expanding a live company row produced Recharts console warnings about chart width/height `-1`. Record as pre-existing behavior for later frontend verification.
- The public Pages API did not expose settings without authentication. Confirm Pages source and branch protection manually before the first deployment.

**Exit gate:** Baseline behavior and deployment paths are recorded; no production writes have occurred.

### Phase 1 — Establish safe build and data contracts

**Status:** Complete — 2026-11-22

- [x] Add/adjust focused Java tests or a deterministic comparison fixture for JSON output and essential scraper behavior.
- [x] Define pipeline inputs, environment selection, generated output path, and exit/failure behavior.
- [x] Ensure test runs write only under the test output path (currently `src/main/resources/screener-data/test`; Phase 2 will move it to `data/stock-screener/test`) and cannot commit or push.
- [x] Define a deployment artifact contract with distinct paths for the portal, app assets, and each dataset.
- [x] Keep production output and GitHub credentials out of local default commands.

**Exit gate:** Test-mode build and scrape succeed without Git publication; output failures surface as errors; output contract is documented in [repo-restructure-contracts.md](../../../docs/repo-restructure-contracts.md).

### Phase 2 — Move the stock-screener pipeline and data

**Status:** Complete — 2026-11-22

- [x] Move Java scraper sources, tests, and sample inputs to `pipelines/stock-screener`.
- [x] Move the Maven module/build configuration; remove frontend npm lifecycle work from that module.
- [x] Retain a root Maven aggregator and preserve root `mvn` commands.
- [x] Move production data and local test output to `data/stock-screener`.
- [x] Update scraper output paths and workflow references without changing the existing JSON schema.
- [x] Remove JGit commit/push behavior from the scraper; publication belongs to GitHub Actions.

**Exit gate:** The scraper runs in test mode from its new location and produces equivalent summary/detail JSON in the new data directory.

**Implementation evidence**

- Java sources, tests, and sample inputs now live under `pipelines/stock-screener`; the root `pom.xml` aggregates this Maven module.
- Production data moved to `data/stock-screener/prod`; test output is at `data/stock-screener/test` and remains ignored.
- The Java scraper no longer has Git/JGit publishing code. The workflow explicitly selects `prod`, builds the frontend independently with Node, and copies only production data to the existing `gh-pages/screener-data/prod/` path until Phase 3 updates the frontend data URL.
- Removed `System.exit` from the in-process Maven entry point so Maven can complete packaging; scraper errors now fail the Maven run.
- Current data inventory is 481 companies in the production summary and 1,258 detail files; 25 companies in the test summary and 52 detail files. All summary companies have corresponding detail files; 777 production and 27 test details are unreferenced and were preserved pending a cleanup/retention decision.
- `mvn -B package` passed all four unit tests, completed a 25-company test scrape, created the scraper JAR, and reported `BUILD SUCCESS`. `npm run build` also succeeded; Vite reported the existing large-chunk warning.

### Phase 3 — Move and decouple the frontend

**Status:** Complete — 2026-11-22

- [x] Move the Vite application to `apps/stock-screener`.
- [x] Make the Vite base URL/deployment prefix configurable for the project root portal and `/stock-screeners/` app path.
- [x] Replace hard-coded `prod` data paths with a single configurable data base path.
- [x] Keep current summary, filtering, sorting, expanded company details, and charts working at `/stock-screeners/`.
- [x] Add `public/index.html` as the root landing page linking to the stock screener.
- [x] Add central theme tokens for colors, typography, spacing, and app branding.
- [x] Extract only components needed by more than one app; leave screener-specific UI in its app.

**Implementation evidence**

- Moved the tracked React/Vite app to `apps/stock-screener`, keeping its local dependencies and generated assets in ignored paths.
- Made the Vite base path and data base URL configurable. Development uses the test JSON after `npm run predev` copies it into an ignored local public directory; the scheduled workflow sets the production Pages paths explicitly.
- Disabled Vite's public-directory copy during production builds so local test data cannot leak into published app assets.
- Added the root portal and registry at `public/` and `shared/config/site.json`, with shared style tokens in `shared/theme/tokens.css`.
- Updated the scheduled workflow to assemble the portal, screener route, and production dataset at their target Pages paths. It has not been run against production or deployed.
- Updated the artifact contract and README to describe the new layout and local commands.

**Exit gate:** Passed. The frontend builds with the configured project-site paths; local browser checks loaded the portal registry and 25 test stocks, filtered to one company, and expanded company details/charts. Test data was absent from the production build.

- Validation: `mvn -B package` passed all four Java tests and completed its test-mode scrape. `npm run predev` and `npm run build` succeeded; the production-configured build contains the `/stock-screeners/` asset base and `/data/stock-screener/prod/` data URL.
- Existing/non-blocking warnings: Vite's >500 kB bundle warning and Recharts chart-dimension warnings when expanding a company.

### Phase 4 — Split CI and deployment

**Status:** Complete — 2026-11-22

- [x] Replace the combined scheduled workflow with `stock-screener.yml` for the stock data pipeline and app build.
- [x] Add path-aware jobs so a scheduled/data-only run does not rebuild the frontend.
- [x] Publish outputs as artifacts with stable, app-scoped paths.
- [x] Implement `deploy.yml` as a reusable deployment workflow: overlay only the changed app/data paths onto `gh-pages`, preserve other apps, and serialize deployments to avoid races.
- [x] Keep scheduled stock-screener runs at the existing time and retain manual workflow dispatch.
- [x] Grant only the deployment job the `contents: write` permission; remove scraper-level Git credentials.
- [x] Add the same artifact contract for future workflows without introducing a monolithic all-pipelines workflow.

**Implementation evidence**

- Replaced the combined workflow with [stock-screener.yml](../../workflows/stock-screener.yml) for path-filtered builds, pipeline tests, production data artifacts, and the existing 18:00 UTC schedule. Scheduled/data-only runs do not build the frontend.
- Added [deploy.yml](../../workflows/deploy.yml), a single serialized deployment that downloads whichever site/data artifacts exist, replaces only their scoped Pages paths, and retains unrelated content.
- Granted `contents: write` only to the reusable deployment job. Scraper code remains free of Git publishing credentials and behavior.
- Added a fail-closed rollout gate: `PAGES_DEPLOYMENT_ENABLED` must be `true` before scheduled production scraping or any Pages deployment; manual dispatch additionally requires the `publish` confirmation input. No production scrape or deployment has been run.
- Updated the README with the gate and workflow behavior.

**Exit gate:** Passed. Both workflow files parse as YAML. A local overlay simulation verified the portal, app, and data paths update while `.nojekyll`, another app, and unrelated legacy content remain intact. No GitHub workflow or deployment was run.

### Phase 5 — Documentation and migration report

**Status:** Not started

- [ ] Update the root README with the new architecture and safe commands.
- [ ] Add `docs/architecture.md`, `docs/deployment.md`, and `docs/adding-apps-and-data.md`.
- [ ] Document how to add an app, pipeline, and dataset using the existing conventions.
- [ ] Add TODO markers at intentional future integration points.
- [ ] Record what moved, why, compatibility behavior, and remaining limitations in a migration summary.

**Exit gate:** A contributor can locate, build, and deploy each current part using repository documentation alone.

### Phase 6 — End-to-end verification and rollout

**Status:** Not started

- [ ] Run Java tests and test-mode scrape; compare output shape and representative values with the baseline.
- [ ] Run frontend lint/build/tests available in the project; verify landing, screener route, and detail charts.
- [ ] Validate workflow YAML and inspect the planned deployment artifact before publishing.
- [ ] Verify `gh-pages` contains root landing page, app files, and `data/stock-screener` without deleting sibling content.
- [ ] After user approval, run one production pipeline and verify the live site and refreshed data.
- [ ] Record final results, deployed commit/run, remaining TODOs, and manual GitHub actions below.

**Exit gate:** Existing screener behavior is preserved, new routes work, and the published output supports adding another independent app/data pipeline.

## Technical-debt work included in the migration

- **Crawler/UI coupling:** Move the crawler out of frontend resources and treat JSON as a versioned data interface.
- **Generation/rendering separation:** Give the pipeline and Vite app independent build commands and outputs.
- **Avoid unnecessary frontend builds:** Use path-aware workflow jobs and deploy data-only artifacts without rebuilding app assets.
- **Caching:** Cache Maven/npm dependencies in Actions. Evaluate HTTP conditional caching only if the upstream supports it and it does not compromise fresh data.
- **Actions bottlenecks:** Avoid duplicate installs/builds; serialize only the final `gh-pages` publisher, not independent pipelines.
- **Repository growth:** Track generated data size and Git history growth. Keep current snapshots distinct from future historical series; choose retention or external storage when measured size warrants it.

## Manual actions and checkpoints

No manual command is required to prepare this plan. During execution, the assistant should run repository commands and tests directly when available, and pause to notify the user when access or approval is needed.

- **Before first live deployment:** User approval is required; do not run the production workflow before this gate.
- **GitHub settings check:** User may need to confirm Pages publishes from `gh-pages` at the repository root, Actions can write repository contents, and branch protection permits the publisher.
- **Enable production rollout:** After approval and local parity checks, set the repository Actions variable `PAGES_DEPLOYMENT_ENABLED` to `true`. A manual production run also requires setting the `publish` input to `true`.
- **Credential/settings blocker:** If GitHub settings or a workflow run cannot be verified from the workspace, request the specific manual check; do not ask the user to run production scraper commands locally.

## Progress and resume log

Update this section after each execution session so work can resume without repeating completed steps.

| Date | Phase/item | Status | Change/evidence | Validation | Manual action/blocker |
|---|---|---|---|---|---|
| 2026-11-22 | Phase 0 — Baseline and safeguards | Complete | Recorded branch, live route, current deployment layout, JSON shape, URL strategy, and baseline warnings | `mvn -Denvironment=test test` succeeded; live page loaded 481 stocks and an expanded company detail | No command required; Pages settings must be confirmed before first deployment |
| 2026-11-22 | Phase 1 — Establish safe build and data contracts | Complete | Set test as Maven default, explicit production workflow flag, surfaced output errors, added tests and artifact contract; documented user's route decision | `mvn -B test`: 4 tests passed; safe test scrape wrote 25 companies | None |
| 2026-11-22 | Phase 2 — Move pipeline and data | Complete | Relocated Java pipeline/tests/samples and datasets; made root POM an aggregator; separated frontend build; removed scraper Git publishing | `mvn -B package`: 4 tests passed, 25-company test scrape, JAR created, `BUILD SUCCESS`; `npm run build` succeeded | None; production workflow was not run |
| 2026-11-22 | Phase 3 — Move and decouple the frontend | Complete | Relocated app; added configurable routes/data URL, portal/registry/theme; updated Pages artifact assembly | Maven package and frontend builds passed; local portal and screener browser checks passed; no production scrape/deployment | None |
| 2026-11-22 | Phase 4 — Split CI and deployment | Complete | Added path-aware pipeline/site/data jobs, scoped artifacts, a serialized overlay publisher, and a fail-closed rollout gate | YAML parse and local preservation simulation passed; no GitHub workflow, production scrape, or deployment was run | Before rollout: verify Pages/branch settings, approve, and set `PAGES_DEPLOYMENT_ENABLED=true` |

### Current handoff

- **Overall status:** Phases 0–4 complete; Phase 5 not started.
- **Next item:** Add architecture, deployment, and onboarding documentation plus a migration summary.
- **Last verified state:** Maven package, local test-data preparation, production-configured frontend build, local portal/screener browser checks, workflow YAML parsing, and scoped overlay simulation succeeded. No GitHub workflow, deployment, or production scrape was run.
- **Unresolved decisions:** Confirm Pages source/branch protection manually before the first deployment. Investigate unreferenced test/production detail files before changing cleanup behavior.
- **Manual commands currently required:** None.
