import { Sparkles } from 'lucide-react'
import IntentBadge from './IntentBadge.jsx'
import ConfidenceBadge from './ConfidenceBadge.jsx'
import EmptyState from './EmptyState.jsx'
import { titleCase } from '../utils/format.js'

export default function ClassificationPanel({ classification }) {
  if (!classification) {
    return (
      <div className="card">
        <PanelHeader />
        <EmptyState
          icon={Sparkles}
          title="Not yet classified"
          description="This email hasn't been processed by the AI classifier yet."
        />
      </div>
    )
  }

  const { intent, confidence, reason, evidence, extractedData, intentProbabilities } = classification
  const extractedEntries = Object.entries(extractedData || {}).filter(([, v]) => v !== null && v !== undefined && v !== '')

  return (
    <div className="card">
      <PanelHeader />
      <div className="p-4 space-y-4">
        <div className="flex flex-wrap items-center gap-2">
          <IntentBadge intent={intent} />
          <ConfidenceBadge confidence={confidence} />
        </div>

        {reason && (
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
              Reason
            </p>
            <p className="text-sm text-slate-700 leading-relaxed">{reason}</p>
          </div>
        )}

        {evidence?.length > 0 && (
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5">
              Evidence
            </p>
            <ul className="space-y-1.5">
              {evidence.map((item, i) => (
                <li
                  key={i}
                  className="text-sm text-slate-700 bg-slate-50 border border-slate-100 rounded-md px-2.5 py-1.5"
                >
                  {item}
                </li>
              ))}
            </ul>
          </div>
        )}

        {extractedEntries.length > 0 && (
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5">
              Extracted Data
            </p>
            <dl className="grid grid-cols-2 gap-2.5">
              {extractedEntries.map(([key, value]) => (
                <div key={key} className="bg-slate-50 border border-slate-100 rounded-md px-2.5 py-1.5">
                  <dt className="text-[11px] text-slate-500">{titleCase(key)}</dt>
                  <dd className="text-sm font-medium text-slate-800 font-mono truncate">{String(value)}</dd>
                </div>
              ))}
            </dl>
          </div>
        )}

        {intentProbabilities && Object.keys(intentProbabilities).length > 0 && (
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5">
              Intent Probabilities
            </p>
            <div className="space-y-1.5">
              {Object.entries(intentProbabilities)
                .sort((a, b) => b[1] - a[1])
                .map(([key, value]) => (
                  <div key={key} className="flex items-center gap-2">
                    <span className="text-xs text-slate-500 w-36 shrink-0">{titleCase(key)}</span>
                    <div className="flex-1 h-1.5 rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className="h-full bg-brand-500 rounded-full"
                        style={{ width: `${Math.round(value * 100)}%` }}
                      />
                    </div>
                    <span className="text-xs font-mono text-slate-500 w-9 text-right">
                      {Math.round(value * 100)}%
                    </span>
                  </div>
                ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function PanelHeader() {
  return (
    <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-100">
      <Sparkles size={16} className="text-brand-500" aria-hidden="true" />
      <h2 className="text-sm font-semibold text-slate-900">AI Classification</h2>
    </div>
  )
}
