import type { ReactNode } from "react";

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
}

export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <div className="min-h-full flex flex-col">
      <header className="border-b border-[var(--color-rule)]">
        <div className="mx-auto max-w-md flex items-center gap-2 px-6 h-14 text-[14px] font-medium tracking-tight">
          <span
            aria-hidden
            className="inline-block w-1.5 h-1.5 rounded-full bg-[var(--color-accent)]"
          />
          Gerador de Dietas
        </div>
      </header>

      <main className="flex-1 flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm enter">
          <h1 className="text-[28px] font-medium tracking-tight leading-tight">
            {title}
          </h1>
          {subtitle && (
            <p className="mt-2 text-[14px] text-[var(--color-ink-3)] leading-relaxed">
              {subtitle}
            </p>
          )}

          <div className="mt-8">{children}</div>

          {footer && (
            <p className="mt-8 pt-6 border-t border-[var(--color-rule)] text-[13px] text-[var(--color-ink-3)] text-center">
              {footer}
            </p>
          )}
        </div>
      </main>
    </div>
  );
}
