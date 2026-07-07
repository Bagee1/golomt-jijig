import { PiggyBank, Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getDepositProducts, getMyDeposits } from '../api/depositApi'
import { useAuth } from '../auth/useAuth'
import { DepositStatusChip } from '../components/Chips'
import { PageHeader } from '../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import type { DepositProduct, DepositResponse } from '../types/deposit'
import { formatExactMnt } from '../utils/format'

export function DepositsPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [products, setProducts] = useState<DepositProduct[]>([])
  const [deposits, setDeposits] = useState<DepositResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      if (!auth.token) {
        return
      }
      setIsLoading(true)
      setError(null)
      try {
        const [loadedProducts, myDeposits] = await Promise.all([
          getDepositProducts(auth.token),
          getMyDeposits(auth.token, 0, 50),
        ])
        if (!cancelled) {
          setProducts(loadedProducts)
          setDeposits(myDeposits.content)
        }
      } catch (exception) {
        if (!cancelled) {
          setError(exception instanceof Error ? exception.message : 'Хадгаламжийн мэдээлэл уншихад алдаа гарлаа')
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
  }, [auth.token])

  return (
    <>
      <PageHeader
        title="Хадгаламж"
        subtitle="Хугацаатай хадгаламж нээх, хүү тооцох, хугацаанд нь буюу хугацаанаас өмнө хаах"
        action={
          <Link className="primary-button" to="/deposits/new">
            <Plus size={16} />
            Шинэ хадгаламж
          </Link>
        }
      />

      {isLoading ? <LoadingState label="Хадгаламжийн мэдээлэл уншиж байна..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {!isLoading && !error ? (
        <>
          <div className="product-grid">
            {products.map((product) => (
              <section key={product.termMonths} className="panel product-card">
                <div className="product-card-icon"><PiggyBank size={18} /></div>
                <div className="product-card-term">{product.termMonths} сар</div>
                <div className="product-card-rate">{product.annualRatePercent}% <span>жилийн хүү</span></div>
                <div className="product-card-range">
                  {formatExactMnt(product.minAmount)} – {formatExactMnt(product.maxAmount)}
                </div>
              </section>
            ))}
          </div>

          <section className="panel table-panel">
            <div className="table-count">Миний хадгаламжууд</div>
            {deposits.length === 0 ? (
              <EmptyState label="Хадгаламж алга. «Шинэ хадгаламж» дарж эхлүүлнэ үү." />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Дугаар</th>
                      <th>Үндсэн дүн</th>
                      <th>Хүү</th>
                      <th>Дуусах огноо</th>
                      <th>Төлөв</th>
                    </tr>
                  </thead>
                  <tbody>
                    {deposits.map((deposit) => (
                      <tr
                        key={deposit.id}
                        className="clickable-row"
                        onClick={() => navigate(`/deposits/${deposit.id}`)}
                      >
                        <td className="mono-cell">{deposit.depositNo}</td>
                        <td>{formatExactMnt(deposit.principal)}</td>
                        <td className="mono-cell">{deposit.annualRate}%</td>
                        <td className="mono-cell">
                          {deposit.maturityDate}
                          {deposit.status === 'OPEN' && deposit.matured
                            ? <span className="matured-hint"> · Хугацаа дууссан</span>
                            : null}
                        </td>
                        <td><DepositStatusChip status={deposit.status} compact /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : null}
    </>
  )
}
