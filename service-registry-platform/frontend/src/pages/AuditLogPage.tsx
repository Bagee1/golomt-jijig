import { Filter, Search } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { getAuditLogs } from '../api/auditApi'
import { useAuth } from '../auth/useAuth'
import { AuditActionChip } from '../components/Chips'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { AuditAction, AuditLogResponse } from '../types/api'
import { actionLabel, extractSystemKey, formatDateTime } from '../utils/format'

const PAGE_SIZE = 20
const ACTION_OPTIONS: AuditAction[] = [
  'LOGIN_SUCCESS',
  'LOGIN_FAILURE',
  'SYSTEM_CREATED',
  'SYSTEM_UPDATED',
  'SYSTEM_DISABLED',
  'SECURITY_CHECK_UPDATED',
]

export function AuditLogPage() {
  const auth = useAuth()
  const [auditLogs, setAuditLogs] = useState<AuditLogResponse[]>([])
  const [keyword, setKeyword] = useState('')
  const [action, setAction] = useState<AuditAction | ''>('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadAuditLogs() {
      if (!auth.token) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const logs = await getAuditLogs(auth.token, page, PAGE_SIZE)
        if (!cancelled) {
          setAuditLogs(logs.content)
          setTotalPages(logs.totalPages)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Audit log уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadAuditLogs()

    return () => {
      cancelled = true
    }
  }, [auth.token, page])

  const filteredLogs = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase()

    return auditLogs.filter((log) => {
      if (action && log.action !== action) {
        return false
      }
      if (!normalizedKeyword) {
        return true
      }
      return [
        log.action,
        log.actorDisplayName,
        log.actorUsername,
        log.targetType,
        extractSystemKey(log.metadataJson),
        log.message,
      ].some((value) => value?.toLowerCase().includes(normalizedKeyword))
    })
  }, [auditLogs, keyword, action])

  return (
    <>
      <PageHeader title="Аудит лог" subtitle="Бүх үйлдлийн бүртгэл" />

      <div className="toolbar audit-toolbar">
        <label className="search-box">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Хайх..."
            aria-label="Аудит лог хайх"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
        </label>
        <label className="filter-select">
          Үйлдэл:
          <select value={action} onChange={(event) => setAction(event.target.value as AuditAction | '')}>
            <option value="">Бүгд</option>
            {ACTION_OPTIONS.map((option) => <option key={option} value={option}>{actionLabel(option)}</option>)}
          </select>
          <Filter size={14} />
        </label>
      </div>

      {isLoading ? <LoadingState label="Audit log уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      <section className="panel table-panel">
        <div className="table-count">{filteredLogs.length} бүртгэл</div>
        {filteredLogs.length === 0 && !isLoading && !error ? <EmptyState label="Audit бүртгэл олдсонгүй." /> : null}
        <div className="table-scroll">
          <table className="data-table audit-table">
            <thead>
              <tr>
                <th>Огноо</th>
                <th>Үйлдэл</th>
                <th>Хэрэглэгч</th>
                <th>Зорилт</th>
                <th>Мессеж</th>
              </tr>
            </thead>
            <tbody>
              {filteredLogs.map((log) => (
                <tr key={log.id}>
                  <td className="mono-cell">{formatDateTime(log.createdAt)}</td>
                  <td><AuditActionChip action={log.action} /></td>
                  <td>
                    <strong>{log.actorDisplayName ?? log.actorUsername ?? 'unknown'}</strong>
                    <span>{log.actorUsername ?? '-'}</span>
                  </td>
                  <td>
                    <strong>{log.targetType}</strong>
                    <span>{extractSystemKey(log.metadataJson)}</span>
                  </td>
                  <td>{log.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <PaginationControls page={page} totalPages={totalPages} onPageChange={setPage} />
      </section>
    </>
  )
}
