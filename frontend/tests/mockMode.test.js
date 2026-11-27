import test from 'node:test'
import assert from 'node:assert/strict'
import process from 'node:process'
import { createServer } from 'vite'

test('mock services, CRUD and demo auth work without making Backend requests', async () => {
  const originalMode = process.env.VITE_DATA_MODE
  process.env.VITE_DATA_MODE = 'mock'
  const entries = new Map([
    ['iot-cloud-session-api', JSON.stringify({ accessToken: 'real-jwt', username: 'api-user' })],
  ])
  globalThis.localStorage = {
    getItem: key => entries.get(key) ?? null,
    setItem: (key, value) => entries.set(key, String(value)),
    removeItem: key => entries.delete(key),
  }

  const vite = await createServer({
    mode: 'mock',
    logLevel: 'silent',
    server: { middlewareMode: true },
    appType: 'custom',
  })
  try {
    const services = await vite.ssrLoadModule('/src/servicesLayer.js')
    const auth = await vite.ssrLoadModule('/src/auth.js')
    const calls = []
    services.api.defaults.adapter = async config => {
      calls.push(config.url)
      throw new Error('Unexpected Backend request in mock mode')
    }

    assert.equal(services.dataMode, 'mock')
    assert.equal(services.devicesService.mode, 'mock')
    const initialDevices = await services.devicesService.list()
    assert.ok(initialDevices.length > 0)
    assert.ok(initialDevices.every(device => Number.isInteger(device.id) && device.deviceId && device.status && Array.isArray(device.lastMeasurements)))

    const created = await services.devicesService.create({
      deviceId: 'sensor-smoke-99', name: 'Capteur de test', intervalSec: 10,
      enabled: true, measurementTypes: ['temperature'],
    })
    assert.equal((await services.devicesService.list()).length, initialDevices.length + 1)
    assert.equal((await services.devicesService.update(created.id, { ...created, name: 'Capteur modifié' })).name, 'Capteur modifié')
    assert.equal((await services.devicesService.setEnabled(created.id, false)).enabled, false)
    await services.devicesService.remove(created.id)
    assert.equal((await services.devicesService.list()).length, initialDevices.length)

    const types = await services.measurementsService.types()
    assert.ok(types.every(item => typeof item.type === 'string' && typeof item.unit === 'string' && item.enabled === true))
    const measurements = await services.measurementsService.byDevice(initialDevices[0].deviceId)
    assert.ok(measurements.length > 0)
    assert.ok(measurements.every(item => item.id && item.deviceId && item.measuredAt && item.receivedAt))
    assert.deepEqual(
      await services.measurementsService.query(initialDevices[0].deviceId, { type: measurements[0].type }),
      measurements.filter(item => item.type === measurements[0].type),
    )
    assert.ok((await services.measurementsService.query(initialDevices[0].deviceId, {
      type: measurements[0].type,
      start: measurements[0].measuredAt,
      end: measurements[0].measuredAt,
    })).some(item => item.id === measurements[0].id))

    const alerts = await services.alertsService.list()
    assert.ok(alerts.length > 0)
    assert.ok(alerts.every(alert => alert.id && alert.deviceId && alert.type && alert.value !== undefined && alert.unit && alert.severity && alert.triggeredAt))
    assert.ok((await services.alertsService.byDevice(alerts[0].deviceId)).every(alert => alert.deviceId === alerts[0].deviceId))

    assert.equal(auth.getSession(), null)
    await auth.login('demo', 'demo')
    assert.equal(auth.getSession().demo, true)
    assert.equal(auth.getSession().accessToken, undefined)
    assert.equal(JSON.parse(entries.get('iot-cloud-session-api')).accessToken, 'real-jwt')
    auth.clearSession()
    assert.equal(entries.has('iot-cloud-session-mock'), false)
    assert.deepEqual(calls, [])
  } finally {
    await vite.close()
    if (originalMode === undefined) delete process.env.VITE_DATA_MODE
    else process.env.VITE_DATA_MODE = originalMode
    delete globalThis.localStorage
  }
})

