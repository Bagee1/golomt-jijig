import type { AuditAction, SystemEnvironment, SystemStatus, SystemType } from '../types/api'
import { actionLabel, statusLabel } from '../utils/format'

interface ChipProps {
  children: string | number
  tone?: 'green' | 'blue' | 'violet' | 'amber' | 'red' | 'gray' | 'teal'
  compact?: boolean
}

export function Chip({ children, tone = 'gray', compact = false }: ChipProps) {
  return <span className={`chip chip-${tone}${compact ? ' chip-compact' : ''}`}>{children}</span>
}

export function SystemTypeChip({ type }: { type: SystemType }) {
  const toneByType: Record<SystemType, ChipProps['tone']> = {
    CORE: 'teal',
    CARD: 'blue',
    DIGITAL: 'violet',
    INTERNAL: 'gray',
  }

  return <Chip tone={toneByType[type]}>{type}</Chip>
}

export function EnvironmentChip({ environment }: { environment: SystemEnvironment }) {
  const tone = environment === 'PROD' ? 'teal' : environment === 'UAT' ? 'amber' : environment === 'TEST' ? 'violet' : 'blue'
  return <Chip tone={tone}>{environment}</Chip>
}

export function StatusChip({ status }: { status: SystemStatus }) {
  const tone = status === 'ACTIVE' ? 'green' : status === 'UNKNOWN' ? 'amber' : status === 'DOWN' ? 'red' : 'gray'
  return (
    <span className={`status-pill status-${tone}`}>
      <span />
      {statusLabel(status)}
    </span>
  )
}

export function SecurityScore({ score }: { score: number }) {
  const tone = score >= 80 ? 'good' : score >= 60 ? 'warn' : 'bad'
  return <span className={`score-dot score-${tone}`}>{score}</span>
}

export function AuditActionChip({ action }: { action: AuditAction }) {
  const tone =
    action === 'LOGIN_SUCCESS'
      ? 'green'
      : action === 'LOGIN_FAILURE' || action === 'SYSTEM_DISABLED'
        ? 'red'
        : action === 'SECURITY_CHECK_UPDATED'
          ? 'amber'
          : action === 'SYSTEM_CREATED'
            ? 'blue'
            : 'violet'

  return <Chip tone={tone} compact>{actionLabel(action)}</Chip>
}
