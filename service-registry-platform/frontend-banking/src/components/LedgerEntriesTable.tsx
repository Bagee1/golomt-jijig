import type { LedgerEntryResponse, LedgerEntryType } from '../types/banking'
import { formatDateTime, formatExactMnt } from '../utils/format'
import { Chip } from './Chips'

export function LedgerTypeChip({ entryType }: { entryType: LedgerEntryType }) {
  return <Chip tone={entryType === 'DEBIT' ? 'red' : 'green'} compact>{entryType}</Chip>
}

export function LedgerEntriesTable({ entries }: { entries: LedgerEntryResponse[] }) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th>Данс</th>
            <th>Төрөл</th>
            <th>Дүн</th>
            <th>Дараах үлдэгдэл</th>
            <th>Огноо</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id}>
              <td className="mono-cell">{entry.accountNo}</td>
              <td><LedgerTypeChip entryType={entry.entryType} /></td>
              <td className={entry.entryType === 'DEBIT' ? 'ledger-amount ledger-debit' : 'ledger-amount ledger-credit'}>
                {entry.entryType === 'DEBIT' ? '-' : '+'}{formatExactMnt(entry.amount)}
              </td>
              <td className="mono-cell">{formatExactMnt(entry.balanceAfter)}</td>
              <td className="mono-cell">{formatDateTime(entry.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
