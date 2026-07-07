import { Plus, Save, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createSystem, getSystem, getSystems, updateSystem } from '../api/systemsApi'
import { useAuth } from '../auth/useAuth'
import { PageHeader } from '../components/PageHeader'
import { ErrorState, LoadingState } from '../components/States'
import type {
  RelationType,
  SystemEnvironment,
  SystemPayload,
  SystemResponse,
  SystemStatus,
  SystemType,
} from '../types/api'

interface RelationRow {
  targetSystemId: number | ''
  relationType: RelationType
  description: string
}

interface SystemFormState {
  systemKey: string
  name: string
  type: SystemType
  valuationMnt: string
  description: string
  developerName: string
  developerTeam: string
  startDate: string
  endDate: string
  inUse: boolean
  environment: SystemEnvironment
  baseUrl: string
  healthUrl: string
  swaggerUrl: string
  repoUrl: string
  status: SystemStatus
  relations: RelationRow[]
}

const SYSTEM_TYPES: SystemType[] = ['CORE', 'CARD', 'DIGITAL', 'INTERNAL']
const ENVIRONMENTS: SystemEnvironment[] = ['DEV', 'TEST', 'UAT', 'PROD']
const STATUSES: SystemStatus[] = ['ACTIVE', 'INACTIVE', 'UNKNOWN', 'DOWN']
const RELATION_TYPES: RelationType[] = ['DEPENDS_ON', 'CALLS', 'INTEGRATES_WITH']

const emptyForm: SystemFormState = {
  systemKey: '',
  name: '',
  type: 'INTERNAL',
  valuationMnt: '0',
  description: '',
  developerName: '',
  developerTeam: '',
  startDate: '',
  endDate: '',
  inUse: true,
  environment: 'DEV',
  baseUrl: '',
  healthUrl: '',
  swaggerUrl: '',
  repoUrl: '',
  status: 'ACTIVE',
  relations: [],
}

