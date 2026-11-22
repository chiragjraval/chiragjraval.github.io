# Repository restructure summary

## Goal

Evolve the single stock-screener site into a home for independent financial analytics applications while preserving the existing screener UI and JSON interface. The repository remains static-site oriented; no backend, database, or second application was introduced.

## What changed

- Moved the React/Vite app from `src/main/resources/app/` to `apps/stock-screener/` and made its Pages base/data URLs configurable.
- Added a root portal, an application registry, and shared theme tokens.
- Moved the Java scraper into `pipelines/stock-screener/`, made the root Maven POM an aggregator, and moved datasets to `data/stock-screener/{test,prod}`.
- Removed scraper-side Git publishing. The pipeline writes JSON; GitHub Actions owns artifact publication.
- Split builds from deployment. Path-aware jobs create app/data artifacts, and the serialized reusable publisher overlays selected paths while retaining unrelated Pages content.
- Kept the summary/detail JSON shape and current screener features. At the time
  of the original restructure, the project-site root became a portal and the
  screener moved to `/stock-screeners/`; the follow-up personal-site migration
  moves it under `/financial-data-lab/`.

## Safety and compatibility

- Maven defaults to test mode. The local frontend serves that test dataset after `npm run predev`; production data is separately configured and never copied into the frontend build.
- Scheduled production scraping and push-triggered Pages publishing remain disabled until `PAGES_DEPLOYMENT_ENABLED` is enabled. A manual first publish requires `publish=true`.
- Baseline and local browser checks confirmed the test screener loads, filters, expands details, and renders charts. Existing non-blocking Vite bundle-size and Recharts dimension warnings remain.

## First live run follow-up

The user-triggered first workflow run published the portal, but the screener showed zero companies. The deployed summary was at `/stock-screener/prod/summary.json` while the app requests `/data/stock-screener/prod/summary.json`. The artifact upload had stripped the `data/` prefix. The workflow now uploads from the parent artifact directory and validates the expected summary path before deployment. After the user reran the action, the expected public summary URL returned 481 companies and the screener loaded successfully.

Scheduled production scraping and deployment remain gated until the repository Actions variable `PAGES_DEPLOYMENT_ENABLED` is set to the lowercase string `true`.

## Original restructure verification

- The corrected project-site deployment completed successfully in workflow
  run [45019853339](https://github.com/chiragjraval/daily_custom_stock_screener/actions/runs/45019853339);
  the production summary endpoint returned 481 companies.
- The `PAGES_DEPLOYMENT_ENABLED` setting remains optional and its current value
  is not verified here. Enabling scheduled refreshes is separate from the
  personal-site migration.
- Unreferenced company detail files were preserved during the data relocation;
  cleanup remains a separate decision.

## Personal-site migration

The follow-up effort is tracked in
[personal-site-plan.md](../.github/modernize/personal-site/personal-site-plan.md).
It changes the target navigation to Home, Financial Data Lab, and Stock
Screeners under `/financial-data-lab/`, and moves the intended site host to
`https://chiragjraval.github.io/`. The repository name, Pages settings, and
live routes have not yet been changed; the current project-site URLs remain
live until the approved cutover.
