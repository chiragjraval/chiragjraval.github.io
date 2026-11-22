# Build and data artifact contract

This contract separates website assets, generated datasets, and pipeline code
so independent jobs can build and publish them without replacing unrelated
content.

## Repository paths

| Output | Repository path | Publish path relative to `gh-pages` |
|---|---|---|
| Personal home and FDL landing | `public/` | `/` and `/financial-data-lab/` |
| Shared navigation registry | `shared/config/site.js` | `shared/config/site.js` |
| Shared shell and theme | `shared/ui/`, `shared/theme/` | `shared/ui/`, `shared/theme/` |
| Stock screener app build | `apps/stock-screener/build/` | `financial-data-lab/stock-screeners/` |
| Production stock-screener data | `data/stock-screener/prod/` | `data/stock-screener/prod/` |
| Test stock-screener data | `data/stock-screener/test/` | Never publish |

After the repository is renamed to `chiragjraval.github.io`, GitHub Pages serves
these paths from the domain root. Until the approved cutover, the existing
project-site deployment remains the live site.

## Artifact responsibilities

- The static pages in `public/` contain the personal home, Financial Data Lab
  landing page, and legacy-route transition pages.
- The app build contains only the stock screener's static files.
- The site artifact includes the personal pages, shared navigation/theme, and
  screener app at their target routes.
- The data artifact contains only production data; test data is excluded.
- The deployment workflow overlays only the root home, explicit application
  routes, shared assets, and data paths. A data-only update does not rebuild or
  replace frontend assets.

## Stock-screener JSON compatibility

Keep the current JSON schema while relocating it:

- `summary.json` has `totalCompanies`, `generatedAt`, and `companies`.
- Company details are named `<companyCode>.json`; they include metadata,
  attributes, score, quarterly results, and technical history.
- The frontend and pipeline must agree on the production data path. Test data
  must never be published.

## Safe local commands

```powershell
# Uses the Maven test default; writes only to the current test output path
mvn test

# Override the query while remaining in test mode
mvn -Denvironment=test "-DscreenerQueryLink=https://www.screener.in/screens/..." test
```

Production mode is explicit and reserved for the scheduled workflow unless
the user intentionally approves a production scrape.
