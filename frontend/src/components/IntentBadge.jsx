import { FileText, MessageCircleQuestion, Gavel, ShieldAlert, HelpCircle } from 'lucide-react'
import { titleCase } from '../utils/format'

const CONFIG = {
  INVOICE_SUBMISSION: { icon: FileText, classes: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  PAYMENT_QUERY: { icon: MessageCircleQuestion, classes: 'bg-sky-50 text-sky-700 border-sky-200' },
  DISPUTE: { icon: Gavel, classes: 'bg-orange-50 text-orange-700 border-orange-200' },
  SPAM: { icon: ShieldAlert, classes: 'bg-slate-200 text-slate-700 border-slate-300' },
}

export default function IntentBadge({ intent, className = '' }) {
  if (!intent) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-400">
        <HelpCircle size={12} aria-hidden="true" />
        Not classified
      </span>
    )
  }

  const entry = CONFIG[intent] || {
    icon: HelpCircle,
    classes: 'bg-slate-100 text-slate-600 border-slate-200',
  }
  const Icon = entry.icon

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium whitespace-nowrap ${entry.classes} ${className}`}
    >
      <Icon size={12} aria-hidden="true" />
      {titleCase(intent)}
    </span>
  )
}
