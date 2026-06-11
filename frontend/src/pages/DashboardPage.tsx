import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { generateDiet, listDiets } from "../services/diet";
import { getProfile } from "../services/profile";
import { extractApiErrorMessage } from "../services/api";
import { Disclaimer } from "../components/Disclaimer";
import { GOAL_LABELS_SHORT } from "../types/labels";

const ACTIVITY_LABELS: Record<string, string> = {
  SEDENTARY: "Sedentário",
  LIGHT: "Leve",
  MODERATE: "Moderado",
  ACTIVE: "Ativo",
  VERY_ACTIVE: "Muito ativo",
};

export function DashboardPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
  });

  const dietsQuery = useQuery({
    queryKey: ["diets"],
    queryFn: listDiets,
  });

  const generateMutation = useMutation({
    mutationFn: generateDiet,
    onSuccess: (plan) => {
      queryClient.invalidateQueries({ queryKey: ["diets"] });
      navigate(`/diet/${plan.id}`);
    },
    onError: (err) => {
      setError(extractApiErrorMessage(err, "Falha ao gerar a dieta"));
    },
  });

  const hasProfile = profileQuery.data != null;
  const profile = profileQuery.data;
  const totalDiets = dietsQuery.data?.length ?? 0;
  const lastDiet = dietsQuery.data?.[0];

  return (
    <div className="space-y-12">
      <header>
        <h1 className="text-[32px] font-medium tracking-tight leading-tight">
          Início
        </h1>
        <p className="mt-2 text-[15px] text-[var(--color-ink-3)]">
          Gere um novo cardápio ou revise os anteriores.
        </p>
      </header>

      {/* Stats — minimal row */}
      <section className="grid grid-cols-3 border-y border-[var(--color-rule)]">
        <Stat label="Cardápios" value={String(totalDiets).padStart(2, "0")} />
        <Stat
          label="Perfil"
          value={hasProfile ? "Completo" : "Pendente"}
          divider
        />
        <Stat
          label="Objetivo"
          value={profile ? GOAL_LABELS_SHORT[profile.goal] ?? profile.goal : "—"}
          divider
        />
      </section>

      {/* Generate */}
      <section className="surface p-6 sm:p-7">
        {!profileQuery.isLoading && !hasProfile && (
          <div className="mb-5 -mx-6 sm:-mx-7 -mt-6 sm:-mt-7 px-6 sm:px-7 py-3 bg-[var(--color-rule-2)] border-b border-[var(--color-rule)] rounded-t-md text-[13px] text-[var(--color-ink-2)]">
            Antes de gerar uma dieta, preencha seu{" "}
            <Link to="/profile" className="link font-medium">perfil</Link>.
          </div>
        )}

        <div className="flex flex-wrap items-start justify-between gap-5">
          <div className="max-w-md">
            <h2 className="text-[18px] font-medium tracking-tight">
              Gerar novo cardápio
            </h2>
            <p className="mt-1.5 text-[14px] text-[var(--color-ink-3)] leading-relaxed">
              Calculamos TMB, TDEE e calorias-alvo a partir do seu perfil e
              pedimos um plano alimentar à IA.
            </p>
          </div>
          <button
            type="button"
            disabled={!hasProfile || generateMutation.isPending}
            onClick={() => {
              setError(null);
              generateMutation.mutate();
            }}
            className="btn shrink-0"
          >
            {generateMutation.isPending ? (
              <>
                <span className="spinner" />
                Gerando
              </>
            ) : (
              "Gerar cardápio"
            )}
          </button>
        </div>

        {profile && (
          <dl className="mt-6 pt-5 border-t border-[var(--color-rule)] grid grid-cols-2 sm:grid-cols-4 gap-x-6 gap-y-3 text-[13px]">
            <Field label="Atividade" value={ACTIVITY_LABELS[profile.activityLevel] ?? profile.activityLevel} />
            <Field label="Peso" value={`${profile.weightKg} kg`} />
            <Field label="Altura" value={`${profile.heightCm} cm`} />
            <Field label="Refeições/dia" value={String(profile.mealsPerDay)} />
          </dl>
        )}

        {error && (
          <p className="mt-4 text-[13px] text-[var(--color-ink)]">{error}</p>
        )}
      </section>

      {lastDiet && (
        <section>
          <div className="flex items-baseline justify-between mb-3">
            <h2 className="label">Último cardápio</h2>
            <Link to="/history" className="text-[12px] text-[var(--color-ink-3)] hover:text-[var(--color-ink)]">
              Ver todos →
            </Link>
          </div>
          <Link
            to={`/diet/${lastDiet.id}`}
            className="surface block p-5 hover:border-[var(--color-ink-5)] transition-colors group"
          >
            <div className="flex items-baseline justify-between gap-4">
              <h3 className="text-[15px] font-medium text-[var(--color-ink)] group-hover:underline underline-offset-2 decoration-[var(--color-ink-5)]">
                Cardápio #{lastDiet.id}
              </h3>
              <span className="text-[13px] tabular text-[var(--color-ink-3)]">
                {lastDiet.targetCalories} kcal · {lastDiet.content.meals.length} refeições
              </span>
            </div>
            <p className="mt-1 text-[13px] text-[var(--color-ink-3)] line-clamp-2">
              {lastDiet.content.summary}
            </p>
          </Link>
        </section>
      )}

      <Disclaimer />
    </div>
  );
}

function Stat({
  label,
  value,
  divider,
}: {
  label: string;
  value: string;
  divider?: boolean;
}) {
  return (
    <div
      className={`py-5 px-1 ${
        divider ? "border-l border-[var(--color-rule)] pl-5" : ""
      }`}
    >
      <p className="label mb-1.5">{label}</p>
      <p className="text-[22px] font-medium tracking-tight tabular text-[var(--color-ink)]">
        {value}
      </p>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-[11px] text-[var(--color-ink-3)] uppercase tracking-wide">{label}</dt>
      <dd className="mt-0.5 text-[var(--color-ink)]">{value}</dd>
    </div>
  );
}
