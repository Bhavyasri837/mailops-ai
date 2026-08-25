import { useCallback } from 'react'
import { useOutletContext } from 'react-router-dom'
import { ListTodo } from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import EmptyState from '../components/EmptyState.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { getTasks } from '../services/api.js'
import { formatDateTime } from '../utils/format.js'

const PRIORITY_CLASSES = {
  LOW: 'bg-slate-100 text-slate-600 border-slate-200',
  MEDIUM: 'bg-amber-50 text-amber-700 border-amber-200',
  HIGH: 'bg-rose-50 text-rose-700 border-rose-200',
}

function PriorityBadge({ priority }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold ${
        PRIORITY_CLASSES[priority] || PRIORITY_CLASSES.LOW
      }`}
    >
      {priority}
    </span>
  )
}

export default function Tasks() {
  const { onMenuClick } = useOutletContext()
  const fetcher = useCallback(() => getTasks(), [])
  const { data: tasks, loading, error, refetch } = useFetch(fetcher)

  return (
    <>
      <Topbar title="Tasks" subtitle="Follow-up tasks created from disputes" onMenuClick={onMenuClick} />
      <main className="flex-1 p-4 md:p-6 max-w-6xl w-full mx-auto space-y-4">
        {loading && <LoadingSpinner label="Loading tasks…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}
        {!loading && !error && tasks?.length === 0 && (
          <div className="card">
            <EmptyState icon={ListTodo} title="No tasks available" />
          </div>
        )}
        {!loading && !error && tasks?.length > 0 && (
          <div className="grid grid-cols-1 gap-3">
            {tasks.map((task) => (
              <div key={task.id} className="card p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-900">{task.title}</p>
                    {task.description && (
                      <p className="text-sm text-slate-600 mt-1 leading-relaxed">{task.description}</p>
                    )}
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <PriorityBadge priority={task.priority} />
                    <StatusBadge status={task.status} />
                  </div>
                </div>
                <p className="text-xs text-slate-400 font-mono mt-3">{formatDateTime(task.createdAt)}</p>
              </div>
            ))}
          </div>
        )}
      </main>
    </>
  )
}
