import { apiRequest } from './httpClient'
import type { SecurityCheckItemPayload, SecurityCheckResultResponse, SecurityScoreResponse } from '../types/api'

export function getSecurityScore(token: string, systemId: number) {
  return apiRequest<SecurityScoreResponse>(`/api/systems/${systemId}/security-score`, { token })
}

export function getSecurityScores(token: string) {
  return apiRequest<SecurityScoreResponse[]>('/api/security-scores', { token })
}

export function getSecurityChecks(token: string, systemId: number) {
  return apiRequest<SecurityCheckResultResponse[]>(`/api/systems/${systemId}/security-checks`, { token })
}

export function updateSecurityChecks(token: string, systemId: number, checks: SecurityCheckItemPayload[]) {
  return apiRequest<SecurityCheckResultResponse[]>(`/api/systems/${systemId}/security-checks`, {
    token,
    method: 'PUT',
    body: JSON.stringify({ checks }),
  })
}
