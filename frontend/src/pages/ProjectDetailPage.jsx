import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchProject } from '../api';
import ProjectHeader from '../components/ProjectHeader';
import TechStackTags from '../components/TechStackTags';
import AchievementCard from '../components/AchievementCard';

export default function ProjectDetailPage() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetchProject(slug)
      .then((data) => {
        setProject(data);
        setLoading(false);
      })
      .catch(() => {
        setError('Failed to load project.');
        setLoading(false);
      });
  }, [slug]);

  if (loading) {
    return (
      <div className="container">
        <div className="detail-loading">
          <div className="skeleton skeleton--title" style={{ width: '60%' }} />
          <div className="skeleton skeleton--text" style={{ width: '80%' }} />
          <div className="skeleton skeleton--text" style={{ width: '40%' }} />
        </div>
      </div>
    );
  }

  if (error || !project) {
    return (
      <div className="container">
        <div className="detail-error">
          <p>{error || 'Project not found.'}</p>
          <button className="btn btn--secondary" onClick={() => navigate('/')}>Back to Dashboard</button>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <button className="btn-back" onClick={() => navigate('/')}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" />
          <polyline points="12 19 5 12 12 5" />
        </svg>
        Back
      </button>

      <ProjectHeader project={project} />
      <TechStackTags techStack={project.techStack} />

      {project.achievements && project.achievements.length > 0 && (
        <section className="section">
          <h2 className="section-title">Achievements</h2>
          <div className="achievement-list">
            {project.achievements.map((ach, idx) => (
              <AchievementCard key={ach.id || idx} achievement={ach} index={idx} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
