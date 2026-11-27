import test from 'node:test'
import assert from 'node:assert/strict'
import { validateSensor } from '../src/utils/sensorValidation.js'
import { createDevicesService, apiErrorMessage } from '../src/utils/devicesServiceCore.js'

const valid = { deviceId: 'sensor-room-13', name: 'Salle 13', intervalSec: 5, measurementTypes: ['temperature'] }
const devices = [{ deviceId: valid.deviceId }]

test('accepts a valid new sensor and allows its ID when editing', () => {
  assert.deepEqual(validateSensor({ ...valid, deviceId: 'sensor-room-14' }, devices), {})
  assert.deepEqual(validateSensor(valid, devices, true), {})
})

test('rejects missing, malformed, duplicate IDs and missing names', () => {
  for (const deviceId of ['', 'Bad_ID', 'ab']) assert.ok(validateSensor({ ...valid, deviceId }, []).deviceId)
  assert.match(validateSensor(valid, devices).deviceId, /déjà utilisé/)
  assert.ok(validateSensor({ ...valid, name: '' }, []).name)
})

test('accepts only integer intervals from 1 through 3600 seconds', () => {
  for (const intervalSec of [0, 3601, 2.5, 'nope']) assert.ok(validateSensor({ ...valid, intervalSec }, []).intervalSec)
  for (const intervalSec of [1, 3600, '12']) assert.equal(validateSensor({ ...valid, intervalSec }, []).intervalSec, undefined)
})

test('mock device service lists, creates and updates devices in memory', async () => {
  const service = createDevicesService({ seed: [] })
  const device = { deviceId: 'sensor-room-13', name: 'Salle 13', intervalSec: 5, measurementTypes: ['temperature'] }
  const created = await service.create(device)
  assert.equal((await service.list()).length, 1)
  await service.update(created.id, { ...device, name: 'Salle serveur' })
  assert.equal((await service.list())[0].name, 'Salle serveur')
  assert.deepEqual((await service.list())[0].measurementTypes, ['temperature'])
  assert.equal((await service.setEnabled(created.id, false)).enabled, false)
  await assert.rejects(service.create(device), { status: 409 })
  await assert.rejects(service.update('unknown-device', device), { status: 404 })
  await service.remove(created.id)
  assert.equal((await service.list()).length, 0)
})

test('API device service sends POST and PUT and adapts DTO responses', async () => {
  const calls = []
  const api = {
    async get(path) { calls.push(['GET', path]); return { data: [{ id: 7, deviceId: 'sensor-room-13', status: 'ONLINE' }] } },
    async post(path, body) { calls.push(['POST', path, body]); return { data: { id: 7, ...body, status: 'ONLINE' } } },
    async put(path, body) { calls.push(['PUT', path, body]); return { data: { id: 7, deviceId: 'sensor-room-13', ...body } } },
    async patch(path, body) { calls.push(['PATCH', path, body]); return { data: { id: 7, deviceId: 'sensor-room-13', status: 'ONLINE', ...body } } },
    async delete(path) { calls.push(['DELETE', path]); return { status: 204 } },
  }
  const service = createDevicesService({ mode: 'api', api })
  assert.equal((await service.list())[0].deviceId, 'sensor-room-13')
  assert.equal((await service.list())[0].status, 'online')
  const row = { deviceId: 'sensor-room-13', name: 'Salle 13', intervalSec: 5 }
  await service.create(row)
  await service.update(7, row)
  await service.setEnabled(7, false)
  await service.remove(7)
  assert.deepEqual(calls.map(([method]) => method), ['GET', 'GET', 'POST', 'PUT', 'PATCH', 'DELETE'])
  assert.equal(calls[3][2].deviceId, 'sensor-room-13')
  assert.match(apiErrorMessage({ response: { status: 409 } }), /existe déjà/)
})
