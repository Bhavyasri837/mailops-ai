import { useCallback, useEffect, useMemo, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Search, PlayCircle } from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import EmailTable from '../components/EmailTable.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import EmptyState from '../components/EmptyState.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { useToast } from '../hooks/useToast.jsx'
import { getEmails, getEmailClassification, processEmail, processAllEmails } from '../services/api.js'

const STATUS_OPTIONS = ['RECEIVED', 'PROCESSING', 'PROCESSED', 'NEEDS_REVIEW', 'SPAM', 'FAILED']
const INTENT_OPTIONS = ['INVOICE_SUBMISSION', 'PAYMENT_QUERY', 'DISPUTE', 'SPAM']

export default function Inbox() {
  const { onMenuClick } = useOutletContext()
  const toast = useToast()

  const [status, setStatus] = useState('')
  const [search, setSearch] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [intentFilter, setIntentFilter] = useState('')
  const [processingId, setProcessingId] = useState(null)
  const [confirmAllOpen, setConfirmAllOpen] = useState(false)
  const [processingAll, setProcessingAll] = useState(false)

  // Debounce free-text search before it hits the server-side `search` param.
  useEffect(() => {
    const t = setTimeout(() => setSearch(searchInput.trim()), 350)
    return () => clearTimeout(t)
  }, [searchInput])

  const fetcher = useCallback(async () => {
    const params = {}
    if (status) params.status = status
    if (search) params.search = search
    const emails = await getEmails(params)

    // GET /api/emails returns EmailDto only (no intent/confidence — those
    // live on Classification). Enrich each row with its latest
    // classification so the Inbox table can show Intent/Confidence columns
    // without inventing data. Dataset is seed-sized, so N+1 is acceptable;
    // failures (e.g. an email with no classification yet) are swallowed per
    // row rather than failing the whole list.
    const withClassification = await Promise.all(
      emails.map(async (email) => {
        if (email.status === 'RECEIVED') return email
        try {
          const c = await getEmailClassification(email.id)
          return { ...email, intent: c.intent, confidence: c.confidence }
        } catch {
          return email
        }
      })
    )
    return withClassification
  }, [status, search])

  const { data: emails, loading, error, refetch } = useFetch(fetcher, [status, search])

  const filtered = useMemo(() => {
    if (!emails) return []
    if (!intentFilter) return emails
    return emails.filter((e) => e.intent === intentFilter)
  }, [emails, intentFilter])

  async function handleProcess(id) {
    setProcessingId(id)
    try {
      const result = await processEmail(id)
      if (result.outcome === 'NEEDS_REVIEW') {
        toast.info('Human review required — confidence below threshold.')
      } else if (result.outcome === 'PROCESSED') {
        toast.success('Email processed successfully.')
      } else if (result.outcome === 'FAILED') {
        toast.error(result.message || 'Unable to process this email.')
      }
      refetch()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setProcessingId(null)
    }
  }

  async function handleProcessAll() {
    setProcessingAll(true)
    try {
      const result = await processAllEmails()
      toast.success(
        `Processed ${result.totalAttempted} email(s): ${result.processed} processed, ${result.needsReview} need review, ${result.failed} failed.`
      )
      refetch()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setProcessingAll(false)
      setConfirmAllOpen(false)
    }
  }

  return (
    <>
      <Topbar title="Inbox" subtitle="All received emails" onMenuClick={onMenuClick}>
        <button className="btn-primary" onClick={() => setConfirmAllOpen(true)}>
          <PlayCircle size={15} aria-hidden="true" />
          Process All
        </button>
      </Topbar>

      <main className="flex-1 p-4 md:p-6 max-w-7xl w-full mx-auto space-y-4">
        <div className="flex flex-col sm:flex-row gap-2.5">
          <div className="relative flex-1">
            <Search
              size={16}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            />
            <input
              type="text"
              className="input pl-9"
              placeholder="Search sender, subject, or body…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              aria-label="Search emails"
            />
          </div>
          <select
            className="input sm:w-48"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            aria-label="Filter by status"
          >
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s.replaceAll('_', ' ')}
              </option>
            ))}
          </select>
          <select
            className="input sm:w-48"
            value={intentFilter}
            onChange={(e) => setIntentFilter(e.target.value)}
            aria-label="Filter by intent"
          >
            <option value="">All intents</option>
            {INTENT_OPTIONS.map((i) => (
              <option key={i} value={i}>
                {i.replaceAll('_', ' ')}
              </option>
            ))}
          </select>
        </div>

        {loading && <LoadingSpinner label="Loading emails…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}
        {!loading && !error && filtered.length === 0 && (
          <div className="card">
            <EmptyState title="No emails found" description="Try adjusting your search or filters." />
          </div>
        )}
        {!loading && !error && filtered.length > 0 && (
          <EmailTable emails={filtered} onProcess={handleProcess} processingId={processingId} />
        )}
      </main>

      <ConfirmDialog
        open={confirmAllOpen}
        title="Process all pending emails?"
        description="This will run the AI classifier and execute the resulting action for every email currently in RECEIVED status."
        confirmLabel="Process All"
        loading={processingAll}
        onConfirm={handleProcessAll}
        onCancel={() => setConfirmAllOpen(false)}
      />
    </>
  )
}
