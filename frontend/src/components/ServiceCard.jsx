import { useState } from 'react';
import { formatUptime } from '../utils/format';
import LogModal from './LogModal';

export default function ServiceCard({ service }) {
  const [logOpen, setLogOpen] = useState(false);

  // 판정은 컨테이너 상태 단일. UNKNOWN(판정 불가)을 UP/DOWN 어느 쪽으로도 칠하지 않는다.
  const badgeClass =
    service.status === 'UP' ? 'badge--up'
    : service.status === 'UNKNOWN' ? 'badge--unknown'
    : 'badge--down';

  return (
    <>
      <div className="card service-card">
        <div className="service-card__header">
          <h3 className="service-card__name">{service.name}</h3>
          <span className={`badge ${badgeClass}`}>
            {service.status}
          </span>
        </div>

        <div className="service-card__metrics">
          <div className="service-card__row">
            <span className="service-card__label">Docker</span>
            <span className={`service-card__value ${
              service.dockerStatus === 'running' ? 'text-green'
              : service.dockerStatus === 'unknown' ? ''
              : 'text-red'}`}>
              {service.dockerStatus || '—'}
            </span>
          </div>
          <div className="service-card__row">
            <span className="service-card__label">가동 시간</span>
            <span className="service-card__value">
              {formatUptime(service.uptimeSeconds)}
            </span>
          </div>
        </div>

        <button className="btn btn--logs" onClick={() => setLogOpen(true)}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
          로그
        </button>
      </div>

      <LogModal
        isOpen={logOpen}
        onClose={() => setLogOpen(false)}
        containerName={service.containerName}
        serviceName={service.name}
      />
    </>
  );
}
