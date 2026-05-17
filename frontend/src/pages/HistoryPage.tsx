import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { listDiets } from "../services/diet";
import { Disclaimer } from "../components/Disclaimer";

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString("pt-BR");
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
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Histórico</h1>
        <p className="text-sm text-slate-500">
          Suas dietas geradas, da mais recente para a mais antiga.
        </p>
      </div>

      <Disclaimer />

      {query.isLoading && <p className="text-slate-500">Carregando...</p>}
      {query.isError && (
        <p className="text-red-600 text-sm">Falha ao carregar o histórico.</p>
      )}

      {query.data && query.data.length === 0 && (
        <div className="rounded-md border border-slate-200 bg-white p-6 text-center text-slate-600 shadow-sm">
          Você ainda não gerou nenhuma dieta.{" "}
          <Link to="/" className="text-emerald-700 underline">
            Gerar agora
          </Link>
          .
        </div>
      )}

      {query.data && query.data.length > 0 && (
        <ul className="space-y-3">
          {query.data.map((plan) => (
            <li
              key={plan.id}
              className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
            >
              <Link
                to={`/diet/${plan.id}`}
                className="flex flex-wrap items-baseline justify-between gap-2"
              >
                <div>
                  <h2 className="font-semibold text-emerald-700">
                    Dieta #{plan.id}
                  </h2>
                  <p className="text-sm text-slate-600">
                    {plan.content.summary}
                  </p>
                </div>
                <div className="text-right text-xs text-slate-500">
                  <p>{formatDate(plan.createdAt)}</p>
                  <p className="mt-1 text-slate-700">
                    {plan.targetCalories} kcal · {plan.content.meals.length}{" "}
                    refeições
                  </p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
