import { Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { listDeposits } from '../api/depositApi'
import { useAuth } from '../auth/useAuth'
import { DepositStatusChip } from '../components/Chips'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { DepositResponse } from '../types/deposit'
import { formatExactMnt } from '../utils/format'

const PAGE_SIZE = 20

export function DepositsAdminPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [filterInput, setFilterInput] = useState('')
  const [usernameFilter, setUsernameFilter] = useState('')
  const [deposits, setDeposits] = useState<DepositResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!auth.token) {
        return
      }
      setIsLoading(true)
      setError(null)
      try {
        const result = await listDeposits(auth.token, {
          username: usernameFilter || undefined,
          page,
          size: PAGE_SIZE,
        })
        if (!cancelled) {
          setDeposits(result.content)
          setTotalPages(result.totalPages)
          setTotalElements(result.totalElements)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Хадгаламж уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [auth.token, usernameFilter, page])

  function onFilter(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setUsernameFilter(filterInput.trim())
  }

  return (
    <>
      <PageHeader title="Хадгаламжийн бүртгэл" subtitle="Бүх харилцагчийн хадгаламж — теллерийн харагдац" />

      <form className="toolbar" onSubmit={onFilter}>
        <label className="search-box">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Хэрэглэгчийн нэрээр шүүх (batbold)..."
            aria-label="Хэрэглэгчийн нэр"
            value={filterInput}
            onChange={(event) => setFilterInput(event.target.value)}
          />
        </label>
        <button type="submit" className="primary-button">Шүүх</button>
      </form>

      {isLoading ? <LoadingState label="Хадгаламж уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {!isLoading && !error ? (
        <section className="panel table-panel">
          <div className="table-count">{totalElements} хадгаламж</div>
          {deposits.length === 0 ? (
            <EmptyState label="Хадгаламж олдсонгүй." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Дугаар</th>
                    <th>Эзэмшигч</th>
                    <th>Үндсэн дүн</th>
                    <th>Хүү</th>
                    <th>Дуусах</th>
                    <th>Төлөв</th>
                  </tr>
                </thead>
                <tbody>
                  {deposits.map((deposit) => (
                    <tr
                      key={deposit.id}
                      className="clickable-row"
                      onClick={() => navigate(`/deposits/${deposit.id}`)}
                    >
                      <td className="mono-cell">{deposit.depositNo}</td>
                      <td>{deposit.customerUsername}</td>
                      <td>{formatExactMnt(deposit.principal)}</td>
                      <td className="mono-cell">{deposit.annualRate}%</td>
                      <td className="mono-cell">{deposit.maturityDate}</td>
                      <td><DepositStatusChip status={deposit.status} compact /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <PaginationControls page={page} totalPages={totalPages} onPageChange={setPage} />
        </section>
      ) : null}
    </>
  )
}
