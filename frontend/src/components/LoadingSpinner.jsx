import { Loader2 } from 'lucide-react'

export default function LoadingSpinner({ label = 'Loading…', size = 20, className = '' }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`flex items-center justify-center gap-2.5 text-slate-500 py-16 ${className}`}
    >
      <Loader2 size={size} className="animate-spin text-brand-500" aria-hidden="true" />
      <span className="text-sm">{label}</span>
    </div>
  )
}
