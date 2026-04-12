import { useEffect, useState, useRef } from 'react';
import { fetchLogs } from '../api';

export default function LogModal({ isOpen, onClose, containerName, serviceName }) {
  const [logs, setLogs] = useState('');
  const [loading, setLoading] = useState(false);
  const overlayRef = useRef(null);
  const logEndRef = useRef(null);

  useEffect(() => {
    if (!isOpen || !containerName) return;

    setLoading(true);
    setLogs('');
    fetchLogs(containerName)
      .then((text) => {
        setLogs(text);
        setLoading(false);
      })
      .catch(() => {
        setLogs('로그를 불러오지 못했습니다.');
        setLoading(false);
      });
  }, [isOpen, containerName]);

  useEffect(() => {
    if (logEndRef.current) {
      logEndRef.current.scrollIntoView();
    }
  }, [logs]);

  useEffect(() => {
    if (!isOpen) return;
    function handleKey(e) {
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  function handleOverlayClick(e) {
    if (e.target === overlayRef.current) onClose();
  }

  return (
    <div className="modal-overlay" ref={overlayRef} onClick={handleOverlayClick}>
      <div className="modal">
        <div className="modal__header">
          <h3 className="modal__title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="4 17 10 11 4 5" />
              <line x1="12" y1="19" x2="20" y2="19" />
            </svg>
            {serviceName || containerName} 로그
          </h3>
          <button className="modal__close" onClick={onClose}>&times;</button>
        </div>

        <div className="modal__body">
          {loading ? (
            <div className="modal__loading">로그 불러오는 중...</div>
          ) : (
            <pre className="modal__logs">
              <code>{logs || '로그가 없습니다.'}</code>
              <span ref={logEndRef} />
            </pre>
          )}
        </div>
      </div>
    </div>
  );
}
