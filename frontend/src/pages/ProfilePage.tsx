import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useLocation, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getProfile, saveProfile } from "../services/profile";
import { extractApiErrorMessage } from "../services/api";
import { FieldShell, SelectField, TextField } from "../components/FormField";
import type { ProfileRequest } from "../types";
import { GOAL_LABELS } from "../types/labels";

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
  goal: z.enum([
    "AGGRESSIVE_LOSS",
    "LOSE_WEIGHT",
    "MAINTAIN",
    "GAIN_MUSCLE",
    "AGGRESSIVE_GAIN",
  ]),
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

export function ProfilePage() {
  const queryClient = useQueryClient();
  const location = useLocation();
  const navigate = useNavigate();
  const requireProfile =
    (location.state as { requireProfile?: boolean } | null)?.requireProfile ??
    false;
  const [feedback, setFeedback] = useState<
    { kind: "success" | "error"; message: string } | null
  >(null);

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
  });

  const isFirstTime = !profileQuery.isLoading && profileQuery.data == null;

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
      const wasFirstTime = isFirstTime;
      queryClient.setQueryData(["profile"], data);
      setFeedback({ kind: "success", message: "Perfil salvo." });
      if (wasFirstTime) {
        navigate("/", { replace: true });
      }
    },
    onError: (err) => {
      setFeedback({
        kind: "error",
        message: extractApiErrorMessage(err, "Falha ao salvar o perfil"),
      });
    },
  });

  if (profileQuery.isLoading) {
    return (
      <div className="py-20 flex items-center justify-center gap-2 text-[var(--color-ink-3)]">
        <span className="spinner" />
        <span className="text-[13px]">Carregando perfil…</span>
      </div>
    );
  }

  return (
    <div className="space-y-10">
      <header>
        <h1 className="text-[32px] font-medium tracking-tight leading-tight">
          Perfil
        </h1>
        <p className="mt-2 text-[15px] text-[var(--color-ink-3)]">
          Esses dados são usados para calcular calorias-alvo e gerar dietas.
        </p>
        {(requireProfile || isFirstTime) && (
          <p className="mt-4 text-[13px] text-[var(--color-ink-2)] border-l-2 border-[var(--color-ink)] pl-3">
            Para continuar, preencha seu perfil antropométrico. Você não
            poderá gerar cardápios sem ele.
          </p>
        )}
      </header>

      <form
        onSubmit={handleSubmit((values) => {
          setFeedback(null);
          mutation.mutate(values);
        })}
        className="space-y-8"
      >
        <Section title="Antropometria" description="Medidas básicas.">
          <Grid cols={3}>
            <TextField
              label="Peso (kg)"
              type="number"
              step="0.1"
              placeholder="80"
              error={errors.weightKg?.message}
              {...register("weightKg")}
            />
            <TextField
              label="Altura (cm)"
              type="number"
              step="0.1"
              placeholder="180"
              error={errors.heightCm?.message}
              {...register("heightCm")}
            />
            <TextField
              label="Idade"
              type="number"
              placeholder="30"
              error={errors.age?.message}
              {...register("age")}
            />
          </Grid>
        </Section>

        <Section title="Estilo & objetivo" description="Define o fator de atividade e o ajuste calórico.">
          <Grid cols={2}>
            <SelectField label="Sexo biológico" error={errors.sex?.message} {...register("sex")}>
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
            <SelectField label="Objetivo" error={errors.goal?.message} {...register("goal")}>
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
              placeholder="4"
              error={errors.mealsPerDay?.message}
              {...register("mealsPerDay")}
            />
          </Grid>
        </Section>

        <Section
          title="Refinamentos"
          description="Opcionais — usados para afinar o cálculo e respeitar restrições."
        >
          <Grid cols={2}>
            <TextField
              label="% de gordura corporal"
              type="number"
              step="0.1"
              placeholder="opcional"
              hint="Se informado, usamos Katch-McArdle no lugar de Mifflin."
              error={errors.bodyFatPercent?.message as string | undefined}
              {...register("bodyFatPercent")}
            />
            <FieldShell label="Restrições alimentares">
              <textarea
                rows={3}
                placeholder="ex.: sem lactose, vegetariano"
                className="field"
                {...register("dietaryRestrictions")}
              />
              {errors.dietaryRestrictions?.message && (
                <p className="mt-1.5 text-xs text-[var(--color-ink)]">
                  {errors.dietaryRestrictions.message}
                </p>
              )}
            </FieldShell>
          </Grid>
        </Section>

        <div className="flex items-center justify-between gap-4 pt-2">
          <div className="min-h-[1.25rem]">
            {feedback && (
              <p
                className={`text-[13px] ${
                  feedback.kind === "success"
                    ? "text-[var(--color-accent)]"
                    : "text-[var(--color-ink)]"
                }`}
              >
                {feedback.message}
              </p>
            )}
          </div>
          <button
            type="submit"
            disabled={mutation.isPending || !isDirty}
            className="btn"
          >
            {mutation.isPending ? (
              <>
                <span className="spinner" />
                Salvando
              </>
            ) : (
              "Salvar perfil"
            )}
          </button>
        </div>
      </form>
    </div>
  );
}

function Section({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: React.ReactNode;
}) {
  return (
    <section className="grid sm:grid-cols-[12rem_1fr] gap-4 sm:gap-8 pb-8 border-b border-[var(--color-rule)] last:border-b-0 last:pb-0">
      <div>
        <h2 className="text-[15px] font-medium tracking-tight">{title}</h2>
        <p className="mt-1 text-[13px] text-[var(--color-ink-3)] leading-relaxed">
          {description}
        </p>
      </div>
      <div>{children}</div>
    </section>
  );
}

function Grid({ cols, children }: { cols: 2 | 3; children: React.ReactNode }) {
  const cls = cols === 3 ? "sm:grid-cols-3" : "sm:grid-cols-2";
  return <div className={`grid grid-cols-1 ${cls} gap-4`}>{children}</div>;
}
