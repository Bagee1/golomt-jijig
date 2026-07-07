import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuditLogPage } from './pages/AuditLogPage'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { SystemDetailPage } from './pages/SystemDetailPage'
import { SystemFormPage } from './pages/SystemFormPage'
import { SystemsPage } from './pages/SystemsPage'
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
              <PortalRoutes />
            </AppShell>
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}

function PortalRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/systems" element={<SystemsPage />} />
      <Route path="/systems/new" element={<SystemFormPage />} />
      <Route path="/systems/:id" element={<SystemDetailPage />} />
      <Route path="/systems/:id/edit" element={<SystemFormPage />} />
      <Route path="/audit-logs" element={<AuditLogPage />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
