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

  // 이 경로는 호스트 nginx 에서 차단돼 있다(현재 return 404, 관리 IP 허용 시 403).
  // 앱이 답한 실패는 text/plain(502 + 진단 문구)이고, nginx 가 답한 차단은 HTML 이다.
  // HTML 을 로그 창에 그대로 뿌리지 않고 정책 안내로 바꾼다.
  const isPlainText = (res.headers.get('content-type') || '').includes('text/plain')
  if (!res.ok && !isPlainText) {
    return '이 서버의 컨테이너 로그는 서버에서 직접 조회하도록 차단돼 있습니다. (docker logs <컨테이너명>)'
  }
  return res.text()
}
