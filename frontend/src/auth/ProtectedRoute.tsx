import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "./AuthContext";
import { getProfile } from "../services/profile";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: getProfile,
    enabled: isAuthenticated,
    staleTime: 60_000,
  });

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (profileQuery.isLoading) {
    return (
      <div className="min-h-full flex items-center justify-center gap-2 text-[var(--color-ink-3)] py-20">
        <span className="spinner" />
        <span className="text-[13px]">Carregando…</span>
      </div>
    );
  }

  const hasProfile = profileQuery.data != null;
  const isOnProfilePage = location.pathname === "/profile";

  if (!hasProfile && !isOnProfilePage) {
    return <Navigate to="/profile" replace state={{ requireProfile: true }} />;
  }

  return <>{children}</>;
}
