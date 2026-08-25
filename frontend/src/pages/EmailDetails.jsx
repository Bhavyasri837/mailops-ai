import { useCallback, useState } from 'react'
import { useParams, useOutletContext, Link } from 'react-router-dom'
import { ArrowLeft, PlayCircle, Mail, History } from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import ClassificationPanel from '../components/ClassificationPanel.jsx'
import ActionPanel from '../components/ActionPanel.jsx'
import AuditTimeline from '../components/AuditTimeline.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { useToast } from '../hooks/useToast.jsx'
import { getEmail, processEmail } from '../services/api.js'
import { formatDateTime } from '../utils/format.js'

export default function EmailDetails() {
  const { id } = useParams()
  const { onMenuClick } = useOutletContext()
  const toast = useToast()
  const [processing, setProcessing] = useState(false)

  const fetcher = useCallback(() => getEmail(id), [id])
  const { data, loading, error, refetch } = useFetch(fetcher, [id])

  async function handleProcess() {
    setProcessing(true)
    try {
      const result = await processEmail(id)
      if (result.outcome === 'NEEDS_REVIEW') {
        toast.info('Human review required — confidence below threshold.')
      } else if (result.outcome === 'PROCESSED') {
        toast.success('Email processed successfully.')
      } else if (result.outcome === 'FAILED') {
        toast.error(result.message || 'Unable to process this email.')
      }
      await refetch()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setProcessing(false)
    }
  }

  return (
    <>
      <Topbar
        title={data ? data.email.subject : 'Email Details'}
        subtitle={data ? `From ${data.email.sender}` : undefined}
        onMenuClick={onMenuClick}
      >
        {data?.email.status === 'RECEIVED' && (
          <button className="btn-primary" onClick={handleProcess} disabled={processing}>
            <PlayCircle size={15} aria-hidden="true" />
            {processing ? 'Processing…' : 'Process Email'}
          </button>
        )}
      </Topbar>

      <main className="flex-1 p-4 md:p-6 max-w-6xl w-full mx-auto">
        <Link
          to="/inbox"
          className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800 mb-4"
        >
          <ArrowLeft size={14} /> Back to Inbox
        </Link>

        {loading && <LoadingSpinner label="Loading email details…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}

        {!loading && !error && data && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="space-y-4">
              <EmailOriginalCard email={data.email} />
              <ClassificationPanel classification={data.latestClassification} />
              <ActionPanel actions={data.actions} />
            </div>
            <div className="card p-4 h-fit">
              <div className="flex items-center gap-2 mb-4">
                <History size={16} className="text-brand-500" aria-hidden="true" />
                <h2 className="text-sm font-semibold text-slate-900">Audit Timeline</h2>
              </div>
              <AuditTimeline entries={data.auditLog} />
            </div>
          </div>
        )}
      </main>
    </>
  )
}

function EmailOriginalCard({ email }) {
  return (
    <div className="card">
      <div className="flex items-center justify-between gap-2 px-4 py-3 border-b border-slate-100">
        <div className="flex items-center gap-2">
          <Mail size={16} className="text-brand-500" aria-hidden="true" />
          <h2 className="text-sm font-semibold text-slate-900">Original Email</h2>
        </div>
        <StatusBadge status={email.status} />
      </div>
      <div className="p-4 space-y-3">
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <Field label="Sender" value={email.sender} />
          <Field label="Recipient" value={email.recipient} />
          <Field label="Received" value={formatDateTime(email.receivedAt)} />
          <Field label="Subject" value={email.subject} span />
        </dl>
        <div>
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1.5">
            Body
          </p>
          <p className="text-sm text-slate-700 whitespace-pre-wrap leading-relaxed bg-slate-50 border border-slate-100 rounded-md px-3 py-2.5">
            {email.body}
          </p>
        </div>
      </div>
    </div>
  )
}

function Field({ label, value, span = false }) {
  return (
    <div className={span ? 'sm:col-span-2' : ''}>
      <dt className="text-[11px] text-slate-400">{label}</dt>
      <dd className="text-slate-800 font-medium truncate">{value || '—'}</dd>
    </div>
  )
}
