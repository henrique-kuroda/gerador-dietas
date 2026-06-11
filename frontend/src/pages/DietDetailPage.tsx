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
    <div className="space-y-8">
      <div className="flex items-center justify-between gap-4">
        <Link
          to="/history"
          className="text-[13px] text-[var(--color-ink-3)] hover:text-[var(--color-ink)]"
        >
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
            className="btn-ghost"
          >
            {pdfMutation.isPending ? (
              <>
                <span className="spinner" />
                Gerando PDF
              </>
            ) : (
              "Baixar PDF"
            )}
          </button>
        )}
      </div>

      {pdfError && (
        <p className="text-[13px] text-[var(--color-ink)]">{pdfError}</p>
      )}

      {query.isLoading && (
        <div className="py-16 flex items-center justify-center gap-2 text-[var(--color-ink-3)]">
          <span className="spinner" />
          <span className="text-[13px]">Carregando cardápio…</span>
        </div>
      )}
      {query.isError && (
        <p className="text-[13px] text-[var(--color-ink)]">
          {extractApiErrorMessage(query.error, "Falha ao carregar a dieta.")}
        </p>
      )}
      {query.data && <DietPlanView plan={query.data} />}

      <Disclaimer />
    </div>
  );
}
