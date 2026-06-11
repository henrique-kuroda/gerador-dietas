import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { useAuth } from "../auth/useAuth";
import { login as loginRequest, register as registerRequest } from "../services/auth";
import { extractApiErrorMessage } from "../services/api";
import { AuthLayout } from "../components/AuthLayout";
import { TextField } from "../components/FormField";

const schema = z.object({
  name: z.string().min(2, "informe seu nome"),
  email: z.string().email("informe um e-mail válido"),
  password: z.string().min(8, "mínimo de 8 caracteres"),
});

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register: rhfRegister,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      await registerRequest(values);
      return loginRequest({ email: values.email, password: values.password });
    },
    onSuccess: (auth) => {
      login(auth.token);
      navigate("/profile", { replace: true });
    },
    onError: (err) => {
      setServerError(extractApiErrorMessage(err, "Falha ao cadastrar"));
    },
  });

  if (isAuthenticated) return <Navigate to="/" replace />;

  return (
    <AuthLayout
      title="Criar conta"
      subtitle="Cadastre-se em segundos para começar."
      footer={
        <>
          Já tem conta? <Link to="/login" className="link">Entrar</Link>
        </>
      }
    >
      <form
        className="space-y-4"
        onSubmit={handleSubmit((values) => {
          setServerError(null);
          mutation.mutate(values);
        })}
      >
        <TextField
          label="Nome"
          autoComplete="name"
          placeholder="Como prefere ser chamado"
          error={errors.name?.message}
          {...rhfRegister("name")}
        />
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@exemplo.com"
          error={errors.email?.message}
          {...rhfRegister("email")}
        />
        <TextField
          label="Senha"
          type="password"
          autoComplete="new-password"
          placeholder="ao menos 8 caracteres"
          hint="Recomendamos uma frase memorável."
          error={errors.password?.message}
          {...rhfRegister("password")}
        />
        {serverError && (
          <p className="text-[13px] text-[var(--color-ink)]">{serverError}</p>
        )}
        <button type="submit" disabled={mutation.isPending} className="btn w-full">
          {mutation.isPending ? (
            <>
              <span className="spinner" />
              Cadastrando
            </>
          ) : (
            "Criar conta"
          )}
        </button>
      </form>
    </AuthLayout>
  );
}
