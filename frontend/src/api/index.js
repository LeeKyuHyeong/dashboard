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
  // 로그 경로는 호스트 nginx 에서 관리 IP 로 제한돼 있다. 403 은 장애가 아니라 정책이므로
  // nginx 가 돌려주는 HTML 을 그대로 뿌리지 않고 안내로 바꾼다.
  if (res.status === 403) return '이 서버의 컨테이너 로그는 관리 IP 에서만 조회할 수 있습니다.'
  return res.text()
}
