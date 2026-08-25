import { useCallback } from 'react'
import { useOutletContext } from 'react-router-dom'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts'
import {
  Mail,
  CheckCircle2,
  UserCheck,
  ShieldAlert,
  XCircle,
  FileText,
  MessageCircleQuestion,
  Gavel,
} from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import StatCard from '../components/StatCard.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { getDashboardStats } from '../services/api.js'
import { titleCase } from '../utils/format.js'

const INTENT_COLORS = {
  INVOICE_SUBMISSION: '#4f46e5',
  PAYMENT_QUERY: '#0284c7',
  DISPUTE: '#ea580c',
  SPAM: '#64748b',
}

export default function Dashboard() {
  const { onMenuClick } = useOutletContext()
  const fetcher = useCallback(() => getDashboardStats(), [])
  const { data: stats, loading, error, refetch } = useFetch(fetcher)

  return (
    <>
      <Topbar title="Dashboard" subtitle="Email operations overview" onMenuClick={onMenuClick} />
      <main className="flex-1 p-4 md:p-6 max-w-7xl w-full mx-auto">
        {loading && <LoadingSpinner label="Loading dashboard…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}
        {!loading && !error && stats && <DashboardContent stats={stats} />}
      </main>
    </>
  )
}

function DashboardContent({ stats }) {
  const {
    totalEmails,
    emailsByStatus = {},
    classificationsByIntent = {},
    autonomousActionsExecuted,
    humanReviewsCompleted,
    pendingHumanReview,
    invoicesCreated,
    tasksCreated,
    replyDraftsCreated,
    spamFlagged,
    actionsFailed,
  } = stats

  const statusChartData = Object.entries(emailsByStatus).map(([status, count]) => ({
    status: titleCase(status),
    count,
  }))

  const intentChartData = Object.entries(classificationsByIntent)
    .filter(([, count]) => count > 0)
    .map(([intent, count]) => ({
      name: titleCase(intent),
      value: count,
      color: INTENT_COLORS[intent] || '#64748b',
    }))

  return (
    <div className="space-y-6">
      <section>
        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2.5">
          Email Volume
        </p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Total Emails" value={totalEmails} icon={Mail} tone="brand" />
          <StatCard
            label="Processed"
            value={emailsByStatus.PROCESSED}
            icon={CheckCircle2}
            tone="emerald"
          />
          <StatCard label="Needs Review" value={pendingHumanReview} icon={UserCheck} tone="amber" />
          <StatCard label="Spam" value={spamFlagged} icon={ShieldAlert} tone="slate" />
        </div>
      </section>

      <section>
        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2.5">
          Actions &amp; Outcomes
        </p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Invoices" value={invoicesCreated} icon={FileText} tone="indigo" />
          <StatCard
            label="Payment Queries"
            value={classificationsByIntent.PAYMENT_QUERY}
            icon={MessageCircleQuestion}
            tone="sky"
          />
          <StatCard label="Disputes" value={classificationsByIntent.DISPUTE} icon={Gavel} tone="orange" />
          <StatCard label="Failed" value={actionsFailed} icon={XCircle} tone="rose" />
        </div>
      </section>

      <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="card p-4">
          <h2 className="text-sm font-semibold text-slate-900 mb-4">Intent Distribution</h2>
          {intentChartData.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-12">No classifications yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={intentChartData}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={90}
                  paddingAngle={2}
                >
                  {intentChartData.map((entry) => (
                    <Cell key={entry.name} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend verticalAlign="bottom" height={32} iconType="circle" />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="card p-4">
          <h2 className="text-sm font-semibold text-slate-900 mb-4">Processing Status</h2>
          {statusChartData.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-12">No emails yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={statusChartData} margin={{ top: 4, right: 8, left: -16, bottom: 4 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                <XAxis dataKey="status" tick={{ fontSize: 11, fill: '#64748b' }} interval={0} angle={-20} textAnchor="end" height={50} />
                <YAxis tick={{ fontSize: 11, fill: '#64748b' }} allowDecimals={false} />
                <Tooltip cursor={{ fill: '#f1f5f9' }} />
                <Bar dataKey="count" fill="#3b5cdb" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </section>

      <section className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard label="Autonomous Actions" value={autonomousActionsExecuted} tone="brand" />
        <StatCard label="Human Reviews Completed" value={humanReviewsCompleted} tone="amber" />
        <StatCard label="Tasks Created" value={tasksCreated} tone="orange" />
        <StatCard label="Reply Drafts" value={replyDraftsCreated} tone="sky" />
      </section>
    </div>
  )
}
