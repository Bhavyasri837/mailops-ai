import { ShieldCheck, ShieldQuestion } from 'lucide-react'
import { formatPercent } from '../utils/format'

const THRESHOLD = 0.85

export default function ConfidenceBadge({ confidence, className = '' }) {
  if (confidence === null || confidence === undefined) {
    return <span className="text-xs text-slate-400">—</span>
  }

  const isHigh = confidence >= THRESHOLD
  const Icon = isHigh ? ShieldCheck : ShieldQuestion

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 font-mono text-xs font-semibold ${
        isHigh
          ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
          : 'bg-amber-50 text-amber-800 border-amber-200'
      } ${className}`}
      title={isHigh ? 'Meets the autonomous-action confidence threshold' : 'Below confidence threshold — requires human review'}
    >
      <Icon size={12} aria-hidden="true" />
      <span>{formatPercent(confidence)}</span>
      <span className="uppercase tracking-wide text-[10px] font-bold opacity-80">
        {isHigh ? 'High' : 'Review'}
      </span>
    </span>
  )
}
