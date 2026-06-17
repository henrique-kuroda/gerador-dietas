import type { BrazilRegion, Budget, Goal } from "./index";

export const GOAL_LABELS: Record<Goal, string> = {
  AGGRESSIVE_LOSS: "Perder peso — agressivo (-30%)",
  LOSE_WEIGHT: "Perder peso (-20%)",
  MAINTAIN: "Manter peso",
  GAIN_MUSCLE: "Ganhar massa (+12%)",
  AGGRESSIVE_GAIN: "Ganhar massa — agressivo (+20%)",
};

export const GOAL_LABELS_SHORT: Record<Goal, string> = {
  AGGRESSIVE_LOSS: "Cutting agressivo",
  LOSE_WEIGHT: "Perder peso",
  MAINTAIN: "Manter",
  GAIN_MUSCLE: "Ganhar massa",
  AGGRESSIVE_GAIN: "Bulking agressivo",
};

export const BUDGET_LABELS: Record<Budget, string> = {
  ECONOMICAL: "Econômico",
  MODERATE: "Moderado",
  UNRESTRICTED: "Livre",
};

export const REGION_LABELS: Record<BrazilRegion, string> = {
  NORTE: "Norte",
  NORDESTE: "Nordeste",
  CENTRO_OESTE: "Centro-Oeste",
  SUDESTE: "Sudeste",
  SUL: "Sul",
};
