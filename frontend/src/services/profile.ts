import axios from "axios";
import { api } from "./api";
import type { ProfileRequest, ProfileResponse } from "../types";

export async function getProfile(): Promise<ProfileResponse | null> {
  try {
    const { data } = await api.get<ProfileResponse>("/api/profile");
    return data;
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.status === 404) {
      return null;
    }
    throw err;
  }
}

export async function saveProfile(
  request: ProfileRequest
): Promise<ProfileResponse> {
  const { data } = await api.put<ProfileResponse>("/api/profile", request);
  return data;
}
