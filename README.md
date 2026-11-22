# Daily Custom Stock Screener

A Java scraper for Screener.in that writes company summaries and details as static JSON. A React/Vite frontend reads that data, and GitHub Actions builds and publishes the static site to GitHub Pages.

## Tech and layout

- Java 25 / Maven: scraper in `pipelines/stock-screener`, with production/test data in `data/stock-screener`.
- React 18 / Vite / Bootstrap / Recharts: stock screener in `apps/stock-screener`; the root portal is in `public/`.
- `shared/theme/` and `shared/config/`: shared style tokens and portal application registry.
- `.github/workflows/daily-eod-build.yml`: scheduled daily build and Pages deployment.

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

Maven defaults to the `test` environment; `mvn test` writes test data and does not push Git changes. Run it before `npm run dev`, which copies the test dataset into the app's ignored local public directory. The scheduled GitHub Actions workflow explicitly selects `prod`. Production scrapes write JSON only; publishing is handled separately by the workflow. The frontend build uses Node.js 24.11.1 and npm.

GitHub Actions runs daily at 18:00 UTC (23:30 IST) or on manual dispatch. The workflow publishes the portal at the project root, the screener at `/stock-screeners/`, and production data under `/data/stock-screener/`.
