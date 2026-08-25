import {
  Clock,
  Loader2,
  CheckCircle2,
  AlertTriangle,
  ShieldAlert,
  XCircle,
  CircleDot,
  FileCheck,
  Copy,
} from 'lucide-react'
import { titleCase } from '../utils/format'

// One config table covering every status enum surfaced by the backend:
// EmailStatus, ActionStatus, InvoiceStatus, TaskStatus, ReplyDraftStatus.
// Keeping them in one component avoids a badge-per-entity duplication while
// still giving each value a distinct, accessible (icon + text, not just
// color) treatment.
const CONFIG = {
  RECEIVED: { icon: Clock, classes: 'bg-slate-100 text-slate-700 border-slate-200' },
  PROCESSING: { icon: Loader2, classes: 'bg-brand-50 text-brand-700 border-brand-200', spin: true },
  PROCESSED: { icon: CheckCircle2, classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  NEEDS_REVIEW: { icon: AlertTriangle, classes: 'bg-amber-50 text-amber-800 border-amber-200' },
  SPAM: { icon: ShieldAlert, classes: 'bg-slate-200 text-slate-700 border-slate-300' },
  FAILED: { icon: XCircle, classes: 'bg-rose-50 text-rose-700 border-rose-200' },

  SUCCESS: { icon: CheckCircle2, classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' },

  PENDING: { icon: Clock, classes: 'bg-amber-50 text-amber-800 border-amber-200' },
  LOGGED: { icon: FileCheck, classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' },

  OPEN: { icon: CircleDot, classes: 'bg-amber-50 text-amber-800 border-amber-200' },
  IN_PROGRESS: { icon: Loader2, classes: 'bg-brand-50 text-brand-700 border-brand-200' },
  RESOLVED: { icon: CheckCircle2, classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' },

  DRAFTED: { icon: FileCheck, classes: 'bg-brand-50 text-brand-700 border-brand-200' },
  COPIED: { icon: Copy, classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
}

export default function StatusBadge({ status, className = '' }) {
  const entry = CONFIG[status] || {
    icon: CircleDot,
    classes: 'bg-slate-100 text-slate-600 border-slate-200',
  }
  const Icon = entry.icon

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium whitespace-nowrap ${entry.classes} ${className}`}
    >
      <Icon size={12} className={entry.spin ? 'animate-spin' : ''} aria-hidden="true" />
      {titleCase(status)}
    </span>
  )
}
