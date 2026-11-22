# Architecture

The repository is a static financial analytics site with independent application, data, and pipeline areas. The current stock screener is the only application; the structure is intended to let future tools use their own builds and datasets without coupling them to it.

```text
public/                         Root portal
shared/config + shared/theme    Portal registry and shared design tokens
apps/stock-screener/            React/Vite user interface
             |                  Fetches JSON; no application backend
             v
data/stock-screener/{test,prod} Generated summary and company-detail JSON
             ^
pipelines/stock-screener/       Java 25 / Maven scraper
```

## Responsibilities

- **Portal:** `public/` is published at the project-site root. It reads `shared/config/site.json` and links to the listed applications.
- **Applications:** `apps/stock-screener/` owns its React components, charts, npm dependencies, and Vite build. Its published route is `/stock-screeners/`.
- **Shared assets:** `shared/theme/tokens.css` is consumed by both the portal and screener. `shared/config/site.json` is the portal's application registry. Keep app-specific components in their application until another app actually uses them.
- **Pipeline and data:** `pipelines/stock-screener/` is a Maven module aggregated by the root `pom.xml`. It writes JSON to `data/stock-screener/{test,prod}`; it does not commit or publish data.
- **Deployment:** `.github/workflows/stock-screener.yml` builds tests, frontend, and data artifacts. `.github/workflows/deploy.yml` overlays selected artifacts onto the existing `gh-pages` branch.

## Data interface

The screener reads `summary.json` and the referenced `details/<companyCode>.json` files. The summary contains `totalCompanies`, `generatedAt`, and `companies`; detail objects preserve the metadata, attributes, score, quarterly results, and technical history used by the current UI.

Development uses the test dataset copied into the app's ignored public directory by `npm run predev`. Production builds use the project-site URL under `/data/stock-screener/prod/`. Test data is never included in the frontend build or a deployment artifact.

## Build boundaries

- `mvn test` runs the Java unit tests and the default test-mode scrape.
- `npm run dev` / `npm run build` in `apps/stock-screener` run the frontend independently.
- The Maven root is an aggregator, not the owner of the frontend lifecycle.
