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

export default function RawSeriesChart({ data, benchmarkName }) {
  return (
    <div className="chart-frame" role="img" aria-label="Raw NHB housing price and benchmark values by quarter">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 12, right: 12, bottom: 12, left: 12 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="quarterEnd" minTickGap={24} />
          <YAxis
            yAxisId="housing"
            label={{ value: 'INR per sq. ft.', angle: -90, position: 'insideLeft' }}
            width={72}
          />
          <YAxis
            yAxisId="benchmark"
            orientation="right"
            label={{ value: 'Index points', angle: 90, position: 'insideRight' }}
            width={72}
          />
          <Tooltip
            formatter={(value, name) => [
              Number(value).toLocaleString('en-IN', { maximumFractionDigits: 2 }),
              name,
            ]}
            labelFormatter={(label, payload) => {
              const benchmarkDate = payload?.[0]?.payload?.benchmarkDate;
              return benchmarkDate
                ? `NHB quarter end: ${label}; benchmark date: ${benchmarkDate}`
                : label;
            }}
          />
          <Legend />
          <Line
            yAxisId="housing"
            type="monotone"
            dataKey="housingPrice"
            name="NHB price (INR per sq. ft.)"
            stroke="#145da0"
            dot={false}
          />
          <Line
            yAxisId="benchmark"
            type="monotone"
            dataKey="benchmarkValue"
            name={`${benchmarkName} (index points)`}
            stroke="#d07824"
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
