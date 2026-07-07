import type { BankErrorCode } from '../types/banking'

const MESSAGES: Record<BankErrorCode, string> = {
  VALIDATION_ERROR: 'Оруулсан утга буруу байна',
  INTERNAL_ERROR: 'Системийн алдаа гарлаа, дахин оролдоно уу',
  ACCOUNT_NOT_FOUND: 'Данс олдсонгүй',
  CUSTOMER_NOT_FOUND: 'Харилцагч олдсонгүй',
  TRANSFER_NOT_FOUND: 'Гүйлгээ олдсонгүй',
  INSUFFICIENT_FUNDS: 'Дансны үлдэгдэл хүрэлцэхгүй байна',
  ACCOUNT_INACTIVE: 'Данс идэвхгүй (блоклогдсон эсвэл хаагдсан) байна',
  SAME_ACCOUNT: 'Илгээгч болон хүлээн авагч данс ижил байж болохгүй',
  CURRENCY_MISMATCH: 'Хоёр дансны валют зөрж байна',
  LIMIT_EXCEEDED: 'Гүйлгээний лимит хэтэрсэн байна',
  DUPLICATE_REQUEST: 'Давхардсан хүсэлт байна',
  TRANSFER_NOT_REVERSIBLE: 'Энэ гүйлгээг буцаах боломжгүй (аль хэдийн буцаагдсан эсвэл амжилтгүй)',
  FORBIDDEN_ACCOUNT: 'Энэ данс руу хандах эрхгүй байна',
  ACCOUNT_NOT_EMPTY: 'Үлдэгдэлтэй дансыг хаах боломжгүй',
  USERNAME_TAKEN: 'Энэ хэрэглэгчийн нэр өөр харилцагчид холбогдсон байна',
  INVALID_STATUS_TRANSITION: 'Дансны төлөвийн шилжилт буруу байна',
  FORBIDDEN: 'Энэ үйлдлийг хийх эрхгүй байна',
}

export function bankErrorMessage(code: string | null | undefined, fallback: string): string {
  if (code && code in MESSAGES) {
    return MESSAGES[code as BankErrorCode]
  }
  return fallback
}
