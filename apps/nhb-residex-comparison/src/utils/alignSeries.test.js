import assert from 'node:assert/strict';
import test from 'node:test';
import { alignSeries } from './alignSeries.js';

test('aligns multiple NHB price series with the latest benchmark and rebases them independently', () => {
  const aligned = alignSeries(
    [
      {
        quarterEnd: '2025-06-30',
        weightedAverageComposite: 5000,
        weightedAverageLessThan60: 7000,
      },
      {
        quarterEnd: '2025-09-30',
        weightedAverageComposite: 6000,
        weightedAverageLessThan60: 8400,
      },
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
  assert.equal(aligned[0].price_composite, 5000);
  assert.equal(aligned[0].ratio_composite, 0.2);
  assert.equal(aligned[1].ratio_composite, 6000 / 27500);
  assert.equal(aligned[1].rebased_composite, 120);
  assert.equal(aligned[0].ratio_upTo646, 7000 / 25000);
  assert.equal(aligned[1].rebased_upTo646, 120);
  assert.ok(Math.abs(aligned[1].benchmarkRebased - 110) < 1e-10);
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

test('keeps a quarter when only one selected area-price series is available', () => {
  const aligned = alignSeries(
    [{
      quarterEnd: '2025-06-30',
      weightedAverageComposite: null,
      weightedAverage60To110: 6000,
    }],
    [{ date: '2025-06-27', value: 25000 }],
  );

  assert.equal(aligned.length, 1);
  assert.equal(aligned[0].price_composite, null);
  assert.equal(aligned[0].ratio_composite, null);
  assert.equal(aligned[0].price_646To1184, 6000);
  assert.equal(aligned[0].ratio_646To1184, 0.24);
  assert.equal(aligned[0].rebased_646To1184, 100);
});
