import { useEffect, useState } from 'react'
import { Outlet, NavLink } from 'react-router-dom'
import { X, Bot } from 'lucide-react'
import Sidebar from './Sidebar.jsx'
import { getDashboardStats } from '../services/api.js'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/inbox', label: 'Inbox' },
  { to: '/review', label: 'Human Review' },
  { to: '/invoices', label: 'Invoices' },
  { to: '/tasks', label: 'Tasks' },
  { to: '/reply-drafts', label: 'Reply Drafts' },
  { to: '/audit', label: 'Audit Log' },
]

export default function Layout() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [reviewCount, setReviewCount] = useState(null)

  // Lightweight, non-blocking poll purely for the sidebar's review-count
  // badge. Individual pages fetch their own data independently — this never
  // gates page rendering.
  useEffect(() => {
    let cancelled = false
    getDashboardStats()
      .then((stats) => {
        if (!cancelled) setReviewCount(stats.pendingHumanReview)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="flex min-h-screen bg-slate-50">
      <Sidebar reviewCount={reviewCount} />

      {mobileOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setMobileOpen(false)} />
          <div className="absolute inset-y-0 left-0 w-64 bg-white shadow-xl flex flex-col">
            <div className="flex items-center justify-between px-4 h-16 border-b border-slate-200">
              <div className="flex items-center gap-2">
                <div className="grid place-items-center h-8 w-8 rounded-lg bg-brand-600 text-white">
                  <Bot size={18} />
                </div>
                <span className="text-sm font-semibold">MailOps AI</span>
              </div>
              <button
                onClick={() => setMobileOpen(false)}
                aria-label="Close navigation menu"
                className="btn-ghost !px-2"
              >
                <X size={18} />
              </button>
            </div>
            <nav className="p-2.5" aria-label="Primary">
              <ul className="space-y-0.5">
                {NAV_ITEMS.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      onClick={() => setMobileOpen(false)}
                      className={({ isActive }) =>
                        `block rounded-lg px-3 py-2 text-sm font-medium ${
                          isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100'
                        }`
                      }
                    >
                      {item.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </nav>
          </div>
        </div>
      )}

      <div className="flex-1 flex flex-col min-w-0">
        <Outlet context={{ onMenuClick: () => setMobileOpen(true), reviewCount }} />
      </div>
    </div>
  )
}
