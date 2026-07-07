import { Search } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { getAccount } from '../api/bankingApi'
import { useAuth } from '../auth/useAuth'
import { AccountBalanceCard } from '../components/AccountBalanceCard'
import { PageHeader } from '../components/PageHeader'
import { ErrorState, LoadingState } from '../components/States'
import type { AccountResponse } from '../types/banking'

export function AccountLookupPage() {
  const auth = useAuth()
  const [accountNo, setAccountNo] = useState('')
  const [account, setAccount] = useState<AccountResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function lookup(target: string) {
    if (!auth.token || !target.trim()) {
      return
    }

    setIsLoading(true)
    setError(null)
    setAccount(null)

    try {
      setAccount(await getAccount(auth.token, target.trim()))
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Данс хайхад алдаа гарлаа')
    } finally {
      setIsLoading(false)
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    void lookup(accountNo)
  }

  return (
    <>
      <PageHeader
        title="Данс хайх"
        subtitle="Дансны дугаараар үлдэгдэл, эзэмшигчийг харах (өөрийн данс; теллер бүх дансыг харна)"
      />

      <form className="toolbar" onSubmit={onSubmit}>
        <label className="search-box">
          <Search size={18} aria-hidden="true" />
          <input
            type="search"
            placeholder="Дансны дугаар..."
            aria-label="Дансны дугаар"
            value={accountNo}
            onChange={(event) => setAccountNo(event.target.value)}
          />
        </label>
        <button type="submit" className="primary-button" disabled={!accountNo.trim() || isLoading}>
          Хайх
        </button>
      </form>

      {isLoading ? <LoadingState label="Данс хайж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}
      {account ? (
        <div className="account-card-grid">
          <AccountBalanceCard account={account} />
        </div>
      ) : null}
    </>
  )
}
