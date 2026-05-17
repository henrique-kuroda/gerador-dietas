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
      <span className="block text-sm font-medium text-slate-700 mb-1">
        {label}
      </span>
      {children}
      {hint && !error && (
        <span className="block mt-1 text-xs text-slate-500">{hint}</span>
      )}
      {error && (
        <span className="block mt-1 text-xs text-red-600">{error}</span>
      )}
    </label>
  );
}

const inputClass =
  "w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm " +
  "focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 " +
  "disabled:bg-slate-100";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

export function TextField({ label, error, hint, ...rest }: TextFieldProps) {
  return (
    <FieldShell label={label} error={error} hint={hint}>
      <input className={inputClass} {...rest} />
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
      <select className={inputClass} {...rest}>
        {children}
      </select>
    </FieldShell>
  );
}
