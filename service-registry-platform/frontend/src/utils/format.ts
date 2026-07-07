import type { AuditAction, SystemStatus } from '../types/api'

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('mn-MN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function formatMoneyMnt(value: number) {
  if (value >= 1_000_000_000_000) {
    return `${trimNumber(value / 1_000_000_000_000)}T ₮`
  }
  if (value >= 1_000_000_000) {
    return `${trimNumber(value / 1_000_000_000)}B ₮`
  }
  if (value >= 1_000_000) {
    return `${trimNumber(value / 1_000_000)}M ₮`
  }
  return `${new Intl.NumberFormat('mn-MN').format(value)} ₮`
}

export function formatExactMnt(value: number, currency = 'MNT') {
  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
  return `${formatted} ${currency}`
}

export function statusLabel(status: SystemStatus) {
  const labels: Record<SystemStatus, string> = {
    ACTIVE: 'Идэвхтэй',
    INACTIVE: 'Идэвхгүй',
    UNKNOWN: 'Мэдэгдэхгүй',
    DOWN: 'DOWN',
  }
  return labels[status]
}

export function actionLabel(action: AuditAction) {
  return action.replaceAll('_', ' ')
}

export function extractSystemKey(metadataJson: string | null) {
  if (!metadataJson) {
    return '-'
  }

  try {
    const metadata = JSON.parse(metadataJson) as { systemKey?: string; username?: string }
    return metadata.systemKey ?? metadata.username ?? '-'
  } catch {
    return '-'
  }
}

function trimNumber(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
}
