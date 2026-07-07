import { Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listTransfers } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { TransfersTable } from '../components/TransfersTable'
import type { TransferResponse } from '../types/banking'

const PAGE_SIZE = 20

export function TransfersPage() {
  const auth = useAuth()
  const [transfers, setTransfers] = useState<TransferResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadTransfers() {
      if (!auth.token) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const result = await listTransfers(auth.token, page, PAGE_SIZE)
        if (!cancelled) {
          setTransfers(result.content)
          setTotalPages(result.totalPages)
          setTotalElements(result.totalElements)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Гүйлгээ уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadTransfers()

    return () => {
      cancelled = true
    }
  }, [auth.token, page])

  return (
    <>
      <PageHeader
        title="Гүйлгээний түүх"
        subtitle="Таны оролцсон шилжүүлгүүд, шинэ нь эхэндээ (теллер бүгдийг харна)"
        action={
          <Link className="primary-button" to="/transfers/new">
            <Plus size={16} />
            Шинэ шилжүүлэг
          </Link>
        }
      />

      {isLoading ? <LoadingState label="Гүйлгээ уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {!isLoading && !error ? (
        <section className="panel table-panel">
          <div className="table-count">{totalElements} гүйлгээ</div>
          {transfers.length === 0
            ? <EmptyState label="Гүйлгээ алга. «Шинэ шилжүүлэг» дарж эхлүүлнэ үү." />
            : <TransfersTable transfers={transfers} />}
          <PaginationControls page={page} totalPages={totalPages} onPageChange={setPage} />
        </section>
      ) : null}
    </>
  )
}
