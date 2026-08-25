import { useNavigate } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import StatusBadge from './StatusBadge.jsx'
import IntentBadge from './IntentBadge.jsx'
import ConfidenceBadge from './ConfidenceBadge.jsx'
import { formatDateTime } from '../utils/format.js'

export default function EmailTable({ emails, onProcess, processingId }) {
  const navigate = useNavigate()

  return (
    <div className="card overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50/60 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
              <th className="px-4 py-3">Sender</th>
              <th className="px-4 py-3">Subject</th>
              <th className="px-4 py-3">Intent</th>
              <th className="px-4 py-3">Confidence</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Received</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {emails.map((email) => (
              <tr
                key={email.id}
                onClick={() => navigate(`/inbox/${email.id}`)}
                className="border-b border-slate-100 last:border-0 hover:bg-slate-50 cursor-pointer transition-colors"
              >
                <td className="px-4 py-3 text-slate-600 max-w-[180px] truncate">{email.sender}</td>
                <td className="px-4 py-3 font-medium text-slate-900 max-w-[260px] truncate">
                  {email.subject}
                </td>
                <td className="px-4 py-3">
                  <IntentBadge intent={email.intent} />
                </td>
                <td className="px-4 py-3">
                  <ConfidenceBadge confidence={email.confidence} />
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={email.status} />
                </td>
                <td className="px-4 py-3 text-slate-500 whitespace-nowrap font-mono text-xs">
                  {formatDateTime(email.receivedAt)}
                </td>
                <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                  <div className="flex items-center justify-end gap-2">
                    {email.status === 'RECEIVED' && onProcess && (
                      <button
                        className="btn-secondary !py-1.5 !px-2.5 text-xs"
                        disabled={processingId === email.id}
                        onClick={() => onProcess(email.id)}
                      >
                        {processingId === email.id ? 'Processing…' : 'Process'}
                      </button>
                    )}
                    <button
                      className="text-slate-400 hover:text-slate-700"
                      aria-label={`View email ${email.subject}`}
                      onClick={() => navigate(`/inbox/${email.id}`)}
                    >
                      <ChevronRight size={16} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
