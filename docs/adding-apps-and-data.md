# Adding apps and data

Keep application assets, generated data, and pipeline code in separate paths. Avoid a shared build or deployment step that replaces unrelated site content.

## Add an application

1. Create `apps/<app-id>/` with an independently buildable frontend and an app-scoped build output.
2. Add an entry to `shared/config/site.json`. Its `path` is relative to the project-site root and should point to the app's published route.
3. Import `shared/theme/tokens.css` if the app uses the shared theme. Move a component into shared code only after multiple apps genuinely need it.
4. Add the app's source paths to `.github/workflows/stock-screener.yml`, build it independently, and place the build under its own artifact path.
5. Add a precise path scope to `.github/workflows/deploy.yml`. Replace only that app's published directory; do not clear the Pages root or another app's content.

> TODO: When the next app is introduced, add its build/artifact and exact deployment scope. Do not add placeholder app code or data in advance.

## Add a pipeline and dataset

1. Put pipeline sources under `pipelines/<pipeline-id>/`; add a Maven module to the root `pom.xml` only when the pipeline uses Maven.
2. Write generated datasets under `data/<dataset-id>/{test,prod}`. Define and document the summary/detail JSON contract consumed by the corresponding app.
3. Keep local/test outputs ignored and out of published artifacts. Make production mode explicit; pipelines should write outputs, not perform Git operations or deployment.
4. Add focused tests and path-aware workflow jobs. Name artifacts by pipeline/dataset and deploy them to an exact path.
5. Extend the serialized publisher for the new scope while preserving existing apps and datasets. Do not grant write permissions to build or scrape jobs.

> TODO: Before adding a new production dataset, decide whether its refresh schedule and deployment gate can share the stock-screener workflow or need a separate workflow.
