import { MoveRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import type { TransferResponse } from '../types/banking'
import { formatDateTime, formatExactMnt } from '../utils/format'
import { TransferStatusChip } from './Chips'

export function TransfersTable({ transfers }: { transfers: TransferResponse[] }) {
  const navigate = useNavigate()

  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th>Гүйлгээний дугаар</th>
            <th>Данс</th>
            <th>Дүн</th>
            <th>Тайлбар</th>
            <th>Төлөв</th>
            <th>Огноо</th>
          </tr>
        </thead>
        <tbody>
          {transfers.map((transfer) => (
            <tr
              key={transfer.id}
              className="clickable-row"
              onClick={() => navigate(`/transfers/${transfer.id}`)}
            >
              <td className="mono-cell">{transfer.transferRef}</td>
              <td className="mono-cell transfer-route">
                {transfer.fromAccountNo}
                <MoveRight size={14} aria-hidden="true" />
                {transfer.toAccountNo}
              </td>
              <td>{formatExactMnt(transfer.amount, transfer.currency)}</td>
              <td>{transfer.description ?? '-'}</td>
              <td><TransferStatusChip status={transfer.status} compact /></td>
              <td className="mono-cell">{formatDateTime(transfer.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
