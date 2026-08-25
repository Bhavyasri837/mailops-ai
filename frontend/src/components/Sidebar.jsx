import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  Inbox as InboxIcon,
  UserCheck,
  FileText,
  ListTodo,
  MailPlus,
  ScrollText,
  Bot,
} from 'lucide-react'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/inbox', label: 'Inbox', icon: InboxIcon },
  { to: '/review', label: 'Human Review', icon: UserCheck, badgeKey: 'pendingHumanReview' },
  { to: '/invoices', label: 'Invoices', icon: FileText },
  { to: '/tasks', label: 'Tasks', icon: ListTodo },
  { to: '/reply-drafts', label: 'Reply Drafts', icon: MailPlus },
  { to: '/audit', label: 'Audit Log', icon: ScrollText },
]

export default function Sidebar({ reviewCount }) {
  return (
    <aside className="hidden md:flex md:w-60 shrink-0 flex-col border-r border-slate-200 bg-white">
      <div className="flex items-center gap-2.5 px-5 h-16 border-b border-slate-200">
        <div className="grid place-items-center h-8 w-8 rounded-lg bg-brand-600 text-white">
          <Bot size={18} aria-hidden="true" />
        </div>
        <div className="leading-tight">
          <p className="text-sm font-semibold text-slate-900">MailOps AI</p>
          <p className="text-[11px] text-slate-400">Email Operations</p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto py-3 px-2.5" aria-label="Primary">
        <ul className="space-y-0.5">
          {NAV_ITEMS.map(({ to, label, icon: Icon, badgeKey }) => {
            const count = badgeKey === 'pendingHumanReview' ? reviewCount : null
            return (
              <li key={to}>
                <NavLink
                  to={to}
                  className={({ isActive }) =>
                    `flex items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-brand-50 text-brand-700'
                        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      <span className="flex items-center gap-2.5">
                        <Icon
                          size={17}
                          className={isActive ? 'text-brand-600' : 'text-slate-400'}
                          aria-hidden="true"
                        />
                        {label}
                      </span>
                      {!!count && (
                        <span className="rounded-full bg-amber-100 text-amber-800 text-[11px] font-semibold px-1.5 py-0.5 min-w-[18px] text-center">
                          {count}
                        </span>
                      )}
                    </>
                  )}
                </NavLink>
              </li>
            )
          })}
        </ul>
      </nav>

      <div className="px-4 py-3 border-t border-slate-200 text-[11px] text-slate-400">
        MAILOPS
      </div>
    </aside>
  )
}
