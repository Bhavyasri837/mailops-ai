import { Zap } from 'lucide-react'
import StatusBadge from './StatusBadge.jsx'
import EmptyState from './EmptyState.jsx'
import { titleCase, formatDateTime } from '../utils/format.js'

export default function ActionPanel({ actions }) {
  const latest = actions && actions.length > 0 ? actions[actions.length - 1] : null

  return (
    <div className="card">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-100">
        <Zap size={16} className="text-brand-500" aria-hidden="true" />
        <h2 className="text-sm font-semibold text-slate-900">Action</h2>
      </div>

      {!latest ? (
        <EmptyState
          icon={Zap}
          title="No action taken yet"
          description="An action is executed automatically once this email is processed."
        />
      ) : (
        <div className="p-4 space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm font-medium text-slate-900">{titleCase(latest.actionType)}</span>
            <StatusBadge status={latest.actionStatus} />
          </div>
          {latest.result && <p className="text-sm text-slate-600 leading-relaxed">{latest.result}</p>}
          <p className="text-xs text-slate-400 font-mono">{formatDateTime(latest.createdAt)}</p>
        </div>
      )}
    </div>
  )
}
