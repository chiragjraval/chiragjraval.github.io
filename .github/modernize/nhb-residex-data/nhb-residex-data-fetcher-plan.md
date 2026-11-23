# NHB Residex Data Fetcher Plan

## Purpose

Add a weekly data pipeline for NHB Residex city prices and NIFTY indices, plus
a Financial Data Lab page for viewing city prices and comparing their
performance with the four selected NIFTY indices.

This file is the **single source of truth for scope, decisions, phase status,
progress, validation, and handoff**. Update it after each work session. The
prompt in this folder is the original input; generated traceability checkpoints
are supporting validation artifacts, not separate execution plans.

Do not run a bulk production scrape or publish data until source behavior,
quote semantics, and the production rollout gates below are satisfied.

## Decisions and constraints

- **Base-year label:** The prompt's “2017-28” means **2017-18**.
- **NHB geography:** Fetch every city listed by the NHB state/city catalogs.
  States are selection/filter groups; do not infer state-level time series.
- **NHB series:** Request the 2024-25, 2017-18, and 2012-13 base-year series
  for each city. When a quarter appears in multiple base-year series, use the
  value from the newer series: `2024-25 > 2017-18 > 2012-13`.
- **Price fields:** Preserve NHB's composite carpet-area price and all three
  unit-size price bands (`<=60`, `>60 to <=110`, and `>110` sq. m.), each in
  INR per sq. ft. Retain `HFC_INDEX` and its base-year metadata, but do not
  stitch differently rebased index values into one raw continuous series.
- **Benchmarks:** This phase includes NIFTY 50, NIFTY 500, NIFTY MIDCAP 150,
  and NIFTY SMALLCAP 250 only. Gold and silver are deferred.
- **Provider status:** The four supplied Screener chart endpoints passed
  daily and long-range probes; `days=10000` returned weekly observations.
  Use Screener for the NIFTY-only implementation.
- **Source preference:** Use the validated Screener endpoints for this
  NIFTY-only implementation.
- **Date alignment:** For each NHB quarter end, select the latest benchmark
  observation on or before that date. Do not look ahead or interpolate.
- **Charts:** Let users select the composite price and any combination of the
  three carpet-area price bands. Show each selected price divided by the
  matched benchmark in the relative chart; show selected NHB prices and the
  matched benchmark on one raw-value axis with units in each line label and
  tooltip; and show each selected price and the benchmark rebased to 100 in a
  third chart. Synchronize date crosshairs and tooltips across all three charts.
- **Navigation:** Show NHB Residex on the Financial Data Lab page and in the
  left sidebar immediately before Stock Screeners.
- **Refresh:** Schedule the target weekly collection for Saturday at
  01:00 UTC (06:30 India time). Pace live requests by at least 500 ms and retry
  HTTP 429, 5xx, and transient I/O failures using bounded exponential backoff
  with jitter. Fail on other client errors or incomplete responses. Monitor
  the initial full-universe runs for NHB throttling. Merge corrections and new
  observations; leave canonical data and published artifacts unchanged when
  fetched values are unchanged. Data-only refreshes must not rebuild either
  frontend.
- **Architecture:** Keep this as an independent Java/Maven pipeline and
  React/Vite app using static JSON. Reuse shared shell/theme patterns. Preserve
  the stock-screener pipeline, site, data contract, and deployment safeguards.
- **Deployment:** The Saturday schedule runs from `main` and is gated by
  `PAGES_DEPLOYMENT_ENABLED=true`; manual production collection remains
  explicitly gated. Deploy only the NHB app/data paths and preserve all
  unrelated `gh-pages` content.

## Target routes and data paths

| Page/data | Target URL/path |
|---|---|
| NHB Residex comparison page | `/financial-data-lab/nhb-residex/` |
| NHB production data | `/data/nhb-residex/prod/` |
| Local/test data | `data/nhb-residex/test/` |
| Production source data | `data/nhb-residex/prod/` |

Add the comparison page as a Financial Data Lab tool card and a sidebar item
before Stock Screeners.

## Current baseline and research evidence

- Repository architecture is a Java 25/Maven pipeline writing static JSON,
  React 18/Vite stock-screener app, framework-neutral shared site shell, and
  GitHub Pages overlay deployment.
