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
