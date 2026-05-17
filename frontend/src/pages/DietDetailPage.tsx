import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { downloadDietPdf, getDiet } from "../services/diet";
import { DietPlanView } from "../components/DietPlanView";
import { Disclaimer } from "../components/Disclaimer";
import { extractApiErrorMessage } from "../services/api";

export function DietDetailPage() {
  const { id } = useParams();
  const numericId = id ? Number(id) : NaN;
  const [pdfError, setPdfError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["diet", numericId],
    queryFn: () => getDiet(numericId),
    enabled: Number.isFinite(numericId),
  });

  const pdfMutation = useMutation({
    mutationFn: () => downloadDietPdf(numericId),
    onError: (err) => {
      setPdfError(extractApiErrorMessage(err, "Falha ao baixar o PDF."));
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Link to="/history" className="text-sm text-emerald-700 underline">
          ← Voltar ao histórico
        </Link>
        {query.data && (
          <button
            type="button"
            onClick={() => {
              setPdfError(null);
              pdfMutation.mutate();
            }}
            disabled={pdfMutation.isPending}
            className="rounded-md border border-emerald-600 px-3 py-1.5 text-sm font-medium text-emerald-700 hover:bg-emerald-50 disabled:opacity-60"
          >
            {pdfMutation.isPending ? "Gerando PDF..." : "Baixar PDF"}
          </button>
        )}
      </div>

      {pdfError && <p className="text-sm text-red-600">{pdfError}</p>}

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
