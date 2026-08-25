import { Bot, Cog, User, History } from 'lucide-react'
import EmptyState from './EmptyState.jsx'
import { formatDateTime, titleCase } from '../utils/format.js'

const ACTOR_CONFIG = {
  AI: { icon: Bot, dot: 'bg-brand-500', ring: 'ring-brand-100', chip: 'bg-brand-50 text-brand-700' },
  SYSTEM: { icon: Cog, dot: 'bg-slate-400', ring: 'ring-slate-100', chip: 'bg-slate-100 text-slate-600' },
  HUMAN: { icon: User, dot: 'bg-amber-500', ring: 'ring-amber-100', chip: 'bg-amber-50 text-amber-700' },
}

export default function AuditTimeline({ entries, showEmailColumn = false, emailSubjects = {} }) {
  if (!entries || entries.length === 0) {
    return (
      <EmptyState
        icon={History}
        title="No audit history"
        description="Events will appear here once this email is processed."
      />
    )
  }

  const sorted = [...entries].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))

  return (
    <ol className="relative pl-7">
      <div className="absolute left-[13px] top-2 bottom-2 w-px bg-slate-200" aria-hidden="true" />
      {sorted.map((entry, i) => {
        const config = ACTOR_CONFIG[entry.actor] || ACTOR_CONFIG.SYSTEM
        const Icon = config.icon
        return (
          <li key={entry.id ?? i} className="relative pb-6 last:pb-0">
            <span
              className={`absolute -left-7 top-0.5 grid place-items-center h-6 w-6 rounded-full ${config.dot} ring-4 ${config.ring}`}
              aria-hidden="true"
            >
              <Icon size={12} className="text-white" />
            </span>
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-medium text-slate-900">{titleCase(entry.eventType)}</span>
              <span className={`text-[11px] font-semibold px-1.5 py-0.5 rounded ${config.chip}`}>
                {entry.actor}
              </span>
              <span className="text-xs text-slate-400 font-mono ml-auto">
                {formatDateTime(entry.createdAt)}
              </span>
            </div>
            {entry.message && <p className="text-sm text-slate-600 mt-0.5">{entry.message}</p>}
            {showEmailColumn && emailSubjects[entry.emailId] && (
              <p className="text-xs text-slate-400 mt-0.5">
                Email: <span className="text-slate-500">{emailSubjects[entry.emailId]}</span>
              </p>
            )}
          </li>
        )
      })}
    </ol>
  )
}
