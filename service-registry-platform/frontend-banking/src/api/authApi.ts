import { apiRequest } from './httpClient'
import type { AuthUser, LoginResponse } from '../types/api'

export function login(username: string, password: string) {
  return apiRequest<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getCurrentUser(token: string) {
  return apiRequest<AuthUser>('/api/auth/me', { token })
}
