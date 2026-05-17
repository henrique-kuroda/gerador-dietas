import { api } from "./api";
import type { DietPlanResponse } from "../types";

export async function generateDiet(): Promise<DietPlanResponse> {
  const { data } = await api.post<DietPlanResponse>("/api/diet/generate");
  return data;
}

export async function listDiets(): Promise<DietPlanResponse[]> {
  const { data } = await api.get<DietPlanResponse[]>("/api/diet");
  return data;
}

export async function getDiet(id: number): Promise<DietPlanResponse> {
  const { data } = await api.get<DietPlanResponse>(`/api/diet/${id}`);
  return data;
}

export async function downloadDietPdf(id: number): Promise<void> {
  const response = await api.get<Blob>(`/api/diet/${id}/pdf`, {
    responseType: "blob",
  });
  const url = window.URL.createObjectURL(response.data);
  const link = document.createElement("a");
  link.href = url;
  link.download = `dieta-${id}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
