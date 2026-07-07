import { ArrowLeft, CheckCircle2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createTransfer, getAccount, getMyAccounts } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { LedgerEntriesTable } from '../components/LedgerEntriesTable'
import { PageHeader } from '../components/PageHeader'
import type { AccountResponse, TransferResponse } from '../types/banking'
import { formatExactMnt } from '../utils/format'

interface AccountCheckState {
  account: AccountResponse | null
  error: string | null
  isLoading: boolean
}

const EMPTY_CHECK: AccountCheckState = { account: null, error: null, isLoading: false }

export function NewTransferPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const isAdmin = auth.user?.role === 'ADMIN'
  const [myAccounts, setMyAccounts] = useState<AccountResponse[]>([])
  const [fromAccountNo, setFromAccountNo] = useState('')
  const [toAccountNo, setToAccountNo] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [toCheck, setToCheck] = useState<AccountCheckState>(EMPTY_CHECK)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [result, setResult] = useState<TransferResponse | null>(null)
  // One key per form fill: a retry of the same submission replays instead of double-charging.
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID())

  useEffect(() => {
    let cancelled = false

    async function loadMyAccounts() {
      if (!auth.token || isAdmin) {
        return
      }
      try {
        const accounts = await getMyAccounts(auth.token)
        if (!cancelled) {
          const active = accounts.filter((account) => account.status === 'ACTIVE')
          setMyAccounts(active)
          if (active.length === 1) {
            setFromAccountNo(active[0].accountNo)
          }
        }
      } catch {
        // The submit path surfaces the error; the dropdown just stays empty.
      }
    }

    void loadMyAccounts()

    return () => {
      cancelled = true
    }
  }, [auth.token, isAdmin])

  const sameAccount = useMemo(
    () => fromAccountNo.trim() !== '' && fromAccountNo.trim() === toAccountNo.trim(),
    [fromAccountNo, toAccountNo],
  )

  const canSubmit =
    fromAccountNo.trim() !== '' &&
    toAccountNo.trim() !== '' &&
    !sameAccount &&
    Number(amount) >= 0.01 &&
    !isSubmitting

  const selectedFrom = myAccounts.find((account) => account.accountNo === fromAccountNo)

  async function checkToAccount() {
    if (!auth.token || !toAccountNo.trim()) {
      return
    }

    setToCheck({ account: null, error: null, isLoading: true })
    try {
      setToCheck({ account: await getAccount(auth.token, toAccountNo.trim()), error: null, isLoading: false })
    } catch (exception) {
      setToCheck({
        account: null,
        error: exception instanceof Error ? exception.message : 'Данс шалгахад алдаа гарлаа',
        isLoading: false,
      })
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!auth.token || !canSubmit) {
      return
    }

    setIsSubmitting(true)
    setSubmitError(null)

    try {
      const transfer = await createTransfer(
        auth.token,
        {
          fromAccountNo: fromAccountNo.trim(),
          toAccountNo: toAccountNo.trim(),
          amount: Number(amount),
          description: description.trim() === '' ? null : description.trim(),
        },
        idempotencyKey,
      )
      setResult(transfer)
    } catch (exception) {
      setSubmitError(exception instanceof Error ? exception.message : 'Шилжүүлэг хийхэд алдаа гарлаа')
    } finally {
      setIsSubmitting(false)
    }
  }

  function resetForm() {
    setResult(null)
    setFromAccountNo(myAccounts.length === 1 ? myAccounts[0].accountNo : '')
    setToAccountNo('')
    setAmount('')
    setDescription('')
    setToCheck(EMPTY_CHECK)
    setSubmitError(null)
    setIdempotencyKey(crypto.randomUUID())
  }

  if (result) {
    return (
      <>
        <PageHeader title="Шилжүүлэг амжилттай" subtitle={result.transferRef} />

        <section className="panel transfer-success">
          <div className="transfer-success-head">
            <CheckCircle2 size={28} aria-hidden="true" />
            <div>
              <strong>{formatExactMnt(result.amount, result.currency)}</strong>
              <span>{result.fromAccountNo} дансаас {result.toAccountNo} данс руу шилжлээ</span>
            </div>
          </div>
          <LedgerEntriesTable entries={result.ledgerEntries} />
          <div className="form-actions">
            <button type="button" className="primary-button" onClick={() => navigate(`/transfers/${result.id}`)}>
              Гүйлгээг харах
            </button>
            <button type="button" className="ghost-button" onClick={() => navigate('/')}>
              Нүүр рүү буцах
            </button>
            <button type="button" className="ghost-button" onClick={resetForm}>
              Дахин шилжүүлэг хийх
            </button>
          </div>
        </section>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Шинэ шилжүүлэг"
        subtitle={isAdmin ? 'Теллер: дурын данс хооронд шилжүүлэг хийнэ' : 'Өөрийн данснаас шилжүүлэг үүсгэх'}
        action={
          <Link className="ghost-button" to="/">
            <ArrowLeft size={16} />
            Буцах
          </Link>
        }
      />

      <form className="panel transfer-form" onSubmit={onSubmit}>
        {isAdmin ? (
          <label className="form-field">
            <span>Илгээгч данс</span>
            <input
              type="text"
              placeholder="Дансны дугаар"
              value={fromAccountNo}
              onChange={(event) => setFromAccountNo(event.target.value)}
              required
            />
          </label>
        ) : (
          <label className="form-field">
            <span>Илгээгч данс</span>
            <select
              value={fromAccountNo}
              onChange={(event) => setFromAccountNo(event.target.value)}
              required
            >
              <option value="">Данс сонгоно уу...</option>
              {myAccounts.map((account) => (
                <option key={account.id} value={account.accountNo}>
                  {account.accountNo} — {formatExactMnt(account.balance, account.currency)}
                </option>
              ))}
            </select>
            {myAccounts.length === 0 ? (
              <p className="account-field-hint">Танд идэвхтэй данс алга. Банкны теллерт хандана уу.</p>
            ) : null}
            {selectedFrom ? (
              <p className="account-field-hint">
                Үлдэгдэл: {formatExactMnt(selectedFrom.balance, selectedFrom.currency)}
              </p>
            ) : null}
          </label>
        )}

        <div className="form-field">
          <span>Хүлээн авагч данс</span>
          <div className="account-field-row">
            <input
              type="text"
              placeholder="Дансны дугаар"
              value={toAccountNo}
              onChange={(event) => {
                setToAccountNo(event.target.value)
                setToCheck(EMPTY_CHECK)
              }}
              required
            />
            <button
              type="button"
              className="ghost-button"
              onClick={() => void checkToAccount()}
              disabled={!toAccountNo.trim() || toCheck.isLoading}
            >
              {toCheck.isLoading ? 'Шалгаж байна...' : 'Шалгах'}
            </button>
          </div>
          {toCheck.account ? (
            <p className="account-field-hint">
              {toCheck.account.customerName} · {toCheck.account.status}
            </p>
          ) : null}
          {toCheck.error ? <p className="field-error">{toCheck.error}</p> : null}
        </div>

        {sameAccount ? <p className="field-error">Илгээгч болон хүлээн авагч данс ижил байж болохгүй.</p> : null}

        <label className="form-field">
          <span>Дүн (MNT)</span>
          <input
            type="number"
            min="0.01"
            step="0.01"
            placeholder="0.00"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
            required
          />
        </label>

        <label className="form-field">
          <span>Тайлбар (заавал биш)</span>
          <textarea
            maxLength={500}
            rows={3}
            placeholder="Гүйлгээний утга..."
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </label>

        {submitError ? <p className="field-error">{submitError}</p> : null}

        <div className="form-actions">
          <button type="submit" className="primary-button" disabled={!canSubmit}>
            {isSubmitting ? 'Шилжүүлж байна...' : 'Шилжүүлэг хийх'}
          </button>
        </div>
      </form>
    </>
  )
}
