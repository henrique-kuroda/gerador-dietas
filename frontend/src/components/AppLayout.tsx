import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/useAuth";

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  [
    "text-[13px] transition-colors",
    isActive
      ? "text-[var(--color-ink)] font-medium"
      : "text-[var(--color-ink-3)] hover:text-[var(--color-ink)]",
  ].join(" ");

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="min-h-full flex flex-col">
      <header className="border-b border-[var(--color-rule)] bg-[var(--color-bg)]/85 backdrop-blur-sm sticky top-0 z-10">
        <div className="mx-auto max-w-4xl flex items-center justify-between gap-6 px-6 h-14">
          <Link
            to="/"
            className="flex items-center gap-2 text-[14px] font-medium text-[var(--color-ink)] tracking-tight"
          >
            <span
              aria-hidden
              className="inline-block w-1.5 h-1.5 rounded-full bg-[var(--color-accent)]"
            />
            Gerador de Dietas
          </Link>

          <nav className="hidden sm:flex items-center gap-7">
            <NavLink to="/" end className={navLinkClass}>
              Início
            </NavLink>
            <NavLink to="/profile" className={navLinkClass}>
              Perfil
            </NavLink>
            <NavLink to="/history" className={navLinkClass}>
              Histórico
            </NavLink>
          </nav>

          <div className="flex items-center gap-3">
            {user && (
              <span className="hidden md:inline text-[12px] text-[var(--color-ink-3)] max-w-[12rem] truncate">
                {user.email}
              </span>
            )}
            <button type="button" onClick={handleLogout} className="btn-ghost">
              Sair
            </button>
          </div>
        </div>

        {/* mobile nav */}
        <div className="sm:hidden border-t border-[var(--color-rule)] px-6 h-11 flex items-center gap-6">
          <NavLink to="/" end className={navLinkClass}>Início</NavLink>
          <NavLink to="/profile" className={navLinkClass}>Perfil</NavLink>
          <NavLink to="/history" className={navLinkClass}>Histórico</NavLink>
        </div>
      </header>

      <main className="flex-1">
        <div key={location.pathname} className="mx-auto max-w-4xl px-6 py-12 enter">
          <Outlet />
        </div>
      </main>

      <footer className="border-t border-[var(--color-rule)] mt-8">
        <div className="mx-auto max-w-4xl px-6 py-5 flex items-center justify-between text-[12px] text-[var(--color-ink-3)]">
          <span>Gerador de Dietas</span>
          <span className="tabular">© 2026</span>
        </div>
      </footer>
    </div>
  );
}
