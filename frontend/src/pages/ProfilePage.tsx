import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getProfile, saveProfile } from "../services/profile";
import { extractApiErrorMessage } from "../services/api";
import { SelectField, TextField } from "../components/FormField";
import type { ProfileRequest } from "../types";

const schema = z.object({
  weightKg: z.coerce.number().positive("deve ser maior que zero").max(500),
  heightCm: z.coerce.number().positive("deve ser maior que zero").max(300),
  age: z.coerce
    .number()
    .int("deve ser inteiro")
    .min(1, "deve ser maior que zero")
    .max(120, "valor irreal"),
  sex: z.enum(["MALE", "FEMALE"]),
  activityLevel: z.enum([
    "SEDENTARY",
    "LIGHT",
    "MODERATE",
    "ACTIVE",
    "VERY_ACTIVE",
  ]),
  goal: z.enum(["LOSE_WEIGHT", "MAINTAIN", "GAIN_MUSCLE"]),
  dietaryRestrictions: z.string().max(1000).optional(),
  mealsPerDay: z.coerce.number().int().min(1).max(8),
  bodyFatPercent: z
    .union([z.coerce.number().min(0).max(99.9), z.literal("")])
    .optional(),
});

type FormValues = z.input<typeof schema>;
type FormOutput = z.output<typeof schema>;

const ACTIVITY_LABELS: Record<ProfileRequest["activityLevel"], string> = {
  SEDENTARY: "Sedentário (pouco/nenhum exercício)",
  LIGHT: "Leve (1–3 dias/semana)",
  MODERATE: "Moderado (3–5 dias/semana)",
  ACTIVE: "Ativo (6–7 dias/semana)",
  VERY_ACTIVE: "Muito ativo / trabalho físico",
};

const GOAL_LABELS: Record<ProfileRequest["goal"], string> = {
  LOSE_WEIGHT: "Perder peso",
  MAINTAIN: "Manter peso",
  GAIN_MUSCLE: "Ganhar massa",
};

export function ProfilePage() {
  const queryClient = useQueryClient();
  const [feedback, setFeedback] = useState<
    { kind: "success" | "error"; message: string } | null
  >(null);

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<FormValues, unknown, FormOutput>({
    resolver: zodResolver(schema),
    defaultValues: {
      sex: "MALE",
      activityLevel: "MODERATE",
      goal: "MAINTAIN",
      mealsPerDay: 4,
    },
  });

  useEffect(() => {
    if (profileQuery.data) {
      reset({
        weightKg: profileQuery.data.weightKg,
        heightCm: profileQuery.data.heightCm,
        age: profileQuery.data.age,
        sex: profileQuery.data.sex,
        activityLevel: profileQuery.data.activityLevel,
        goal: profileQuery.data.goal,
        dietaryRestrictions: profileQuery.data.dietaryRestrictions ?? "",
        mealsPerDay: profileQuery.data.mealsPerDay,
        bodyFatPercent: profileQuery.data.bodyFatPercent ?? "",
      });
    }
  }, [profileQuery.data, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormOutput) => {
      const payload: ProfileRequest = {
        weightKg: Number(values.weightKg),
        heightCm: Number(values.heightCm),
        age: Number(values.age),
        sex: values.sex,
        activityLevel: values.activityLevel,
        goal: values.goal,
        dietaryRestrictions: values.dietaryRestrictions?.trim() || null,
        mealsPerDay: Number(values.mealsPerDay),
        bodyFatPercent:
          values.bodyFatPercent === "" || values.bodyFatPercent == null
            ? null
            : Number(values.bodyFatPercent),
      };
      return saveProfile(payload);
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["profile"], data);
      setFeedback({ kind: "success", message: "Perfil salvo com sucesso." });
    },
    onError: (err) => {
      setFeedback({
        kind: "error",
        message: extractApiErrorMessage(err, "Falha ao salvar o perfil"),
      });
    },
  });

  if (profileQuery.isLoading) {
    return <p className="text-slate-500">Carregando perfil...</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Seu perfil</h1>
        <p className="text-sm text-slate-500">
          Esses dados são usados para calcular suas calorias-alvo e gerar dietas
          personalizadas.
        </p>
      </div>

      <form
        className="grid grid-cols-1 gap-4 sm:grid-cols-2 rounded-lg border border-slate-200 bg-white p-6 shadow-sm"
        onSubmit={handleSubmit((values) => {
          setFeedback(null);
          mutation.mutate(values);
        })}
      >
        <TextField
          label="Peso (kg)"
          type="number"
          step="0.1"
          error={errors.weightKg?.message}
          {...register("weightKg")}
        />
        <TextField
          label="Altura (cm)"
          type="number"
          step="0.1"
          error={errors.heightCm?.message}
          {...register("heightCm")}
        />
        <TextField
          label="Idade"
          type="number"
          error={errors.age?.message}
          {...register("age")}
        />
        <SelectField
          label="Sexo"
          error={errors.sex?.message}
          {...register("sex")}
        >
          <option value="MALE">Masculino</option>
          <option value="FEMALE">Feminino</option>
        </SelectField>
        <SelectField
          label="Nível de atividade"
          error={errors.activityLevel?.message}
          {...register("activityLevel")}
        >
          {Object.entries(ACTIVITY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>
        <SelectField
          label="Objetivo"
          error={errors.goal?.message}
          {...register("goal")}
        >
          {Object.entries(GOAL_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>
        <TextField
          label="Refeições por dia"
          type="number"
          min={1}
          max={8}
          error={errors.mealsPerDay?.message}
          {...register("mealsPerDay")}
        />
        <TextField
          label="% de gordura corporal (opcional)"
          type="number"
          step="0.1"
          hint="se informado, usamos a fórmula Katch-McArdle"
          error={errors.bodyFatPercent?.message as string | undefined}
          {...register("bodyFatPercent")}
        />
        <div className="sm:col-span-2">
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Restrições alimentares (opcional)
          </label>
          <textarea
            rows={3}
            placeholder="Ex.: sem lactose, vegetariano, alergia a amendoim"
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            {...register("dietaryRestrictions")}
          />
          {errors.dietaryRestrictions?.message && (
            <p className="mt-1 text-xs text-red-600">
              {errors.dietaryRestrictions.message}
            </p>
          )}
        </div>

        <div className="sm:col-span-2 flex items-center justify-between">
          {feedback ? (
            <p
              className={`text-sm ${
                feedback.kind === "success"
                  ? "text-emerald-700"
                  : "text-red-600"
              }`}
            >
              {feedback.message}
            </p>
          ) : (
            <span />
          )}
          <button
            type="submit"
            disabled={mutation.isPending || !isDirty}
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
          >
            {mutation.isPending ? "Salvando..." : "Salvar perfil"}
          </button>
        </div>
      </form>
    </div>
  );
}
