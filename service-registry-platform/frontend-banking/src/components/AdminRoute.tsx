import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

interface AdminRouteProps {
  children: ReactNode
}

export function AdminRoute({ children }: AdminRouteProps) {
  const auth = useAuth()

  if (auth.user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return children
}
