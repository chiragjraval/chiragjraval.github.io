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
- Preserve the existing published URL during migration as a compatibility alias; use `/stock-screener/` as the new canonical application route. Do not remove the alias without user approval.
- Do not add a future app's code or data merely to demonstrate the architecture.

## Execution checklist

### Phase 0 — Baseline and safeguards

**Status:** Complete — 2026-11-22

- [x] Inspect current branch/worktree and retain unrelated user changes.
- [x] Record the current Pages URL and verify the site and representative company-detail view.
- [x] Capture the summary/detail JSON contract and a test-environment scrape result.
- [x] Record current scheduled workflow timing, workflow permissions, and `gh-pages` deployment layout.
- [x] Record the URL compatibility strategy: preserve the existing route and add `/stock-screener/`; do not retire the old route without approval.

**Baseline evidence**

- Local branch is `main`; remote `origin` has `main` and `gh-pages`. The `gh-pages` branch currently has `.nojekyll`, `assets/`, `index.html`, and `screener-data/` at its root.
- Live URL: `https://chiragjraval.github.io/daily_custom_stock_screener/`. It loaded 481 stocks, and expanding a company row loaded its score details. The live summary endpoint also reports 481 companies.
- A safe `mvn -Denvironment=test test` completed successfully: 25 companies were written to the test summary. Summary keys are `totalCompanies`, `generatedAt`, and `companies`; a company detail includes `metadata`, `attributes`, `companyScore`, `quarterlyResults`, `technicalHistory`, `latestQuarterlyResult`, and `latestTechnicalAttributes`.
- The test detail directory contained 52 JSON files after the run, 27 of which were not referenced by its 25-company summary. The pre-run count was not captured; investigate whether these are stale files during Phase 1 and do not delete them as part of this baseline.
- The production summary in the local checkout had 481 companies and was not modified. The test invocation did not push or publish.
- Workflow timing is `18:00 UTC` daily (`23:30 IST`) plus manual dispatch; workflow-level permission is `contents: write`. Published assets and data are combined at the `gh-pages` branch root.
- Expanding a live company row produced Recharts console warnings about chart width/height `-1`. Record as pre-existing behavior for later frontend verification.
- The public Pages API did not expose settings without authentication. Confirm Pages source and branch protection manually before the first deployment.

**Exit gate:** Baseline behavior and deployment paths are recorded; no production writes have occurred.

### Phase 1 — Establish safe build and data contracts

**Status:** In progress — 2026-11-22

- [ ] Add/adjust focused Java tests or a deterministic comparison fixture for JSON output and essential scraper behavior.
- [ ] Define pipeline inputs, environment selection, generated output path, and exit/failure behavior.
- [ ] Ensure test runs write only under the test output path (currently `src/main/resources/screener-data/test`; Phase 2 will move it to `data/stock-screener/test`) and cannot commit or push.
- [ ] Define a deployment artifact contract with distinct paths for the portal, app assets, and each dataset.
- [ ] Keep production output and GitHub credentials out of local default commands.

**Exit gate:** Test-mode build and scrape succeed without network publication; output contract is documented.

### Phase 2 — Move the stock-screener pipeline and data

**Status:** Not started

- [ ] Move Java scraper sources and sample inputs to `pipelines/stock-screener`.
- [ ] Move the Maven module/build configuration; remove frontend npm lifecycle work from that module.
- [ ] Retain a root Maven aggregator or document the new direct module command.
- [ ] Move checked-in/generated stock-screener data to `data/stock-screener`.
- [ ] Update scraper output paths and references without changing the existing JSON schema.
- [ ] Remove JGit commit/push behavior from the scraper; publication belongs to GitHub Actions.

**Exit gate:** The scraper runs in test mode from its new location and produces equivalent summary/detail JSON in the new data directory.

### Phase 3 — Move and decouple the frontend

**Status:** Not started

- [ ] Move the Vite application to `apps/stock-screener`.
- [ ] Make the Vite base URL/deployment prefix configurable for both legacy and `/stock-screener/` paths.
- [ ] Replace hard-coded `prod` data paths with a single configurable data base path.
- [ ] Keep current summary, filtering, sorting, expanded company details, and charts working.
- [ ] Add `public/index.html` as the root landing page linking to the stock screener.
- [ ] Add central theme tokens for colors, typography, spacing, and app branding.
- [ ] Extract only components needed by more than one app; leave screener-specific UI in its app.

**Exit gate:** Frontend builds and runs against static JSON; both the compatibility URL and new canonical route resolve correctly.

### Phase 4 — Split CI and deployment

**Status:** Not started

- [ ] Replace the combined scheduled workflow with `stock-screener.yml` for the stock data pipeline and app build.
- [ ] Add path-aware jobs so a scheduled/data-only run does not rebuild the frontend.
- [ ] Publish outputs as artifacts with stable, app-scoped paths.
- [ ] Implement `deploy.yml` as a reusable deployment workflow: overlay only the changed app/data paths onto `gh-pages`, preserve other apps, and serialize deployments to avoid races.
- [ ] Keep scheduled stock-screener runs at the existing time and retain manual workflow dispatch.
- [ ] Grant only the deployment job the `contents: write` permission; remove scraper-level Git credentials.
- [ ] Add the same artifact contract for future workflows without introducing a monolithic all-pipelines workflow.

**Exit gate:** Workflow validation succeeds; test artifacts have the expected layout; deployment logic preserves unrelated `gh-pages` content.

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
- **Credential/settings blocker:** If GitHub settings or a workflow run cannot be verified from the workspace, request the specific manual check; do not ask the user to run production scraper commands locally.

## Progress and resume log

Update this section after each execution session so work can resume without repeating completed steps.

| Date | Phase/item | Status | Change/evidence | Validation | Manual action/blocker |
|---|---|---|---|---|---|
| 2026-11-22 | Phase 0 — Baseline and safeguards | Complete | Recorded branch, live route, current deployment layout, JSON shape, URL strategy, and baseline warnings | `mvn -Denvironment=test test` succeeded; live page loaded 481 stocks and an expanded company detail | No command required; Pages settings must be confirmed before first deployment |
| 2026-11-22 | Phase 1 — Establish safe build and data contracts | In progress | Inspecting build defaults, execution context, and output serialization | Pending | None |

### Current handoff

- **Overall status:** Phase 0 complete; Phase 1 in progress.
- **Next item:** Complete Phase 1 tests, safe defaults, output/error behavior, and artifact contract.
- **Last verified state:** Test scrape succeeded with 25 summary entries; live Pages has 481 entries. Production mode was not run.
- **Unresolved decisions:** Confirm Pages source/branch protection manually before the first deployment. Investigate the 27 test detail files not referenced by the current summary before changing cleanup behavior.
- **Manual commands currently required:** None.
