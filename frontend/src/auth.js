import { useSyncExternalStore } from 'react'
import { api, dataMode } from './servicesLayer'

const API_SESSION_KEY = 'iot-cloud-session-api'
const MOCK_SESSION_KEY = 'iot-cloud-session-mock'
const LEGACY_SESSION_KEY = 'iot-cloud-session'
const sessionKey = dataMode === 'mock' ? MOCK_SESSION_KEY : API_SESSION_KEY
const listeners = new Set()

function readSession() {
  try {
    const saved = localStorage.getItem(sessionKey)
    if (saved) return JSON.parse(saved)

    // Keep an existing real API session during migration, but never import a demo token.
    if (dataMode === 'api') {
      const legacy = JSON.parse(localStorage.getItem(LEGACY_SESSION_KEY) || 'null')
      if (legacy && !legacy.demo && legacy.accessToken) {
        localStorage.setItem(API_SESSION_KEY, JSON.stringify(legacy))
        return legacy
      }
    }
  } catch {
    return null
  }
  return null
}

let session = readSession()
const publish = () => {
  if (session) localStorage.setItem(sessionKey, JSON.stringify(session))
  else localStorage.removeItem(sessionKey)
  listeners.forEach(listener => listener())
}

export const getSession = () => session
export const useSession = () => useSyncExternalStore(
  listener => { listeners.add(listener); return () => listeners.delete(listener) },
  getSession,
)

export function clearSession() {
  session = null
  publish()
}

export async function login(username, password) {
  if (dataMode === 'mock') {
    if (!username.trim() || !password) throw new Error('Saisissez un nom d’utilisateur et un mot de passe.')
    session = { username: username.trim(), role: 'Administrateur', demo: true }
  } else {
    const { data } = await api.post('/api/auth/login', { username, password })
    session = { ...data, demo: false }
  }
  publish()
  return session
}

if (dataMode === 'api') {
  api.interceptors.request.use(config => {
    const token = session && !session.demo ? session.accessToken : null
    if (token) config.headers.Authorization = `${session.tokenType || 'Bearer'} ${token}`
    return config
  })

  api.interceptors.response.use(response => response, error => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/api/auth/login')) {
      clearSession()
      if (window.location.pathname !== '/login') window.location.assign('/login')
    }
    return Promise.reject(error)
  })
}
