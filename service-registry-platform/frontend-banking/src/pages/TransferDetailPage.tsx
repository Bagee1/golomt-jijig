import { ArrowLeft, MoveRight, Undo2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getTransfer, reverseTransfer } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { TransferStatusChip } from '../components/Chips'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { LedgerEntriesTable } from '../components/LedgerEntriesTable'
import { PageHeader } from '../components/PageHeader'
import { ErrorState, LoadingState } from '../components/States'
import type { TransferResponse } from '../types/banking'
import { bankErrorMessage } from '../utils/bankingErrors'
import { formatDateTime, formatExactMnt } from '../utils/format'

export function TransferDetailPage() {
  const auth = useAuth()
  const { id } = useParams()
  const isAdmin = auth.user?.role === 'ADMIN'
  const [transfer, setTransfer] = useState<TransferResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showReverseDialog, setShowReverseDialog] = useState(false)
  const [isReversing, setIsReversing] = useState(false)
  const [reverseError, setReverseError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = useCallback(() => setReloadKey((key) => key + 1), [])

  useEffect(() => {
    let cancelled = false

    async function loadTransfer() {
      if (!auth.token || !id) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const result = await getTransfer(auth.token, id)
        if (!cancelled) {
          setTransfer(result)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Гүйлгээ уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadTransfer()

    return () => {
      cancelled = true
    }
  }, [auth.token, id, reloadKey])

  async function onReverse() {
    if (!auth.token || !transfer) {
      return
    }

    setIsReversing(true)
    setReverseError(null)

    try {
      await reverseTransfer(auth.token, transfer.id)
      setShowReverseDialog(false)
      reload()
    } catch (exception) {
      setReverseError(exception instanceof Error ? exception.message : 'Гүйлгээ буцаахад алдаа гарлаа')
    } finally {
      setIsReversing(false)
    }
  }

  const canReverse = isAdmin && transfer?.status === 'SUCCESS' && !transfer.reversalOfTransferId

  return (
    <>
      <PageHeader
        title={transfer ? transfer.transferRef : 'Гүйлгээний дэлгэрэнгүй'}
        subtitle="Гүйлгээ ба давхар бичилтийн ledger"
        action={
          <Link className="ghost-button" to="/transfers">
            <ArrowLeft size={16} />
            Жагсаалт руу буцах
          </Link>
        }
      />

      {isLoading ? <LoadingState label="Гүйлгээ уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {transfer ? (
        <>
          <section className="panel transfer-summary">
            <div className="transfer-summary-amount">
              <strong>{formatExactMnt(transfer.amount, transfer.currency)}</strong>
              <TransferStatusChip status={transfer.status} />
              {canReverse ? (
                <button
                  type="button"
                  className="danger-button"
                  onClick={() => {
                    setReverseError(null)
                    setShowReverseDialog(true)
                  }}
                >
                  <Undo2 size={15} />
                  Гүйлгээ буцаах
                </button>
              ) : null}
            </div>
            <div className="transfer-summary-meta">
              <div className="mono-cell transfer-route">
                {transfer.fromAccountNo}
                <MoveRight size={16} aria-hidden="true" />
                {transfer.toAccountNo}
              </div>
              <span>{transfer.description ?? 'Тайлбаргүй'}</span>
              <span>{formatDateTime(transfer.createdAt)}</span>
              {transfer.failureReason ? (
                <span className="field-error">
                  Шалтгаан: {bankErrorMessage(transfer.failureReason, transfer.failureReason)}
                </span>
              ) : null}
              {transfer.reversalOfTransferId ? (
                <span>
                  Энэ нь буцаалтын гүйлгээ:{' '}
                  <Link to={`/transfers/${transfer.reversalOfTransferId}`}>анхны гүйлгээг харах</Link>
                </span>
              ) : null}
              {transfer.reversedByTransferId ? (
                <span>
                  Буцаагдсан:{' '}
                  <Link to={`/transfers/${transfer.reversedByTransferId}`}>буцаалтын гүйлгээг харах</Link>
                </span>
              ) : null}
            </div>
            {reverseError ? <p className="field-error">{reverseError}</p> : null}
          </section>

          <section className="panel table-panel">
            <div className="table-count">Ledger бичилтүүд (давхар бичилт)</div>
            <LedgerEntriesTable entries={transfer.ledgerEntries} />
          </section>

          {showReverseDialog ? (
            <ConfirmDialog
              title="Гүйлгээ буцаах"
              message={`${transfer.transferRef} гүйлгээний ${formatExactMnt(transfer.amount, transfer.currency)}-г ${transfer.toAccountNo} данснаас ${transfer.fromAccountNo} данс руу буцаана. Итгэлтэй байна уу?`}
              confirmLabel="Буцаах"
              isBusy={isReversing}
              onConfirm={() => void onReverse()}
              onCancel={() => setShowReverseDialog(false)}
            />
          ) : null}
        </>
      ) : null}
    </>
  )
}
