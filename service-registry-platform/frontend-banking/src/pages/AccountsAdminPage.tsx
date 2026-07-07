import { Plus, Search } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { openAccount, blockAccount, closeAccount, listAccounts, unblockAccount } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { AccountStatusChip } from '../components/Chips'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { PageHeader } from '../components/PageHeader'
import { PaginationControls } from '../components/Pagination'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { AccountResponse } from '../types/banking'
import { formatExactMnt } from '../utils/format'

const PAGE_SIZE = 20

type PendingAction = { kind: 'block' | 'unblock' | 'close'; account: AccountResponse } | null

export function AccountsAdminPage() {
  const auth = useAuth()
  const [searchParams] = useSearchParams()
  // "Данс нээх" товчоор харилцагчийн хуудаснаас ирэхэд дугаар нь бөглөгдсөн байна
  const prefillCustomerNo = searchParams.get('customerNo')?.trim() ?? ''
  const [filterInput, setFilterInput] = useState(prefillCustomerNo)
  const [customerNoFilter, setCustomerNoFilter] = useState(prefillCustomerNo)
  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const [showOpenForm, setShowOpenForm] = useState(Boolean(prefillCustomerNo))
  const [newCustomerNo, setNewCustomerNo] = useState(prefillCustomerNo)
  const [newInitialBalance, setNewInitialBalance] = useState('')
  const [isOpening, setIsOpening] = useState(false)
  const [openError, setOpenError] = useState<string | null>(null)

  const [pendingAction, setPendingAction] = useState<PendingAction>(null)
  const [isActing, setIsActing] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const reload = useCallback(() => setReloadKey((key) => key + 1), [])

  useEffect(() => {
    let cancelled = false

    async function loadAccounts() {
      if (!auth.token) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const result = await listAccounts(auth.token, {
          customerNo: customerNoFilter || undefined,
          page,
          size: PAGE_SIZE,
        })
        if (!cancelled) {
          setAccounts(result.content)
          setTotalPages(result.totalPages)
          setTotalElements(result.totalElements)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Данс уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadAccounts()

    return () => {
      cancelled = true
    }
  }, [auth.token, customerNoFilter, page, reloadKey])

  function onFilter(event: FormEvent) {
    event.preventDefault()
    setPage(0)
    setCustomerNoFilter(filterInput.trim())
  }

  async function onOpenAccount(event: FormEvent) {
    event.preventDefault()
    if (!auth.token || !newCustomerNo.trim()) {
      return
    }

    setIsOpening(true)
    setOpenError(null)

    try {
      await openAccount(auth.token, {
        customerNo: newCustomerNo.trim(),
        initialBalance: newInitialBalance.trim() === '' ? null : Number(newInitialBalance),
      })
      setShowOpenForm(false)
      setNewCustomerNo('')
      setNewInitialBalance('')
      reload()
    } catch (exception) {
      setOpenError(exception instanceof Error ? exception.message : 'Данс нээхэд алдаа гарлаа')
    } finally {
      setIsOpening(false)
    }
  }

  async function onConfirmAction() {
    if (!auth.token || !pendingAction) {
      return
    }

    setIsActing(true)
    setActionError(null)

    const { kind, account } = pendingAction
    try {
      if (kind === 'block') {
        await blockAccount(auth.token, account.accountNo)
      } else if (kind === 'unblock') {
        await unblockAccount(auth.token, account.accountNo)
      } else {
        await closeAccount(auth.token, account.accountNo)
      }
      setPendingAction(null)
      reload()
    } catch (exception) {
      setActionError(exception instanceof Error ? exception.message : 'Үйлдэл хийхэд алдаа гарлаа')
      setPendingAction(null)
    } finally {
      setIsActing(false)
    }
  }

  const actionLabels: Record<'block' | 'unblock' | 'close', { title: string; confirm: string }> = {
    block: { title: 'Данс блоклох', confirm: 'Блоклох' },
    unblock: { title: 'Данс идэвхжүүлэх', confirm: 'Идэвхжүүлэх' },
    close: { title: 'Данс хаах', confirm: 'Хаах' },
  }

  return (
    <>
      <PageHeader
        title="Данс удирдлага"
        subtitle="Данс нээх, блоклох, хаах — теллерийн үйлдлүүд"
        action={
          <button type="button" className="primary-button" onClick={() => setShowOpenForm((value) => !value)}>
            <Plus size={16} />
            Данс нээх
          </button>
        }
      />

      {showOpenForm ? (
        <form className="panel transfer-form" onSubmit={onOpenAccount}>
          <label className="form-field">
            <span>Харилцагчийн дугаар</span>
            <input
              placeholder="CUST-0001"
              value={newCustomerNo}
              required
              onChange={(event) => setNewCustomerNo(event.target.value)}
            />
          </label>
          <label className="form-field">
            <span>Эхний үлдэгдэл (заавал биш, MNT)</span>
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={newInitialBalance}
              onChange={(event) => setNewInitialBalance(event.target.value)}
            />
          </label>
          {openError ? <p className="field-error">{openError}</p> : null}
          <div className="form-actions">
            <button type="submit" className="primary-button" disabled={isOpening || !newCustomerNo.trim()}>
              {isOpening ? 'Нээж байна...' : 'Данс нээх'}
            </button>
            <button type="button" className="ghost-button" onClick={() => setShowOpenForm(false)}>
              Болих
            </button>
          </div>
        </form>
      ) : null}

      <form className="toolbar" onSubmit={onFilter}>
        <label className="search-box">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Харилцагчийн дугаараар шүүх (CUST-0001)..."
            aria-label="Харилцагчийн дугаар"
            value={filterInput}
            onChange={(event) => setFilterInput(event.target.value)}
          />
        </label>
        <button type="submit" className="primary-button">Шүүх</button>
      </form>

      {isLoading ? <LoadingState label="Данс уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}
      {actionError ? <ErrorState message={actionError} /> : null}

      {!isLoading && !error ? (
        <section className="panel table-panel">
          <div className="table-count">{totalElements} данс</div>
          {accounts.length === 0 ? (
            <EmptyState label="Данс олдсонгүй." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Дансны дугаар</th>
                    <th>Эзэмшигч</th>
                    <th>Үлдэгдэл</th>
                    <th>Төлөв</th>
                    <th>Үйлдэл</th>
                  </tr>
                </thead>
                <tbody>
                  {accounts.map((account) => (
                    <tr key={account.id}>
                      <td className="mono-cell">{account.accountNo}</td>
                      <td>{account.customerName} · {account.customerNo}</td>
                      <td>{formatExactMnt(account.balance, account.currency)}</td>
                      <td><AccountStatusChip status={account.status} compact /></td>
                      <td>
                        <div className="form-actions table-row-actions">
                          {account.status === 'ACTIVE' ? (
                            <button
                              type="button"
                              className="ghost-button"
                              onClick={() => setPendingAction({ kind: 'block', account })}
                            >
                              Блоклох
                            </button>
                          ) : null}
                          {account.status === 'BLOCKED' ? (
                            <button
                              type="button"
                              className="ghost-button"
                              onClick={() => setPendingAction({ kind: 'unblock', account })}
                            >
                              Идэвхжүүлэх
                            </button>
                          ) : null}
                          {account.status !== 'CLOSED' ? (
                            <button
                              type="button"
                              className="danger-button"
                              onClick={() => setPendingAction({ kind: 'close', account })}
                            >
                              Хаах
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <PaginationControls page={page} totalPages={totalPages} onPageChange={setPage} />
        </section>
      ) : null}

      {pendingAction ? (
        <ConfirmDialog
          title={actionLabels[pendingAction.kind].title}
          message={`${pendingAction.account.accountNo} (${pendingAction.account.customerName}) данс дээр энэ үйлдлийг хийх үү?${pendingAction.kind === 'close' ? ' Хаахын тулд үлдэгдэл 0 байх ёстой.' : ''}`}
          confirmLabel={actionLabels[pendingAction.kind].confirm}
          isBusy={isActing}
          onConfirm={() => void onConfirmAction()}
          onCancel={() => setPendingAction(null)}
        />
      ) : null}
    </>
  )
}
