import { Eye, Filter, Link2Off, Pencil, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import type { ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { disableSystem } from '../api/systemsApi'
import { useAuth } from '../auth/useAuth'
import { EnvironmentChip, SecurityScore, StatusChip, SystemTypeChip } from '../components/Chips'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useSystemsWithScores, type SystemWithScore } from '../hooks/useSystemsWithScores'
import type { SystemStatus, SystemType } from '../types/api'
import { formatMoneyMnt } from '../utils/format'

const PAGE_SIZE = 20

export function SystemsPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [type, setType] = useState<SystemType | ''>('')
  const [status, setStatus] = useState<SystemStatus | ''>('')
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [disableTarget, setDisableTarget] = useState<SystemWithScore | null>(null)
  const [isDisabling, setIsDisabling] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const debouncedKeyword = useDebouncedValue(keyword.trim(), 300)

  const { systems, totalElements, totalPages, isLoading, error } = useSystemsWithScores({
    keyword: debouncedKeyword,
    type,
    status,
    page,
    size: PAGE_SIZE,
    reloadKey,
  })

  const isAdmin = auth.user?.role === 'ADMIN'

  function handleKeywordChange(event: ChangeEvent<HTMLInputElement>) {
    setKeyword(event.target.value)
    setPage(0)
  }

  function handleTypeChange(event: ChangeEvent<HTMLSelectElement>) {
    setType(event.target.value as SystemType | '')
    setPage(0)
  }

  function handleStatusChange(event: ChangeEvent<HTMLSelectElement>) {
    setStatus(event.target.value as SystemStatus | '')
    setPage(0)
  }

  async function handleDisable() {
    if (!auth.token || !disableTarget) {
      return
    }

    setIsDisabling(true)
    setActionError(null)

    try {
      await disableSystem(auth.token, disableTarget.id)
      setDisableTarget(null)
      setReloadKey((key) => key + 1)
    } catch (exception) {
      setActionError(exception instanceof Error ? exception.message : 'Идэвхгүй болгоход алдаа гарлаа')
    } finally {
      setIsDisabling(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Системийн бүртгэл"
        subtitle="Бүртгэлтэй систем, хайлт, шүүлтүүр"
        action={isAdmin ? (
          <button className="primary-button" type="button" onClick={() => navigate('/systems/new')}>
            <Plus size={17} />
            Систем нэмэх
          </button>
        ) : undefined}
      />

      <div className="toolbar">
        <label className="search-box">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Систем хайх..."
            aria-label="Систем хайх"
            value={keyword}
            onChange={handleKeywordChange}
          />
        </label>
        <label className="filter-select">
          Төрөл:
          <select value={type} onChange={handleTypeChange}>
            <option value="">Бүгд</option>
            <option value="CORE">CORE</option>
            <option value="CARD">CARD</option>
            <option value="DIGITAL">DIGITAL</option>
            <option value="INTERNAL">INTERNAL</option>
          </select>
          <Filter size={14} />
        </label>
        <label className="filter-select">
          Төлөв:
          <select value={status} onChange={handleStatusChange}>
            <option value="">Бүгд</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
            <option value="UNKNOWN">UNKNOWN</option>
            <option value="DOWN">DOWN</option>
          </select>
          <Filter size={14} />
        </label>
      </div>

      {isLoading ? <LoadingState label="Системүүд уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}
      {actionError ? <ErrorState message={actionError} /> : null}

      <section className="panel table-panel">
        <div className="table-count">{totalElements} систем олдов</div>
        {systems.length === 0 && !isLoading && !error ? <EmptyState label="Илэрц олдсонгүй." /> : null}
        <div className="table-scroll">
          <table className="data-table systems-table">
            <thead>
              <tr>
                <th>Нэр / Код</th>
                <th>Төрөл</th>
                <th>Үнэлгээ</th>
                <th>Хөгжүүлэгч</th>
                <th>Орчин</th>
                <th>Ашиглалт</th>
                <th>Төлөв</th>
                <th>Security</th>
                <th>Үйлдэл</th>
              </tr>
            </thead>
            <tbody>
              {systems.map((system) => (
                <tr key={system.id}>
                  <td>
                    <strong>{system.name}</strong>
                    <span>{system.systemKey}</span>
                  </td>
                  <td><SystemTypeChip type={system.type} /></td>
                  <td><strong>{formatMoneyMnt(system.valuationMnt)}</strong></td>
                  <td>
                    <strong>{system.developerName ?? '-'}</strong>
                    <span>{system.developerTeam ?? '-'}</span>
                  </td>
                  <td><EnvironmentChip environment={system.environment} /></td>
                  <td><span className={system.inUse ? 'yes-text' : 'muted-text'}>{system.inUse ? 'Тийм' : 'Үгүй'}</span></td>
                  <td><StatusChip status={system.status} /></td>
                  <td><SecurityScore score={system.securityScore} /></td>
                  <td>
                    <div className="row-actions">
                      <button
                        type="button"
                        aria-label={`${system.name} харах`}
                        onClick={() => navigate(`/systems/${system.id}`)}
                      >
                        <Eye size={16} />
                      </button>
                      {isAdmin ? (
                        <>
                          <button
                            type="button"
                            aria-label={`${system.name} засах`}
                            onClick={() => navigate(`/systems/${system.id}/edit`)}
                          >
                            <Pencil size={16} />
                          </button>
                          <button
                            type="button"
                            aria-label={`${system.name} идэвхгүй болгох`}
                            onClick={() => setDisableTarget(system)}
                          >
                            <Link2Off size={16} />
                          </button>
                        </>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <PaginationControls page={page} totalPages={totalPages} onPageChange={setPage} />
      </section>

      {disableTarget ? (
        <ConfirmDialog
          title="Систем идэвхгүй болгох"
          message={`${disableTarget.name} системийг идэвхгүй болгох уу? Бүртгэл устахгүй, статус INACTIVE болно.`}
          confirmLabel="Идэвхгүй болгох"
          isBusy={isDisabling}
          onConfirm={handleDisable}
          onCancel={() => setDisableTarget(null)}
        />
      ) : null}
    </>
  )
}
