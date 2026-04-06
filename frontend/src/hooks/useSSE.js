import { useEffect, useRef } from 'react'
import useMonitoringStore from '../stores/useMonitoringStore'

export default function useSSE() {
  const setMonitoringData = useMonitoringStore((s) => s.setMonitoringData)
  const setConnected = useMonitoringStore((s) => s.setConnected)
  const reconnectTimer = useRef(null)

  useEffect(() => {
    let eventSource = null

    function connect() {
      eventSource = new EventSource('/api/monitoring/stream')

      eventSource.addEventListener('monitoring', (event) => {
        try {
          const data = JSON.parse(event.data)
          setMonitoringData(data)
        } catch (e) {
          console.error('Failed to parse monitoring event:', e)
        }
      })

      eventSource.addEventListener('open', () => {
        setConnected(true)
      })

      eventSource.addEventListener('error', () => {
        setConnected(false)
        eventSource.close()
        reconnectTimer.current = setTimeout(connect, 5000)
      })
    }

    connect()

    return () => {
      if (eventSource) {
        eventSource.close()
      }
      if (reconnectTimer.current) {
        clearTimeout(reconnectTimer.current)
      }
      setConnected(false)
    }
  }, [setMonitoringData, setConnected])
}
