# Build and data artifact contract

This contract separates application assets, generated datasets, and the portal so independent workflows can build and publish them without replacing unrelated content.

## Repository paths

| Output | Repository path | Publish path relative to `gh-pages` |
|---|---|---|
| Portal | `public/` | Site root |
| Stock screener app build | `apps/stock-screener/build/` | `stock-screeners/` |
| Production stock-screener data | `data/stock-screener/prod/` | `data/stock-screener/prod/` |
| Test stock-screener data | `data/stock-screener/test/` | Never publish |

GitHub Pages is a project site, so URLs include the repository base path (`/daily_custom_stock_screener/`) before these relative paths.

The site root is the application portal; the screener is published at `/stock-screeners/` and loads production JSON from `/data/stock-screener/prod/`.

## Artifact responsibilities

- The portal build contains only root landing-page files.
- The app build contains only that app's static files.
- A pipeline artifact contains only its production dataset directory; test data is excluded.
- `stock-screener-site` contains the portal, shared theme/configuration, and screener app; `stock-screener-data-prod` contains only the production dataset.
- The deployment workflow overlays the artifact's publish path onto `gh-pages`, preserves files for other apps and datasets, and serializes branch updates.
- A data-only update does not rebuild or replace frontend assets.

## Stock-screener JSON compatibility

Keep the current JSON schema while relocating it:

- `summary.json` has `totalCompanies`, `generatedAt`, and `companies`.
- Company details are named `<companyCode>.json`; they include metadata, attributes, score, quarterly results, and technical history.
- The frontend and pipeline must agree on the production data path. Test data must never be published.

## Safe local commands

```powershell
# Uses the Maven test default; writes only to the current test output path
mvn test

# Override the query while remaining in test mode
mvn -Denvironment=test "-DscreenerQueryLink=https://www.screener.in/screens/..." test
```

Production mode is explicit and reserved for the scheduled workflow unless the user intentionally requests a local production run.
