import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

export default function RelativeSeriesChart({ data }) {
  return (
    <div className="chart-frame" role="img" aria-label="Raw NHB housing price divided by the matched benchmark index value">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 12, right: 16, bottom: 12, left: 12 }}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="quarterEnd" minTickGap={24} />
          <YAxis
            label={{ value: 'INR per sq. ft. per index point', angle: -90, position: 'insideLeft' }}
            width={82}
          />
          <Tooltip
            formatter={(value) => [Number(value).toFixed(4), 'Housing price / index value']}
            labelFormatter={(label) => `NHB quarter end: ${label}`}
          />
          <Line
            type="monotone"
            dataKey="ratio"
            name="Housing price / index value"
            stroke="#147d70"
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
