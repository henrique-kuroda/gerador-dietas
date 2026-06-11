import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { useAuth } from "../auth/AuthContext";
import { login as loginRequest } from "../services/auth";
import { extractApiErrorMessage } from "../services/api";
import { AuthLayout } from "../components/AuthLayout";
import { TextField } from "../components/FormField";

const schema = z.object({
  email: z.string().email("informe um e-mail válido"),
  password: z.string().min(1, "informe sua senha"),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: loginRequest,
    onSuccess: (data) => {
      login(data.token);
      const from = (location.state as { from?: { pathname: string } } | null)
        ?.from?.pathname;
      navigate(from && from !== "/login" ? from : "/", { replace: true });
    },
    onError: (err) => {
      setServerError(extractApiErrorMessage(err, "Falha ao entrar"));
    },
  });

  useEffect(() => {
    if (mutation.isPending) setServerError(null);
  }, [mutation.isPending]);

  if (isAuthenticated) return <Navigate to="/" replace />;

  return (
    <AuthLayout
      title="Entrar"
      subtitle="Acesse sua conta para gerar e revisar dietas."
      footer={
        <>
          Ainda não tem conta? <Link to="/register" className="link">Cadastre-se</Link>
        </>
      }
    >
      <form
        className="space-y-4"
        onSubmit={handleSubmit((values) => mutation.mutate(values))}
      >
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@exemplo.com"
          error={errors.email?.message}
          {...register("email")}
        />
        <TextField
          label="Senha"
          type="password"
          autoComplete="current-password"
          placeholder="••••••••"
          error={errors.password?.message}
          {...register("password")}
        />
        {serverError && (
          <p className="text-[13px] text-[var(--color-ink)]">{serverError}</p>
        )}
        <button type="submit" disabled={mutation.isPending} className="btn w-full">
          {mutation.isPending ? (
            <>
              <span className="spinner" />
              Entrando
            </>
          ) : (
            "Entrar"
          )}
        </button>
      </form>
    </AuthLayout>
  );
}
