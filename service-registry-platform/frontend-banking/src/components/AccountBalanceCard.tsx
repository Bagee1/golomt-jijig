import { ScrollText, Wallet } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { AccountResponse } from '../types/banking'
import { formatDateTime, formatExactMnt } from '../utils/format'
import { AccountStatusChip } from './Chips'

export function AccountBalanceCard({ account }: { account: AccountResponse }) {
  return (
    <section className="panel account-card">
      <div className="account-card-head">
        <div className="account-card-icon">
          <Wallet size={18} />
        </div>
        <div>
          <strong className="mono-cell">{account.accountNo}</strong>
          <span>{account.customerName} · {account.customerNo}</span>
        </div>
        <AccountStatusChip status={account.status} />
      </div>
      <div className="account-card-balance">
        <strong>{formatExactMnt(account.balance, account.currency)}</strong>
        <span>Шинэчлэгдсэн: {formatDateTime(account.updatedAt)}</span>
      </div>
      <div className="account-card-actions">
        <Link className="ghost-button" to={`/accounts/${encodeURIComponent(account.accountNo)}/statement`}>
          <ScrollText size={15} />
          Хуулга харах
        </Link>
      </div>
    </section>
  )
}