- `docs/adding-apps-and-data.md` requires app, pipeline, and generated data to
  remain separately scoped. The existing React app already uses Recharts.
- The NHB state endpoint returned `{value,text}` state entries; the city
  endpoint for Maharashtra returned `{value,text}` city entries. The city
  trend POST returned a status plus a JSON-encoded data string with quarter,
  city, index, weighted-price, count, and related fields.
- Sample city requests confirmed:
  - `DashboardBY2018` returns 31 Chakan rows from Jun 2018 to Dec 2025 by
    default; with `chkbefore1718=1`, it returned 51 rows starting Jun 2013.
  - `DashboardBY2013` returned 20 rows from Jun 2013 to Mar 2018.
  - `DashboardBY2024` returned five rows from Jun 2025 to Jun 2026.
  - Jun/Sep/Dec 2025 overlap between the 2017-18 and 2024-25 series.
  - In Jun 2025, the two base-year series reported different `HFC_INDEX`
    levels (117.40 vs 101.63) but similar composite prices (5473 vs 5477 INR
    per sq. ft.). The base-year on `HFC_INDEX` must be retained.
  - A legacy `WTAVG_COMPOSITE_sqm` sample was inconsistent with its
    square-foot value. Validate this field before use; it is not the selected
    comparison metric.
- Initial sample requests were bounded. The authorized 2026-11-23 local full
  run collected all 50 catalog cities without HTTP throttling; monitor future
  runs for source changes and freshness.
- **Screener.in index probe:** All four supplied chart URLs returned JSON for
  a `days=30` sample without cookies. Each response had three datasets:
  `Index Price`, `Index DMA50`, and `Index DMA200`; the price values were
  numeric strings paired with ISO dates. `meta.is_weekly` was `false` for all
  four, and the sample contained daily trading observations from Oct. 26 to
  Nov. 20, 2026. Repeating with `days=10000` returned `meta.is_weekly=true`
  for all four and weekly `Index Price` values:
  - NIFTY 50: 1,064 observations, Jul. 7, 2006–Nov. 20, 2026.
  - NIFTY 500: 1,064 observations, Jul. 7, 2006–Nov. 20, 2026.
  - NIFTY MIDCAP 150: 1,130 observations, Apr. 1, 2005–Nov. 20, 2026.
  - NIFTY SMALLCAP 250: 412 observations, Jan. 4, 2019–Nov. 20, 2026.
  Use only `Index Price`, not the moving-average datasets. `days` changes the
  response cadence: short windows are daily; this long window is weekly. Small
  cap history is substantially shorter than the other index series.
- **Current Screener response shape:** During the 2026-11-23 live run, the
  response no longer included `meta.is_weekly`; the price dataset used
  `metric: "Index Price"` and `label: "Index value"`. NIFTY 50 still returned
  1,064 weekly points from 2006-07-07 through 2026-11-20, with no adjacent
  points closer than five days. The parser now selects by `metric` and
  validates dated cadence when weekly metadata is absent.
- **Deferred scope:** Gold and silver comparisons are excluded from this
  phase. Prior MCX and Yahoo research does not affect the NIFTY-only source
  selection.

## UX and data acceptance criteria

1. A user can select a state and city, then a benchmark with available data.
2. The raw chart shows every selected NHB price band and the matched benchmark
   on one axis; line labels and tooltips make the different units explicit and
   identify each housing area range. Plot every available observation, let
   the chart library choose a readable subset of slanted X-axis labels, and
   keep every plotted point available in the hover tooltip.
3. For each selected housing series, the relative chart shows the raw NHB
   price divided by the latest benchmark value on or before each quarter end.
   The ratio has no look-ahead or synthetic points and is labeled in INR per
   sq. ft. per index point.
4. The rebased chart shows each selected NHB series and the benchmark starting
   at 100 at its first available quarter.
5. Hovering any chart synchronizes the date crosshair and tooltip on all three.
6. NHB Residex is visible in the sidebar and precedes Stock Screeners on the
   Financial Data Lab page.
7. Overlapping quarters use newer base-year source values; missing/invalid
   responses are surfaced, not silently treated as valid zero values.
8. Weekly refreshes publish nothing when canonical output is unchanged and do
   not rebuild the frontend.
9. Keyboard operation, visible focus, accessible labels, explicit loading and
   error states, and a usable narrow-screen layout are verified.
