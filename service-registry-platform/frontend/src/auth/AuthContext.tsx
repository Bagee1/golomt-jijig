import { useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as authApi from '../api/authApi'
import { UNAUTHORIZED_EVENT } from '../api/httpClient'
import type { AuthUser } from '../types/api'
import { AuthContext } from './authContextValue'
import type { AuthContextValue } from './authContextValue'

interface AuthProviderProps {
  children: ReactNode
}

const TOKEN_KEY = 'service-registry-token'
const USER_KEY = 'service-registry-user'
const EXPIRES_KEY = 'service-registry-token-expires'

export function AuthProvider({ children }: AuthProviderProps) {
  const [token, setToken] = useState<string | null>(() => readValidToken())
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser())
  const [isBootstrapping, setIsBootstrapping] = useState(Boolean(localStorage.getItem(TOKEN_KEY)))

  useEffect(() => {
    function handleUnauthorized() {
      clearSession()
      setToken(null)
      setUser(null)
    }

    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [])

  useEffect(() => {
    let cancelled = false

    async function bootstrapUser() {
      if (!token) {
        setIsBootstrapping(false)
        return
      }

      try {
        const currentUser = await authApi.getCurrentUser(token)
        if (!cancelled) {
          persistSession(token, currentUser)
          setUser(currentUser)
        }
      } catch {
        if (!cancelled) {
          clearSession()
          setToken(null)
          setUser(null)
        }
      } finally {
        if (!cancelled) {
          setIsBootstrapping(false)
        }
      }
    }

    void bootstrapUser()

    return () => {
      cancelled = true
    }
  }, [token])

  const value = useMemo<AuthContextValue>(() => ({
    token,
    user,
    isAuthenticated: Boolean(token && user),
    isBootstrapping,
    login: async (username: string, password: string) => {
      const response = await authApi.login(username, password)
      persistSession(response.accessToken, response.user)
      localStorage.setItem(EXPIRES_KEY, String(Date.now() + response.expiresInSeconds * 1000))
      setToken(response.accessToken)
      setUser(response.user)
    },
    logout: () => {
      clearSession()
      setToken(null)
      setUser(null)
    },
  }), [isBootstrapping, token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

function readValidToken(): string | null {
  const storedToken = localStorage.getItem(TOKEN_KEY)
  if (!storedToken) {
    return null
  }

  const expiresAt = Number(localStorage.getItem(EXPIRES_KEY))
  if (expiresAt && Date.now() >= expiresAt) {
    clearSession()
    return null
  }

  return storedToken
}

function readStoredUser() {
  const storedUser = localStorage.getItem(USER_KEY)
  if (!storedUser) {
    return null
  }

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

function persistSession(token: string, user: AuthUser) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(EXPIRES_KEY)
}
