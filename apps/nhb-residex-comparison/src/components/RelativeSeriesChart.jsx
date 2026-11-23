import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

const SYNC_ID = 'nhb-residex-date';

export default function RelativeSeriesChart({ data, housingSeries, benchmarkName }) {
  return (
    <div className="chart-frame" role="img" aria-label="NHB carpet-area price to selected NIFTY index ratios by quarter">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          data={data}
          margin={{ top: 12, right: 20, bottom: 12, left: 12 }}
          syncId={SYNC_ID}
          syncMethod="value"
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="quarterEnd"
            interval={0}
            tick={{ fontSize: 9 }}
            tickFormatter={formatQuarter}
            angle={-60}
            textAnchor="end"
            height={52}
          />
          <YAxis
            label={{ value: 'INR per sq. ft. per index point', angle: -90, position: 'insideLeft' }}
            width={82}
          />
          <Tooltip
            cursor={{ stroke: '#587086', strokeWidth: 1, strokeDasharray: '4 4' }}
            formatter={(value, name) => [Number(value).toFixed(4), name]}
            labelFormatter={(label, payload) => {
              const benchmarkDate = payload?.[0]?.payload?.benchmarkDate;
              return benchmarkDate
                ? `NHB quarter end: ${label}; ${benchmarkName} date: ${benchmarkDate}`
                : `NHB quarter end: ${label}`;
            }}
          />
          <Legend />
          {housingSeries.map((series) => (
            <Line
              key={series.id}
              type="monotone"
              dataKey={`ratio_${series.id}`}
              name={`${series.label} / ${benchmarkName}`}
              stroke={series.color}
              dot={{ r: 1.5 }}
              activeDot={{ r: 4 }}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

function formatQuarter(value) {
  const [year, month] = value.split('-');
  return `${year.slice(-2)} Q${Math.ceil(Number(month) / 3)}`;
}
