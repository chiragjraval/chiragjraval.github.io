function positiveNumber(value, label) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${label} must be a positive number`);
  }
  return parsed;
}

function validDate(value, label) {
  if (
    typeof value !== 'string'
    || !/^\d{4}-\d{2}-\d{2}$/.test(value)
    || Number.isNaN(Date.parse(`${value}T00:00:00Z`))
    || new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) !== value
  ) {
    throw new Error(`${label} must be a valid ISO date`);
  }
  return value;
}

export function alignSeries(cityObservations, benchmarkObservations) {
  if (!Array.isArray(cityObservations) || !Array.isArray(benchmarkObservations)) {
    throw new Error('City and benchmark observations must be arrays');
  }

  const city = cityObservations
    .map((item) => ({
      quarterEnd: validDate(item.quarterEnd, 'City quarterEnd'),
      value: positiveNumber(item.weightedAverageComposite, 'City price'),
    }))
    .sort((left, right) => left.quarterEnd.localeCompare(right.quarterEnd));
  const benchmark = benchmarkObservations
    .map((item) => ({
      date: validDate(item.date, 'Benchmark date'),
      value: positiveNumber(item.value, 'Benchmark value'),
    }))
    .sort((left, right) => left.date.localeCompare(right.date));

  const aligned = [];
  let benchmarkIndex = -1;
  for (const cityPoint of city) {
    while (
      benchmarkIndex + 1 < benchmark.length
      && benchmark[benchmarkIndex + 1].date <= cityPoint.quarterEnd
    ) {
      benchmarkIndex += 1;
    }

    if (benchmarkIndex < 0) {
      continue;
    }

    const benchmarkPoint = benchmark[benchmarkIndex];
    aligned.push({
      quarterEnd: cityPoint.quarterEnd,
      benchmarkDate: benchmarkPoint.date,
      housingPrice: cityPoint.value,
      benchmarkValue: benchmarkPoint.value,
    });
  }

  if (aligned.length === 0) {
    return [];
  }

  return aligned.map((point) => {
    return {
      ...point,
      ratio: point.housingPrice / point.benchmarkValue,
    };
  });
}
