import { useEffect, useRef } from 'react'
import { AlertTriangle } from 'lucide-react'

export default function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  destructive = false,
  loading = false,
  onConfirm,
  onCancel,
}) {
  const confirmRef = useRef(null)

  useEffect(() => {
    if (open) confirmRef.current?.focus()
  }, [open])

  useEffect(() => {
    function onKey(e) {
      if (e.key === 'Escape' && open) onCancel?.()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onCancel])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
    >
      <div className="card w-full max-w-md p-5">
        <div className="flex gap-3">
          <div
            className={`shrink-0 rounded-full p-2 h-fit ${
              destructive ? 'bg-rose-50' : 'bg-brand-50'
            }`}
          >
            <AlertTriangle
              size={18}
              className={destructive ? 'text-rose-600' : 'text-brand-600'}
              aria-hidden="true"
            />
          </div>
          <div>
            <h2 id="confirm-dialog-title" className="text-sm font-semibold text-slate-900">
              {title}
            </h2>
            {description && <p className="text-sm text-slate-500 mt-1">{description}</p>}
          </div>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <button className="btn-secondary" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
          <button
            ref={confirmRef}
            className={destructive ? 'btn bg-rose-600 text-white hover:bg-rose-700' : 'btn-primary'}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
