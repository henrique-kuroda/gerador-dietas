import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { listDiets } from "../services/diet";
import { Disclaimer } from "../components/Disclaimer";

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  } catch {
    return iso;
  }
}

export function HistoryPage() {
  const query = useQuery({
    queryKey: ["diets"],
    queryFn: listDiets,
  });

  return (
    <div className="space-y-10">
      <header>
        <h1 className="text-[32px] font-medium tracking-tight leading-tight">
          Histórico
        </h1>
        <p className="mt-2 text-[15px] text-[var(--color-ink-3)]">
          Cardápios gerados, do mais recente para o mais antigo.
        </p>
      </header>

      {query.isLoading && (
        <div className="py-16 flex items-center justify-center gap-2 text-[var(--color-ink-3)]">
          <span className="spinner" />
          <span className="text-[13px]">Carregando…</span>
        </div>
      )}

      {query.isError && (
        <p className="text-[13px] text-[var(--color-ink)]">Falha ao carregar o histórico.</p>
      )}

      {query.data && query.data.length === 0 && (
        <div className="surface px-6 py-16 text-center">
          <h2 className="text-[18px] font-medium tracking-tight">Nada por aqui ainda</h2>
          <p className="mt-2 text-[14px] text-[var(--color-ink-3)] max-w-sm mx-auto">
            Gere seu primeiro cardápio para começar a montar o histórico.
          </p>
          <Link to="/" className="btn mt-6 inline-flex">
            Gerar cardápio
          </Link>
        </div>
      )}

      {query.data && query.data.length > 0 && (
        <ul className="surface divide-y divide-[var(--color-rule)] overflow-hidden">
          {query.data.map((plan) => (
            <li key={plan.id}>
              <Link
                to={`/diet/${plan.id}`}
                className="grid grid-cols-[1fr_auto] sm:grid-cols-[1fr_auto_8rem] items-center gap-4 sm:gap-6 px-5 py-4 hover:bg-[var(--color-rule-2)] transition-colors group"
              >
                <div className="min-w-0">
                  <div className="flex items-baseline gap-3">
                    <span className="text-[12px] tabular text-[var(--color-ink-4)]">
                      #{String(plan.id).padStart(3, "0")}
                    </span>
                    <h2 className="text-[14px] font-medium text-[var(--color-ink)] group-hover:underline underline-offset-2 decoration-[var(--color-ink-5)] truncate">
                      Cardápio
                    </h2>
                  </div>
                  <p className="mt-0.5 text-[13px] text-[var(--color-ink-3)] line-clamp-1">
                    {plan.content.summary}
                  </p>
                </div>

                <div className="hidden sm:flex flex-col items-end text-[12px] tabular text-[var(--color-ink-3)]">
                  <span className="text-[var(--color-ink)] text-[14px]">
                    {plan.targetCalories} kcal
                  </span>
                  <span>{plan.content.meals.length} refeições</span>
                </div>

                <div className="text-right text-[12px] text-[var(--color-ink-3)] tabular">
                  {formatDate(plan.createdAt)}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <Disclaimer />
    </div>
  );
}
