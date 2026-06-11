export type Sex = "MALE" | "FEMALE";

export type ActivityLevel =
  | "SEDENTARY"
  | "LIGHT"
  | "MODERATE"
  | "ACTIVE"
  | "VERY_ACTIVE";

export type Goal =
  | "AGGRESSIVE_LOSS"
  | "LOSE_WEIGHT"
  | "MAINTAIN"
  | "GAIN_MUSCLE"
  | "AGGRESSIVE_GAIN";

export type Formula = "HARRIS_BENEDICT" | "MIFFLIN_ST_JEOR" | "KATCH_MCARDLE";

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  expiresIn: number;
}

export interface MeResponse {
  id: number;
  name: string;
  email: string;
}

export interface ProfileRequest {
  weightKg: number;
  heightCm: number;
  age: number;
  sex: Sex;
  activityLevel: ActivityLevel;
  goal: Goal;
  dietaryRestrictions?: string | null;
  mealsPerDay: number;
  bodyFatPercent?: number | null;
}

export type ProfileResponse = ProfileRequest;

export interface DietMealItem {
  food: string;
  portion: string;
  calories: number;
}

export interface DietMeal {
  name: string;
  calories: number;
  items: DietMealItem[];
}

export interface DietMacros {
  proteinG: number;
  carbsG: number;
  fatG: number;
}

export interface DietContent {
  summary: string;
  totalCalories: number;
  meals: DietMeal[];
  macros: DietMacros;
}

export interface DietPlanResponse {
  id: number;
  tmb: number;
  tdee: number;
  targetCalories: number;
  formulaUsed: Formula;
  content: DietContent;
  createdAt: string;
  /** Perfil no momento da geração; null em planos antigos. */
  profileSnapshot: ProfileResponse | null;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}
