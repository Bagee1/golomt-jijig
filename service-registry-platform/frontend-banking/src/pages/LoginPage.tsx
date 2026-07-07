import { Eye, EyeOff, Landmark, LogIn } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

interface LoginLocationState {
  from?: {
    pathname?: string
  }
}

export function LoginPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const state = location.state as LoginLocationState | null
  const redirectTo = state?.from?.pathname ?? '/'

  if (auth.isAuthenticated) {
    return <Navigate to={redirectTo} replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await auth.login(username, password)
      navigate(redirectTo, { replace: true })
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Нэвтрэх үед алдаа гарлаа')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-hero">
        <div className="login-brand-icon">
          <Landmark size={28} />
        </div>
        <h1>Banking Transfer</h1>
        <p>Данс хоорондын шилжүүлэг, дансны хуулга, давхар бичилттэй ledger — нэг дор.</p>
      </section>

      <section className="login-panel" aria-labelledby="login-title">
        <div>
          <h2 id="login-title">Нэвтрэх</h2>
          <p>Харилцагч эсвэл теллерийн эрхээр нэвтэрнэ.</p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            <span>Username</span>
            <input
              value={username}
              name="username"
              required
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
            />
          </label>

          <label>
            <span>Password</span>
            <div className="password-input">
              <input
                value={password}
                name="password"
                required
                type={showPassword ? 'text' : 'password'}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
              />
              <button type="button" onClick={() => setShowPassword((value) => !value)} aria-label="Toggle password">
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>
          </label>

          {error ? <div className="login-error">{error}</div> : null}

          <button className="login-submit" type="submit" disabled={isSubmitting || !username || !password}>
            <LogIn size={17} />
            {isSubmitting ? 'Нэвтэрч байна...' : 'Нэвтрэх'}
          </button>
        </form>
      </section>
    </main>
  )
}
