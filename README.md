# Daily Custom Stock Screener

A Java scraper for Screener.in that writes company summaries and details as static JSON. A React/Vite frontend reads that data, and GitHub Actions builds and publishes the static site to GitHub Pages.

## Tech and layout

- Java 25 / Maven: scraper in `pipelines/stock-screener`, with production/test data in `data/stock-screener`.
- React 18 / Vite / Bootstrap / Recharts: stock screener in `apps/stock-screener`; the root portal is in `public/`.
- `shared/theme/` and `shared/config/`: shared style tokens and portal application registry.
- `.github/workflows/stock-screener.yml` and `deploy.yml`: path-aware builds and serialized Pages deployment.

## Handy commands

Run from the repository root:

```powershell
# Scrape one page into test data; does not push Git changes
mvn test

# Build the frontend for local development
Set-Location apps/stock-screener
npm ci
npm run dev
```

Maven defaults to the `test` environment; `mvn test` writes test data and does not push Git changes. Run it before `npm run dev`, which copies the test dataset into the app's ignored local public directory. Scheduled production scraping explicitly selects `prod` and is disabled until the deployment gate is enabled; a manual production run requires `publish=true`. Production scrapes write JSON only; publishing is handled separately by the workflow. The frontend build uses Node.js 24.11.1 and npm.

GitHub Actions is scheduled daily at 18:00 UTC (23:30 IST). Scheduled production scraping and deployment remain disabled until the repository variable `PAGES_DEPLOYMENT_ENABLED` is set to `true`. A manual production publish requires the explicit `publish` confirmation input; set the repository variable after the first approved rollout to enable scheduled runs.

See [architecture](docs/architecture.md), [deployment](docs/deployment.md), [adding apps and data](docs/adding-apps-and-data.md), and the [migration summary](docs/repo-restructure-summary.md) for the full layout and operating details.
