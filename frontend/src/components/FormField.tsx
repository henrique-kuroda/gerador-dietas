import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";

interface FieldShellProps {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}

export function FieldShell({ label, error, hint, children }: FieldShellProps) {
  return (
    <label className="block">
      <span className="block text-[13px] font-medium text-[var(--color-ink)] mb-1.5">
        {label}
      </span>
      {children}
      {hint && !error && (
        <span className="block mt-1.5 text-xs text-[var(--color-ink-3)]">
          {hint}
        </span>
      )}
      {error && (
        <span className="block mt-1.5 text-xs text-[var(--color-ink)]">
          {error}
        </span>
      )}
    </label>
  );
}

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

export function TextField({ label, error, hint, ...rest }: TextFieldProps) {
  return (
    <FieldShell label={label} error={error} hint={hint}>
      <input className="field" {...rest} />
    </FieldShell>
  );
}

interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}

export function SelectField({
  label,
  error,
  hint,
  children,
  ...rest
}: SelectFieldProps) {
  return (
    <FieldShell label={label} error={error} hint={hint}>
      <select className="field" {...rest}>
        {children}
      </select>
    </FieldShell>
  );
}
