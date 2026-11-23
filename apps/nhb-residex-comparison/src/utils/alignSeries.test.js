import assert from 'node:assert/strict';
import test from 'node:test';
import { alignSeries } from './alignSeries.js';

test('matches the latest benchmark point on or before each NHB quarter and divides raw values', () => {
  const aligned = alignSeries(
    [
      { quarterEnd: '2025-06-30', weightedAverageComposite: 5000 },
      { quarterEnd: '2025-09-30', weightedAverageComposite: 6000 },
    ],
    [
      { date: '2025-06-27', value: 25000 },
      { date: '2025-07-04', value: 26000 },
      { date: '2025-09-26', value: 27500 },
    ],
  );

  assert.equal(aligned.length, 2);
  assert.equal(aligned[0].benchmarkDate, '2025-06-27');
  assert.equal(aligned[1].benchmarkDate, '2025-09-26');
  assert.equal(aligned[0].ratio, 0.2);
  assert.equal(aligned[1].ratio, 6000 / 27500);
});

test('does not use a benchmark observation after the NHB quarter', () => {
  const aligned = alignSeries(
    [{ quarterEnd: '2018-03-31', weightedAverageComposite: 4800 }],
    [{ date: '2018-04-06', value: 10000 }],
  );

  assert.deepEqual(aligned, []);
});

test('rejects invalid or zero prices rather than plotting them as valid data', () => {
  assert.throws(
    () => alignSeries(
      [{ quarterEnd: '2025-06-30', weightedAverageComposite: 0 }],
      [{ date: '2025-06-27', value: 25000 }],
    ),
    /positive number/,
  );
});
