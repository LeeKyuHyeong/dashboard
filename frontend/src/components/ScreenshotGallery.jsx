import { useEffect, useState } from 'react';

const MAX_IMAGES = 3;

// 상세 페이지 스크린샷 갤러리(최대 3장). 클릭하면 원본 크기로 확대해 보여준다.
export default function ScreenshotGallery({ images, name }) {
  const [openIndex, setOpenIndex] = useState(null);
  const list = (images || []).filter(Boolean).slice(0, MAX_IMAGES);

  useEffect(() => {
    if (openIndex === null) return;
    function handleKey(e) {
      if (e.key === 'Escape') setOpenIndex(null);
      if (e.key === 'ArrowRight') setOpenIndex((i) => (i + 1) % list.length);
      if (e.key === 'ArrowLeft') setOpenIndex((i) => (i - 1 + list.length) % list.length);
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [openIndex, list.length]);

  if (list.length === 0) return null;

  return (
    <section className="section">
      <h2 className="section-title">화면</h2>
      <div className={`gallery gallery--${list.length}`}>
        {list.map((src, idx) => (
          <button
            key={src}
            type="button"
            className="gallery__item"
            onClick={() => setOpenIndex(idx)}
            aria-label={`${name} 화면 ${idx + 1} 크게 보기`}
          >
            <img src={src} alt={`${name} 화면 ${idx + 1}`} loading="lazy" />
          </button>
        ))}
      </div>

      {openIndex !== null && (
        <div className="lightbox" onClick={() => setOpenIndex(null)} role="dialog" aria-modal="true">
          <img
            className="lightbox__img"
            src={list[openIndex]}
            alt={`${name} 화면 ${openIndex + 1}`}
            onClick={(e) => e.stopPropagation()}
          />
          {list.length > 1 && (
            <div className="lightbox__counter">{openIndex + 1} / {list.length}</div>
          )}
          <button type="button" className="lightbox__close" onClick={() => setOpenIndex(null)} aria-label="닫기">
            &times;
          </button>
        </div>
      )}
    </section>
  );
}
