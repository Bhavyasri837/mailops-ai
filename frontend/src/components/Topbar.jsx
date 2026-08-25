import { Menu } from 'lucide-react'

export default function Topbar({ title, subtitle, onMenuClick, children }) {
  return (
    <header className="sticky top-0 z-30 flex items-center justify-between gap-4 h-16 px-4 md:px-6 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="flex items-center gap-3 min-w-0">
        <button
          onClick={onMenuClick}
          className="md:hidden btn-ghost !px-2"
          aria-label="Open navigation menu"
        >
          <Menu size={20} />
        </button>
        <div className="min-w-0">
          <h1 className="text-base font-semibold text-slate-900 truncate">{title}</h1>
          {subtitle && <p className="text-xs text-slate-500 truncate">{subtitle}</p>}
        </div>
      </div>
      {children && <div className="flex items-center gap-2 shrink-0">{children}</div>}
    </header>
  )
}
