import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { generateDiet } from "../services/diet";
import { getProfile } from "../services/profile";
import { extractApiErrorMessage } from "../services/api";
import { Disclaimer } from "../components/Disclaimer";

export function DashboardPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
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

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">
          Bem-vindo de volta
        </h1>
        <p className="text-sm text-slate-500">
          Gere uma nova dieta a partir do seu perfil ou revise o histórico.
        </p>
      </div>

      <Disclaimer />

      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        {!profileQuery.isLoading && !hasProfile && (
          <p className="mb-4 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
            Antes de gerar uma dieta, preencha seu perfil em{" "}
            <Link to="/profile" className="font-medium underline">
              /perfil
            </Link>
            .
          </p>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">
              Gerar nova dieta
            </h2>
            <p className="text-sm text-slate-500">
              Calculamos suas calorias-alvo e pedimos um plano alimentar
              personalizado à IA.
            </p>
          </div>
          <button
            type="button"
            disabled={!hasProfile || generateMutation.isPending}
            onClick={() => {
              setError(null);
              generateMutation.mutate();
            }}
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
          >
            {generateMutation.isPending ? "Gerando..." : "Gerar dieta"}
          </button>
        </div>

        {error && (
          <p className="mt-3 text-sm text-red-600">{error}</p>
        )}
      </section>
    </div>
  );
}
