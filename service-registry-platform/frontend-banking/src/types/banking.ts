export type AccountStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED'
export type LedgerEntryType = 'DEBIT' | 'CREDIT'
export type TransferStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REVERSED'

export type BankErrorCode =
  | 'VALIDATION_ERROR'
  | 'INTERNAL_ERROR'
  | 'ACCOUNT_NOT_FOUND'
  | 'CUSTOMER_NOT_FOUND'
  | 'TRANSFER_NOT_FOUND'
  | 'INSUFFICIENT_FUNDS'
  | 'ACCOUNT_INACTIVE'
  | 'SAME_ACCOUNT'
  | 'CURRENCY_MISMATCH'
  | 'LIMIT_EXCEEDED'
  | 'DUPLICATE_REQUEST'
  | 'TRANSFER_NOT_REVERSIBLE'
  | 'FORBIDDEN_ACCOUNT'
  | 'ACCOUNT_NOT_EMPTY'
  | 'USERNAME_TAKEN'
  | 'INVALID_STATUS_TRANSITION'
  | 'FORBIDDEN'

export interface AccountResponse {
  id: number
  accountNo: string
  customerNo: string
  customerName: string
  currency: string
  balance: number
  status: AccountStatus
  createdAt: string
  updatedAt: string
}

export interface AccountCreatePayload {
  customerNo: string
  currency?: string | null
  initialBalance?: number | null
}

export interface LedgerEntryResponse {
  id: number
  accountNo: string
  entryType: LedgerEntryType
  amount: number
  balanceAfter: number
  createdAt: string
}

export interface TransferRequest {
  fromAccountNo: string
  toAccountNo: string
  amount: number
  description?: string | null
}

export interface TransferResponse {
  id: number
  transferRef: string
  fromAccountNo: string
  toAccountNo: string
  amount: number
  currency: string
  description: string | null
  status: TransferStatus
  failureReason: BankErrorCode | null
  reversalOfTransferId: number | null
  reversedByTransferId: number | null
  createdAt: string
  ledgerEntries: LedgerEntryResponse[]
}

export interface StatementEntryResponse {
  id: number
  entryType: LedgerEntryType
  amount: number
  balanceAfter: number
  createdAt: string
  transferRef: string
  description: string | null
  counterpartyAccountNo: string
}

export interface StatementResponse {
  accountNo: string
  currency: string
  from: string
  to: string
  openingBalance: number
  closingBalance: number
  totalDebit: number
  totalCredit: number
  entries: import('./api').PageResponse<StatementEntryResponse>
}

export interface CustomerPayload {
  firstName: string
  lastName: string
  phone?: string | null
  email?: string | null
  username?: string | null
}

export interface CustomerResponse {
  id: number
  customerNo: string
  firstName: string
  lastName: string
  phone: string | null
  email: string | null
  username: string | null
  active: boolean
  createdAt: string
  accounts: AccountResponse[] | null
}

export type BankAuditAction =
  | 'TRANSFER_CREATED'
  | 'TRANSFER_FAILED'
  | 'TRANSFER_REVERSED'
  | 'ACCOUNT_OPENED'
  | 'ACCOUNT_BLOCKED'
  | 'ACCOUNT_UNBLOCKED'
  | 'ACCOUNT_CLOSED'
  | 'CUSTOMER_CREATED'
  | 'CUSTOMER_UPDATED'
  | 'CUSTOMER_DEACTIVATED'

export interface BankAuditLogResponse {
  id: number
  action: BankAuditAction
  targetType: string
  targetId: number | null
  message: string
  metadataJson: string | null
  actorUsername: string | null
  actorDisplayName: string | null
  actorRole: string | null
  createdAt: string
}
