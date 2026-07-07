import { useEffect, useState } from 'react'
import { listBankAuditLogs } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { BankAuditActionChip } from '../components/Chips'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { BankAuditLogResponse } from '../types/banking'
import { formatDateTime } from '../utils/format'

const PAGE_SIZE = 20

export function BankAuditLogPage() {
  const auth = useAuth()
  const [logs, setLogs] = useState<BankAuditLogResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadLogs() {
      if (!auth.token) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const result = await listBankAuditLogs(auth.token, page, PAGE_SIZE)
        if (!cancelled) {
          setLogs(result.content)
          setTotalPages(result.totalPages)
          setTotalElements(result.totalElements)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Аудит лог уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadLogs()

    return () => {
      cancelled = true
    }
  }, [auth.token, page])

  return (
    <>
      <PageHeader title="Аудит лог" subtitle="Банкны бүх үйлдлийн бүртгэл, шинэ нь эхэндээ" />

      {isLoading ? <LoadingState label="Аудит лог уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {!isLoading && !error ? (
        <section className="panel table-panel">
          <div className="table-count">{totalElements} бичилт</div>
          {logs.length === 0 ? (
            <EmptyState label="Аудит бичилт алга." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Огноо</th>
                    <th>Үйлдэл</th>
                    <th>Гүйцэтгэсэн</th>
                    <th>Тайлбар</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <tr key={log.id}>
                      <td className="mono-cell">{formatDateTime(log.createdAt)}</td>
                      <td><BankAuditActionChip action={log.action} /></td>
                      <td>
                        {log.actorDisplayName ?? log.actorUsername ?? '-'}
                        {log.actorRole ? <span className="mono-cell"> ({log.actorRole})</span> : null}
                      </td>
                      <td>{log.message}</td>
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
