import { ExternalLinkIcon, GitHubIcon } from './icons';

export default function ProjectHeader({ project }) {
  return (
    <div className="project-header">
      <h1 className="project-header__title">{project.name}</h1>
      <p className="project-header__desc">{project.description}</p>

      <div className="project-header__links">
        {project.demoUrl && (
          <a href={project.demoUrl} target="_blank" rel="noopener noreferrer" className="btn btn--primary">
            <ExternalLinkIcon />
            라이브 데모
          </a>
        )}
        {project.githubUrl && (
          <a href={project.githubUrl} target="_blank" rel="noopener noreferrer" className="btn btn--secondary">
            <GitHubIcon />
            GitHub
          </a>
        )}
      </div>
    </div>
  );
}
