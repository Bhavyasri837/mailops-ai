export default function StatCard({ label, value, icon: Icon, tone = 'slate' }) {
  const toneClasses = {
    slate: 'bg-slate-100 text-slate-600',
    brand: 'bg-brand-50 text-brand-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-700',
    rose: 'bg-rose-50 text-rose-600',
    indigo: 'bg-indigo-50 text-indigo-600',
    sky: 'bg-sky-50 text-sky-600',
    orange: 'bg-orange-50 text-orange-600',
  }[tone]

  return (
    <div className="card p-4 flex items-center gap-3.5">
      {Icon && (
        <div className={`shrink-0 grid place-items-center h-10 w-10 rounded-lg ${toneClasses}`}>
          <Icon size={19} aria-hidden="true" />
        </div>
      )}
      <div className="min-w-0">
        <p className="text-xs font-medium text-slate-500 truncate">{label}</p>
        <p className="text-2xl font-semibold text-slate-900 font-mono tabular-nums leading-tight">
          {value ?? 0}
        </p>
      </div>
    </div>
  )
}
