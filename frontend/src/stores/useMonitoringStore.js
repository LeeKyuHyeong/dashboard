import { create } from 'zustand'

const useMonitoringStore = create((set) => ({
  services: [],
  server: null,
  connected: false,
  setMonitoringData: (data) => set({ services: data.services, server: data.server }),
  setConnected: (connected) => set({ connected }),
}))

export default useMonitoringStore
