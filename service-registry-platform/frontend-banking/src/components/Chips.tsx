import type { AccountStatus, BankAuditAction, TransferStatus } from '../types/banking'
import type { DepositStatus } from '../types/deposit'
import { accountStatusLabel, bankActionLabel, depositStatusLabel, transferStatusLabel } from '../utils/format'

interface ChipProps {
  children: string | number
  tone?: 'green' | 'blue' | 'violet' | 'amber' | 'red' | 'gray' | 'teal'
  compact?: boolean
}

export function Chip({ children, tone = 'gray', compact = false }: ChipProps) {
  return <span className={`chip chip-${tone}${compact ? ' chip-compact' : ''}`}>{children}</span>
}

export function TransferStatusChip({ status, compact = false }: { status: TransferStatus; compact?: boolean }) {
  const tone: ChipProps['tone'] =
    status === 'SUCCESS' ? 'green' : status === 'FAILED' ? 'red' : status === 'REVERSED' ? 'amber' : 'blue'
  return <Chip tone={tone} compact={compact}>{transferStatusLabel(status)}</Chip>
}

export function AccountStatusChip({ status, compact = false }: { status: AccountStatus; compact?: boolean }) {
  const tone: ChipProps['tone'] = status === 'ACTIVE' ? 'green' : status === 'BLOCKED' ? 'amber' : 'gray'
  return <Chip tone={tone} compact={compact}>{accountStatusLabel(status)}</Chip>
}

export function DepositStatusChip({ status, compact = false }: { status: DepositStatus; compact?: boolean }) {
  const tone: ChipProps['tone'] =
    status === 'OPEN' ? 'green'
      : status === 'FUNDING' || status === 'PAYOUT_PENDING' ? 'amber'
        : status === 'CLOSED_EARLY' ? 'violet'
          : status === 'CANCELLED' ? 'red'
            : 'gray'
  return <Chip tone={tone} compact={compact}>{depositStatusLabel(status)}</Chip>
}

export function BankAuditActionChip({ action }: { action: BankAuditAction }) {
  const tone: ChipProps['tone'] =
    action === 'TRANSFER_FAILED' || action === 'ACCOUNT_BLOCKED' || action === 'CUSTOMER_DEACTIVATED'
      ? 'red'
      : action === 'TRANSFER_REVERSED' || action === 'ACCOUNT_CLOSED'
        ? 'amber'
        : action === 'TRANSFER_CREATED'
          ? 'green'
          : action === 'ACCOUNT_OPENED' || action === 'CUSTOMER_CREATED'
            ? 'blue'
            : 'violet'

  return <Chip tone={tone} compact>{bankActionLabel(action)}</Chip>
}
