import { CircleCheck, Server, ShieldCheck, XCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAuditLogs } from '../api/auditApi'
import { useAuth } from '../auth/useAuth'
import { Chip, EnvironmentChip, SecurityScore, StatusChip, SystemTypeChip } from '../components/Chips'
import { MetricCard } from '../components/MetricCard'
import { PageHeader } from '../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { useSystemsWithScores } from '../hooks/useSystemsWithScores'
import type { AuditLogResponse, SystemStatus, SystemType } from '../types/api'
import { formatDateTime } from '../utils/format'

export function DashboardPage() {
  const auth = useAuth()
  const { systems, totalElements, isLoading, error } = useSystemsWithScores({ size: 100 })
  const [recentAuditLogs, setRecentAuditLogs] = useState<AuditLogResponse[]>([])
  const [auditError, setAuditError] = useState<string | null>(null)
  const activeCount = systems.filter((system) => system.status === 'ACTIVE').length
  const inactiveCount = systems.length - activeCount
  const downOrInactiveCount = systems.filter((system) => system.status === 'DOWN' || system.status === 'INACTIVE').length
  const averageSecurity = systems.length === 0
    ? 0
    : Math.round(systems.reduce((sum, system) => sum + system.securityScore, 0) / systems.length)
  const lowSecuritySystems = [...systems]
    .filter((system) => system.securityScore < 65)
    .sort((a, b) => a.securityScore - b.securityScore)

  useEffect(() => {
    let cancelled = false

    async function loadAuditLogs() {
      if (!auth.token) {
        return
      }

      try {
        const logs = await getAuditLogs(auth.token, 0, 5)
        if (!cancelled) {
          setRecentAuditLogs(logs.content)
          setAuditError(null)
        }
      } catch (exception) {
        if (!cancelled) {
          setAuditError(exception instanceof Error ? exception.message : 'Audit log уншихад алдаа гарлаа')
        }
      }
    }

    void loadAuditLogs()

    return () => {
      cancelled = true
    }
  }, [auth.token])

  const statusRows: Array<{ label: string; value: number; tone: string }> = [
    { label: 'Идэвхтэй', value: activeCount, tone: 'green' },
    { label: 'UNKNOWN', value: countByStatus(systems, 'UNKNOWN'), tone: 'amber' },
    { label: 'DOWN', value: countByStatus(systems, 'DOWN'), tone: 'red' },
    { label: 'Идэвхгүй', value: countByStatus(systems, 'INACTIVE'), tone: 'gray' },
  ]
  const typeRows: Array<{ type: SystemType; count: number }> = [
    { type: 'CORE', count: countByType(systems, 'CORE') },
    { type: 'CARD', count: countByType(systems, 'CARD') },
    { type: 'DIGITAL', count: countByType(systems, 'DIGITAL') },
    { type: 'INTERNAL', count: countByType(systems, 'INTERNAL') },
  ]

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Системийн нийт байдал, security overview" />

      {isLoading ? <LoadingState label="Dashboard өгөгдөл уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      <div className="dashboard-grid metrics-grid">
        <MetricCard label="Нийт систем" value={totalElements} helper="бүртгэлтэй" icon={Server} tone="teal" />
        <MetricCard label="Идэвхтэй" value={activeCount} helper={`${inactiveCount} идэвхгүй`} icon={CircleCheck} tone="green" />
        <MetricCard label="DOWN / Идэвхгүй" value={downOrInactiveCount} helper="анхаарал шаардтай" icon={XCircle} tone="red" />
        <MetricCard label="Дундаж Security" value={averageSecurity} helper="0–100 оноо" icon={ShieldCheck} tone="blue" />
      </div>

      <div className="dashboard-grid middle-grid">
        <section className="panel status-panel">
          <h2>Системийн төлөв</h2>
          {statusRows.map((item) => (
            <div className="status-row" key={item.label}>
              <div>
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
              <div className="status-bar">
                <span
                  className={`bar-${item.tone}`}
                  style={{ width: systems.length === 0 ? '0%' : `${Math.max(4, Math.round((item.value / systems.length) * 100))}%` }}
                />
              </div>
            </div>
          ))}
        </section>

        <section className="panel type-panel">
          <h2>Системийн төрөл</h2>
          <div className="type-grid">
            {typeRows.map((item) => (
              <div className="type-tile" key={item.type}>
                <strong>{item.count}</strong>
                <SystemTypeChip type={item.type} />
              </div>
            ))}
          </div>
        </section>

        <section className="panel recent-panel">
          <div className="panel-title-row">
            <h2>Сүүлийн үйлдлүүд</h2>
            <Link to="/audit-logs">Бүгдийг харах →</Link>
          </div>
          {auditError ? <p className="inline-error">{auditError}</p> : null}
          <ul className="recent-list">
            {recentAuditLogs.map((log) => (
              <li key={log.id}>
                <span className={log.action.includes('DISABLED') || log.action.includes('FAILURE') ? 'dot-red' : 'dot-green'} />
                <div>
                  <strong>{log.message}</strong>
                  <p>{log.actorDisplayName ?? log.actorUsername ?? 'unknown'} · {formatDateTime(log.createdAt)}</p>
                </div>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <section className="panel table-panel">
        <div className="panel-title-row warning-title">
          <div>
            <span className="warning-icon">△</span>
            <h2>Анхаарал шаардтай системүүд</h2>
            <Chip tone="amber" compact>Security score &lt; 65</Chip>
          </div>
          <Link to="/systems">Бүх систем →</Link>
        </div>

        {lowSecuritySystems.length === 0 && !isLoading ? <EmptyState label="Анхаарал шаардтай систем алга." /> : null}
        <div className="table-scroll">
          <table className="data-table compact-table">
            <thead>
              <tr>
                <th>Систем</th>
                <th>Төрөл</th>
                <th>Орчин</th>
                <th>Төлөв</th>
                <th>Security</th>
              </tr>
            </thead>
            <tbody>
              {lowSecuritySystems.map((system) => (
                <tr key={system.id}>
                  <td>
                    <strong>{system.name}</strong>
                    <span>{system.systemKey}</span>
                  </td>
                  <td><SystemTypeChip type={system.type} /></td>
                  <td><EnvironmentChip environment={system.environment} /></td>
                  <td><StatusChip status={system.status} /></td>
                  <td><SecurityScore score={system.securityScore} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}

function countByStatus(systems: Array<{ status: SystemStatus }>, status: SystemStatus) {
  return systems.filter((system) => system.status === status).length
}

function countByType(systems: Array<{ type: SystemType }>, type: SystemType) {
  return systems.filter((system) => system.type === type).length
}
