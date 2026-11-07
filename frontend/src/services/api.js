import axios from "axios";
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 10000,
  headers: { "Content-Type": "application/json" },
});
export function adaptDevice(dto = {}) {
  return {
    ...dto,
    deviceId: dto.deviceId ?? dto.id ?? "",
    name: dto.name ?? dto.deviceId ?? dto.id ?? "",
    location: dto.location ?? "",
    status: dto.status ?? "offline",
    enabled: dto.enabled ?? false,
    intervalSec: dto.intervalSec ?? 5,
    measurementTypes: dto.measurementTypes ?? dto.types ?? [],
  };
}
export const adaptDevicePayload = ({ deviceId, ...rest }) => rest;