test('API mode uses the REST services and real JWT session without mock fallback', async () => {
  const originalMode = process.env.VITE_DATA_MODE
  process.env.VITE_DATA_MODE = 'api'
  const entries = new Map([['iot-cloud-session-mock', JSON.stringify({ demo: true, username: 'demo' })]])
  globalThis.localStorage = {
    getItem: key => entries.get(key) ?? null,
    setItem: (key, value) => entries.set(key, String(value)),
    removeItem: key => entries.delete(key),
  }

  const vite = await createServer({ mode: 'api', logLevel: 'silent', server: { middlewareMode: true }, appType: 'custom' })
  try {
    const services = await vite.ssrLoadModule('/src/servicesLayer.js')
    const auth = await vite.ssrLoadModule('/src/auth.js')
    const calls = []
    const history = [{ id: 12, deviceId: 'sensor-api-01', type: 'temperature', value: 22, unit: '°C', measuredAt: '2026-01-01T10:00:00', receivedAt: '2026-01-01T10:00:01' }]
    services.api.defaults.adapter = async config => {
      calls.push({ method: config.method.toUpperCase(), url: config.url, authorization: config.headers.Authorization })
      if (config.url === '/api/auth/login') return { data: { accessToken: 'jwt-real', tokenType: 'Bearer', username: 'api-user' }, status: 200, statusText: 'OK', headers: {}, config }
      if (config.url === '/api/devices') return { data: [{ id: 9, deviceId: 'sensor-api-01', status: 'ONLINE', enabled: true, lastMeasurements: [] }], status: 200, statusText: 'OK', headers: {}, config }
      if (config.url === '/api/alerts') return { data: [{ id: 3, deviceId: 'sensor-api-01', type: 'temperature', value: 22, unit: '°C', severity: 'WARNING', triggeredAt: '2026-01-01T10:00:00' }], status: 200, statusText: 'OK', headers: {}, config }
      if (config.url.includes('/api/alerts/device/')) return { data: [], status: 200, statusText: 'OK', headers: {}, config }
      if (config.url === '/api/measurement-types') return { data: [{ type: 'temperature', unit: '°C', enabled: true }], status: 200, statusText: 'OK', headers: {}, config }
      if (config.url === '/api/measurements/device/sensor-api-01') return { data: history, status: 200, statusText: 'OK', headers: {}, config }
      if (config.url.includes('/type/temperature')) return { data: history, status: 200, statusText: 'OK', headers: {}, config }
      if (config.url.includes('/period?')) return { data: history, status: 200, statusText: 'OK', headers: {}, config }
      if (config.url.includes('/api/alerts/')) return { data: [], status: 200, statusText: 'OK', headers: {}, config }
      throw Object.assign(new Error('API request failed'), { config, response: { status: 500, data: {} } })
    }

    assert.equal(services.dataMode, 'api')
    assert.equal(auth.getSession(), null)
    await auth.login('api-user', 'password')
    assert.equal(auth.getSession().accessToken, 'jwt-real')
    assert.equal((await services.devicesService.list())[0].status, 'online')
    assert.equal((await services.alertsService.list())[0].severity, 'WARNING')
    assert.equal((await services.alertsService.byDevice('sensor-api-01')).length, 0)
    assert.equal((await services.measurementsService.types())[0].type, 'temperature')
    assert.equal((await services.measurementsService.byDevice('sensor-api-01'))[0].measuredAt, history[0].measuredAt)
    await services.measurementsService.query('sensor-api-01', { type: 'temperature' })
    await services.measurementsService.query('sensor-api-01', { start: '2026-01-01T00:00', end: '2026-01-01T23:59' })
    await assert.rejects(services.api.get('/api/unavailable'))
    assert.equal(entries.has('iot-cloud-session-mock'), true)
    assert.ok(calls.filter(call => call.url !== '/api/auth/login').every(call => call.authorization === 'Bearer jwt-real'))
    assert.ok(calls.some(call => call.url === '/api/measurements/device/sensor-api-01/type/temperature'))
    assert.ok(calls.some(call => call.url.startsWith('/api/measurements/device/sensor-api-01/period?')))
  } finally {
    await vite.close()
    if (originalMode === undefined) delete process.env.VITE_DATA_MODE
    else process.env.VITE_DATA_MODE = originalMode
    delete globalThis.localStorage
  }
})
