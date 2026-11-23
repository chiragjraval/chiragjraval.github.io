export default function ChartFrame({ label, ariaLabel, children }) {
  return (
    <div className="chart-frame">
      <p className="chart-y-label">{label}</p>
      <div className="chart-canvas" role="img" aria-label={ariaLabel}>
        {children}
      </div>
    </div>
  );
}
