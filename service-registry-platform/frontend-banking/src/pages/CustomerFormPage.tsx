import { ArrowLeft, Plus, UserX } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createCustomer, deactivateCustomer, getCustomer, updateCustomer } from '../api/bankingApi'
import { createPlatformUser } from '../api/usersApi'
import { useAuth } from '../auth/useAuth'
import { AccountBalanceCard } from '../components/AccountBalanceCard'
import { Chip } from '../components/Chips'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { PageHeader } from '../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { CustomerResponse } from '../types/banking'

export function CustomerFormPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const { id } = useParams()
  const isEdit = Boolean(id)
  const [customer, setCustomer] = useState<CustomerResponse | null>(null)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [createLogin, setCreateLogin] = useState(!isEdit)
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(isEdit)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [showDeactivateDialog, setShowDeactivateDialog] = useState(false)
  const [isDeactivating, setIsDeactivating] = useState(false)

  useEffect(() => {
    let cancelled = false

    async function loadCustomer() {
      if (!auth.token || !id) {
        return
      }

      setIsLoading(true)
      setLoadError(null)

      try {
        const result = await getCustomer(auth.token, id)
        if (!cancelled) {
          setCustomer(result)
          setFirstName(result.firstName)
          setLastName(result.lastName)
          setPhone(result.phone ?? '')
          setEmail(result.email ?? '')
          setUsername(result.username ?? '')
        }
      } catch (exception) {
        if (!cancelled) {
          setLoadError(exception instanceof Error ? exception.message : 'Харилцагч уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadCustomer()

    return () => {
      cancelled = true
    }
  }, [auth.token, id])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!auth.token) {
      return
    }

    setIsSubmitting(true)
    setSubmitError(null)

    const payload = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phone: phone.trim() || null,
      email: email.trim() || null,
      username: username.trim() || null,
    }

    try {
      if (isEdit && id) {
        await updateCustomer(auth.token, id, payload)
        navigate('/admin/customers')
      } else {
        if (createLogin) {
          const trimmedUsername = username.trim()
          if (!trimmedUsername) {
            setSubmitError('Нэвтрэх эрх үүсгэхийн тулд нэвтрэх нэрээ оруулна уу.')
            return
          }
          if (password.length < 8) {
            setSubmitError('Нууц үг хамгийн багадаа 8 тэмдэгт байна.')
            return
          }

          await createPlatformUser(auth.token, {
            username: trimmedUsername,
            password,
            displayName: `${firstName.trim()} ${lastName.trim()}`.trim(),
            role: 'VIEWER',
          })
        }

        try {
          const created = await createCustomer(auth.token, payload)
          navigate(`/admin/customers/${created.id}`)
        } catch (customerError) {
          if (createLogin) {
            const reason = customerError instanceof Error ? customerError.message : 'тодорхойгүй алдаа'
            throw new Error(
              `Нэвтрэх эрх үүссэн ч харилцагч үүсгэхэд алдаа гарлаа: ${reason}. ` +
              'Дахин илгээхдээ "Нэвтрэх эрх зэрэг үүсгэх" сонголтыг унтраагаарай.'
            )
          }
          throw customerError
        }
      }
    } catch (exception) {
      setSubmitError(exception instanceof Error ? exception.message : 'Хадгалахад алдаа гарлаа')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function onDeactivate() {
    if (!auth.token || !id) {
      return
    }

    setIsDeactivating(true)
    try {
      await deactivateCustomer(auth.token, id)
      setShowDeactivateDialog(false)
      navigate('/admin/customers')
    } catch (exception) {
      setSubmitError(exception instanceof Error ? exception.message : 'Идэвхгүй болгоход алдаа гарлаа')
      setShowDeactivateDialog(false)
    } finally {
      setIsDeactivating(false)
    }
  }

  return (
    <>
      <PageHeader
        title={isEdit ? `Харилцагч — ${customer?.customerNo ?? ''}` : 'Шинэ харилцагч'}
        subtitle={isEdit ? 'Мэдээлэл засах, данснуудыг харах' : 'Шинэ харилцагч бүртгэх'}
        action={
          <Link className="ghost-button" to="/admin/customers">
            <ArrowLeft size={16} />
            Буцах
          </Link>
        }
      />

      {isLoading ? <LoadingState label="Харилцагч уншиж байна..." /> : null}
      {loadError ? <ErrorState message={loadError} /> : null}

      {!isLoading && !loadError ? (
        <>
          <form className="panel transfer-form" onSubmit={onSubmit}>
            {isEdit && customer ? (
              <div className="form-field">
                <span>Төлөв</span>
                <div>
                  <Chip tone={customer.active ? 'green' : 'gray'}>
                    {customer.active ? 'Идэвхтэй' : 'Идэвхгүй'}
                  </Chip>
                </div>
              </div>
            ) : null}

            <label className="form-field">
              <span>Овог</span>
              <input value={lastName} required maxLength={80} onChange={(event) => setLastName(event.target.value)} />
            </label>
            <label className="form-field">
              <span>Нэр</span>
              <input value={firstName} required maxLength={80} onChange={(event) => setFirstName(event.target.value)} />
            </label>
            <label className="form-field">
              <span>Утас</span>
              <input value={phone} maxLength={30} onChange={(event) => setPhone(event.target.value)} />
            </label>
            <label className="form-field">
              <span>И-мэйл</span>
              <input type="email" value={email} maxLength={160} onChange={(event) => setEmail(event.target.value)} />
            </label>
            <label className="form-field">
              <span>Нэвтрэх нэр (portal хэрэглэгчтэй холбоно)</span>
              <input
                value={username}
                required={!isEdit && createLogin}
                maxLength={80}
                onChange={(event) => setUsername(event.target.value)}
              />
            </label>

            {!isEdit ? (
              <>
                <label className="form-field form-field-inline">
                  <input
                    type="checkbox"
                    checked={createLogin}
                    onChange={(event) => setCreateLogin(event.target.checked)}
                  />
                  <span>Нэвтрэх эрх зэрэг үүсгэх (portal хэрэглэгч, VIEWER эрхтэй)</span>
                </label>
                {createLogin ? (
                  <label className="form-field">
                    <span>Нууц үг (8+ тэмдэгт)</span>
                    <input
                      type="password"
                      value={password}
                      required
                      minLength={8}
                      maxLength={72}
                      autoComplete="new-password"
                      onChange={(event) => setPassword(event.target.value)}
                    />
                  </label>
                ) : (
                  <p className="form-hint">
                    Нэвтрэх эрхгүй бол харилцагч banking app-д өөрөө нэвтэрч чадахгүй — зөвхөн теллер үйлчилнэ.
                  </p>
                )}
              </>
            ) : null}

            {submitError ? <p className="field-error">{submitError}</p> : null}

            <div className="form-actions">
              <button type="submit" className="primary-button" disabled={isSubmitting || !firstName.trim() || !lastName.trim()}>
                {isSubmitting ? 'Хадгалж байна...' : 'Хадгалах'}
              </button>
              {isEdit && customer?.active ? (
                <button
                  type="button"
                  className="danger-button"
                  onClick={() => setShowDeactivateDialog(true)}
                >
                  <UserX size={15} />
                  Идэвхгүй болгох
                </button>
              ) : null}
            </div>
          </form>

          {isEdit && customer ? (
            <section className="panel table-panel">
              <div className="panel-head-row">
                <div className="table-count">Данснууд</div>
                <button
                  type="button"
                  className="ghost-button"
                  onClick={() => navigate(`/admin/accounts?customerNo=${encodeURIComponent(customer.customerNo)}`)}
                >
                  <Plus size={15} />
                  Данс нээх
                </button>
              </div>
              {(customer.accounts ?? []).length === 0 ? (
                <EmptyState label='Данс алга — "Данс нээх" товчоор нээнэ үү.' />
              ) : (
                <div className="account-card-grid">
                  {(customer.accounts ?? []).map((account) => (
                    <AccountBalanceCard key={account.id} account={account} />
                  ))}
                </div>
              )}
            </section>
          ) : null}
        </>
      ) : null}

      {showDeactivateDialog && customer ? (
        <ConfirmDialog
          title="Харилцагч идэвхгүй болгох"
          message={`${customer.firstName} ${customer.lastName} (${customer.customerNo})-г идэвхгүй болгох уу? Данснууд нь хэвээр үлдэнэ.`}
          confirmLabel="Идэвхгүй болгох"
          isBusy={isDeactivating}
          onConfirm={() => void onDeactivate()}
          onCancel={() => setShowDeactivateDialog(false)}
        />
      ) : null}
    </>
  )
}
