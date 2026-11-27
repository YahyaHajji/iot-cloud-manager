export function adaptDevice(dto = {}) {
  return { ...dto, id: dto.id, deviceId: dto.deviceId ?? '', name: dto.name ?? dto.deviceId ?? '', location: dto.location ?? '', description: dto.description ?? '', status: (dto.status ?? 'unknown').toLowerCase(), enabled: dto.enabled ?? false, intervalSec: dto.intervalSec ?? 5, measurementTypes: dto.measurementTypes ?? dto.types ?? [], lastMeasurements: dto.lastMeasurements ?? [] }
}

export const adaptDevicePayload = ({ deviceId, name, intervalSec, location, description }) => ({ deviceId, name, intervalSec: Number(intervalSec), location: location ?? '', description: description ?? '' })

export function createDevicesService({ mode = 'mock', api, seed = [] }) {
  let memory = seed.map((device, index) => ({ id: index + 1, ...device }))
  const path = '/api/devices'
  const copy = value => ({
    ...value,
    measurementTypes: [...(value.measurementTypes ?? [])],
    lastMeasurements: (value.lastMeasurements ?? []).map(measurement => ({ ...measurement })),
  })
  return {
    mode,
    async list() { return mode === 'api' ? (await api.get(path)).data.map(adaptDevice) : memory.map(copy) },
    async create(data) {
      const body = adaptDevicePayload(data)
      if (mode === 'api') return adaptDevice((await api.post(path, body)).data)
      if (memory.some(device => device.deviceId === body.deviceId)) throw Object.assign(new Error('Cet identifiant existe déjà.'), { status: 409 })
      const row = adaptDevice({ ...body, id: Math.max(0, ...memory.map(x => Number(x.id) || 0)) + 1, status: 'unknown', enabled: data.enabled ?? true, firmware: 'sim-1.0.0', measurementTypes: data.measurementTypes ?? [], lastSeenAt: null, lastMeasurements: [] })
      memory = [row, ...memory]; return copy(row)
    },
    async update(id, data) {
      const body = adaptDevicePayload(data)
      if (mode === 'api') return adaptDevice((await api.put(`${path}/${encodeURIComponent(id)}`, body)).data)
      const index = memory.findIndex(device => device.id === id)
      if (index < 0) throw Object.assign(new Error('Capteur introuvable.'), { status: 404 })
      memory[index] = adaptDevice({ ...memory[index], ...body }); return copy(memory[index])
    },
    async remove(id) {
      if (mode === 'api') return api.delete(`${path}/${encodeURIComponent(id)}`)
      const index = memory.findIndex(device => device.id === id)
      if (index < 0) throw Object.assign(new Error('Capteur introuvable.'), { status: 404 })
      memory = memory.filter(device => device.id !== id)
    },
    async setEnabled(id, enabled) {
      if (mode === 'api') return api.patch(`${path}/${encodeURIComponent(id)}/enabled`, { enabled })
      const index = memory.findIndex(device => device.id === id)
      if (index < 0) throw Object.assign(new Error('Capteur introuvable.'), { status: 404 })
      memory[index] = { ...memory[index], enabled }; return copy(memory[index])
    },
  }
}

export function apiErrorMessage(error) {
  const status = error.response?.status ?? error.status
  const server = error.response?.data?.message ?? error.response?.data?.error
  if (server) return server
  return ({ 400: 'Les données envoyées sont invalides.', 401: 'Authentification requise par le serveur.', 403: 'Vous n’avez pas accès à cette opération.', 404: 'Capteur introuvable sur le serveur.', 409: 'Un capteur avec cet identifiant existe déjà.', 500: 'Erreur interne du serveur.' }[status] ?? (error.code === 'ECONNABORTED' ? 'Le serveur met trop de temps à répondre. Vérifiez la connexion au backend.' : error.message || 'Impossible de joindre le backend. Vérifiez que Spring Boot est démarré.'))
}
