import { useCallback } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Copy, MailPlus } from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import EmptyState from '../components/EmptyState.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { useToast } from '../hooks/useToast.jsx'
import { getReplyDrafts } from '../services/api.js'
import { formatDateTime } from '../utils/format.js'

export default function ReplyDrafts() {
  const { onMenuClick } = useOutletContext()
  const toast = useToast()
  const fetcher = useCallback(() => getReplyDrafts(), [])
  const { data: drafts, loading, error, refetch } = useFetch(fetcher)

  async function handleCopy(text) {
    try {
      await navigator.clipboard.writeText(text)
      toast.success('Draft copied to clipboard.')
    } catch {
      toast.error('Unable to copy — your browser blocked clipboard access.')
    }
  }

  return (
    <>
      <Topbar
        title="Reply Drafts"
        subtitle="AI-generated draft replies for payment queries"
        onMenuClick={onMenuClick}
      />
      <main className="flex-1 p-4 md:p-6 max-w-5xl w-full mx-auto space-y-4">
        {loading && <LoadingSpinner label="Loading reply drafts…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}
        {!loading && !error && drafts?.length === 0 && (
          <div className="card">
            <EmptyState icon={MailPlus} title="No reply drafts yet" />
          </div>
        )}
        {!loading && !error && drafts?.length > 0 && (
          <div className="grid grid-cols-1 gap-3">
            {drafts.map((draft) => (
              <div key={draft.id} className="card p-4">
                <div className="flex items-center justify-between gap-2 mb-2.5">
                  <p className="text-xs text-slate-400 font-mono">
                    Email #{draft.emailId} · {formatDateTime(draft.createdAt)}
                  </p>
                  <StatusBadge status={draft.status} />
                </div>
                <p className="text-sm text-slate-700 whitespace-pre-wrap leading-relaxed bg-slate-50 border border-slate-100 rounded-md px-3 py-2.5">
                  {draft.draftText}
                </p>
                <button
                  className="btn-secondary mt-3 text-xs !py-1.5"
                  onClick={() => handleCopy(draft.draftText)}
                >
                  <Copy size={13} aria-hidden="true" />
                  Copy Draft
                </button>
              </div>
            ))}
          </div>
        )}
      </main>
    </>
  )
}
