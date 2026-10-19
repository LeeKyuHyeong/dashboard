import { useState, useEffect } from 'react';
import useSSE from '../hooks/useSSE';
import useMonitoringStore from '../stores/useMonitoringStore';
import { fetchProjects } from '../api';
import MetricCard from '../components/MetricCard';
import ProjectCard from '../components/ProjectCard';
import { GitHubIcon } from '../components/icons';

// 상단 소개. 자기소개 섹션은 두지 않는다 — 프로젝트가 곧 자기소개 (CLAUDE.md).
const SITE = {
  title: 'kyuhyeong.com',
  tagline: '직접 만들고 운영 중인 서비스들 — 지금 이 순간의 상태 그대로',
  githubUrl: 'https://github.com/LeeKyuHyeong',
};

export default function MainPage() {
  useSSE();
  const services = useMonitoringStore((s) => s.services);
  const server = useMonitoringStore((s) => s.server);
  const connected = useMonitoringStore((s) => s.connected);

  const [projects, setProjects] = useState([]);
  const [tab, setTab] = useState('projects');

  useEffect(() => {
    fetchProjects()
      .then(setProjects)
      .catch((err) => console.error('Failed to fetch projects:', err));
  }, []);

  // 서비스 상태(SSE, projectSlug 키)를 프로젝트(slug)에 붙인다.
  const serviceBySlug = {};
  services.forEach((svc) => {
    if (svc.projectSlug) serviceBySlug[svc.projectSlug] = svc;
  });

  return (
    <div className="container">
      <header className="page-header">
        <div className="page-header__intro">
          <h1 className="page-header__title">{SITE.title}</h1>
          <p className="page-header__tagline">{SITE.tagline}</p>
        </div>
        <div className="page-header__side">
          <a href={SITE.githubUrl} target="_blank" rel="noopener noreferrer" className="icon-link">
            <GitHubIcon /> GitHub
          </a>
          <div className="page-header__status">
            <span className={`status-dot ${connected ? 'status-dot--connected' : 'status-dot--disconnected'}`} />
            {connected ? '실시간' : '연결 끊김'}
          </div>
        </div>
      </header>

      <div className="tabs" role="tablist">
        <button
          role="tab"
          aria-selected={tab === 'projects'}
          className={`tab ${tab === 'projects' ? 'tab--active' : ''}`}
          onClick={() => setTab('projects')}
        >
          프로젝트
        </button>
        <button
          role="tab"
          aria-selected={tab === 'metrics'}
          className={`tab ${tab === 'metrics' ? 'tab--active' : ''}`}
          onClick={() => setTab('metrics')}
        >
          서버 메트릭
        </button>
      </div>

      {tab === 'projects' && (
        <section className="section">
          <div className="grid grid--projects">
            {projects.length > 0
              ? projects.map((proj) => (
                  <ProjectCard key={proj.slug} project={proj} service={serviceBySlug[proj.slug]} />
                ))
              : [0, 1].map((i) => (
                  <div key={i} className="card card--skeleton">
                    <div className="skeleton skeleton--title" />
                    <div className="skeleton skeleton--text" />
                    <div className="skeleton skeleton--text" />
                  </div>
                ))
            }
          </div>
        </section>
      )}

      {tab === 'metrics' && (
        <section className="section">
          <div className="grid grid--3">
            {server ? (
              <>
                <MetricCard
                  title="CPU"
                  icon="&#x1F4BB;"
                  type="cpu"
                  value={server.cpuUsage}
                />
                <MetricCard
                  title="메모리"
                  icon="&#x1F9E0;"
                  type="memory"
                  value={server.memoryUsedBytes}
                  total={server.memoryTotalBytes}
                />
                <MetricCard
                  title="디스크"
                  icon="&#x1F4BE;"
                  type="disk"
                  value={server.diskUsedBytes}
                  total={server.diskTotalBytes}
                />
              </>
            ) : (
              [0, 1, 2].map((i) => (
                <div key={i} className="card card--skeleton">
                  <div className="skeleton skeleton--title" />
                  <div className="skeleton skeleton--bar" />
                </div>
              ))
            )}
          </div>
        </section>
      )}
    </div>
  );
}
