import { useNavigate } from 'react-router-dom';
import { formatUptime } from '../utils/format';
import { ExternalLinkIcon, GitHubIcon } from './icons';

// 메인 통합 카드. 썸네일은 작게 줄이면 무슨 화면인지 알아볼 수 없어 2026-10-19 에 카드에서 뺐다 —
// 스크린샷은 상세 페이지 갤러리(ScreenshotGallery)에서 크게 보여준다.
export default function ProjectCard({ project, service }) {
  const navigate = useNavigate();

  // 판정은 컨테이너 상태 단일. UNKNOWN(판정 불가)을 UP/DOWN 어느 쪽으로도 칠하지 않는다.
  const badgeClass =
    service?.status === 'UP' ? 'badge--up'
    : service?.status === 'UNKNOWN' ? 'badge--unknown'
    : 'badge--down';

  const dockerClass =
    service?.dockerStatus === 'running' ? 'text-green'
    : service?.dockerStatus === 'unknown' ? ''
    : 'text-red';

  return (
    <article className="card project-card">
      <div className="project-card__header">
        <h3 className="project-card__name">{project.name}</h3>
        {service && (
          <span className={`badge ${badgeClass}`}>{service.status}</span>
        )}
      </div>

      <p className="project-card__desc">{project.description}</p>

      {project.techStack && project.techStack.length > 0 && (
        <div className="tag-list">
          {project.techStack.map((tech) => (
            <span key={tech} className="tag">{tech}</span>
          ))}
        </div>
      )}

      {service && (
        <dl className="project-card__status">
          <div className="project-card__status-item">
            <dt>Docker</dt>
            <dd className={dockerClass}>{service.dockerStatus || '—'}</dd>
          </div>
          <div className="project-card__status-item">
            <dt>가동 시간</dt>
            <dd>{formatUptime(service.uptimeSeconds)}</dd>
          </div>
        </dl>
      )}

      <div className="project-card__actions">
        <div className="project-card__links">
          {project.demoUrl && (
            <a href={project.demoUrl} target="_blank" rel="noopener noreferrer" className="icon-link">
              <ExternalLinkIcon /> Live
            </a>
          )}
          {project.githubUrl && (
            <a href={project.githubUrl} target="_blank" rel="noopener noreferrer" className="icon-link">
              <GitHubIcon /> GitHub
            </a>
          )}
        </div>
        <button className="btn btn--primary" onClick={() => navigate(`/projects/${project.slug}`)}>
          상세 &rarr;
        </button>
      </div>
    </article>
  );
}
