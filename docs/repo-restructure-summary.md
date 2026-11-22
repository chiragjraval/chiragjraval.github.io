# Repository restructure summary

## Goal

Evolve the single stock-screener site into a home for independent financial analytics applications while preserving the existing screener UI and JSON interface. The repository remains static-site oriented; no backend, database, or second application was introduced.

## What changed

- Moved the React/Vite app from `src/main/resources/app/` to `apps/stock-screener/` and made its Pages base/data URLs configurable.
- Added a root portal, an application registry, and shared theme tokens.
- Moved the Java scraper into `pipelines/stock-screener/`, made the root Maven POM an aggregator, and moved datasets to `data/stock-screener/{test,prod}`.
- Removed scraper-side Git publishing. The pipeline writes JSON; GitHub Actions owns artifact publication.
- Split builds from deployment. Path-aware jobs create app/data artifacts, and the serialized reusable publisher overlays selected paths while retaining unrelated Pages content.
- Kept the summary/detail JSON shape and current screener features. The root route changes from the screener to the portal; the screener is now at `/stock-screeners/`.

## Safety and compatibility

- Maven defaults to test mode. The local frontend serves that test dataset after `npm run predev`; production data is separately configured and never copied into the frontend build.
- Scheduled production scraping and push-triggered Pages publishing remain disabled until `PAGES_DEPLOYMENT_ENABLED` is enabled. A manual first publish requires `publish=true`.
- No production scrape, GitHub workflow run, or Pages deployment has been run as part of this migration.
- Baseline and local browser checks confirmed the test screener loads, filters, expands details, and renders charts. Existing non-blocking Vite bundle-size and Recharts dimension warnings remain.

## Remaining rollout work

- Complete end-to-end artifact/workflow verification and compare representative output against the recorded baseline.
- Confirm Pages source, Actions write permission, and branch-protection behavior in GitHub.
- Obtain approval before setting `PAGES_DEPLOYMENT_ENABLED=true` and dispatching the first production publish.
- Decide whether to remove unreferenced company detail files; they were preserved during the data relocation.
