export default function AchievementCard({ achievement, index }) {
  return (
    <div className="card achievement-card">
      <div className="achievement-card__number">{String(index + 1).padStart(2, '0')}</div>
      <div className="achievement-card__content">
        <h3 className="achievement-card__title">{achievement.title}</h3>
        <p className="achievement-card__desc">{achievement.description}</p>
        {achievement.metricValue && (
          <div className="achievement-card__metric">{achievement.metricValue}</div>
        )}
      </div>
    </div>
  );
}
