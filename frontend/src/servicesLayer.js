import axios from "axios";
import { mockDevices, mockMeasurements, mockAlerts, measurementCatalog } from "./data/mockData";
import {
  createDevicesService,
  apiErrorMessage,
} from "./utils/devicesServiceCore";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
export const dataMode = import.meta.env.VITE_DATA_MODE === "mock" ? "mock" : "api";

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: { "Content-Type": "application/json" },
});
export const devicesService = createDevicesService({
  mode: dataMode, api, seed: mockDevices,
});
const mode = dataMode;

const mockAlertRows = mockAlerts.map(alert => ({
  id: alert.id,
  deviceId: alert.deviceId,
  deviceName: mockDevices.find(device => device.deviceId === alert.deviceId)?.name ?? alert.deviceId,
  type: alert.type,
  value: alert.value,
  unit: alert.unit,
  severity: alert.severity || "WARNING",
  triggeredAt: alert.triggeredAt,
}));

export const alertsService = {
  mode,
  async list() {
    if (mode === "mock") return mockAlertRows;
    const { data } = await api.get('/api/alerts');
    return Array.isArray(data) ? data : data.content ?? data.alerts ?? [];
  },
  async byDevice(deviceId) {
    if (mode === "mock") return mockAlertRows.filter(alert => alert.deviceId === deviceId);
    const { data } = await api.get(`/api/alerts/device/${encodeURIComponent(deviceId)}`);
    return Array.isArray(data) ? data : data.content ?? data.alerts ?? [];
  },
};

export const measurementsService = {
  mode,
  async types() {
    if (mode === "mock") return Object.entries(measurementCatalog).map(([type, values]) => ({
      type, unit: values.unit, minValue: values.min, maxValue: values.max, enabled: true,
    }));
    const { data } = await api.get('/api/measurement-types');
    return Array.isArray(data) ? data : data.content ?? data.types ?? [];
  },
  async byDevice(deviceId) {
    if (mode === "mock") return mockMeasurements.flatMap((row, rowIndex) => row.deviceId === deviceId ? row.measurements.map((measurement, index) => ({
      id: rowIndex * 10 + index + 1,
      messageId: row.messageId,
      deviceId,
      type: measurement.type,
      value: measurement.value,
      unit: measurement.unit,
      measuredAt: row.timestamp,
      receivedAt: row.timestamp,
    })) : []).sort((a, b) => a.measuredAt.localeCompare(b.measuredAt));
    const { data } = await api.get(`/api/measurements/device/${encodeURIComponent(deviceId)}`);
    return Array.isArray(data) ? data : data.content ?? data.measurements ?? [];
  },
  async query(deviceId, { type, start, end } = {}) {
    let rows;
    if (mode === 'mock') {
      rows = await this.byDevice(deviceId);
      if (start) rows = rows.filter(m => new Date(m.measuredAt).getTime() >= new Date(start).getTime());
      if (end) rows = rows.filter(m => new Date(m.measuredAt).getTime() <= new Date(end).getTime());
    } else if (start || end) {
      const params = new URLSearchParams({ start: start ?? '', end: end ?? '' });
      const { data } = await api.get(`/api/measurements/device/${encodeURIComponent(deviceId)}/period?${params}`);
      rows = Array.isArray(data) ? data : data.content ?? data.measurements ?? [];
    } else if (type) {
      const { data } = await api.get(`/api/measurements/device/${encodeURIComponent(deviceId)}/type/${encodeURIComponent(type)}`);
      rows = Array.isArray(data) ? data : data.content ?? data.measurements ?? [];
    } else rows = await this.byDevice(deviceId);
    return rows.filter(m => !type || m.type === type)
      .map(m => ({ ...m, deviceId, measuredAt: m.measuredAt ?? m.timestamp }))
      .sort((a, b) => a.measuredAt.localeCompare(b.measuredAt));
  },
};
export { apiErrorMessage };
