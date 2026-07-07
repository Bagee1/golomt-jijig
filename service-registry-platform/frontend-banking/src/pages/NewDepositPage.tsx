import { ArrowLeft, CheckCircle2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getDepositProducts, openDeposit } from '../api/depositApi'
import { getMyAccounts } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { PageHeader } from '../components/PageHeader'
import type { AccountResponse } from '../types/banking'
import type { DepositProduct, DepositResponse } from '../types/deposit'
import { formatExactMnt } from '../utils/format'

export function NewDepositPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const isAdmin = auth.user?.role === 'ADMIN'
  const [products, setProducts] = useState<DepositProduct[]>([])
  const [myAccounts, setMyAccounts] = useState<AccountResponse[]>([])
  const [linkedAccountNo, setLinkedAccountNo] = useState('')
  const [termMonths, setTermMonths] = useState('')
  const [amount, setAmount] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [result, setResult] = useState<DepositResponse | null>(null)
  // One key per form fill: a retry of the same submission replays instead of opening twice.
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID())

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!auth.token) {
        return
      }
      try {
        const loadedProducts = await getDepositProducts(auth.token)
        if (!cancelled) {
          setProducts(loadedProducts)
        }
        if (!isAdmin) {
          const accounts = await getMyAccounts(auth.token)
          if (!cancelled) {
            const active = accounts.filter((account) => account.status === 'ACTIVE')
            setMyAccounts(active)
            if (active.length === 1) {
              setLinkedAccountNo(active[0].accountNo)
            }
          }
        }
      } catch (exception) {
        if (!cancelled) {
          setSubmitError(exception instanceof Error ? exception.message : 'Мэдээлэл уншихад алдаа гарлаа')
        }
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [auth.token, isAdmin])

  const selectedProduct = products.find((product) => product.termMonths === Number(termMonths))
  const selectedAccount = myAccounts.find((account) => account.accountNo === linkedAccountNo)

  // Client-side preview only; the server recomputes the authoritative figure at open time.
  const preview = useMemo(() => {
    const amountValue = Number(amount)
    if (!selectedProduct || !(amountValue > 0)) {
      return null
    }
    const maturity = new Date()
    maturity.setMonth(maturity.getMonth() + selectedProduct.termMonths)
    const days = Math.max(1, Math.round((maturity.getTime() - Date.now()) / 86_400_000))
    const interest = (amountValue * selectedProduct.annualRatePercent * days) / (365 * 100)
    return {
      interest,
      payout: amountValue + interest,
      maturityDate: maturity.toISOString().slice(0, 10),
    }
  }, [amount, selectedProduct])

  const withinRange =
    selectedProduct != null &&
    Number(amount) >= selectedProduct.minAmount &&
    Number(amount) <= selectedProduct.maxAmount

  const canSubmit = linkedAccountNo.trim() !== '' && selectedProduct != null && withinRange && !isSubmitting

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!auth.token || !canSubmit) {
      return
    }

    setIsSubmitting(true)
    setSubmitError(null)
    try {
      const deposit = await openDeposit(
        auth.token,
        { linkedAccountNo: linkedAccountNo.trim(), termMonths: Number(termMonths), amount: Number(amount) },
        idempotencyKey,
      )
      setResult(deposit)
    } catch (exception) {
      setSubmitError(exception instanceof Error ? exception.message : 'Хадгаламж нээхэд алдаа гарлаа')
      // A fresh key on the next attempt only if this was a hard failure, not a network replay.
      setIdempotencyKey(crypto.randomUUID())
    } finally {
      setIsSubmitting(false)
    }
  }

  if (result) {
    return (
      <>
        <PageHeader title="Хадгаламж нээгдлээ" subtitle={result.depositNo} />
        <section className="panel transfer-success">
          <div className="transfer-success-head">
            <CheckCircle2 size={28} aria-hidden="true" />
            <div>
              <strong>{formatExactMnt(result.principal)}</strong>
              <span>{result.linkedAccountNo} данснаас {result.termMonths} сарын хугацаатай хадгаламжид байршлаа</span>
            </div>
          </div>
          <div className="deposit-field-grid">
            <div><span>Жилийн хүү</span><strong>{result.annualRate}%</strong></div>
            <div><span>Дуусах огноо</span><strong>{result.maturityDate}</strong></div>
            <div><span>Таамаг хүү</span><strong>{formatExactMnt(result.projectedInterest)}</strong></div>
          </div>
          <div className="form-actions">
            <button type="button" className="primary-button" onClick={() => navigate(`/deposits/${result.id}`)}>
              Дэлгэрэнгүй харах
            </button>
            <button type="button" className="ghost-button" onClick={() => navigate('/deposits')}>
              Хадгаламж руу буцах
            </button>
          </div>
        </section>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Шинэ хадгаламж"
        subtitle="Хугацаа сонгож, өөрийн данснаас хадгаламж нээх"
        action={
          <Link className="ghost-button" to="/deposits">
            <ArrowLeft size={16} />
            Буцах
          </Link>
        }
      />

      <form className="panel transfer-form" onSubmit={onSubmit}>
        {isAdmin ? (
          <label className="form-field">
            <span>Холбох данс</span>
            <input
              type="text"
              placeholder="Дансны дугаар"
              value={linkedAccountNo}
              onChange={(event) => setLinkedAccountNo(event.target.value)}
              required
            />
          </label>
        ) : (
          <label className="form-field">
            <span>Холбох данс</span>
            <select value={linkedAccountNo} onChange={(event) => setLinkedAccountNo(event.target.value)} required>
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
            {selectedAccount ? (
              <p className="account-field-hint">Үлдэгдэл: {formatExactMnt(selectedAccount.balance, selectedAccount.currency)}</p>
            ) : null}
          </label>
        )}

        <label className="form-field">
          <span>Хугацаа</span>
          <select value={termMonths} onChange={(event) => setTermMonths(event.target.value)} required>
            <option value="">Хугацаа сонгоно уу...</option>
            {products.map((product) => (
              <option key={product.termMonths} value={product.termMonths}>
                {product.termMonths} сар — {product.annualRatePercent}% жилийн хүү
              </option>
            ))}
          </select>
        </label>

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
          {selectedProduct ? (
            <p className="account-field-hint">
              Зөвшөөрөгдөх хязгаар: {formatExactMnt(selectedProduct.minAmount)} – {formatExactMnt(selectedProduct.maxAmount)}
            </p>
          ) : null}
          {selectedProduct && amount !== '' && !withinRange ? (
            <p className="field-error">Дүн зөвшөөрөгдсөн хязгаараас гадуур байна.</p>
          ) : null}
        </label>

        {preview ? (
          <div className="interest-preview">
            Хугацааны эцэст: <strong>{formatExactMnt(preview.payout)}</strong> (үндсэн + хүү {formatExactMnt(preview.interest)})
            <span>Урьдчилсан тооцоо — эцсийн дүнг сервер тогтооно. Дуусах огноо: {preview.maturityDate}</span>
          </div>
        ) : null}

        {submitError ? <p className="field-error">{submitError}</p> : null}

        <div className="form-actions">
          <button type="submit" className="primary-button" disabled={!canSubmit}>
            {isSubmitting ? 'Нээж байна...' : 'Хадгаламж нээх'}
          </button>
        </div>
      </form>
    </>
  )
}
