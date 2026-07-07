export type DepositStatus =
  | 'FUNDING'
  | 'OPEN'
  | 'PAYOUT_PENDING'
  | 'CLOSED'
  | 'CLOSED_EARLY'
  | 'CANCELLED'

export type CloseType = 'EARLY' | 'MATURED'

export type DepositErrorCode =
  | 'VALIDATION_ERROR'
  | 'INTERNAL_ERROR'
  | 'FORBIDDEN'
  | 'DEPOSIT_NOT_FOUND'
  | 'PRODUCT_NOT_FOUND'
  | 'AMOUNT_OUT_OF_RANGE'
  | 'INVALID_STATUS_TRANSITION'
  | 'DEPOSIT_ALREADY_CLOSED'
  | 'DUPLICATE_REQUEST'
  | 'BANKING_UNAVAILABLE'
  | 'FUNDING_FAILED'
  | 'PAYOUT_FAILED'
  | 'INSUFFICIENT_FUNDS'
  | 'ACCOUNT_NOT_FOUND'
  | 'ACCOUNT_INACTIVE'
  | 'LIMIT_EXCEEDED'
  | 'FORBIDDEN_ACCOUNT'

export interface DepositProduct {
  termMonths: number
  annualRatePercent: number
  minAmount: number
  maxAmount: number
}

export interface DepositOpenPayload {
  linkedAccountNo: string
  termMonths: number
  amount: number
}

export interface DepositResponse {
  id: number
  depositNo: string
  customerUsername: string
  linkedAccountNo: string
  principal: number
  annualRate: number
  termMonths: number
  openedAt: string
  maturityDate: string
  status: DepositStatus
  closeType: CloseType | null
  interestAmount: number | null
  payoutAmount: number | null
  projectedInterest: number
  matured: boolean
  fundingTransferRef: string | null
  payoutTransferRef: string | null
  failureReason: string | null
  closedAt: string | null
}
