import type { DepositErrorCode } from '../types/deposit'
import { bankErrorMessage } from './bankingErrors'

// Deposit-specific codes. Pass-through codes shared with banking
// (INSUFFICIENT_FUNDS, ACCOUNT_*, LIMIT_EXCEEDED, FORBIDDEN_ACCOUNT, ...)
// fall back to bankErrorMessage so their translations are not duplicated.
const MESSAGES: Partial<Record<DepositErrorCode, string>> = {
  DEPOSIT_NOT_FOUND: 'Хадгаламж олдсонгүй',
  PRODUCT_NOT_FOUND: 'Ийм хугацаатай хадгаламжийн бүтээгдэхүүн алга',
  AMOUNT_OUT_OF_RANGE: 'Дүн зөвшөөрөгдсөн хязгаараас гадуур байна',
  INVALID_STATUS_TRANSITION: 'Хадгаламжийн төлөвийн шилжилт буруу байна',
  DEPOSIT_ALREADY_CLOSED: 'Хадгаламж аль хэдийн хаагдсан байна',
  BANKING_UNAVAILABLE: 'Банкны гүйлгээний систем түр ажиллахгүй байна — дараа дахин оролдоно уу',
  FUNDING_FAILED: 'Хадгаламжийн санхүүжилт амжилтгүй боллоо',
  PAYOUT_FAILED: 'Хадгаламжийн эргэн төлөлт амжилтгүй боллоо',
}

export function depositErrorMessage(code: string | null | undefined, fallback: string): string {
  if (code && code in MESSAGES) {
    return MESSAGES[code as DepositErrorCode] as string
  }
  // shared pass-through codes reuse the banking translations
  return bankErrorMessage(code, fallback)
}
