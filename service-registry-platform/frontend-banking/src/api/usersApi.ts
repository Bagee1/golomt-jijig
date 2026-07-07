import { apiRequest } from './httpClient'

export interface PlatformUserPayload {
  username: string
  password: string
  displayName: string
  role: 'VIEWER'
}

export interface PlatformUserResponse {
  id: number
  username: string
  displayName: string
  role: string
  enabled: boolean
}

export function createPlatformUser(token: string, payload: PlatformUserPayload) {
  return apiRequest<PlatformUserResponse>('/api/users', {
    token,
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
