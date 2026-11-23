function joinBase(baseUrl, path) {
  return `${baseUrl.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`;
}

export async function fetchJson(url, signal, fetchImplementation = fetch) {
  const response = await fetchImplementation(url, { signal });
  if (!response.ok) {
    throw new Error(`Could not load ${url}: HTTP ${response.status}`);
  }

  try {
    return await response.json();
  } catch (error) {
    throw new Error(`Invalid JSON from ${url}`, { cause: error });
  }
}

export async function loadManifest(baseUrl, signal, fetchImplementation = fetch) {
  const manifest = await fetchJson(joinBase(baseUrl, 'manifest.json'), signal, fetchImplementation);
  if (
    manifest.schemaVersion !== 1
    || !Array.isArray(manifest.cities)
    || !Array.isArray(manifest.benchmarks)
  ) {
    throw new Error('The NHB dataset manifest has an unsupported shape');
  }
  return manifest;
}

export function loadCity(baseUrl, file, signal, fetchImplementation = fetch) {
  return fetchJson(joinBase(baseUrl, file), signal, fetchImplementation);
}

export function loadBenchmark(baseUrl, file, signal, fetchImplementation = fetch) {
  return fetchJson(joinBase(baseUrl, file), signal, fetchImplementation);
}
