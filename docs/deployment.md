# Deployment

The target is a GitHub Pages user site at
`https://chiragjraval.github.io/`. Its routes after the approved repository
rename and cutover are:

| Content | Published path |
|---|---|
| Personal home | `/` |
| Financial Data Lab | `/financial-data-lab/` |
| Stock Screeners | `/financial-data-lab/stock-screeners/` |
| Production data | `/data/stock-screener/prod/` |

Until cutover, the live site remains at
`https://chiragjraval.github.io/daily_custom_stock_screener/`. The repository
rename and first publication are manual, approval-gated actions; do not infer
that a source change has moved the live site.

## Workflows

[`stock-screener.yml`](../.github/workflows/stock-screener.yml) is scheduled
daily at 18:00 UTC and supports manual dispatch.

- A push to `main` that changes app/portal/theme files builds the site artifact.
  A pipeline-code change runs Maven tests. A production-data-only change
  assembles a data artifact without rebuilding the frontend.
- The scheduled job builds production data only; it does not rebuild the
  frontend. It runs only when the repository Actions variable
  `PAGES_DEPLOYMENT_ENABLED=true`.
- A manual dispatch always builds the site. `publish_site=true` publishes the
  site without scraping production data. `publish=true` explicitly approves a
  production scrape and publishes both site and data artifacts.
- Push-triggered publishing is gated by `PAGES_DEPLOYMENT_ENABLED=true`. With
  the variable unset or false, scheduled runs do not scrape production and
  push events do not publish to Pages.

[`deploy.yml`](../.github/workflows/deploy.yml) downloads the requested
artifacts and overlays the root home, Financial Data Lab route, screener route,
shared assets, and/or production-data path. It preserves unrelated
`gh-pages` content and serializes branch updates. Only the reusable deployment
job receives `contents: write`.

## First user-site rollout checklist

Do not rename the repository or publish the new routes until local parity
checks are complete and the repository owner approves the cutover.

1. Confirm the repository name `chiragjraval.github.io` is available and
   rename this repository in **Settings → General → Repository name**.
2. Confirm GitHub Pages publishes from the `gh-pages` branch at its root.
   Confirm Actions can write repository contents and branch protection permits
   the publisher.
3. Update the local `origin` URL after the rename if needed.
4. Publish the site with `publish_site=true` to retain the current production
   dataset without running a production scrape. Verify the summary endpoint
   and all target routes before enabling any automatic publishing.
5. If daily refreshes are desired, set the Actions variable
   `PAGES_DEPLOYMENT_ENABLED=true`. This enables scheduled data refreshes and
   push-triggered publishing; it does not itself trigger a run.

If GitHub settings or a workflow run cannot be verified locally, verify them
in GitHub before enabling the variable. Do not run a local production scrape as
a substitute. The old project-site routes receive static transition pages where
possible; direct links to its former data paths are not duplicated.
