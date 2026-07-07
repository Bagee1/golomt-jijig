import { Navigate, Route, Routes } from 'react-router-dom'
import { AdminRoute } from './components/AdminRoute'
import { AppShell } from './components/AppShell'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AccountLookupPage } from './pages/AccountLookupPage'
import { AccountsAdminPage } from './pages/AccountsAdminPage'
import { BankAuditLogPage } from './pages/BankAuditLogPage'
import { CustomerFormPage } from './pages/CustomerFormPage'
import { CustomersPage } from './pages/CustomersPage'
import { DepositsPage } from './pages/DepositsPage'
import { LoginPage } from './pages/LoginPage'
import { NewTransferPage } from './pages/NewTransferPage'
import { OverviewPage } from './pages/OverviewPage'
import { StatementPage } from './pages/StatementPage'
import { TransferDetailPage } from './pages/TransferDetailPage'
import { TransfersPage } from './pages/TransfersPage'
import './styles.css'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <AppShell>
              <BankingRoutes />
            </AppShell>
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}

function BankingRoutes() {
  return (
    <Routes>
      <Route path="/" element={<OverviewPage />} />
      <Route path="/accounts" element={<AccountLookupPage />} />
      <Route path="/accounts/:accountNo/statement" element={<StatementPage />} />
      <Route path="/transfers" element={<TransfersPage />} />
      <Route path="/transfers/new" element={<NewTransferPage />} />
      <Route path="/transfers/:id" element={<TransferDetailPage />} />
      <Route path="/deposits" element={<DepositsPage />} />
      <Route path="/admin/customers" element={<AdminRoute><CustomersPage /></AdminRoute>} />
      <Route path="/admin/customers/new" element={<AdminRoute><CustomerFormPage /></AdminRoute>} />
      <Route path="/admin/customers/:id" element={<AdminRoute><CustomerFormPage /></AdminRoute>} />
      <Route path="/admin/accounts" element={<AdminRoute><AccountsAdminPage /></AdminRoute>} />
      <Route path="/admin/audit-logs" element={<AdminRoute><BankAuditLogPage /></AdminRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
