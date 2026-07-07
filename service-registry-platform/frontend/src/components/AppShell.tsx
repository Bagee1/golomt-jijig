import { Database, LayoutDashboard, LogOut, ScrollText, Server } from 'lucide-react'
import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

interface AppShellProps {
  children: ReactNode
}

const navItems = [
  { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { label: 'Системүүд', href: '/systems', icon: Server },
  { label: 'Аудит лог', href: '/audit-logs', icon: ScrollText },
]

export function AppShell({ children }: AppShellProps) {
  const auth = useAuth()
  const navigate = useNavigate()
  const initials = auth.user?.displayName
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase() ?? 'AA'

  function handleLogout() {
    auth.logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon" aria-hidden="true">
            <Database size={19} strokeWidth={2.3} />
          </div>
          <div>
            <div className="brand-title">System Registry</div>
            <div className="brand-subtitle">ADMIN PORTAL</div>
          </div>
        </div>

        <nav className="sidebar-nav" aria-label="Main navigation">
          {navItems.map((item) => {
            const Icon = item.icon
            return (
              <NavLink key={item.href} to={item.href} className="nav-link">
                <Icon size={18} strokeWidth={2.1} />
                <span>{item.label}</span>
              </NavLink>
            )
          })}
        </nav>

        <div className="sidebar-user">
          <div className="avatar">{initials}</div>
          <div className="user-copy">
            <div className="user-name">{auth.user?.displayName ?? 'User'}</div>
            <div className="user-role">{auth.user?.role ?? '-'}</div>
          </div>
          <button className="icon-button sidebar-logout" type="button" aria-label="Logout" onClick={handleLogout}>
            <LogOut size={17} />
          </button>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <div aria-hidden="true" />
        </header>
        <main className="page-area">{children}</main>
      </div>
    </div>
  )
}
