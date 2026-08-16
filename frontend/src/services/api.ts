import axios, { AxiosError } from "axios";
import type { ApiError } from "../types";
import { clearToken, getToken } from "./token";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const api = axios.create({ baseURL });

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401 && getToken()) {
      clearToken();
      // Recarrega para que o auth context detecte o logout e mande para /login.
      if (window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);

export function hasApiStatus(err: unknown, status: number): boolean {
  return axios.isAxiosError<ApiError>(err) && err.response?.status === status;
}

export function extractApiErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(err) && err.response?.data?.message) {
    return err.response.data.message;
  }
  return fallback;
}
