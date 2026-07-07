import { ArrowLeftRight, Landmark, LogOut, ScrollText, Search, Users, Wallet } from 'lucide-react'
import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

interface AppShellProps {
  children: ReactNode
}

const customerNavItems = [
  { label: 'Нүүр', href: '/', icon: Landmark },
  { label: 'Данс хайх', href: '/accounts', icon: Search },
  { label: 'Гүйлгээнүүд', href: '/transfers', icon: ArrowLeftRight },
]

const adminNavItems = [
  { label: 'Харилцагчид', href: '/admin/customers', icon: Users },
  { label: 'Данс удирдлага', href: '/admin/accounts', icon: Wallet },
  { label: 'Аудит лог', href: '/admin/audit-logs', icon: ScrollText },
]

export function AppShell({ children }: AppShellProps) {
  const auth = useAuth()
  const navigate = useNavigate()
  const isAdmin = auth.user?.role === 'ADMIN'
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
            <Landmark size={19} strokeWidth={2.3} />
          </div>
          <div>
            <div className="brand-title">Banking Transfer</div>
            <div className="brand-subtitle">DEMO BANK</div>
          </div>
        </div>

        <nav className="sidebar-nav" aria-label="Main navigation">
          {customerNavItems.map((item) => {
            const Icon = item.icon
            return (
              <NavLink key={item.href} to={item.href} end={item.href === '/'} className="nav-link">
                <Icon size={18} strokeWidth={2.1} />
                <span>{item.label}</span>
              </NavLink>
            )
          })}
          {isAdmin ? adminNavItems.map((item) => {
            const Icon = item.icon
            return (
              <NavLink key={item.href} to={item.href} className="nav-link">
                <Icon size={18} strokeWidth={2.1} />
                <span>{item.label}</span>
              </NavLink>
            )
          }) : null}
        </nav>

        <div className="sidebar-user">
          <div className="avatar">{initials}</div>
          <div className="user-copy">
            <div className="user-name">{auth.user?.displayName ?? 'User'}</div>
            <div className="user-role">{isAdmin ? 'ТЕЛЛЕР / ADMIN' : 'ХАРИЛЦАГЧ'}</div>
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
