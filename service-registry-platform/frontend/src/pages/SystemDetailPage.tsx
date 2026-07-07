import { ArrowLeft, CircleOff, ExternalLink, Pencil } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getSecurityChecks, getSecurityScore, updateSecurityChecks } from '../api/securityApi'
import { disableSystem, getSystem } from '../api/systemsApi'
import { useAuth } from '../auth/useAuth'
import { Chip, EnvironmentChip, SecurityScore, StatusChip, SystemTypeChip } from '../components/Chips'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type {
  SecurityCheckResultResponse,
  SecurityCheckResultStatus,
  SecurityScoreResponse,
  SystemResponse,
} from '../types/api'
import { formatDateTime, formatMoneyMnt } from '../utils/format'

type DetailTab = 'overview' | 'security'

const RESULT_OPTIONS: SecurityCheckResultStatus[] = ['PASS', 'WARNING', 'FAIL', 'NOT_CHECKED']

export function SystemDetailPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const { id } = useParams()
  const systemId = Number(id)

  const [system, setSystem] = useState<SystemResponse | null>(null)
  const [score, setScore] = useState<SecurityScoreResponse | null>(null)
  const [checks, setChecks] = useState<SecurityCheckResultResponse[]>([])
  const [tab, setTab] = useState<DetailTab>('overview')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [showDisableConfirm, setShowDisableConfirm] = useState(false)
  const [isDisabling, setIsDisabling] = useState(false)
  const [isSavingChecks, setIsSavingChecks] = useState(false)
  const [checksMessage, setChecksMessage] = useState<string | null>(null)
  const [checksError, setChecksError] = useState<string | null>(null)

  const canAdmin = auth.user?.role === 'ADMIN'
  const canEditSecurity = auth.user?.role === 'ADMIN' || auth.user?.role === 'SECURITY'

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!auth.token || Number.isNaN(systemId)) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const [systemResponse, scoreResponse, checksResponse] = await Promise.all([
          getSystem(auth.token, systemId),
          getSecurityScore(auth.token, systemId),
          getSecurityChecks(auth.token, systemId),
        ])

        if (!cancelled) {
          setSystem(systemResponse)
          setScore(scoreResponse)
          setChecks(checksResponse)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Системийн мэдээлэл уншихад алдаа гарлаа')
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
  }, [auth.token, systemId, reloadKey])

  if (isLoading) {
    return <LoadingState label="Системийн мэдээлэл уншиж байна..." />
  }

  if (error) {
    return <ErrorState message={error} />
  }

  if (!system) {
    return <ErrorState message="Систем олдсонгүй." />
  }

  function updateCheck(controlId: number, patch: Partial<Pick<SecurityCheckResultResponse, 'result' | 'evidence'>>) {
    setChecksMessage(null)
    setChecks((current) => current.map((check) => (check.controlId === controlId ? { ...check, ...patch } : check)))
  }

  async function handleSaveChecks() {
    if (!auth.token) {
      return
    }

    setIsSavingChecks(true)
    setChecksError(null)
    setChecksMessage(null)

    try {
      await updateSecurityChecks(auth.token, systemId, checks.map((check) => ({
        controlId: check.controlId,
        result: check.result,
        evidence: check.evidence && check.evidence.trim() ? check.evidence.trim() : null,
      })))
      setChecksMessage('Security checklist хадгалагдлаа.')
      setReloadKey((key) => key + 1)
    } catch (exception) {
      setChecksError(exception instanceof Error ? exception.message : 'Хадгалахад алдаа гарлаа')
    } finally {
      setIsSavingChecks(false)
    }
  }

  async function handleDisable() {
    if (!auth.token) {
      return
    }

    setIsDisabling(true)

    try {
      await disableSystem(auth.token, systemId)
      setShowDisableConfirm(false)
      setReloadKey((key) => key + 1)
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Идэвхгүй болгоход алдаа гарлаа')
    } finally {
      setIsDisabling(false)
    }
  }

  const links = [
    { label: 'Base URL', url: system.baseUrl },
    { label: 'Health', url: system.healthUrl },
    { label: 'Swagger', url: system.swaggerUrl },
    { label: 'Repo', url: system.repoUrl },
  ]

  return (
    <>
      <Link className="back-link" to="/systems">
        <ArrowLeft size={15} />
        Системийн жагсаалт руу буцах
      </Link>

      <div className="detail-header panel">
        <div className="detail-title-row">
          <div>
            <h1>{system.name}</h1>
            <p className="detail-key">{system.systemKey}</p>
          </div>
          {canAdmin ? (
            <div className="detail-actions">
              <button className="ghost-button" type="button" onClick={() => navigate(`/systems/${system.id}/edit`)}>
                <Pencil size={15} />
                Засах
              </button>
              <button
                className="danger-button"
                type="button"
                disabled={system.status === 'INACTIVE' && !system.inUse}
                onClick={() => setShowDisableConfirm(true)}
              >
                <CircleOff size={15} />
                Идэвхгүй болгох
              </button>
            </div>
          ) : null}
        </div>
        <div className="detail-chips">
          <SystemTypeChip type={system.type} />
          <EnvironmentChip environment={system.environment} />
          <StatusChip status={system.status} />
          {score ? <SecurityScore score={score.score} /> : null}
          <Chip tone={system.inUse ? 'green' : 'gray'} compact>{system.inUse ? 'Ашиглалтад байгаа' : 'Ашиглалтгүй'}</Chip>
        </div>
      </div>

      <div className="tab-bar">
        <button
          type="button"
          className={`tab-button${tab === 'overview' ? ' active' : ''}`}
          onClick={() => setTab('overview')}
        >
          Ерөнхий мэдээлэл
        </button>
        <button
          type="button"
          className={`tab-button${tab === 'security' ? ' active' : ''}`}
          onClick={() => setTab('security')}
        >
          Security checklist
        </button>
      </div>

      {tab === 'overview' ? (
        <div className="detail-grid">
          <section className="panel detail-card">
            <h2>Үндсэн мэдээлэл</h2>
            <div className="kv"><span>Үнэлгээ</span><strong>{formatMoneyMnt(system.valuationMnt)}</strong></div>
            <div className="kv"><span>Хөгжүүлэгч</span><strong>{system.developerName ?? '-'}</strong></div>
            <div className="kv"><span>Баг</span><strong>{system.developerTeam ?? '-'}</strong></div>
            <div className="kv"><span>Тайлбар</span><strong>{system.description ?? '-'}</strong></div>
          </section>

          <section className="panel detail-card">
            <h2>Хугацаа</h2>
            <div className="kv"><span>Эхэлсэн</span><strong>{system.startDate ?? '-'}</strong></div>
            <div className="kv"><span>Дуусах</span><strong>{system.endDate ?? '-'}</strong></div>
            <div className="kv"><span>Бүртгэсэн</span><strong>{formatDateTime(system.createdAt)}</strong></div>
            <div className="kv"><span>Шинэчилсэн</span><strong>{formatDateTime(system.updatedAt)}</strong></div>
          </section>

          <section className="panel detail-card">
            <h2>Холбоосууд</h2>
            {links.every((link) => !link.url) ? <p className="form-hint">Холбоос бүртгээгүй.</p> : null}
            <div className="link-list">
              {links.filter((link) => link.url).map((link) => (
                <a key={link.label} className="link-chip" href={link.url ?? '#'} target="_blank" rel="noreferrer">
                  <ExternalLink size={13} />
                  {link.label}
                </a>
              ))}
            </div>
          </section>

          <section className="panel detail-card detail-card-wide">
            <h2>Холбоотой системүүд</h2>
            {system.relatedSystems.length === 0 ? <EmptyState label="Холбоотой систем бүртгээгүй." /> : (
              <div className="table-scroll">
                <table className="data-table compact-table">
                  <thead>
                    <tr>
                      <th>Систем</th>
                      <th>Холболтын төрөл</th>
                      <th>Тайлбар</th>
                    </tr>
                  </thead>
                  <tbody>
                    {system.relatedSystems.map((relation) => (
                      <tr key={relation.id}>
                        <td>
                          <strong>{relation.targetSystemName}</strong>
                          <span>{relation.targetSystemKey}</span>
                        </td>
                        <td><Chip tone="blue" compact>{relation.relationType}</Chip></td>
                        <td>{relation.description ?? '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      ) : (
        <>
          {score ? (
            <section className="panel score-panel">
              <div className="score-panel-main">
                <span className="score-panel-value">{score.score}</span>
                <div>
                  <strong>Security score</strong>
                  <p>{score.earnedWeight} / {score.totalWeight} жин</p>
                </div>
              </div>
              <div className="score-panel-chips">
                <Chip tone="green" compact>{`PASS ${score.passCount}`}</Chip>
                <Chip tone="amber" compact>{`WARNING ${score.warningCount}`}</Chip>
                <Chip tone="red" compact>{`FAIL ${score.failCount}`}</Chip>
                <Chip tone="gray" compact>{`ШАЛГААГҮЙ ${score.notCheckedCount}`}</Chip>
              </div>
            </section>
          ) : null}

          <section className="panel table-panel">
            <div className="table-scroll">
              <table className="data-table checklist-table">
                <thead>
                  <tr>
                    <th>Хяналт</th>
                    <th>Жин</th>
                    <th>Заавал</th>
                    <th>Үр дүн</th>
                    <th>Нотолгоо</th>
                    <th>Шалгасан</th>
                  </tr>
                </thead>
                <tbody>
                  {checks.map((check) => (
                    <tr key={check.controlId}>
                      <td>
                        <strong>{check.title}</strong>
                        <span>{check.standardRef ?? check.controlKey}</span>
                      </td>
                      <td><strong>{check.weight}</strong></td>
                      <td>{check.required ? 'Тийм' : '-'}</td>
                      <td>
                        <select
                          className="result-select"
                          value={check.result}
                          disabled={!canEditSecurity}
                          onChange={(event) => updateCheck(check.controlId, { result: event.target.value as SecurityCheckResultStatus })}
                        >
                          {RESULT_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
                        </select>
                      </td>
                      <td>
                        <input
                          className="evidence-input"
                          value={check.evidence ?? ''}
                          placeholder="Нотолгоо..."
                          disabled={!canEditSecurity}
                          onChange={(event) => updateCheck(check.controlId, { evidence: event.target.value })}
                        />
                      </td>
                      <td className="mono-cell">{check.checkedAt ? formatDateTime(check.checkedAt) : '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {canEditSecurity ? (
              <div className="save-bar">
                {checksError ? <span className="inline-error">{checksError}</span> : null}
                {checksMessage ? <span className="inline-success">{checksMessage}</span> : null}
                <button className="primary-button" type="button" disabled={isSavingChecks} onClick={handleSaveChecks}>
                  {isSavingChecks ? 'Хадгалж байна...' : 'Хадгалах'}
                </button>
              </div>
            ) : null}
          </section>
        </>
      )}

      {showDisableConfirm ? (
        <ConfirmDialog
          title="Систем идэвхгүй болгох"
          message={`${system.name} системийг идэвхгүй болгох уу? Бүртгэл устахгүй, статус INACTIVE болно.`}
          confirmLabel="Идэвхгүй болгох"
          isBusy={isDisabling}
          onConfirm={handleDisable}
          onCancel={() => setShowDisableConfirm(false)}
        />
      ) : null}
    </>
  )
}
