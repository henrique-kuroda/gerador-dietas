import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getDiet } from "../services/diet";
import { DietPlanView } from "../components/DietPlanView";
import { Disclaimer } from "../components/Disclaimer";
import { extractApiErrorMessage } from "../services/api";

export function DietDetailPage() {
  const { id } = useParams();
  const numericId = id ? Number(id) : NaN;

  const query = useQuery({
    queryKey: ["diet", numericId],
    queryFn: () => getDiet(numericId),
    enabled: Number.isFinite(numericId),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Link to="/history" className="text-sm text-emerald-700 underline">
          ← Voltar ao histórico
        </Link>
      </div>

      <Disclaimer />

      {query.isLoading && <p className="text-slate-500">Carregando dieta...</p>}
      {query.isError && (
        <p className="text-sm text-red-600">
          {extractApiErrorMessage(query.error, "Falha ao carregar a dieta.")}
        </p>
      )}
      {query.data && <DietPlanView plan={query.data} />}
    </div>
  );
}
