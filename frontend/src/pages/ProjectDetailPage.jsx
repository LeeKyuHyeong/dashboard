import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchProject } from '../api';
import ProjectHeader from '../components/ProjectHeader';
import TechStackTags from '../components/TechStackTags';
import AchievementCard from '../components/AchievementCard';
import ScreenshotGallery from '../components/ScreenshotGallery';

// 갤러리 이미지: screenshotUrls(최대 3장)가 있으면 그것, 없으면 기존 thumbnailUrl 1장으로 대체.
// 운영 DB 에 screenshot_urls 가 아직 비어 있어도 상세가 빈 화면이 되지 않게 한다.
function screenshotsOf(project) {
  if (project.screenshotUrls && project.screenshotUrls.length > 0) return project.screenshotUrls;
  return project.thumbnailUrl ? [project.thumbnailUrl] : [];
}

export default function ProjectDetailPage() {
  const { slug } = useParams();
  const navigate = useNavigate();
  // 응답을 slug 와 함께 보관한다. 로딩 여부는 "현재 slug 의 응답이 아직 없다" 로 계산하므로
  // effect 안에서 setState 를 직접 부를 필요가 없고(react-hooks/set-state-in-effect),
  // slug 가 바뀐 뒤 늦게 도착한 이전 응답도 화면에 올라오지 않는다.
  const [result, setResult] = useState({ slug: null, project: null, error: null });

  useEffect(() => {
    let cancelled = false;
    fetchProject(slug)
      .then((data) => {
        if (!cancelled) setResult({ slug, project: data, error: null });
      })
      .catch(() => {
        if (!cancelled) setResult({ slug, project: null, error: '프로젝트를 불러오지 못했습니다.' });
      });
    return () => { cancelled = true; };
  }, [slug]);

  const loading = result.slug !== slug;
  const project = loading ? null : result.project;
  const error = loading ? null : result.error;

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
          <p>{error || '프로젝트를 찾을 수 없습니다.'}</p>
          <button className="btn btn--secondary" onClick={() => navigate('/')}>대시보드로 돌아가기</button>
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
        뒤로
      </button>

      <ProjectHeader project={project} />
      <ScreenshotGallery images={screenshotsOf(project)} name={project.name} />
      <TechStackTags techStack={project.techStack} />

      {project.achievements && project.achievements.length > 0 && (
        <section className="section">
          <h2 className="section-title">주요 성과</h2>
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
