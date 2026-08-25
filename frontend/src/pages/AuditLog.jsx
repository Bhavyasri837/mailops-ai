import { useCallback, useMemo, useState } from 'react'
import { useOutletContext, Link } from 'react-router-dom'
import { ScrollText, Search } from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import EmptyState from '../components/EmptyState.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { getEmails, getEmailAudit } from '../services/api.js'
import { formatDateTime, titleCase } from '../utils/format.js'

const ACTOR_CLASSES = {
  AI: 'bg-brand-50 text-brand-700 border-brand-200',
  SYSTEM: 'bg-slate-100 text-slate-600 border-slate-200',
  HUMAN: 'bg-amber-50 text-amber-700 border-amber-200',
}

export default function AuditLog() {
  const { onMenuClick } = useOutletContext()
  const [search, setSearch] = useState('')

  // The backend exposes audit only per-email (GET /api/emails/{id}/audit) —
  // there is no global /api/audit endpoint. This page adapts on the
  // frontend: list every email, fetch each one's audit trail, and merge
  // into a single chronological log. Reasonable at the seeded sample-data
  // scale used by this assessment; a dedicated aggregate endpoint would be
  // worth adding server-side if this needs to scale beyond that.
  const fetcher = useCallback(async () => {
    const emails = await getEmails()
    const perEmail = await Promise.all(
      emails.map(async (email) => {
        try {
          const audit = await getEmailAudit(email.id)
          return audit.map((entry) => ({ ...entry, emailSubject: email.subject }))
        } catch {
          return []
        }
      })
    )
    return perEmail.flat().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }, [])

  const { data: entries, loading, error, refetch } = useFetch(fetcher)

  const filtered = useMemo(() => {
    if (!entries) return []
    const q = search.trim().toLowerCase()
    if (!q) return entries
    return entries.filter(
      (e) =>
        e.emailSubject?.toLowerCase().includes(q) ||
        e.message?.toLowerCase().includes(q) ||
        e.eventType?.toLowerCase().includes(q)
    )
  }, [entries, search])

  return (
    <>
      <Topbar title="Audit Log" subtitle="Every decision and action, across all emails" onMenuClick={onMenuClick} />
      <main className="flex-1 p-4 md:p-6 max-w-6xl w-full mx-auto space-y-4">
        <div className="relative max-w-sm">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            className="input pl-9"
            placeholder="Search event, email, or message…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search audit log"
          />
        </div>

        {loading && <LoadingSpinner label="Loading audit log…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}
        {!loading && !error && filtered.length === 0 && (
          <div className="card">
            <EmptyState icon={ScrollText} title="No audit events found" />
          </div>
        )}
        {!loading && !error && filtered.length > 0 && (
          <div className="card overflow-hidden overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/60 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  <th className="px-4 py-3">Timestamp</th>
                  <th className="px-4 py-3">Actor</th>
                  <th className="px-4 py-3">Event</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Message</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((entry) => (
                  <tr
                    key={`${entry.emailId}-${entry.id}`}
                    className="border-b border-slate-100 last:border-0 hover:bg-slate-50"
                  >
                    <td className="px-4 py-3 text-slate-500 font-mono text-xs whitespace-nowrap">
                      {formatDateTime(entry.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${
                          ACTOR_CLASSES[entry.actor] || ACTOR_CLASSES.SYSTEM
                        }`}
                      >
                        {entry.actor}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium text-slate-900 whitespace-nowrap">
                      {titleCase(entry.eventType)}
                    </td>
                    <td className="px-4 py-3 text-slate-600 max-w-[220px] truncate">
                      <Link to={`/inbox/${entry.emailId}`} className="hover:text-brand-600 hover:underline">
                        {entry.emailSubject || `Email #${entry.emailId}`}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-slate-600 max-w-[360px]">{entry.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </>
  )
}
