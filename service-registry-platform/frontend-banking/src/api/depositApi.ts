import type { PageResponse } from '../types/api'
import type { DepositOpenPayload, DepositProduct, DepositResponse } from '../types/deposit'
import type { BankAuditLogResponse } from '../types/banking'
import { depositRequest } from './depositHttpClient'

export function getDepositProducts(token: string) {
  return depositRequest<DepositProduct[]>('/api/deposit-products', { token })
}

export function openDeposit(token: string, payload: DepositOpenPayload, idempotencyKey: string) {
  return depositRequest<DepositResponse>('/api/deposits', {
    method: 'POST',
    token,
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(payload),
  })
}

export function getMyDeposits(token: string, page: number, size: number) {
  return depositRequest<PageResponse<DepositResponse>>(`/api/deposits/my?page=${page}&size=${size}`, { token })
}

export function getDeposit(token: string, id: number | string) {
  return depositRequest<DepositResponse>(`/api/deposits/${id}`, { token })
}

export function closeDeposit(token: string, id: number | string) {
  return depositRequest<DepositResponse>(`/api/deposits/${id}/close`, { method: 'POST', token })
}

export function retryDepositFunding(token: string, id: number | string) {
  return depositRequest<DepositResponse>(`/api/deposits/${id}/retry-funding`, { method: 'POST', token })
}

export function listDeposits(token: string, params: { username?: string; page?: number; size?: number }) {
  const query = new URLSearchParams()
  if (params.username) query.set('username', params.username)
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 20))
  return depositRequest<PageResponse<DepositResponse>>(`/api/deposits?${query.toString()}`, { token })
}

export function listDepositAuditLogs(token: string, page: number, size: number) {
  return depositRequest<PageResponse<BankAuditLogResponse>>(`/api/audit-logs?page=${page}&size=${size}`, { token })
}
