import type { DietPlanResponse, Formula } from "../types";

const FORMULA_LABEL: Record<Formula, string> = {
  HARRIS_BENEDICT: "Harris-Benedict",
  MIFFLIN_ST_JEOR: "Mifflin-St Jeor",
  KATCH_MCARDLE: "Katch-McArdle",
};

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString("pt-BR", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

export function DietPlanView({ plan }: { plan: DietPlanResponse }) {
  const { content } = plan;
  const totalMacros =
    content.macros.proteinG + content.macros.carbsG + content.macros.fatG;
  const pct = (g: number) =>
    totalMacros > 0 ? Math.round((g / totalMacros) * 100) : 0;

  return (
    <article className="space-y-10">
      <header>
        <p className="label">Cardápio #{plan.id}</p>
        <h1 className="mt-2 text-[28px] font-medium tracking-tight leading-tight">
          {content.summary}
        </h1>
        <p className="mt-2 text-[13px] tabular text-[var(--color-ink-3)]">
          {formatDate(plan.createdAt)} · {FORMULA_LABEL[plan.formulaUsed]}
        </p>
      </header>

      {/* Calorie ledger */}
      <section className="surface grid grid-cols-3 divide-x divide-[var(--color-rule)]">
        <Metric label="TMB" value={plan.tmb} unit="kcal" />
        <Metric label="TDEE" value={plan.tdee} unit="kcal" />
        <Metric label="Alvo" value={plan.targetCalories} unit="kcal" emphasis />
      </section>

      {/* Macros */}
      <section>
        <h2 className="label mb-3">Macronutrientes</h2>
        <div className="surface p-5 space-y-4">
          <MacroBar label="Proteína" g={content.macros.proteinG} pct={pct(content.macros.proteinG)} />
          <MacroBar label="Carboidrato" g={content.macros.carbsG} pct={pct(content.macros.carbsG)} />
          <MacroBar label="Gordura" g={content.macros.fatG} pct={pct(content.macros.fatG)} />
        </div>
      </section>

      {/* Meals */}
      <section>
        <h2 className="label mb-3">Refeições</h2>
        <div className="space-y-3">
          {content.meals.map((meal, i) => (
            <article key={`${meal.name}-${i}`} className="surface p-5">
              <header className="flex items-baseline justify-between gap-3 pb-3 mb-3 border-b border-[var(--color-rule)]">
                <div className="flex items-baseline gap-3 min-w-0">
                  <span className="text-[12px] tabular text-[var(--color-ink-4)]">
                    {String(i + 1).padStart(2, "0")}
                  </span>
                  <h3 className="text-[15px] font-medium tracking-tight truncate">
                    {meal.name}
                  </h3>
                </div>
                <span className="text-[13px] tabular text-[var(--color-ink-2)]">
                  {meal.calories} kcal
                </span>
              </header>
              <ul className="space-y-2">
                {meal.items.map((item, j) => (
                  <li
                    key={`${item.food}-${j}`}
                    className="grid grid-cols-[1fr_auto] items-baseline gap-3 py-0.5"
                  >
                    <div className="min-w-0">
                      <p className="text-[14px] text-[var(--color-ink)] truncate">
                        {item.food}
                      </p>
                      <p className="text-[12px] text-[var(--color-ink-3)]">
                        {item.portion}
                      </p>
                    </div>
                    <span className="text-[13px] tabular text-[var(--color-ink-3)]">
                      {item.calories} kcal
                    </span>
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </section>
    </article>
  );
}

function Metric({
  label,
  value,
  unit,
  emphasis,
}: {
  label: string;
  value: number;
  unit: string;
  emphasis?: boolean;
}) {
  return (
    <div className="p-5">
      <p className="label">{label}</p>
      <p
        className={`mt-1.5 text-[24px] font-medium tracking-tight tabular ${
          emphasis ? "text-[var(--color-ink)]" : "text-[var(--color-ink-2)]"
        }`}
      >
        {value}
        <span className="ml-1 text-[12px] text-[var(--color-ink-3)] font-normal tracking-normal">
          {unit}
        </span>
      </p>
    </div>
  );
}

function MacroBar({ label, g, pct }: { label: string; g: number; pct: number }) {
  return (
    <div>
      <div className="flex items-baseline justify-between mb-1.5 text-[13px]">
        <span className="text-[var(--color-ink-2)]">{label}</span>
        <span className="tabular text-[var(--color-ink-3)]">
          <span className="text-[var(--color-ink)]">{g}g</span>
          <span className="ml-2">{pct}%</span>
        </span>
      </div>
      <div className="h-1 bg-[var(--color-rule-2)] rounded-full overflow-hidden">
        <div
          className="h-full bg-[var(--color-ink)] transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
