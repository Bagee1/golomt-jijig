export type UserRole = 'ADMIN' | 'SECURITY' | 'VIEWER'

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface AuthUser {
  id: number
  username: string
  displayName: string
  role: UserRole
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  user: AuthUser
}
