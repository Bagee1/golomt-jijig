import { apiRequest } from './httpClient'
import type { AuditLogResponse, PageResponse } from '../types/api'

export function getAuditLogs(token: string, page = 0, size = 20) {
  return apiRequest<PageResponse<AuditLogResponse>>(`/api/audit-logs?page=${page}&size=${size}`, { token })
}
