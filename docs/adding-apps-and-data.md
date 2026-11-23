# Adding apps and data

Keep application assets, generated data, and pipeline code in separate paths. Avoid a shared build or deployment step that replaces unrelated site content.

## Add an application

1. Create `apps/<app-id>/` with an independently buildable frontend and an app-scoped build output.
2. Add the route to `shared/config/site.js` and keep navigation labels and paths in that shared configuration.
3. Use the `fdl-site-shell` Web Component from `shared/ui/site-shell.js` and import `shared/theme/tokens.css` for the common site experience.
4. Add the app's source paths to `.github/workflows/stock-screener.yml`, build it independently, and place the build under its own artifact path.
5. Add a precise path scope to `.github/workflows/deploy.yml`. Replace only that app's published directory; do not clear the Pages root or another app's content.

Current app routes include `/financial-data-lab/stock-screeners/` and
`/financial-data-lab/nhb-residex/`. The public user-site root is not available
until the repository rename and approved Pages cutover.

## Add a pipeline and dataset

1. Put pipeline sources under `pipelines/<pipeline-id>/`; add a Maven module to the root `pom.xml` only when the pipeline uses Maven.
2. Write generated datasets under `data/<dataset-id>/{test,prod}`. Define and document the summary/detail JSON contract consumed by the corresponding app.
3. Keep local/test outputs ignored and out of published artifacts. Make production mode explicit; pipelines should write outputs, not perform Git operations or deployment.
4. Add focused tests and path-aware workflow jobs. Name artifacts by pipeline/dataset and deploy them to an exact path.
5. Extend the serialized publisher for the new scope while preserving existing apps and datasets. Do not grant write permissions to build or scrape jobs.

The NHB Residex implementation is an example of this separation: the Java
pipeline writes static JSON, the React app reads it, and its manual data
workflow publishes only the NHB production data path.