10. Existing screener routes, data, scheduled job, deployment gates, and
   unrelated Pages content remain unchanged.

## Execution phases

### Phase 0 — Baseline and source behavior

**Status:** In progress — the full NHB catalog was collected with pacing and
retries; the live baseline shows NHB data through 2026-06-30. Weekly freshness
still needs monitoring.

- [x] Review the existing pipeline, app, static-data, and Pages deployment
  boundaries.
- [x] Sample NHB catalogs and all three city trend endpoints; record response
  shapes, base-year overlap, and observed field inconsistency.
- [x] Probe all four supplied Screener index endpoints with `days=30` and
  `days=10000`; the short window was daily, the long window weekly. Record
  each series' point count and coverage dates above.
- [x] **T001 [Plan:1.1]** Select Screener as the NIFTY source after validating
  all four index responses, weekly cadence for `days=10000`, available history,
  and the `Index Price` dataset.
- [x] Record NHB latest-data freshness and confirm the full catalog is
  collected without persistent throttling in the initial run.
- [ ] Monitor later Saturday runs for changes in freshness or throttling.

**Exit gate:** The NIFTY source and data contract are selected, with date
coverage and the `Index Price` dataset validated. Review the live baseline and
monitor initial scheduled runs before relying on weekly freshness.

### Phase 1 — Data contract and merge rules

**Status:** Complete — the composite and three area-band fields, parsing,
merge rules, and output contract are covered by fixtures and focused tests.

- [x] Confirm all-city scope, 2017-18 label, weekly cadence, no-look-ahead
  date alignment, and raw/relative chart views.
- [x] Set base-year precedence to `2024-25 > 2017-18 > 2012-13`.
- [x] Set the relative ratio formula: divide each NHB price by its latest
  benchmark value on or before that quarter end; do not rebase.
- [x] **T002 [Plan:1.2]** Save sanitized NHB catalog and dashboard response
  fixtures under `pipelines/nhb-residex/src/test/resources/`; verify quarter
  parsing, null behavior, price-method selection, and price units in
  `NhbResidexClientTest.java` and `FixturePipelineTest.java`.
- [x] **T003 [Plan:1.3]** Record the stable city/quarter key, field meanings,
  provenance, correction/upsert behavior, and schema in
  `data/nhb-residex/README.md`.

**Exit gate:** The source data contract is fixture-backed; no base-year
discontinuity or uncertain field is silently represented as a continuous
price series.

### Phase 2 — Build the NHB and selected benchmark pipeline

**Status:** Complete — implemented with fixture-default execution and explicit
live/production safeguards.

- [x] **T004 [Plan:2.1]** Add `pipelines/nhb-residex/pom.xml`, register the
  module in root `pom.xml`, and implement fixture-only CLI/environment
  handling in
  `pipelines/nhb-residex/src/main/java/com/chirag/nhbresidex/NhbResidexApplication.java`.
- [x] **T005 [US1] [Plan:2.2]** Implement bounded NHB state/city catalog
  retrieval and per-city requests to all three dashboard endpoints in
  `pipelines/nhb-residex/src/main/java/com/chirag/nhbresidex/client/NhbResidexClient.java`.
- [x] **T006 [US1] [Plan:2.3]** Normalize quarter dates, retain source
  provenance, merge city observations by base-year priority, and atomically
  write changed records in
  `pipelines/nhb-residex/src/main/java/com/chirag/nhbresidex/service/CitySeriesMerger.java`
  and
  `pipelines/nhb-residex/src/test/java/com/chirag/nhbresidex/CitySeriesMergerTest.java`.
- [x] **T007 [US1] [Plan:2.4]** After Phase 0 passes, implement fixture-tested
  adapters for the selected benchmark sources in
  `pipelines/nhb-residex/src/main/java/com/chirag/nhbresidex/client/BenchmarkDataClient.java`.
- [x] **T008 [US1] [Plan:2.5]** Generate and validate the manifest, per-city
  files, benchmark files, schema version, attribution, and changed-output
  signal in
  `pipelines/nhb-residex/src/main/java/com/chirag/nhbresidex/service/JsonOutputService.java`.
- [x] Keep test and production output separate under
  `data/nhb-residex/{test,prod}/`; never commit credentials or perform Git
  operations from pipeline code. Reviewed production baseline output may be
  tracked in the repository.

