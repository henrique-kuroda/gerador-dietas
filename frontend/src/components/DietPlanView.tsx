import type { DietPlanResponse, Formula } from "../types";

const FORMULA_LABEL: Record<Formula, string> = {
  HARRIS_BENEDICT: "Harris-Benedict",
  MIFFLIN_ST_JEOR: "Mifflin-St Jeor",
  KATCH_MCARDLE: "Katch-McArdle",
};

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString("pt-BR");
  } catch {
    return iso;
  }
}

export function DietPlanView({ plan }: { plan: DietPlanResponse }) {
  const { content } = plan;
  return (
    <div className="space-y-6">
      <header className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="text-lg font-semibold text-slate-900">
            Dieta #{plan.id}
          </h2>
          <span className="text-xs text-slate-500">
            Gerada em {formatDate(plan.createdAt)}
          </span>
        </div>
        <p className="mt-1 text-sm text-slate-600">{content.summary}</p>

        <dl className="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
          <Metric label="TMB" value={`${plan.tmb} kcal`} />
          <Metric label="TDEE" value={`${plan.tdee} kcal`} />
          <Metric label="Alvo" value={`${plan.targetCalories} kcal`} />
          <Metric label="Fórmula" value={FORMULA_LABEL[plan.formulaUsed]} />
        </dl>

        <div className="mt-4 grid grid-cols-3 gap-3 text-sm">
          <Metric label="Proteína" value={`${content.macros.proteinG} g`} />
          <Metric label="Carbo." value={`${content.macros.carbsG} g`} />
          <Metric label="Gordura" value={`${content.macros.fatG} g`} />
        </div>
      </header>

      <section className="space-y-4">
        {content.meals.map((meal, i) => (
          <article
            key={`${meal.name}-${i}`}
            className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
          >
            <div className="flex items-baseline justify-between">
              <h3 className="font-semibold text-slate-900">{meal.name}</h3>
              <span className="text-sm text-slate-500">
                {meal.calories} kcal
              </span>
            </div>
            <ul className="mt-3 divide-y divide-slate-100 text-sm">
              {meal.items.map((item, j) => (
                <li
                  key={`${item.food}-${j}`}
                  className="flex items-baseline justify-between py-2"
                >
                  <div>
                    <p className="text-slate-800">{item.food}</p>
                    <p className="text-xs text-slate-500">{item.portion}</p>
                  </div>
                  <span className="text-slate-600">{item.calories} kcal</span>
                </li>
              ))}
            </ul>
          </article>
        ))}
      </section>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-slate-50 px-3 py-2">
      <dt className="text-xs uppercase tracking-wide text-slate-500">
        {label}
      </dt>
      <dd className="text-sm font-medium text-slate-900">{value}</dd>
    </div>
  );
}
