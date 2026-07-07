import { Plus } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyAccounts, listTransfers } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { AccountBalanceCard } from '../components/AccountBalanceCard'
import { PageHeader } from '../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { TransfersTable } from '../components/TransfersTable'
import type { AccountResponse, TransferResponse } from '../types/banking'

export function OverviewPage() {
  const auth = useAuth()
  const isAdmin = auth.user?.role === 'ADMIN'
  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [transfers, setTransfers] = useState<TransferResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = useCallback(() => setReloadKey((key) => key + 1), [])

  useEffect(() => {
    let cancelled = false

    async function loadOverview() {
      if (!auth.token) {
        return
      }

      setIsLoading(true)
      setError(null)

      try {
        const [loadedAccounts, transferPage] = await Promise.all([
          getMyAccounts(auth.token),
          listTransfers(auth.token, 0, 5),
        ])
        if (!cancelled) {
          setAccounts(loadedAccounts)
          setTransfers(transferPage.content)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Banking мэдээлэл уншихад алдаа гарлаа')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadOverview()

    return () => {
      cancelled = true
    }
  }, [auth.token, reloadKey])

  return (
    <>
      <PageHeader
        title={isAdmin ? 'Теллерийн нүүр' : 'Миний данснууд'}
        subtitle={isAdmin
          ? 'Бүх гүйлгээ, данс, харилцагчийн удирдлага'
          : 'Таны данснууд болон сүүлийн гүйлгээнүүд'}
        action={
          <Link className="primary-button" to="/transfers/new">
            <Plus size={16} />
            Шинэ шилжүүлэг
          </Link>
        }
      />

      {isLoading ? <LoadingState label="Banking мэдээлэл уншиж байна..." /> : null}
      {error ? <ErrorState message={error} onRetry={reload} /> : null}

      {!isLoading && !error ? (
        <>
          {accounts.length === 0 ? (
            <EmptyState label={isAdmin
              ? 'Танд холбогдсон данс алга. Данс удирдлага хэсгээс бүх дансыг харна уу.'
              : 'Танд холбогдсон данс алга. Банкны теллерт хандана уу.'} />
          ) : (
            <div className="account-card-grid">
              {accounts.map((account) => <AccountBalanceCard key={account.id} account={account} />)}
            </div>
          )}

          <section className="panel table-panel">
            <div className="table-count">Сүүлийн гүйлгээнүүд</div>
            {transfers.length === 0
              ? <EmptyState label="Гүйлгээ алга. «Шинэ шилжүүлэг» дарж эхлүүлнэ үү." />
              : <TransfersTable transfers={transfers} />}
          </section>
        </>
      ) : null}
    </>
  )
}
