import { formatBytes, formatPercentage } from '../utils/format';

export default function MetricCard({ title, icon, type, value, total }) {
  let displayValue, displaySub, percentage;

  if (type === 'cpu') {
    percentage = value ?? 0;
    displayValue = formatPercentage(value);
    displaySub = null;
  } else {
    percentage = total > 0 ? (value / total) * 100 : 0;
    displayValue = formatBytes(value);
    displaySub = `/ ${formatBytes(total)}`;
  }

  const barColor = percentage > 90 ? 'var(--color-danger)' : percentage > 70 ? 'var(--color-warning)' : 'var(--color-success)';

  return (
    <div className="card metric-card">
      <div className="metric-card__header">
        <span className="metric-card__icon">{icon}</span>
        <h3 className="metric-card__title">{title}</h3>
      </div>

      <div className="metric-card__value-row">
        <span className="metric-card__value">{displayValue}</span>
        {displaySub && <span className="metric-card__sub">{displaySub}</span>}
      </div>

      <div className="progress-bar">
        <div
          className="progress-bar__fill"
          style={{ width: `${Math.min(percentage, 100)}%`, backgroundColor: barColor }}
        />
      </div>

      <span className="metric-card__percent">{formatPercentage(percentage)}</span>
    </div>
  );
}
