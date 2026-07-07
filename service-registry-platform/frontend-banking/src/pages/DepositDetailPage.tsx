import { ArrowLeft } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { closeDeposit, getDeposit, retryDepositFunding } from '../api/depositApi'
import { useAuth } from '../auth/useAuth'
import { DepositStatusChip } from '../components/Chips'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { PageHeader } from '../components/PageHeader'
import { ErrorState, LoadingState } from '../components/States'
import type { DepositResponse } from '../types/deposit'
import { formatDateTime, formatExactMnt } from '../utils/format'

type PendingAction = 'close' | 'retry-funding' | null

export function DepositDetailPage() {
  const auth = useAuth()
  const { id } = useParams()
  const [deposit, setDeposit] = useState<DepositResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [pending, setPending] = useState<PendingAction>(null)
  const [isActing, setIsActing] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const reload = useCallback(() => setReloadKey((key) => key + 1), [])

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!auth.token || !id) {
        return
      }
      setIsLoading(true)
      setError(null)
      try {
        const result = await getDeposit(auth.token, id)
        if (!cancelled) {
          setDeposit(result)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Хадгаламж уншихад алдаа гарлаа')
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
  }, [auth.token, id, reloadKey])

  async function runAction() {
    if (!auth.token || !id || !pending) {
      return
    }
    setIsActing(true)
    setActionError(null)
    try {
      if (pending === 'close') {
        await closeDeposit(auth.token, id)
      } else {
        await retryDepositFunding(auth.token, id)
      }
      setPending(null)
      reload()
    } catch (exception) {
      setActionError(exception instanceof Error ? exception.message : 'Үйлдэл хийхэд алдаа гарлаа')
      setPending(null)
    } finally {
      setIsActing(false)
    }
  }

  const daysLeft = deposit
    ? Math.round((new Date(deposit.maturityDate).getTime() - Date.now()) / 86_400_000)
    : 0

  return (
    <>
      <PageHeader
        title={deposit ? deposit.depositNo : 'Хадгаламж'}
        subtitle="Хадгаламжийн дэлгэрэнгүй, хүү, эргэн төлөлт"
        action={
          <Link className="ghost-button" to="/deposits">
            <ArrowLeft size={16} />
            Жагсаалт руу буцах
          </Link>
        }
      />

      {isLoading ? <LoadingState label="Хадгаламж уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}
      {actionError ? <ErrorState message={actionError} /> : null}

      {deposit ? (
        <>
          <section className="panel deposit-summary">
            <div className="deposit-summary-amount">
              <strong>{formatExactMnt(deposit.principal)}</strong>
              {' '}
              <DepositStatusChip status={deposit.status} />
            </div>
            <div className="deposit-summary-meta">
              <span>{deposit.annualRate}% жилийн хүү · {deposit.termMonths} сар</span>
              <span>Дуусах: {deposit.maturityDate}
                {deposit.status === 'OPEN'
                  ? (deposit.matured ? ' · Хугацаа дууссан' : ` · ${daysLeft} хоног үлдсэн`)
                  : ''}
              </span>
            </div>
          </section>

          <section className="panel table-panel" style={{ padding: '18px 20px' }}>
            <div className="deposit-field-grid">
              <div><span>Холбосон данс</span><strong className="mono-cell">{deposit.linkedAccountNo}</strong></div>
              <div><span>Нээсэн огноо</span><strong>{formatDateTime(deposit.openedAt)}</strong></div>
              <div><span>Таамаг хүү (хугацаанд нь)</span><strong>{formatExactMnt(deposit.projectedInterest)}</strong></div>
              {deposit.interestAmount != null ? (
                <div><span>Олгосон хүү</span><strong>{formatExactMnt(deposit.interestAmount)}</strong></div>
              ) : null}
              {deposit.payoutAmount != null ? (
                <div><span>Эргэн төлөлт</span><strong>{formatExactMnt(deposit.payoutAmount)}</strong></div>
              ) : null}
              {deposit.fundingTransferRef ? (
                <div><span>Санхүүжилтийн гүйлгээ</span><strong className="mono-cell">{deposit.fundingTransferRef}</strong></div>
              ) : null}
              {deposit.payoutTransferRef ? (
                <div><span>Эргэн төлөлтийн гүйлгээ</span><strong className="mono-cell">{deposit.payoutTransferRef}</strong></div>
              ) : null}
              {deposit.failureReason ? (
                <div><span>Алдаа</span><strong className="field-error">{deposit.failureReason}</strong></div>
              ) : null}
            </div>

            <div className="form-actions" style={{ marginTop: 18 }}>
              {deposit.status === 'OPEN' ? (
                <button type="button" className="primary-button" onClick={() => setPending('close')}>
                  {deposit.matured ? 'Хадгаламж хаах' : 'Хугацаанаас өмнө хаах'}
                </button>
              ) : null}
              {deposit.status === 'FUNDING' ? (
                <button type="button" className="primary-button" onClick={() => setPending('retry-funding')}>
                  Санхүүжилтийг дахин оролдох
                </button>
              ) : null}
              {deposit.status === 'PAYOUT_PENDING' ? (
                <button type="button" className="primary-button" onClick={() => setPending('close')}>
                  Эргэн төлөлтийг дахин оролдох
                </button>
              ) : null}
            </div>
          </section>
        </>
      ) : null}

      {pending === 'close' && deposit ? (
        <ConfirmDialog
          title={deposit.matured ? 'Хадгаламж хаах' : 'Хугацаанаас өмнө хаах'}
          message={deposit.matured
            ? `${formatExactMnt(deposit.principal)} үндсэн дүн + ${formatExactMnt(deposit.projectedInterest)} хүү таны данс руу шилжинэ.`
            : `Анхаар: хугацаанаас өмнө хаавал хүү 0₮ — зөвхөн үндсэн дүн ${formatExactMnt(deposit.principal)} буцна.`}
          confirmLabel="Хаах"
          isBusy={isActing}
          onConfirm={() => void runAction()}
          onCancel={() => setPending(null)}
        />
      ) : null}

      {pending === 'retry-funding' && deposit ? (
        <ConfirmDialog
          title="Санхүүжилтийг дахин оролдох"
          message={`${formatExactMnt(deposit.principal)} дүнг ${deposit.linkedAccountNo} данснаас дахин татахыг оролдох уу?`}
          confirmLabel="Дахин оролдох"
          isBusy={isActing}
          onConfirm={() => void runAction()}
          onCancel={() => setPending(null)}
        />
      ) : null}
    </>
  )
}
