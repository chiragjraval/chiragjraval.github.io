function positiveNumber(value, label) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${label} must be a positive number`);
  }
  return parsed;
}

function optionalPositiveNumber(value, label) {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  return positiveNumber(value, label);
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

export const HOUSING_SERIES = Object.freeze([
  {
    id: 'composite',
    field: 'weightedAverageComposite',
    label: 'Composite price',
    color: '#145da0',
  },
  {
    id: 'upTo646',
    field: 'weightedAverageLessThan60',
    label: '<=60 sq. m. (<=646 sq. ft.)',
    color: '#b04a8b',
  },
  {
    id: '646To1184',
    field: 'weightedAverage60To110',
    label: '>60 to <=110 sq. m. (646-1,184 sq. ft.)',
    color: '#d07824',
  },
  {
    id: 'over1184',
    field: 'weightedAverageGreaterThan110',
    label: '>110 sq. m. (>1,184 sq. ft.)',
    color: '#147d70',
  },
]);

export function alignSeries(cityObservations, benchmarkObservations) {
  if (!Array.isArray(cityObservations) || !Array.isArray(benchmarkObservations)) {
    throw new Error('City and benchmark observations must be arrays');
  }

  const city = cityObservations
    .map((item) => {
      const point = {
        quarterEnd: validDate(item.quarterEnd, 'City quarterEnd'),
      };
      for (const series of HOUSING_SERIES) {
        point[`price_${series.id}`] = optionalPositiveNumber(
          item[series.field],
          `City ${series.label}`,
        );
      }
      return point;
    })
    .filter((point) => HOUSING_SERIES.some((series) => point[`price_${series.id}`] !== null))
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
      ...cityPoint,
      benchmarkDate: benchmarkPoint.date,
      benchmarkValue: benchmarkPoint.value,
    });
  }

  if (aligned.length === 0) {
    return [];
  }

  const firstPriceBySeries = Object.fromEntries(HOUSING_SERIES.map((series) => [
    series.id,
    aligned.find((point) => point[`price_${series.id}`] !== null)?.[`price_${series.id}`] ?? null,
  ]));
  const firstBenchmarkValue = aligned[0].benchmarkValue;

  return aligned.map((point) => {
    const result = {
      ...point,
      benchmarkRebased: (point.benchmarkValue / firstBenchmarkValue) * 100,
    };
    for (const series of HOUSING_SERIES) {
      const price = point[`price_${series.id}`];
      const firstPrice = firstPriceBySeries[series.id];
      result[`ratio_${series.id}`] = price === null ? null : price / point.benchmarkValue;
      result[`rebased_${series.id}`] = price === null ? null : (price / firstPrice) * 100;
    }
    return result;
  });
}
