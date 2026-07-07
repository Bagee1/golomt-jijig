import type { AccountStatus, BankAuditAction, TransferStatus } from '../types/banking'
import type { DepositStatus } from '../types/deposit'

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('mn-MN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function formatExactMnt(value: number, currency = 'MNT') {
  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
  return `${formatted} ${currency}`
}

export function transferStatusLabel(status: TransferStatus) {
  const labels: Record<TransferStatus, string> = {
    PENDING: 'Хүлээгдэж буй',
    SUCCESS: 'Амжилттай',
    FAILED: 'Амжилтгүй',
    REVERSED: 'Буцаагдсан',
  }
  return labels[status]
}

export function accountStatusLabel(status: AccountStatus) {
  const labels: Record<AccountStatus, string> = {
    ACTIVE: 'Идэвхтэй',
    BLOCKED: 'Блоклогдсон',
    CLOSED: 'Хаагдсан',
  }
  return labels[status]
}

export function bankActionLabel(action: BankAuditAction) {
  return action.replaceAll('_', ' ')
}

export function depositStatusLabel(status: DepositStatus) {
  const labels: Record<DepositStatus, string> = {
    FUNDING: 'Санхүүжилт хүлээгдэж буй',
    OPEN: 'Идэвхтэй',
    PAYOUT_PENDING: 'Эргэн төлөлт хүлээгдэж буй',
    CLOSED: 'Хаагдсан',
    CLOSED_EARLY: 'Хугацаанаас өмнө хаагдсан',
    CANCELLED: 'Цуцлагдсан',
  }
  return labels[status]
}
