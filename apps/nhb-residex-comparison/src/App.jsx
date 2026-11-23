import { useEffect, useMemo, useState } from 'react';
import { alignSeries, HOUSING_SERIES } from './utils/alignSeries.js';
import { loadBenchmark, loadCity, loadManifest } from './utils/dataClient.js';
import RawSeriesChart from './components/RawSeriesChart.jsx';
import RelativeSeriesChart from './components/RelativeSeriesChart.jsx';
import RebasedSeriesChart from './components/RebasedSeriesChart.jsx';
import './App.css';

const dataBaseUrl = import.meta.env.VITE_NHB_DATA_BASE_URL || '/nhb-residex/';

export default function App() {
  const [manifest, setManifest] = useState(null);
  const [manifestError, setManifestError] = useState('');
  const [stateId, setStateId] = useState('');
  const [cityFile, setCityFile] = useState('');
  const [benchmarkId, setBenchmarkId] = useState('');
  const [selectedSeriesIds, setSelectedSeriesIds] = useState(['composite']);
  const [series, setSeries] = useState(null);
  const [seriesError, setSeriesError] = useState('');
  const [loadingManifest, setLoadingManifest] = useState(true);
  const [loadingSeries, setLoadingSeries] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    loadManifest(dataBaseUrl, controller.signal)
      .then((value) => {
        setManifest(value);
        const firstCity = value.cities[0];
        const firstBenchmark = value.benchmarks[0];
        if (firstCity) {
          setStateId(firstCity.stateId);
          setCityFile(firstCity.file);
        }
        if (firstBenchmark) {
          setBenchmarkId(firstBenchmark.id);
        }
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setManifestError(error.message);
        }
      })
      .finally(() => setLoadingManifest(false));

    return () => controller.abort();
  }, []);

  const states = useMemo(() => {
    if (!manifest) {
      return [];
    }
    return [...new Map(
      manifest.cities.map((city) => [city.stateId, { id: city.stateId, name: city.stateName }]),
    ).values()].sort((left, right) => left.name.localeCompare(right.name));
  }, [manifest]);

  const cities = useMemo(
    () => manifest?.cities
      .filter((city) => city.stateId === stateId)
      .sort((left, right) => left.cityName.localeCompare(right.cityName)) ?? [],
    [manifest, stateId],
  );
  const selectedCity = manifest?.cities.find((city) => city.file === cityFile);
  const selectedBenchmark = manifest?.benchmarks.find(
    (benchmark) => benchmark.id === benchmarkId,
  );
  const selectedSeries = HOUSING_SERIES.filter((item) => selectedSeriesIds.includes(item.id));
  const availableSeries = selectedSeries.filter((item) => (
    series?.aligned.some((point) => point[`price_${item.id}`] !== null)
  ));

  useEffect(() => {
    if (!selectedCity || !selectedBenchmark) {
      setSeries(null);
      return undefined;
    }

    const controller = new AbortController();
    setLoadingSeries(true);
    setSeriesError('');
    setSeries(null);
    Promise.all([
      loadCity(dataBaseUrl, selectedCity.file, controller.signal),
      loadBenchmark(dataBaseUrl, selectedBenchmark.file, controller.signal),
    ])
      .then(([cityData, benchmarkData]) => {
        setSeries({
          cityData,
          benchmarkData,
          aligned: alignSeries(cityData.observations, benchmarkData.observations),
        });
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setSeriesError(error.message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoadingSeries(false);
        }
      });

    return () => controller.abort();
  }, [selectedCity?.file, selectedBenchmark?.file]);

  function changeState(nextStateId) {
    setStateId(nextStateId);
    const firstCity = manifest?.cities
      .filter((city) => city.stateId === nextStateId)
      .sort((left, right) => left.cityName.localeCompare(right.cityName))[0];
    setCityFile(firstCity?.file ?? '');
  }

  function toggleSeries(seriesId) {
    setSelectedSeriesIds((current) => (
      current.includes(seriesId)
        ? current.filter((id) => id !== seriesId)
        : [...current, seriesId]
    ));
  }

  return (
    <div className="site-page nhb-page">
      <header className="nhb-heading">
        <p className="page-eyebrow">Financial Data Lab</p>
        <h1>NHB Residex comparison</h1>
        <p className="page-intro">
          Compare quarterly NHB city prices with weekly NIFTY index observations.
          Each quarter uses the latest benchmark value on or before its end date.
        </p>
      </header>

      {loadingManifest && <p className="nhb-notice" role="status">Loading available cities and benchmarks…</p>}
      {manifestError && <p className="nhb-notice nhb-error" role="alert">Could not load comparison data: {manifestError}</p>}

      {manifest && (
        <>
          <section className="nhb-controls" aria-label="Comparison options">
            <div>
              <label className="form-label" htmlFor="nhb-state">State</label>
              <select
                className="form-select"
                id="nhb-state"
                value={stateId}
                onChange={(event) => changeState(event.target.value)}
              >
                {states.map((state) => (
                  <option key={state.id} value={state.id}>{state.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label" htmlFor="nhb-city">City</label>
              <select
                className="form-select"
                id="nhb-city"
                value={cityFile}
                onChange={(event) => setCityFile(event.target.value)}
                disabled={cities.length === 0}
              >
                {cities.map((city) => (
                  <option key={city.file} value={city.file}>{city.cityName}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label" htmlFor="nhb-benchmark">NIFTY index</label>
              <select
                className="form-select"
                id="nhb-benchmark"
                value={benchmarkId}
                onChange={(event) => setBenchmarkId(event.target.value)}
              >
                {manifest.benchmarks.map((benchmark) => (
                  <option key={benchmark.id} value={benchmark.id}>{benchmark.name}</option>
                ))}
              </select>
            </div>
            <fieldset className="nhb-series-picker">
              <legend className="form-label">Housing price series</legend>
              <p className="nhb-muted">
                Select one or more carpet-area price series. All values are in INR per sq. ft.
              </p>
              <div className="nhb-series-options">
                {HOUSING_SERIES.map((item) => (
                  <label className="nhb-series-option" key={item.id}>
                    <input
                      type="checkbox"
                      checked={selectedSeriesIds.includes(item.id)}
                      onChange={() => toggleSeries(item.id)}
                    />
                    <span>{item.label}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          </section>

          {loadingSeries && <p className="nhb-notice" role="status">Loading selected series…</p>}
          {seriesError && <p className="nhb-notice nhb-error" role="alert">Could not load the selected series: {seriesError}</p>}
          {!loadingSeries && !seriesError && series?.aligned.length === 0 && (
            <p className="nhb-notice" role="status">
              No benchmark observation is available on or before the selected city series.
            </p>
          )}
          {!loadingSeries && selectedSeriesIds.length === 0 && (
            <p className="nhb-notice" role="status">
              Select at least one housing price series to display the charts.
            </p>
          )}
          {!loadingSeries && selectedSeriesIds.length > 0 && series?.aligned.length > 0
            && availableSeries.length === 0 && (
              <p className="nhb-notice" role="status">
                No observations are available for the selected housing price series.
              </p>
            )}

          {series?.aligned.length > 0 && availableSeries.length > 0 && (
            <>
              <p className="nhb-muted">
                {selectedCity.cityName}, {selectedCity.stateName} · {selectedBenchmark.name}
                {' · '}Relative comparison starts {series.aligned[0].quarterEnd}
              </p>
              <div className="nhb-chart-grid">
                <section className="nhb-chart-card" aria-labelledby="relative-chart-heading">
                  <h2 id="relative-chart-heading">Relative performance</h2>
                  <p className="nhb-muted">
                    A rising ratio means housing prices are outperforming the selected market
                    index; a falling ratio means the market index is outperforming housing.
                  </p>
                  <p className="nhb-muted">
                    Each selected housing price series is divided by the matched index value.
                    No rebasing is applied.
                  </p>
                  <RelativeSeriesChart
                    data={series.aligned}
                    housingSeries={availableSeries}
                    benchmarkName={selectedBenchmark.name}
                  />
                </section>
                <section className="nhb-chart-card" aria-labelledby="raw-chart-heading">
                  <h2 id="raw-chart-heading">Raw values</h2>
                  <p className="nhb-muted">
                    Selected NHB prices and matched index values share one raw-value axis.
                    Units differ, so use the line labels and tooltips; housing labels include
                    each area range.
                  </p>
                  <RawSeriesChart
                    data={series.aligned}
                    housingSeries={availableSeries}
                    benchmarkName={selectedBenchmark.name}
                  />
                </section>
                <section className="nhb-chart-card" aria-labelledby="rebased-chart-heading">
                  <h2 id="rebased-chart-heading">Rebased values</h2>
                  <p className="nhb-muted">
                    Each selected housing series and the selected index starts at 100 at its
                    first available quarter.
                  </p>
                  <RebasedSeriesChart
                    data={series.aligned}
                    housingSeries={availableSeries}
                    benchmarkName={selectedBenchmark.name}
                  />
                </section>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
