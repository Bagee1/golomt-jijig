import { apiRequest } from './httpClient'
import type { PageResponse, SystemPayload, SystemResponse, SystemStatus, SystemType } from '../types/api'

export interface SystemSearchParams {
  keyword?: string
  type?: SystemType | ''
  status?: SystemStatus | ''
  page?: number
  size?: number
}

export function getSystems(token: string, params: SystemSearchParams = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 100))

  if (params.keyword) query.set('keyword', params.keyword)
  if (params.type) query.set('type', params.type)
  if (params.status) query.set('status', params.status)

  return apiRequest<PageResponse<SystemResponse>>(`/api/systems?${query.toString()}`, { token })
}

export function getSystem(token: string, id: number) {
  return apiRequest<SystemResponse>(`/api/systems/${id}`, { token })
}

export function createSystem(token: string, payload: SystemPayload) {
  return apiRequest<SystemResponse>('/api/systems', {
    token,
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateSystem(token: string, id: number, payload: SystemPayload) {
  return apiRequest<SystemResponse>(`/api/systems/${id}`, {
    token,
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function disableSystem(token: string, id: number) {
  return apiRequest<void>(`/api/systems/${id}`, { token, method: 'DELETE' })
}
