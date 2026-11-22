# Chirag Raval — Financial Data Lab

A personal website and a collection of focused financial analytics tools. The
stock screener uses a Java pipeline to produce static JSON consumed by a
React/Vite frontend. The planned canonical host is
`https://chiragjraval.github.io/`; the repository rename and live route cutover
are gated on local validation and user approval.

## Tech and layout

- Java 25 / Maven: scraper in `pipelines/stock-screener`, with production/test data in `data/stock-screener`.
- React 18 / Vite / Bootstrap / Recharts: stock screener in `apps/stock-screener`.
- `public/`: personal home page, Financial Data Lab landing page, and legacy-route redirects.
- `shared/ui/site-shell.js`, `shared/config/site.js`, and `shared/theme/`: common navigation, page routes, and design tokens.
- `.github/workflows/stock-screener.yml` and `deploy.yml`: path-aware builds and serialized Pages deployment.

Target routes after the approved cutover:

- `/` — personal home
- `/financial-data-lab/` — Financial Data Lab
- `/financial-data-lab/stock-screeners/` — Stock Screeners
- `/data/stock-screener/prod/` — production JSON

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

Maven defaults to the `test` environment; `mvn test` writes test data and does not push Git changes. Run it before `npm run dev`, which copies the test dataset into the app's ignored local public directory. Scheduled production scraping explicitly selects `prod` and is disabled until the deployment gate is enabled; a manual production scrape requires `publish=true`. A manual site-only publish uses `publish_site=true` and does not scrape data. Production scrapes write JSON only; publishing is handled separately by the workflow. The frontend build uses Node.js 24.11.1 and npm.

GitHub Actions is scheduled daily at 18:00 UTC (23:30 IST). Scheduled production scraping and deployment remain disabled until the repository variable `PAGES_DEPLOYMENT_ENABLED` is set to `true`. Setting that variable is optional and does not itself trigger a run.

See [architecture](docs/architecture.md), [deployment](docs/deployment.md), [adding apps and data](docs/adding-apps-and-data.md), the [restructure summary](docs/repo-restructure-summary.md), and the [personal-site execution plan](.github/modernize/personal-site/personal-site-plan.md).
