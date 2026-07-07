import { ArrowLeft, MoveDownLeft, MoveUpRight } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getStatement } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { StatementResponse } from '../types/banking'
import { formatDateTime, formatExactMnt } from '../utils/format'

const PAGE_SIZE = 20

export function StatementPage() {
  const auth = useAuth()
  const { accountNo } = useParams()
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [appliedRange, setAppliedRange] = useState<{ from?: string; to?: string }>({})
  const [page, setPage] = useState(0)
  const [statement, setStatement] = useState<StatementResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadStatement() {
      if (!auth.token || !accountNo) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const result = await getStatement(auth.token, accountNo, { ...appliedRange, page, size: PAGE_SIZE })
        if (!cancelled) {
          setStatement(result)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Хуулга уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadStatement()

    return () => {
      cancelled = true
    }
  }, [auth.token, accountNo, appliedRange, page])

  function onApplyRange(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setAppliedRange({ from: fromDate || undefined, to: toDate || undefined })
  }

  return (
    <>
      <PageHeader
        title={`Дансны хуулга — ${accountNo ?? ''}`}
        subtitle={statement ? `${statement.from} → ${statement.to}` : 'Сонгосон хугацааны гүйлгээний хуулга'}
        action={
          <Link className="ghost-button" to="/">
            <ArrowLeft size={16} />
            Буцах
          </Link>
        }
      />

      <form className="toolbar" onSubmit={onApplyRange}>
        <label className="form-field statement-date-field">
          <span>Эхлэх огноо</span>
          <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} />
        </label>
        <label className="form-field statement-date-field">
          <span>Дуусах огноо</span>
          <input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} />
        </label>
        <button type="submit" className="primary-button">Шүүх</button>
      </form>

      {isLoading ? <LoadingState label="Хуулга уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {!isLoading && !error && statement ? (
        <>
          <div className="account-card-grid">
            <SummaryCard label="Эхний үлдэгдэл" value={formatExactMnt(statement.openingBalance, statement.currency)} />
            <SummaryCard
              label="Орлого"
              value={`+${formatExactMnt(statement.totalCredit, statement.currency)}`}
              icon={<MoveDownLeft size={16} aria-hidden="true" />}
            />
            <SummaryCard
              label="Зарлага"
              value={`-${formatExactMnt(statement.totalDebit, statement.currency)}`}
              icon={<MoveUpRight size={16} aria-hidden="true" />}
            />
            <SummaryCard label="Эцсийн үлдэгдэл" value={formatExactMnt(statement.closingBalance, statement.currency)} />
          </div>

          <section className="panel table-panel">
            <div className="table-count">{statement.entries.totalElements} бичилт</div>
            {statement.entries.content.length === 0 ? (
              <EmptyState label="Энэ хугацаанд гүйлгээ алга." />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Огноо</th>
                      <th>Гүйлгээний дугаар</th>
                      <th>Харьцсан данс</th>
                      <th>Тайлбар</th>
                      <th>Дүн</th>
                      <th>Үлдэгдэл</th>
                    </tr>
                  </thead>
                  <tbody>
                    {statement.entries.content.map((entry) => (
                      <tr key={entry.id}>
                        <td className="mono-cell">{formatDateTime(entry.createdAt)}</td>
                        <td className="mono-cell">{entry.transferRef}</td>
                        <td className="mono-cell">{entry.counterpartyAccountNo}</td>
                        <td>{entry.description ?? '-'}</td>
                        <td className={entry.entryType === 'DEBIT' ? 'ledger-amount ledger-debit' : 'ledger-amount ledger-credit'}>
                          {entry.entryType === 'DEBIT' ? '-' : '+'}{formatExactMnt(entry.amount)}
                        </td>
                        <td className="mono-cell">{formatExactMnt(entry.balanceAfter)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <PaginationControls page={page} totalPages={statement.entries.totalPages} onPageChange={setPage} />
          </section>
        </>
      ) : null}
    </>
  )
}

function SummaryCard({ label, value, icon }: { label: string; value: string; icon?: React.ReactNode }) {
  return (
    <section className="panel account-card">
      <div className="account-card-balance">
        <span>{icon} {label}</span>
        <strong>{value}</strong>
      </div>
    </section>
  )
}
