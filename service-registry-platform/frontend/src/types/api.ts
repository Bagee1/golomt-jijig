export type UserRole = 'ADMIN' | 'SECURITY' | 'VIEWER'
export type SystemType = 'CARD' | 'CORE' | 'INTERNAL' | 'DIGITAL'
export type SystemStatus = 'ACTIVE' | 'INACTIVE' | 'UNKNOWN' | 'DOWN'
export type SystemEnvironment = 'DEV' | 'TEST' | 'UAT' | 'PROD'
export type RelationType = 'DEPENDS_ON' | 'CALLS' | 'INTEGRATES_WITH'
export type SecurityCheckResultStatus = 'PASS' | 'WARNING' | 'FAIL' | 'NOT_CHECKED'
export type AuditAction =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILURE'
  | 'SYSTEM_CREATED'
  | 'SYSTEM_UPDATED'
  | 'SYSTEM_DISABLED'
  | 'SECURITY_CHECK_UPDATED'

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

export interface SystemRelationResponse {
  id: number
  targetSystemId: number
  targetSystemKey: string
  targetSystemName: string
  relationType: string
  description: string | null
}

export interface SystemResponse {
  id: number
  systemKey: string
  name: string
  type: SystemType
  valuationMnt: number
  description: string | null
  developerName: string | null
  developerTeam: string | null
  startDate: string | null
  endDate: string | null
  inUse: boolean
  environment: SystemEnvironment
  baseUrl: string | null
  healthUrl: string | null
  swaggerUrl: string | null
  repoUrl: string | null
  status: SystemStatus
  createdBy: number | null
  createdAt: string
  updatedAt: string
  relatedSystems: SystemRelationResponse[]
}

export interface SystemRelationPayload {
  targetSystemId: number
  relationType: RelationType
  description?: string | null
}

export interface SystemPayload {
  systemKey?: string | null
  name: string
  type: SystemType
  valuationMnt: number
  description: string | null
  developerName: string | null
  developerTeam: string | null
  startDate: string | null
  endDate: string | null
  inUse: boolean
  environment: SystemEnvironment
  baseUrl: string | null
  healthUrl: string | null
  swaggerUrl: string | null
  repoUrl: string | null
  status: SystemStatus
  relatedSystems: SystemRelationPayload[]
}

export interface SecurityCheckResultResponse {
  controlId: number
  controlKey: string
  title: string
  description: string
  weight: number
  required: boolean
  automated: boolean
  standardRef: string | null
  result: SecurityCheckResultStatus
  evidence: string | null
  checkedBy: number | null
  checkedAt: string | null
}

export interface SecurityCheckItemPayload {
  controlId: number
  result: SecurityCheckResultStatus
  evidence: string | null
}

export interface SecurityScoreResponse {
  systemId: number
  score: number
  earnedWeight: number
  totalWeight: number
  passCount: number
  failCount: number
  warningCount: number
  notCheckedCount: number
}

export interface AuditLogResponse {
  id: number
  action: AuditAction
  targetType: string
  targetId: number | null
  message: string
  metadataJson: string | null
  actorUserId: number | null
  actorUsername: string | null
  actorDisplayName: string | null
  createdAt: string
}
