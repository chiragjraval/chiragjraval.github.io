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
import ChartFrame from './ChartFrame.jsx';
import {
  CHART_MARGIN,
  TOOLTIP_CONTENT_STYLE,
  TOOLTIP_POSITION,
  TOOLTIP_WRAPPER_STYLE,
  Y_AXIS_TICK,
} from './chartOptions.js';

const SYNC_ID = 'nhb-residex-date';

export default function RelativeSeriesChart({ data, housingSeries, benchmarkName }) {
  return (
    <ChartFrame
      label="INR/sq. ft. per index point"
      ariaLabel="NHB carpet-area price to selected NIFTY index ratios by quarter"
    >
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          data={data}
          margin={CHART_MARGIN}
          syncId={SYNC_ID}
          syncMethod="value"
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="quarterEnd"
            tickFormatter={formatQuarter}
            angle={-45}
            textAnchor="end"
            height={50}
          />
          <YAxis width={50} tick={Y_AXIS_TICK} />
          <Tooltip
            cursor={{ stroke: '#587086', strokeWidth: 1, strokeDasharray: '4 4' }}
            position={TOOLTIP_POSITION}
            contentStyle={TOOLTIP_CONTENT_STYLE}
            wrapperStyle={TOOLTIP_WRAPPER_STYLE}
            formatter={(value, name) => [Number(value).toFixed(4), name]}
            labelFormatter={(label, payload) => {
              const benchmarkDate = payload?.[0]?.payload?.benchmarkDate;
              return benchmarkDate
                ? `Q end: ${label} · ${benchmarkName}: ${benchmarkDate}`
                : `Q end: ${label}`;
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
    </ChartFrame>
  );
}

function formatQuarter(value) {
  const [year, month] = value.split('-');
  return `${year.slice(-2)} Q${Math.ceil(Number(month) / 3)}`;
}
