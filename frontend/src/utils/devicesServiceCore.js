export function adaptDevice(dto = {}) {
  return { ...dto, deviceId: dto.deviceId ?? dto.id ?? '', name: dto.name ?? dto.deviceId ?? dto.id ?? '', location: dto.location ?? '', status: dto.status ?? 'offline', enabled: dto.enabled ?? false, intervalSec: dto.intervalSec ?? 5, measurementTypes: dto.measurementTypes ?? dto.types ?? [] }
}

export const adaptDevicePayload = (payload) => Object.fromEntries(Object.entries(payload).filter(([key]) => key !== 'deviceId'))

export function createDevicesService({ mode = 'mock', api, seed = [] }) {
  let memory = seed.map((device) => ({ ...device }))
  const path = '/api/devices'
  return {
    mode,
    async list() {
      return mode === 'api' ? (await api.get(path)).data.map(adaptDevice) : memory.map((device) => ({ ...device }))
    },
    async create(data) {
      if (mode === 'api') return adaptDevice((await api.post(path, { deviceId: data.deviceId, ...adaptDevicePayload(data) })).data)
      if (memory.some((device) => device.deviceId === data.deviceId)) throw Object.assign(new Error('Cet identifiant existe déjà.'), { status: 409 })
      const row = { ...data, status: 'offline', firmware: 'sim-1.0.0', lastSeenAt: null }
      memory = [row, ...memory]
      return { ...row }
    },
    async update(id, data) {
      if (mode === 'api') return adaptDevice((await api.put(`${path}/${encodeURIComponent(id)}`, adaptDevicePayload(data))).data)
      const index = memory.findIndex((device) => device.deviceId === id)
      if (index < 0) throw Object.assign(new Error('Capteur introuvable.'), { status: 404 })
      memory[index] = { ...memory[index], ...data, deviceId: id }
      return { ...memory[index] }
    },
  }
}

export function apiErrorMessage(error) {
  const status = error.response?.status ?? error.status
  const server = error.response?.data?.message ?? error.response?.data?.error
  if (server) return server
  return (
    {
      400: "Les données envoyées sont invalides.",
      401: "Authentification requise par le serveur.",
      403: "Vous n’avez pas accès à cette opération.",
      404: "Capteur introuvable sur le serveur.",
      409: "Un capteur avec cet identifiant existe déjà.",
      500: "Erreur interne du serveur.",
    }[status] ??
    (error.code === "ECONNABORTED"
      ? "Le serveur met trop de temps à répondre. Vérifiez la connexion au backend."
      : "Impossible de joindre le backend. Vérifiez que Spring Boot est démarré.")
  );
}
