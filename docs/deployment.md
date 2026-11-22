# Deployment

GitHub Pages is a project site, so the repository prefix appears in every public URL:

| Content | Published path |
|---|---|
| Portal | `/daily_custom_stock_screener/` |
| Stock screener | `/daily_custom_stock_screener/stock-screeners/` |
| Production data | `/daily_custom_stock_screener/data/stock-screener/prod/` |

## Workflows

[`stock-screener.yml`](../.github/workflows/stock-screener.yml) is scheduled daily at 18:00 UTC and supports manual dispatch.

- A push to `main` that changes app/portal/theme files builds the site artifact. A pipeline-code change runs Maven tests. A production-data-only change assembles a data artifact without rebuilding the frontend.
- The scheduled job builds production data only; it does not rebuild the frontend. It runs only when the repository Actions variable `PAGES_DEPLOYMENT_ENABLED=true`.
- A manual dispatch always builds the site. Production scraping and publishing require the `publish` input; that input defaults to `false` and serves as explicit approval for that run.
- Push-triggered publishing is gated by `PAGES_DEPLOYMENT_ENABLED=true`. With the variable unset or false, scheduled runs do not scrape production and push events do not publish to Pages.

[`deploy.yml`](../.github/workflows/deploy.yml) downloads the requested artifacts, replaces only the portal/app or production-data paths, and commits the overlay to `gh-pages`. The deployment workflow is serialized so concurrent site/data updates cannot overwrite one another. It preserves `.nojekyll` and files belonging to other apps and datasets. Only the reusable deployment job receives `contents: write`.

## First rollout checklist

Do not open the deployment gate until local parity checks are complete and the repository owner approves the rollout.

1. Confirm GitHub Pages publishes from the `gh-pages` branch at its root.
2. Confirm Actions can write repository contents and that branch protection permits the workflow publisher.
3. Confirm the new portal and screener paths and production JSON contract.
4. After approval, manually dispatch `Stock Screener` with `publish=true` to run the first production scrape and publish both artifacts.
5. After verifying the first deployment, set the repository Actions variable `PAGES_DEPLOYMENT_ENABLED` to `true` to enable scheduled data refreshes and push-triggered publishing.

If GitHub settings or a workflow run cannot be verified locally, verify them in GitHub before enabling the variable. Do not run a local production scrape as a substitute.
