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
- Baseline and local browser checks confirmed the test screener loads, filters, expands details, and renders charts. Existing non-blocking Vite bundle-size and Recharts dimension warnings remain.

## First live run follow-up

The user-triggered first workflow run published the portal, but the screener showed zero companies. The deployed summary was at `/stock-screener/prod/summary.json` while the app requests `/data/stock-screener/prod/summary.json`. The artifact upload had stripped the `data/` prefix. The workflow now uploads from the parent artifact directory and validates the expected summary path before deployment. After the user reran the action, the expected public summary URL returned 481 companies and the screener loaded successfully.

Scheduled production scraping and deployment remain gated until the repository Actions variable `PAGES_DEPLOYMENT_ENABLED` is set to the lowercase string `true`.

## Remaining rollout work

- Complete end-to-end artifact/workflow verification and compare representative output against the recorded baseline.
- Confirm Pages source, Actions write permission, and branch-protection behavior in GitHub.
- Obtain approval before setting `PAGES_DEPLOYMENT_ENABLED=true` and dispatching the first production publish.
- Decide whether to remove unreferenced company detail files; they were preserved during the data relocation.
