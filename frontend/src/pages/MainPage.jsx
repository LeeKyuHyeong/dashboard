import { useState, useEffect } from 'react';
import useSSE from '../hooks/useSSE';
import useMonitoringStore from '../stores/useMonitoringStore';
import { fetchProjects } from '../api';
import ServiceCard from '../components/ServiceCard';
import MetricCard from '../components/MetricCard';
import ProjectCard from '../components/ProjectCard';

export default function MainPage() {
  useSSE();
  const services = useMonitoringStore((s) => s.services);
  const server = useMonitoringStore((s) => s.server);
  const connected = useMonitoringStore((s) => s.connected);

  const [projects, setProjects] = useState([]);

  useEffect(() => {
    fetchProjects()
      .then(setProjects)
      .catch((err) => console.error('Failed to fetch projects:', err));
  }, []);

  return (
    <div className="container">
      <header className="page-header">
        <h1 className="page-header__title">대시보드</h1>
        <div className="page-header__status">
          <span className={`status-dot ${connected ? 'status-dot--connected' : 'status-dot--disconnected'}`} />
          {connected ? '실시간' : '연결 끊김'}
        </div>
      </header>

      {/* Service Status */}
      <section className="section">
        <h2 className="section-title">서비스 상태</h2>
        <div className="grid grid--3">
          {services.length > 0
            ? services.map((svc) => <ServiceCard key={svc.name} service={svc} />)
            : [0, 1, 2].map((i) => (
                <div key={i} className="card card--skeleton">
                  <div className="skeleton skeleton--title" />
                  <div className="skeleton skeleton--text" />
                  <div className="skeleton skeleton--text" />
                </div>
              ))
          }
        </div>
      </section>

      {/* Server Metrics */}
      <section className="section">
        <h2 className="section-title">서버 메트릭</h2>
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

      {/* Projects */}
      <section className="section">
        <h2 className="section-title">프로젝트</h2>
        <div className="grid grid--3">
          {projects.map((proj) => (
            <ProjectCard key={proj.slug} project={proj} />
          ))}
        </div>
      </section>
    </div>
  );
}
