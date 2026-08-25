import { Inbox } from 'lucide-react'

export default function EmptyState({ icon: Icon = Inbox, title, description, action = null }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-6">
      <div className="rounded-full bg-slate-100 p-3 mb-3">
        <Icon size={22} className="text-slate-400" aria-hidden="true" />
      </div>
      <p className="text-sm font-medium text-slate-700">{title}</p>
      {description && <p className="text-sm text-slate-500 mt-1 max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
