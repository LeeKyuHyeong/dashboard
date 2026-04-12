export default function TechStackTags({ techStack }) {
  if (!techStack || techStack.length === 0) return null;

  return (
    <div className="tech-stack-section">
      <h2 className="section-title">기술 스택</h2>
      <div className="tag-list tag-list--lg">
        {techStack.map((tech) => (
          <span key={tech} className="tag tag--lg">{tech}</span>
        ))}
      </div>
    </div>
  );
}
