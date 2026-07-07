import { Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listCustomers } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { Chip } from '../components/Chips'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { CustomerResponse } from '../types/banking'
import { formatDateTime } from '../utils/format'

const PAGE_SIZE = 20

export function CustomersPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [query, setQuery] = useState('')
  const [customers, setCustomers] = useState<CustomerResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadCustomers() {
      if (!auth.token) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const result = await listCustomers(auth.token, { q: query || undefined, page, size: PAGE_SIZE })
        if (!cancelled) {
          setCustomers(result.content)
          setTotalPages(result.totalPages)
          setTotalElements(result.totalElements)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Харилцагч уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadCustomers()

    return () => {
      cancelled = true
    }
  }, [auth.token, query, page])

  function onSearch(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setQuery(searchInput.trim())
  }

  return (
    <>
      <PageHeader
        title="Харилцагчид"
        subtitle="Банкны харилцагчийн бүртгэл, удирдлага"
        action={
          <Link className="primary-button" to="/admin/customers/new">
            <Plus size={16} />
            Шинэ харилцагч
          </Link>
        }
      />

      <form className="toolbar" onSubmit={onSearch}>
        <label className="search-box">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Нэр эсвэл харилцагчийн дугаар..."
            aria-label="Харилцагч хайх"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
          />
        </label>
        <button type="submit" className="primary-button">Хайх</button>
      </form>

      {isLoading ? <LoadingState label="Харилцагч уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {!isLoading && !error ? (
        <section className="panel table-panel">
          <div className="table-count">{totalElements} харилцагч</div>
          {customers.length === 0 ? (
            <EmptyState label="Харилцагч олдсонгүй." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Дугаар</th>
                    <th>Нэр</th>
                    <th>Утас</th>
                    <th>И-мэйл</th>
                    <th>Нэвтрэх нэр</th>
                    <th>Төлөв</th>
                    <th>Бүртгэсэн</th>
                  </tr>
                </thead>
                <tbody>
                  {customers.map((customer) => (
                    <tr
                      key={customer.id}
                      className="clickable-row"
                      onClick={() => navigate(`/admin/customers/${customer.id}`)}
                    >
                      <td className="mono-cell">{customer.customerNo}</td>
                      <td>{customer.firstName} {customer.lastName}</td>
                      <td className="mono-cell">{customer.phone ?? '-'}</td>
                      <td>{customer.email ?? '-'}</td>
                      <td className="mono-cell">{customer.username ?? '-'}</td>
                      <td>
                        <Chip tone={customer.active ? 'green' : 'gray'} compact>
                          {customer.active ? 'Идэвхтэй' : 'Идэвхгүй'}
                        </Chip>
                      </td>
                      <td className="mono-cell">{formatDateTime(customer.createdAt)}</td>
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
