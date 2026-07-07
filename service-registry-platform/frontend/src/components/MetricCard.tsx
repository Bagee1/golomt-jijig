import type { LucideIcon } from 'lucide-react'

interface MetricCardProps {
  label: string
  value: string | number
  helper: string
  icon: LucideIcon
  tone: 'green' | 'blue' | 'red' | 'teal'
}

export function MetricCard({ label, value, helper, icon: Icon, tone }: MetricCardProps) {
  return (
    <section className="metric-card">
      <div>
        <p className="metric-label">{label}</p>
        <strong>{value}</strong>
        <span>{helper}</span>
      </div>
      <div className={`metric-icon metric-${tone}`}>
        <Icon size={20} />
      </div>
    </section>
  )
}
