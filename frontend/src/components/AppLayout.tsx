import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const navItem =
  "px-3 py-2 rounded-md text-sm font-medium transition-colors";
const activeItem = "bg-emerald-100 text-emerald-900";
const inactiveItem = "text-slate-600 hover:bg-slate-100";

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="min-h-full flex flex-col">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-5xl flex items-center justify-between px-4 py-3">
          <Link to="/" className="text-lg font-semibold text-emerald-700">
            Gerador de Dietas
          </Link>
          <nav className="flex items-center gap-1">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                `${navItem} ${isActive ? activeItem : inactiveItem}`
              }
            >
              Dashboard
            </NavLink>
            <NavLink
              to="/profile"
              className={({ isActive }) =>
                `${navItem} ${isActive ? activeItem : inactiveItem}`
              }
            >
              Perfil
            </NavLink>
            <NavLink
              to="/history"
              className={({ isActive }) =>
                `${navItem} ${isActive ? activeItem : inactiveItem}`
              }
            >
              Histórico
            </NavLink>
          </nav>
          <div className="flex items-center gap-3 text-sm text-slate-600">
            {user && <span className="hidden sm:inline">{user.email}</span>}
            <button
              type="button"
              onClick={handleLogout}
              className="rounded-md border border-slate-300 px-3 py-1.5 hover:bg-slate-100"
            >
              Sair
            </button>
          </div>
        </div>
      </header>
      <main className="flex-1">
        <div className="mx-auto max-w-5xl px-4 py-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
