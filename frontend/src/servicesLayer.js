import axios from "axios";
import { mockDevices, mockMeasurements } from "./data/mockData";
import {
  createDevicesService,
  apiErrorMessage,
} from "./utils/devicesServiceCore";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: { "Content-Type": "application/json" },
});
export const devicesService = createDevicesService({
  mode: import.meta.env.VITE_DATA_MODE ?? "api", api, seed: mockDevices,
});
const mode = import.meta.env.VITE_DATA_MODE ?? "api";

export const measurementsService = {
  mode,
  async types() {
    if (mode === "mock") return (await import('./data/mockData')).measurementTypes;
    const { data } = await api.get('/api/measurement-types');
    return Array.isArray(data) ? data : data.content ?? data.types ?? [];
  },
  async byDevice(deviceId) {
    if (mode === "mock") return mockMeasurements.flatMap(row => row.deviceId === deviceId ? row.measurements.map(m => ({ ...m, deviceId, measuredAt: row.timestamp })) : []);
    const { data } = await api.get(`/api/measurements/device/${encodeURIComponent(deviceId)}`);
    return Array.isArray(data) ? data : data.content ?? data.measurements ?? [];
  },
  async query(deviceId, { type, start, end } = {}) {
    let rows;
    if (mode === 'mock') {
      rows = await this.byDevice(deviceId);
      if (start) rows = rows.filter(m => m.measuredAt >= start);
      if (end) rows = rows.filter(m => m.measuredAt <= end);
    } else if (start || end) {
      const params = new URLSearchParams({ start: start ?? '', end: end ?? '' });
      const { data } = await api.get(`/api/measurements/device/${encodeURIComponent(deviceId)}/period?${params}`);
      rows = Array.isArray(data) ? data : data.content ?? data.measurements ?? [];
    } else if (type) {
      const { data } = await api.get(`/api/measurements/device/${encodeURIComponent(deviceId)}/type/${encodeURIComponent(type)}`);
      rows = Array.isArray(data) ? data : data.content ?? data.measurements ?? [];
    } else rows = await this.byDevice(deviceId);
    return rows.filter(m => !type || m.type === type).map(m => ({ ...m, deviceId, measuredAt: m.measuredAt ?? m.timestamp }));
  },
};
export { apiErrorMessage };
