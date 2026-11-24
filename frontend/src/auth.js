import { useSyncExternalStore } from 'react'
import { api } from './servicesLayer'

const KEY = 'iot-cloud-session'
let session = (() => {
  try { return JSON.parse(localStorage.getItem(KEY)) } catch { return null }
})()
const listeners = new Set()
const publish = () => { localStorage.setItem(KEY, JSON.stringify(session)); listeners.forEach(fn => fn()) }
export const getSession = () => session
export const useSession = () => useSyncExternalStore(fn => { listeners.add(fn); return () => listeners.delete(fn) }, getSession)
export function clearSession() { session = null; localStorage.removeItem(KEY); listeners.forEach(fn => fn()) }
export async function login(username, password, mode) {
  if (mode === 'mock') {
    if (!username.trim() || !password) throw new Error('Saisissez un nom d’utilisateur et un mot de passe.')
    session = { accessToken: `demo-${Date.now()}`, tokenType: 'Bearer', expiresIn: null, username, role: 'Administrateur', demo: true }
  } else {
    const { data } = await api.post('/api/auth/login', { username, password })
    session = { ...data, demo: false }
  }
  publish()
  return session
}
api.interceptors.request.use(config => {
  const token = session?.accessToken
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
