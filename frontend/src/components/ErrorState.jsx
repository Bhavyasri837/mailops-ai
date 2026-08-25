import { AlertOctagon, RotateCw } from 'lucide-react'

export default function ErrorState({ message, onRetry, title = 'Unable to load data' }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-6">
      <div className="rounded-full bg-rose-50 p-3 mb-3">
        <AlertOctagon size={22} className="text-rose-500" aria-hidden="true" />
      </div>
      <p className="text-sm font-medium text-slate-800">{title}</p>
      <p className="text-sm text-slate-500 mt-1 max-w-sm">
        {message || 'Something went wrong while talking to the server.'}
      </p>
      {onRetry && (
        <button onClick={onRetry} className="btn-secondary mt-4">
          <RotateCw size={14} aria-hidden="true" />
          Retry
        </button>
      )}
    </div>
  )
}
