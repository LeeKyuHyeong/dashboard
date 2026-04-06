const API_BASE = '/api'

export async function fetchProjects() {
  const res = await fetch(`${API_BASE}/projects`)
  return res.json()
}

export async function fetchProject(slug) {
  const res = await fetch(`${API_BASE}/projects/${slug}`)
  return res.json()
}

export async function fetchLogs(containerName, tail = 100) {
  const res = await fetch(`${API_BASE}/monitoring/logs/${containerName}?tail=${tail}`)
  return res.text()
}
