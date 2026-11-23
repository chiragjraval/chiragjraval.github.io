# NHB Residex data contract

The NHB pipeline writes generated datasets under `data/nhb-residex/{test,dev,prod}/`.
Test and development outputs are local-only and ignored by Git. Production
output is not ignored, so reviewed baseline data can be committed with the
feature. The pipeline itself never performs Git operations.

## Files

- `manifest.json` identifies the schema version, generation time, available
  cities and benchmarks, source names, file paths, date coverage, and SHA-256
  content hashes.
- `cities/<state-id>-<city-id>.json` contains the state/city identity and
  quarterly NHB observations.
- `benchmarks/<benchmark-id>.json` contains weekly index observations from the
  Screener `Index Price` dataset.

## Value semantics

- NHB composite and carpet-area price series are measured in INR per sq. ft.
  The API provides the composite price plus unit-size bands for <=60 sq. m.,
  >60 to <=110 sq. m., and >110 sq. m. Each quarter may omit unavailable bands.
  Zero-valued area-band responses are treated as unavailable and logged;
  negative or malformed prices fail collection.
- `quarterEnd` is an ISO-8601 date. For overlapping quarters, the observation
  from the newest base year wins: `2024-25`, then `2017-18`, then `2012-13`.
- `hfcIndex` is retained with the selected observation's `baseYear`; index
  values from different base years are not treated as one continuous series.
- Benchmark values are index points. Their dates remain weekly trading dates.
  The comparison app matches each NHB quarter to the latest benchmark date on
  or before that quarter end; it must not look ahead or interpolate.

The checked-in parser and sample-pipeline fixtures are synthetic test inputs,
not production observations. Run the pipeline in its default fixture mode to
create a local one-city test dataset. Use `--live` to make source requests;
production output additionally requires `--confirm-prod`.

Live HTTP requests are separated by at least 500 ms. HTTP 429, 5xx, and
transient I/O failures are retried up to five attempts with exponential
backoff and jitter; other 4xx responses fail immediately. The GitHub Actions
workflow is scheduled for Saturdays at 01:00 UTC (06:30 India time), but runs
only after this workflow is committed to the default branch, on `main`, and
when `PAGES_DEPLOYMENT_ENABLED=true`. Monitor the initial full-universe runs
for NHB throttling or incomplete responses.

NHB may return older-quarter rows without carpet-area prices from a newer
base-year endpoint. Those rows are logged and skipped so a valid observation
from an older base-year endpoint can be used; malformed numeric prices still
fail the collection.
