import axios from "axios";
import { mockDevices } from "./data/mockData";
import {
  createDevicesService,
  apiErrorMessage,
} from "./utils/devicesServiceCore";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 10000,
  headers: { "Content-Type": "application/json" },
});
export const devicesService = createDevicesService({
  mode: import.meta.env.VITE_DATA_MODE || "mock",
  api,
  seed: mockDevices,
});
export { apiErrorMessage };