**Exit gate:** Offline tests cover parsing/merge/write behavior; production
output is deterministic, schema-checked, and only changes when source content
changes. No benchmark connector bypasses Phase 0.

### Phase 3 — Add the Financial Data Lab comparison app

**Status:** Complete — composite/area-band selection, three synchronized
charts, and the requested navigation order are implemented.

- [x] **T009 [US2] [Plan:3.1]** Scaffold the React/Vite entry point in
  `apps/nhb-residex-comparison/src/main.jsx` and
  `apps/nhb-residex-comparison/vite.config.js`; add its tool card to
  `public/financial-data-lab/index.html`.
- [x] **T010 [US2] [Plan:3.2]** Load the manifest and implement state, city,
  multiple housing-price-series, and benchmark selection in
  `apps/nhb-residex-comparison/src/App.jsx`.
- [x] **T011 [US2] [Plan:3.3]** Render selected raw housing-price series and
  matched benchmark values on one axis with per-line unit labels in
  `apps/nhb-residex-comparison/src/components/RawSeriesChart.jsx`.
- [x] **T012 [US2] [Plan:3.4]** Implement latest-on-or-before alignment,
  per-series price-to-index ratios, and independent rebasing in
  `apps/nhb-residex-comparison/src/utils/alignSeries.js`; cover edge cases in
  `apps/nhb-residex-comparison/src/utils/alignSeries.test.js`.
- [x] **T013 [US2] [Plan:3.5]** Add accessible loading, error, empty,
  keyboard, focus, and mobile-layout states in
  `apps/nhb-residex-comparison/src/App.jsx` and `App.css`.
- [x] Add NHB Residex to the left sidebar and move its Financial Data Lab card
  before Stock Screeners.
- [x] Preserve NHB composite and three area-band prices; add multi-series
  ratio, single-axis raw, rebased, and synchronized chart views.

**Exit gate:** The route `/financial-data-lab/nhb-residex/` uses the shared site
shell and displays correct raw/relative data for fixtures with explicit
loading/error states and no horizontal page overflow on small screens.

### Phase 4 — Production workflow and Pages integration

**Status:** Complete — Saturday schedule and manual production workflow are
gated to `main` and `PAGES_DEPLOYMENT_ENABLED=true`; the local overlay
simulation confirmed NHB-only replacement.

- [x] **T014 [Plan:4.1]** Add the Saturday `01:00 UTC` schedule, explicit
  manual production input, production safeguards, artifact validation, and
  unchanged-output behavior in `.github/workflows/nhb-residex.yml`.
- [x] **T015 [Plan:4.2]** Add optional NHB artifact inputs, JSON preflights,
  and path-scoped overlay commands in `.github/workflows/deploy.yml`.
- [x] Add the comparison app build to
  `.github/workflows/stock-screener.yml` for app/site changes; keep the weekly
  data-only job independent of frontend builds.

**Exit gate:** Passed — local overlay simulation updated only NHB production
data and preserved existing site and Stock Screener paths.

### Phase 5 — Validation and documentation

**Status:** In progress — pipeline/frontend tests and builds pass. A one-city
live fetch confirmed all three area-band fields; the browser verified chart
order, sidebar order, and synchronized tooltips. Production data refresh
remains.

- [x] **T016 [Plan:4.3]** Run Maven fixture tests, app tests and production
  build, and browser checks; update `README.md`, `docs/architecture.md`,
  `docs/deployment.md`, and `docs/adding-apps-and-data.md`.
- [x] Validate chart units, quarter alignment, no-look-ahead ratio
  calculations, no-change refresh behavior, and narrow-screen layout.
- [x] Do not dispatch a workflow or publish Pages during local validation;
  one local full live collection is explicitly approved as a baseline.
- [x] Simulate the Pages overlay with unrelated existing paths; NHB data was
  replaced while the site and Stock Screener data were preserved.
- [ ] Run Stock Screener regression checks.

**Exit gate:** Local tests and builds pass, requirements are covered, selected
source contracts are recorded, and the overlay is verified against a fixture
containing unrelated existing site/data paths.

### Phase 6 — Manual production rollout

**Status:** The existing 50-city baseline is present; its checked-in schema
does not yet include the newly added area-band values. A fresh production
collection is required after the updated pipeline is on `main`.

