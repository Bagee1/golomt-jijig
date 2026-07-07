import type { PageResponse } from '../types/api'
import type {
  AccountCreatePayload,
  AccountResponse,
  BankAuditLogResponse,
  CustomerPayload,
  CustomerResponse,
  StatementResponse,
  TransferRequest,
  TransferResponse,
} from '../types/banking'
import { bankingRequest } from './bankingHttpClient'

// --- Accounts ---

export function getAccount(token: string, accountNo: string) {
  return bankingRequest<AccountResponse>(`/api/accounts/${encodeURIComponent(accountNo)}`, { token })
}

export function getMyAccounts(token: string) {
  return bankingRequest<AccountResponse[]>('/api/accounts/my', { token })
}

export function getStatement(
  token: string,
  accountNo: string,
  params: { from?: string; to?: string; page?: number; size?: number },
) {
  const query = new URLSearchParams()
  if (params.from) query.set('from', params.from)
  if (params.to) query.set('to', params.to)
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 20))
  return bankingRequest<StatementResponse>(
    `/api/accounts/${encodeURIComponent(accountNo)}/statement?${query.toString()}`,
    { token },
  )
}

export function listAccounts(token: string, params: { customerNo?: string; page?: number; size?: number }) {
  const query = new URLSearchParams()
  if (params.customerNo) query.set('customerNo', params.customerNo)
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 20))
  return bankingRequest<PageResponse<AccountResponse>>(`/api/accounts?${query.toString()}`, { token })
}

export function openAccount(token: string, payload: AccountCreatePayload) {
  return bankingRequest<AccountResponse>('/api/accounts', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function blockAccount(token: string, accountNo: string) {
  return bankingRequest<AccountResponse>(`/api/accounts/${encodeURIComponent(accountNo)}/block`, {
    method: 'POST',
    token,
  })
}

export function unblockAccount(token: string, accountNo: string) {
  return bankingRequest<AccountResponse>(`/api/accounts/${encodeURIComponent(accountNo)}/unblock`, {
    method: 'POST',
    token,
  })
}

export function closeAccount(token: string, accountNo: string) {
  return bankingRequest<AccountResponse>(`/api/accounts/${encodeURIComponent(accountNo)}/close`, {
    method: 'POST',
    token,
  })
}

// --- Transfers ---

export function createTransfer(token: string, request: TransferRequest, idempotencyKey: string) {
  return bankingRequest<TransferResponse>('/api/transfers', {
    method: 'POST',
    token,
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(request),
  })
}

export function listTransfers(token: string, page: number, size: number) {
  return bankingRequest<PageResponse<TransferResponse>>(`/api/transfers?page=${page}&size=${size}`, { token })
}

export function getTransfer(token: string, id: number | string) {
  return bankingRequest<TransferResponse>(`/api/transfers/${id}`, { token })
}

export function reverseTransfer(token: string, id: number | string) {
  return bankingRequest<TransferResponse>(`/api/transfers/${id}/reversal`, { method: 'POST', token })
}

// --- Customers (admin) ---

export function listCustomers(token: string, params: { q?: string; page?: number; size?: number }) {
  const query = new URLSearchParams()
  if (params.q) query.set('q', params.q)
  query.set('page', String(params.page ?? 0))
  query.set('size', String(params.size ?? 20))
  return bankingRequest<PageResponse<CustomerResponse>>(`/api/customers?${query.toString()}`, { token })
}

export function getCustomer(token: string, id: number | string) {
  return bankingRequest<CustomerResponse>(`/api/customers/${id}`, { token })
}

export function createCustomer(token: string, payload: CustomerPayload) {
  return bankingRequest<CustomerResponse>('/api/customers', {
    method: 'POST',
    token,
    body: JSON.stringify(payload),
  })
}

export function updateCustomer(token: string, id: number | string, payload: CustomerPayload) {
  return bankingRequest<CustomerResponse>(`/api/customers/${id}`, {
    method: 'PUT',
    token,
    body: JSON.stringify(payload),
  })
}

export function deactivateCustomer(token: string, id: number | string) {
  return bankingRequest<CustomerResponse>(`/api/customers/${id}/deactivate`, { method: 'POST', token })
}

// --- Audit (admin) ---

export function listBankAuditLogs(token: string, page: number, size: number) {
  return bankingRequest<PageResponse<BankAuditLogResponse>>(`/api/audit-logs?page=${page}&size=${size}`, { token })
}
