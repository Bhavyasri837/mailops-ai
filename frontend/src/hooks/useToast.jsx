import { createContext, useCallback, useContext, useRef, useState } from 'react'
import { CheckCircle2, XCircle, Info, X } from 'lucide-react'

const ToastContext = createContext(null)

let idCounter = 0

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const timers = useRef({})

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((t) => t.id !== id))
    clearTimeout(timers.current[id])
    delete timers.current[id]
  }, [])

  const push = useCallback(
    (message, variant = 'success') => {
      const id = ++idCounter
      setToasts((current) => [...current, { id, message, variant }])
      timers.current[id] = setTimeout(() => dismiss(id), 5000)
      return id
    },
    [dismiss]
  )

  const toast = {
    success: (message) => push(message, 'success'),
    error: (message) => push(message, 'error'),
    info: (message) => push(message, 'info'),
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div
        className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 w-full max-w-sm"
        role="region"
        aria-label="Notifications"
      >
        {toasts.map((t) => (
          <ToastItem key={t.id} toast={t} onDismiss={() => dismiss(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function ToastItem({ toast, onDismiss }) {
  const config = {
    success: {
      icon: CheckCircle2,
      classes: 'bg-white border-emerald-200 text-slate-900',
      iconClass: 'text-emerald-600',
    },
    error: {
      icon: XCircle,
      classes: 'bg-white border-rose-200 text-slate-900',
      iconClass: 'text-rose-600',
    },
    info: {
      icon: Info,
      classes: 'bg-white border-brand-200 text-slate-900',
      iconClass: 'text-brand-600',
    },
  }[toast.variant]

  const Icon = config.icon

  return (
    <div
      role="status"
      className={`flex items-start gap-2.5 rounded-lg border shadow-lg px-4 py-3 ${config.classes} animate-[fadeIn_0.15s_ease-out]`}
    >
      <Icon size={18} className={`shrink-0 mt-0.5 ${config.iconClass}`} aria-hidden="true" />
      <p className="text-sm leading-snug flex-1">{toast.message}</p>
      <button
        onClick={onDismiss}
        aria-label="Dismiss notification"
        className="text-slate-400 hover:text-slate-600 shrink-0"
      >
        <X size={16} />
      </button>
    </div>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within a ToastProvider')
  return ctx
}
