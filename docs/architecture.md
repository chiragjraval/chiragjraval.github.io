# Architecture

This repository contains a static personal website and an independently built
financial analytics app. The planned canonical host is
`https://chiragjraval.github.io/`; the current project-site deployment remains
in place until the manual rename and cutover are approved.

```text
public/                         Personal home and Financial Data Lab pages
shared/config/site.js           Site identity and global navigation
shared/ui/site-shell.js         Framework-neutral navigation shell
shared/theme/tokens.css         Shared colors, typography, and content styles
apps/stock-screener/            React/Vite user interface
  |                             Fetches JSON; no application backend
  v
data/stock-screener/{test,prod} Generated summary and company-detail JSON
  ^
pipelines/stock-screener/       Java 25 / Maven scraper
apps/nhb-residex-comparison/    React/Vite NHB and NIFTY comparison interface
  |                             Fetches static JSON; no application backend
             v
data/nhb-residex/{test,dev,prod} City and weekly benchmark JSON
  ^
pipelines/nhb-residex/          Java 25 / Maven collection and normalization
```

## Responsibilities

- **Personal site:** `public/` provides the home page, Financial Data Lab
  landing page, and legacy-route transition pages.
- **Shared shell:** `shared/ui/site-shell.js` provides the accessible,
  responsive left navigation to both the static pages and React app.
  `shared/config/site.js` is the route registry; `shared/theme/tokens.css`
  defines shared colors, typography, compact spacing, and page-card styles.
- **Applications:** `apps/stock-screener/` owns its React components, charts,
  npm dependencies, and Vite build. Its target route is
  `/financial-data-lab/stock-screeners/`. The NHB comparison app is
  independently built from `apps/nhb-residex-comparison/` and lives at
  `/financial-data-lab/nhb-residex/`.
- **Pipeline and data:** `pipelines/stock-screener/` is a Maven module
  aggregated by the root `pom.xml`. It writes JSON to
  `data/stock-screener/{test,prod}`. `pipelines/nhb-residex/` writes its
  manifest, city series, and four NIFTY index series to
  `data/nhb-residex/{test,dev,prod}`. Pipelines do not commit or publish data.
- **Deployment:** `.github/workflows/stock-screener.yml` builds site and data
  artifacts independently. `.github/workflows/deploy.yml` overlays only the
  selected paths onto `gh-pages`.

## Data interface

The screener reads `summary.json` and the referenced
`details/<companyCode>.json` files. The summary contains `totalCompanies`,
`generatedAt`, and `companies`; detail objects preserve the metadata,
attributes, score, quarterly results, and technical history used by the UI.

Development uses the test dataset copied into the app's ignored public
directory by `npm run predev`. Production builds use
`/data/stock-screener/prod/`. Test data is never included in the frontend build
or a deployment artifact.

The NHB app reads `manifest.json`, one selected city series, and one selected
benchmark series. It matches each city quarter to the latest weekly benchmark
date on or before that quarter end. Its relative chart plots the raw NHB
weighted composite price divided by the matched index-point value.

## Build boundaries

- `mvn test` runs Java unit tests and the default test-mode scrape.
- `mvn -pl pipelines/nhb-residex test` runs fixture-only tests. Running the
  NHB pipeline uses synthetic fixtures by default; `--live` is required for
  source requests, and production additionally requires `--confirm-prod`.
- `npm run dev` / `npm run build` in `apps/stock-screener` run the frontend
  independently.
- `apps/nhb-residex-comparison` is independently tested and built; local
  development copies generated test data into its ignored public directory.
- The scheduled workflow refreshes data without rebuilding the frontend.
- The Maven root is an aggregator, not the owner of the frontend lifecycle.