- [x] Review the selected source's data freshness, units, output size, and JSON
  contract: 50 cities, 53 observations each, four benchmarks, 0.76 MiB, latest
  NHB quarter 2026-06-30, and NIFTY data through 2026-11-20.
- [x] Do not change `PAGES_DEPLOYMENT_ENABLED`; enable it only by user choice.
- [x] Review the existing local live baseline for completeness, latest dates,
  and output size before committing it.
- [ ] After the updated pipeline is on `main`, run the gated NHB workflow and
  verify the new area-band fields are published only under the NHB data path.
- [ ] Confirm a scheduled data-only refresh does not rebuild the frontend or
  replace Stock Screener data.

**Exit gate:** Live route and data work are explicitly approved by the user,
and existing site/data paths remain intact.

### Phase 7 — Handoff

**Status:** In progress — the requested UI/data enhancements are under
validation; a fresh production data run and deployment remain.

- [x] Record implemented paths, workflow state, source configuration, test
  results, and remaining blockers in this plan.
- [ ] Keep repository separation deferred; it is outside this capability.

**Exit gate:** The next session can resume from this file without repeating
source research or validation.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| NIFTY SMALLCAP 250 has shorter history than the other index series | Record per-index coverage dates and start each relative comparison at the first shared observation. |
| Provider endpoints or symbols change or return partial history | Keep adapters isolated, validate schema/date coverage, and surface provider errors rather than silently accepting incomplete series. |
| NHB endpoints throttle or change during weekly all-city collection | Pace requests, use bounded retries, and fixture-test response shapes before enabling scheduled refreshes. |
| Different base years produce discontinuous `HFC_INDEX` values | Retain base-year metadata and use the validated weighted composite price for long-run price comparison. |
| NHB field meaning/units vary by response version | Keep fixtures for each endpoint and validate field semantics; do not silently convert suspect legacy values. |
| Source returns a temporarily incomplete response | Upsert corrections/new points but do not delete stored valid points solely because a response omits them. Surface failures. |
| Weekly job rebuilds the frontend or overwrites other Pages apps | Keep data workflow separate and extend the overlay only for exact NHB app/data paths. Test preservation with a fixture. |
| Relative charts use future or synthetic data | Use latest-on-or-before matching; exclude missing pairs and disclose the common start date. |

## Manual actions and checkpoints

- The assistant can edit this repository, run local fixture tests/builds, and
  simulate overlays.
- Do not create provider accounts or store credentials without user approval.
- The full local live collection was explicitly approved in this session.
  Future collection/publishing stays behind the workflow gate and the user's
  repository-variable choice.
- Current preference is a simple, preferably no-cost source for this hobby
  project. Screener is the selected technical source for NIFTY data. The local
  baseline was approved; GitHub publication awaits the workflow being pushed
  to `main` and the repository variable being enabled.

## Progress log

