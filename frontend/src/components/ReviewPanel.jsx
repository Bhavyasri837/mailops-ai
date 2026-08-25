import { useState } from 'react'
import { AlertTriangle, FileText, MessageCircleQuestion, Gavel, ShieldAlert, CheckCircle2 } from 'lucide-react'
import ConfidenceBadge from './ConfidenceBadge.jsx'
import IntentBadge from './IntentBadge.jsx'
import { formatDateTime } from '../utils/format.js'

const INTENT_OPTIONS = [
  { value: 'INVOICE_SUBMISSION', label: 'Invoice Submission', icon: FileText },
  { value: 'PAYMENT_QUERY', label: 'Payment Query', icon: MessageCircleQuestion },
  { value: 'DISPUTE', label: 'Dispute', icon: Gavel },
  { value: 'SPAM', label: 'Spam', icon: ShieldAlert },
]

export default function ReviewPanel({ email, classification, onSubmit, submitting, justResolved }) {
  const [selected, setSelected] = useState(null)

  return (
    <div className="card overflow-hidden">
      <div className="px-4 py-3 bg-amber-50 border-b border-amber-100 flex items-center gap-2">
        <AlertTriangle size={16} className="text-amber-600" aria-hidden="true" />
        <h2 className="text-sm font-semibold text-amber-900">Human Review Required</h2>
      </div>

      <div className="p-4 space-y-4">
        <div>
          <p className="text-sm font-medium text-slate-900">{email.subject}</p>
          <p className="text-xs text-slate-500 mt-0.5">
            From {email.sender} · {formatDateTime(email.receivedAt)}
          </p>
        </div>

        <p className="text-sm text-slate-600 bg-slate-50 border border-slate-100 rounded-md px-3 py-2 line-clamp-3">
          {email.body}
        </p>

        {classification && (
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs text-slate-500">AI predicted:</span>
            <IntentBadge intent={classification.intent} />
            <ConfidenceBadge confidence={classification.confidence} />
          </div>
        )}

        {classification?.reason && (
          <p className="text-sm text-slate-600 leading-relaxed">
            <span className="font-medium text-slate-700">Reason: </span>
            {classification.reason}
          </p>
        )}

        <p className="text-xs text-slate-400 italic">
          This email was not processed automatically because the AI confidence was below the
          configured threshold.
        </p>

        {justResolved ? (
          <div className="rounded-md bg-emerald-50 border border-emerald-100 px-3 py-2.5 space-y-1">
            <p className="flex items-center gap-1.5 text-sm text-emerald-800 font-medium">
              <CheckCircle2 size={14} /> Human decision recorded
            </p>
            <p className="flex items-center gap-1.5 text-sm text-emerald-800 font-medium">
              <CheckCircle2 size={14} /> Action executed
            </p>
            <p className="flex items-center gap-1.5 text-sm text-emerald-800 font-medium">
              <CheckCircle2 size={14} /> Audit trail updated
            </p>
          </div>
        ) : (
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
              Select correct intent
            </p>
            <div className="grid grid-cols-2 gap-2">
              {INTENT_OPTIONS.map(({ value, label, icon: Icon }) => (
                <button
                  key={value}
                  onClick={() => setSelected(value)}
                  className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                    selected === value
                      ? 'border-brand-500 bg-brand-50 text-brand-700'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  <Icon size={15} aria-hidden="true" />
                  {label}
                </button>
              ))}
            </div>
            <button
              className="btn-primary w-full mt-3"
              disabled={!selected || submitting}
              onClick={() => onSubmit(selected)}
            >
              {submitting ? 'Submitting…' : 'Confirm Decision'}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
