import { api } from "./api";
import type { DietPlanResponse, DietPlanSummary, Page } from "../types";

export async function generateDiet(): Promise<DietPlanResponse> {
  const { data } = await api.post<DietPlanResponse>("/api/diet/generate");
  return data;
}

export async function listDiets(): Promise<DietPlanSummary[]> {
  const { data } = await api.get<Page<DietPlanSummary>>("/api/diet", {
    params: { size: 50, sort: "createdAt,desc" },
  });
  return data.content;
}

export async function deleteDiet(id: number): Promise<void> {
  await api.delete(`/api/diet/${id}`);
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