| Date | Phase/item | Status | Change/evidence | Validation | Manual action/blocker |
|---|---|---|---|---|---|
| 2026-11-23 | Prompt review and clarifications | Complete | Confirmed corrected base-year label, all-city scope, weekly cadence, nearest prior benchmark date, raw/relative views, and hobby-project preference for a simple source. | Recorded decisions in this plan. | None. |
| 2026-11-23 | Phase 0 — Source and NHB baseline | In progress | Sampled NHB catalogs/series and all four Screener chart endpoints with `days=30` and `days=10000`; the live run found the current metric-based Screener schema. | Screener returned daily data for 30 days and weekly data for 10000 days. Weekly histories range from 412 to 1130 points; Smallcap 250 starts in 2019, others in 2005/2006. NHB data currently ends 2026-06-30. | Monitor weekly freshness; no HTTP throttles appeared in the initial full collection. |
| 2026-11-23 | Scope narrowed to NIFTY indices | Complete | Deferred gold and silver; retained NIFTY 50, NIFTY 500, NIFTY MIDCAP 150, and NIFTY SMALLCAP 250. | Screener weekly index responses and coverage are recorded above. | Implement only the NIFTY comparison in this phase. |
| 2026-11-23 | Plan creation | Complete | Created this phase-based, single-source execution plan with requirements, tasks, gates, risks, and handoff. | Requirement and task traceability recorded in the supporting checkpoint files. | Do not begin production benchmark ingestion until Phase 0 passes. |
| 2026-11-23 | One-file plan format | Complete | Consolidated source findings and execution tracking into this phase-based plan, modeled on the personal-site plan format. | Task and requirement checkpoints remain derived from this plan. | None. |
| 2026-11-23 | NIFTY implementation, Saturday schedule, and live baseline | In progress | Added 500 ms request pacing, five-attempt exponential backoff with jitter, support for the current Screener metric-based weekly response, explicit warnings for NHB null-price rows, and a Saturday 01:00 UTC workflow. Collected the local production baseline. | Maven tests: 18 passed. Live run: 50 catalog cities, 53 observations each, four weekly indices, schema 1, 0.76 MiB. No HTTP retries were needed. Each city had 20 null-price rows in the older 2017-18 endpoint; the 2012-13 series supplied those quarters. Latest NHB quarter: 2026-06-30; NIFTY data through 2026-11-20. Hashes and overlay preservation passed. | Main push is approved; no Pages publish has occurred. Stock Screener regression remains. |
| 2026-11-23 | Multi-series prices, charts, and navigation | Implemented | Added composite plus three NHB carpet-area bands to the output contract and UI; added multi-series ratios, single-axis raw values, a 100-rebased chart, synchronized tooltips/crosshairs, and NHB-before-Stock navigation. | 18 Maven tests and 4 frontend tests passed; both frontend builds passed. One-city live DEV fetch returned all three bands (53 values each); browser confirmed three charts show the same hovered quarter and sidebar order. | The checked-in production baseline predates the new fields; run the updated gated NHB workflow after deployment to publish the area-band data. |

## Current handoff

- **Overall status:** Original implementation is on `main`; the three-chart
  multi-series enhancement is being completed. The new schema needs a fresh
  NHB production run before all area-band controls have published data.
- **Plan source of truth:** This file is the only file to update for phase
  status, progress, decisions, validation, and handoff. The original prompt is
  retained as input; generated checkpoint files are derived traceability
  outputs only. All source research and decisions are recorded in this file.
- **Immediate next step:** Complete the focused frontend/pipeline tests, then
  publish the updated code and manually run the gated NHB workflow to refresh
  area-band data. The scheduled run is Saturday at 01:00 UTC (06:30 India
  time).
- **Baseline:** [data/nhb-residex/prod/manifest.json](../../../data/nhb-residex/prod/manifest.json)
  indexes 50 cities and four benchmarks. The pipeline itself performs no Git
  operations.
- **Validation:** The NHB module has 18 passing tests; frontend tests and
  production build pass. A one-city live DEV fetch returned all three area
  bands; generated DEV files were removed after verification.
- **Safety:** The checked-in production baseline has not been rewritten during
  this enhancement. A full production refresh stays behind the gated NHB
  workflow.

## Requirement Mapping

| REQ ID | Description | Plan Items | Implementation Evidence |
|---|---|---|---|
| REQ-001 | Retrieve all-city NHB quarterly series across the three base years | 1.2, 2.1, 2.2, 2.3, 2.5, 3.2 | NHB fixtures, pipeline client/normalizer, manifest and city JSON, selectors |
| REQ-002 | Prefer newer base-year values and persist/publish only changed data | 1.3, 2.3, 2.5, 4.1 | Merge/upsert logic, deterministic writer, changed-output signal |
| REQ-003 | Provide the four NIFTY index series with correct dates and index values | 1.1, 2.4, 2.5 | Screener adapter, normalized index JSON |
| REQ-004 | Add city/benchmark raw and relative comparison views | 3.1, 3.2, 3.3, 3.4, 3.5 | Financial Data Lab page, selectors, charts, accessible states |
| REQ-005 | Use latest-on-or-before matching and raw price-to-index ratios | 1.3, 2.4, 3.4 | Alignment contract and focused calculation checks |
| REQ-006 | Refresh weekly and deploy only changed data without rebuilding the frontend | 1.1, 2.5, 4.1, 4.2 | Gated workflow, change detection, scoped Pages overlay |
| REQ-007 | Preserve existing behavior and verify/document the new pipeline and app | 1.2, 1.3, 2.1, 2.5, 3.1, 3.5, 4.2, 4.3 | Fixture tests, separate module/app, regression checks, updated docs |
