# Daily Custom Stock Screener

A Java scraper for Screener.in that writes company summaries and details as static JSON. A React/Vite frontend reads that data, and GitHub Actions builds and publishes the static site to GitHub Pages.

## Tech and layout

- Java 25 / Maven: scraper in `pipelines/stock-screener`, with production/test data in `data/stock-screener`.
- React 18 / Vite / Bootstrap / Recharts: static frontend in `src/main/resources/app`.
- `.github/workflows/daily-eod-build.yml`: scheduled daily build and Pages deployment.

## Handy commands

Run from the repository root:

```powershell
# Scrape one page into test data; does not push Git changes
mvn test

# Build the frontend for local development
Set-Location src/main/resources/app
npm install
npm run dev
```

Maven defaults to the `test` environment; `mvn test` writes test data and does not push Git changes. The scheduled GitHub Actions workflow explicitly selects `prod`. Production scrapes write JSON only; publishing is handled separately by the workflow. The frontend build uses Node.js 24.11.1 and npm.

GitHub Actions runs daily at 18:00 UTC (23:30 IST) or on manual dispatch. The workflow packages the site and publishes the frontend build plus `screener-data` to GitHub Pages.