export function SystemFormPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const { id } = useParams()
  const systemId = id ? Number(id) : null
  const isEdit = systemId !== null

  const [form, setForm] = useState<SystemFormState>(emptyForm)
  const [options, setOptions] = useState<SystemResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!auth.token) {
        return
      }

      try {
        const optionsPage = await getSystems(auth.token, { size: 100 })
        let loadedForm: SystemFormState | null = null
        if (systemId !== null) {
          const system = await getSystem(auth.token, systemId)
          loadedForm = toFormState(system)
        }

        if (!cancelled) {
          setOptions(optionsPage.content)
          if (loadedForm) {
            setForm(loadedForm)
          }
        }
      } catch (exception) {
        if (!cancelled) {
          setLoadError(exception instanceof Error ? exception.message : 'Мэдээлэл уншихад алдаа гарлаа')
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
  }, [auth.token, systemId])

  if (auth.user?.role !== 'ADMIN') {
    return (
      <>
        <PageHeader title="Систем бүртгэх" subtitle="Хандах эрх шаардлагатай" />
        <ErrorState message="Энэ хуудсанд зөвхөн ADMIN эрхтэй хэрэглэгч хандана." />
      </>
    )
  }

  if (isLoading) {
    return <LoadingState label="Форм бэлдэж байна..." />
  }

  if (loadError) {
    return <ErrorState message={loadError} />
  }

  function set<K extends keyof SystemFormState>(key: K, value: SystemFormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function addRelation() {
    set('relations', [...form.relations, { targetSystemId: '', relationType: 'DEPENDS_ON', description: '' }])
  }

  function updateRelation(index: number, patch: Partial<RelationRow>) {
    set('relations', form.relations.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)))
  }

  function removeRelation(index: number) {
    set('relations', form.relations.filter((_, rowIndex) => rowIndex !== index))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!auth.token) {
      return
    }

    const valuation = Number(form.valuationMnt)
    if (!form.name.trim()) {
      setFormError('Системийн нэр хоосон байж болохгүй.')
      return
    }
    if (Number.isNaN(valuation) || valuation < 0) {
      setFormError('Үнэлгээ 0-ээс их буюу тэнцүү тоо байх ёстой.')
      return
    }
    if (form.startDate && form.endDate && form.startDate > form.endDate) {
      setFormError('Эхлэх огноо дуусах огнооноос хойш байж болохгүй.')
      return
    }
    if (form.relations.some((row) => row.targetSystemId === '')) {
      setFormError('Холболт бүр дээр зорилтот системээ сонгоно уу.')
      return
    }

    const payload: SystemPayload = {
      name: form.name.trim(),
      type: form.type,
      valuationMnt: valuation,
      description: form.description.trim() || null,
      developerName: form.developerName.trim() || null,
      developerTeam: form.developerTeam.trim() || null,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      inUse: form.inUse,
      environment: form.environment,
      baseUrl: form.baseUrl.trim() || null,
      healthUrl: form.healthUrl.trim() || null,
      swaggerUrl: form.swaggerUrl.trim() || null,
      repoUrl: form.repoUrl.trim() || null,
      status: form.status,
      relatedSystems: form.relations.map((row) => ({
        targetSystemId: Number(row.targetSystemId),
        relationType: row.relationType,
        description: row.description.trim() || null,
      })),
    }

    if (!isEdit && form.systemKey.trim()) {
      payload.systemKey = form.systemKey.trim()
    }

    setIsSaving(true)
    setFormError(null)

    try {
      const saved = isEdit && systemId !== null
        ? await updateSystem(auth.token, systemId, payload)
        : await createSystem(auth.token, payload)
      navigate(`/systems/${saved.id}`)
    } catch (exception) {
      setFormError(exception instanceof Error ? exception.message : 'Хадгалахад алдаа гарлаа')
    } finally {
      setIsSaving(false)
    }
  }

  const relationOptions = options.filter((option) => option.id !== systemId)
  const chosenTargetIds = form.relations
    .map((row) => row.targetSystemId)
    .filter((value): value is number => value !== '')

  return (
    <>
      <PageHeader
        title={isEdit ? 'Систем засах' : 'Шинэ систем бүртгэх'}
        subtitle={isEdit ? `${form.name} системийн мэдээллийг шинэчлэх` : 'Даалгаврын бүх талбартай бүртгэлийн форм'}
      />

      <form className="form-card" onSubmit={handleSubmit}>
        <section className="form-section">
          <h3>Үндсэн мэдээлэл</h3>
          <div className="form-grid">
            <label className="form-field">
              <span>Системийн код {isEdit ? '(өөрчлөгдөхгүй)' : '(хоосон бол нэрээс үүснэ)'}</span>
              <input
                value={form.systemKey}
                readOnly={isEdit}
                onChange={(event) => set('systemKey', event.target.value)}
                placeholder="core-banking"
              />
            </label>
            <label className="form-field">
              <span>Нэр *</span>
              <input value={form.name} required onChange={(event) => set('name', event.target.value)} />
            </label>
            <label className="form-field">
              <span>Төрөл *</span>
              <select value={form.type} onChange={(event) => set('type', event.target.value as SystemType)}>
                {SYSTEM_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Үнэлгээ (₮) *</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.valuationMnt}
                required
                onChange={(event) => set('valuationMnt', event.target.value)}
              />
            </label>
            <label className="form-field form-field-wide">
              <span>Тайлбар</span>
              <textarea rows={3} value={form.description} onChange={(event) => set('description', event.target.value)} />
            </label>
          </div>
        </section>

        <section className="form-section">
          <h3>Хөгжүүлэгч</h3>
          <div className="form-grid">
            <label className="form-field">
              <span>Хөгжүүлэгчийн нэр</span>
              <input value={form.developerName} onChange={(event) => set('developerName', event.target.value)} />
            </label>
            <label className="form-field">
              <span>Баг</span>
              <input value={form.developerTeam} onChange={(event) => set('developerTeam', event.target.value)} />
            </label>
          </div>
        </section>

        <section className="form-section">
          <h3>Хугацаа ба ажиллагаа</h3>
          <div className="form-grid">
            <label className="form-field">
              <span>Эхэлсэн огноо</span>
              <input type="date" value={form.startDate} onChange={(event) => set('startDate', event.target.value)} />
            </label>
            <label className="form-field">
              <span>Дуусах огноо</span>
              <input type="date" value={form.endDate} onChange={(event) => set('endDate', event.target.value)} />
            </label>
            <label className="form-field">
              <span>Орчин</span>
              <select value={form.environment} onChange={(event) => set('environment', event.target.value as SystemEnvironment)}>
                {ENVIRONMENTS.map((environment) => <option key={environment} value={environment}>{environment}</option>)}
              </select>
            </label>
            <label className="form-field">
              <span>Төлөв</span>
              <select value={form.status} onChange={(event) => set('status', event.target.value as SystemStatus)}>
                {STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
              </select>
            </label>
            <label className="form-field form-field-inline">
              <input
                type="checkbox"
                checked={form.inUse}
                onChange={(event) => set('inUse', event.target.checked)}
              />
              <span>Ашиглалтад байгаа</span>
            </label>
          </div>
        </section>

        <section className="form-section">
          <h3>Холбоосууд</h3>
          <div className="form-grid">
            <label className="form-field">
              <span>Base URL</span>
              <input type="url" value={form.baseUrl} onChange={(event) => set('baseUrl', event.target.value)} placeholder="https://" />
            </label>
            <label className="form-field">
              <span>Health URL</span>
              <input type="url" value={form.healthUrl} onChange={(event) => set('healthUrl', event.target.value)} placeholder="https://" />
            </label>
            <label className="form-field">
              <span>Swagger URL</span>
              <input type="url" value={form.swaggerUrl} onChange={(event) => set('swaggerUrl', event.target.value)} placeholder="https://" />
            </label>
            <label className="form-field">
              <span>Repo URL</span>
              <input type="url" value={form.repoUrl} onChange={(event) => set('repoUrl', event.target.value)} placeholder="https://" />
            </label>
          </div>
        </section>

        <section className="form-section">
          <h3>Холбоотой системүүд</h3>
          {form.relations.length === 0 ? <p className="form-hint">Холбоотой систем нэмээгүй байна.</p> : null}
          {form.relations.map((row, index) => (
            <div className="relation-row" key={index}>
              <select
                value={row.targetSystemId}
                onChange={(event) => updateRelation(index, { targetSystemId: event.target.value ? Number(event.target.value) : '' })}
              >
                <option value="">Систем сонгох...</option>
                {relationOptions.map((option) => (
                  <option
                    key={option.id}
                    value={option.id}
                    disabled={option.id !== row.targetSystemId && chosenTargetIds.includes(option.id)}
                  >
                    {option.name} ({option.systemKey})
                  </option>
                ))}
              </select>
              <select
                value={row.relationType}
                onChange={(event) => updateRelation(index, { relationType: event.target.value as RelationType })}
              >
                {RELATION_TYPES.map((relationType) => <option key={relationType} value={relationType}>{relationType}</option>)}
              </select>
              <input
                value={row.description}
                placeholder="Тайлбар"
                onChange={(event) => updateRelation(index, { description: event.target.value })}
              />
              <button type="button" className="icon-button" aria-label="Холболт устгах" onClick={() => removeRelation(index)}>
                <Trash2 size={15} />
              </button>
            </div>
          ))}
          <button type="button" className="ghost-button" onClick={addRelation}>
            <Plus size={15} />
            Холболт нэмэх
          </button>
        </section>

        {formError ? <div className="form-error">{formError}</div> : null}

        <div className="form-actions">
          <button
            type="button"
            className="ghost-button"
            onClick={() => navigate(isEdit && systemId !== null ? `/systems/${systemId}` : '/systems')}
          >
            Болих
          </button>
          <button className="primary-button" type="submit" disabled={isSaving}>
            <Save size={16} />
            {isSaving ? 'Хадгалж байна...' : 'Хадгалах'}
          </button>
        </div>
      </form>
    </>
  )
}

function toFormState(system: SystemResponse): SystemFormState {
  return {
    systemKey: system.systemKey ?? '',
    name: system.name,
    type: system.type,
    valuationMnt: String(system.valuationMnt),
    description: system.description ?? '',
    developerName: system.developerName ?? '',
    developerTeam: system.developerTeam ?? '',
    startDate: system.startDate ?? '',
    endDate: system.endDate ?? '',
    inUse: system.inUse,
    environment: system.environment,
    baseUrl: system.baseUrl ?? '',
    healthUrl: system.healthUrl ?? '',
    swaggerUrl: system.swaggerUrl ?? '',
    repoUrl: system.repoUrl ?? '',
    status: system.status,
    relations: system.relatedSystems.map((relation) => ({
      targetSystemId: relation.targetSystemId,
      relationType: relation.relationType as RelationType,
      description: relation.description ?? '',
    })),
  }
}
