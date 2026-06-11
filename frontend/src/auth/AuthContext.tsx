import { useCallback, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api } from "../services/api";
import { clearToken, getToken, setToken } from "../services/token";
import type { MeResponse } from "../types";
import { AuthContext, type AuthContextValue, type AuthUser } from "./useAuth";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getToken());
  const [user, setUser] = useState<AuthUser | null>(null);

  // Os dados do usuário vêm de GET /api/auth/me; o token é opaco para o front.
  // Token expirado/inválido cai no interceptor 401 do api, que limpa e redireciona.
  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    api
      .get<MeResponse>("/api/auth/me")
      .then((res) => {
        if (!cancelled) setUser({ name: res.data.name, email: res.data.email });
      })
      .catch(() => {
        if (!cancelled) setUser(null);
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const login = useCallback((newToken: string) => {
    setToken(newToken);
    setTokenState(newToken);
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setTokenState(null);
    setUser(null);
  }, []);

  useEffect(() => {
    function syncFromStorage() {
      setTokenState(getToken());
    }
    window.addEventListener("storage", syncFromStorage);
    return () => window.removeEventListener("storage", syncFromStorage);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: token ? user : null,
      isAuthenticated: token !== null,
      login,
      logout,
    }),
    [user, token, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
