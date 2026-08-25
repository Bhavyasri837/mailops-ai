import { useCallback, useMemo, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Search, FileText } from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import EmptyState from '../components/EmptyState.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { getInvoices } from '../services/api.js'
import { formatCurrency, formatDate, formatDateTime } from '../utils/format.js'

export default function Invoices() {
  const { onMenuClick } = useOutletContext()
  const [search, setSearch] = useState('')
  const fetcher = useCallback(() => getInvoices(), [])
  const { data: invoices, loading, error, refetch } = useFetch(fetcher)

  const filtered = useMemo(() => {
    if (!invoices) return []
    const q = search.trim().toLowerCase()
    if (!q) return invoices
    return invoices.filter(
      (inv) =>
        inv.invoiceNumber?.toLowerCase().includes(q) || inv.vendor?.toLowerCase().includes(q)
    )
  }, [invoices, search])

  return (
    <>
      <Topbar title="Invoices" subtitle="Created from submitted invoice emails" onMenuClick={onMenuClick} />
      <main className="flex-1 p-4 md:p-6 max-w-6xl w-full mx-auto space-y-4">
        <div className="relative max-w-sm">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            className="input pl-9"
            placeholder="Search invoice number or vendor…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search invoices"
          />
        </div>

        {loading && <LoadingSpinner label="Loading invoices…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}
        {!loading && !error && filtered.length === 0 && (
          <div className="card">
            <EmptyState icon={FileText} title="No invoices created yet" />
          </div>
        )}
        {!loading && !error && filtered.length > 0 && (
          <div className="card overflow-hidden overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/60 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  <th className="px-4 py-3">Invoice Number</th>
                  <th className="px-4 py-3">Vendor</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Due Date</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Created</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((inv) => (
                  <tr key={inv.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-900 font-mono">
                      {inv.invoiceNumber || '—'}
                    </td>
                    <td className="px-4 py-3 text-slate-600">{inv.vendor || '—'}</td>
                    <td className="px-4 py-3 text-slate-800 font-mono">
                      {formatCurrency(inv.amount, inv.currency)}
                    </td>
                    <td className="px-4 py-3 text-slate-600">{formatDate(inv.dueDate)}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={inv.status} />
                    </td>
                    <td className="px-4 py-3 text-slate-500 font-mono text-xs whitespace-nowrap">
                      {formatDateTime(inv.createdAt)}
                    </td>
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
