import { useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adjustDiet, downloadDietPdf, getDiet } from "../services/diet";
import { DietPlanView } from "../components/DietPlanView";
import { Disclaimer } from "../components/Disclaimer";
import { extractApiErrorMessage, hasApiStatus } from "../services/api";

const ADJUST_LIMIT = 10;

export function DietDetailPage() {
  const { id } = useParams();
  const numericId = id ? Number(id) : NaN;
  const queryClient = useQueryClient();
  const [pdfError, setPdfError] = useState<string | null>(null);
  const [instruction, setInstruction] = useState("");
  const [adjusted, setAdjusted] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

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

  const adjustMutation = useMutation({
    mutationFn: () => adjustDiet(numericId, instruction.trim()),
    onSuccess: (data) => {
      queryClient.setQueryData(["diet", numericId], data);
      queryClient.invalidateQueries({ queryKey: ["diets"] });
      setInstruction("");
      setAdjusted(true);
    },
    onError: (error) => {
      // 409: outro ajuste do mesmo plano ganhou a corrida e o que está na tela
      // envelheceu. Recarrega para o usuário decidir sobre o plano já atualizado.
      if (hasApiStatus(error, 409)) {
        queryClient.invalidateQueries({ queryKey: ["diet", numericId] });
      }
    },
  });

  const plan = query.data;
  const adjustmentCount = plan?.adjustmentCount ?? 0;
  const limitReached = adjustmentCount >= ADJUST_LIMIT;

  function prefillSwap(mealName: string, food: string) {
    setAdjusted(false);
    setInstruction(`Troque "${food}" do "${mealName}" por algo equivalente em calorias.`);
    textareaRef.current?.focus();
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between gap-4">
        <Link
          to="/history"
          className="text-[13px] text-[var(--color-ink-3)] hover:text-[var(--color-ink)]"
        >
          ← Voltar ao histórico
        </Link>
        {plan && (
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
      {plan && <DietPlanView plan={plan} onSwap={prefillSwap} />}

      {plan && (
        <section className="surface p-5 space-y-3">
          <div className="flex items-baseline justify-between gap-3">
            <h2 className="label">Ajustar plano</h2>
            <span className="text-[12px] tabular text-[var(--color-ink-3)]">
              {adjustmentCount}/{ADJUST_LIMIT} ajustes
            </span>
          </div>
          <p className="text-[13px] text-[var(--color-ink-3)]">
            Não curtiu um item? Descreva a mudança — as metas calóricas são mantidas.
          </p>
          <textarea
            ref={textareaRef}
            rows={2}
            maxLength={500}
            className="field"
            placeholder='ex.: "troque a tapioca do café"; "deixe o jantar mais leve"'
            value={instruction}
            onChange={(e) => {
              setInstruction(e.target.value);
              setAdjusted(false);
            }}
            disabled={adjustMutation.isPending || limitReached}
          />
          <div className="flex items-center justify-between gap-3">
            <div className="min-h-[1.25rem] text-[13px]">
              {limitReached && (
                <span className="text-[var(--color-ink-3)]">
                  Limite de ajustes deste plano atingido.
                </span>
              )}
              {adjusted && !adjustMutation.isPending && (
                <span className="text-[var(--color-ink-3)]">Plano ajustado.</span>
              )}
              {adjustMutation.isError && (
                <span className="text-[var(--color-ink)]">
                  {extractApiErrorMessage(
                    adjustMutation.error,
                    "Não foi possível ajustar o plano.",
                  )}
                </span>
              )}
            </div>
            <button
              type="button"
              className="btn shrink-0"
              disabled={
                adjustMutation.isPending || limitReached || !instruction.trim()
              }
              onClick={() => adjustMutation.mutate()}
            >
              {adjustMutation.isPending ? (
                <>
                  <span className="spinner" />
                  Ajustando
                </>
              ) : (
                "Aplicar ajuste"
              )}
            </button>
          </div>
        </section>
      )}

      <Disclaimer />
    </div>
  );
}
