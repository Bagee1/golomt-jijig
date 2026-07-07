# API Contract Draft

## Auth

```text
POST /api/auth/login
GET  /api/auth/me
```

### `POST /api/auth/login`

Request:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "displayName": "System Admin",
    "role": "ADMIN"
  }
}
```

### `GET /api/auth/me`

Header:

```text
Authorization: Bearer <jwt>
```

Response:

```json
{
  "id": 1,
  "username": "admin",
  "displayName": "System Admin",
  "role": "ADMIN"
}
```

## Users

```text
POST /api/users
```

`ADMIN` role шаардана. Banking app-ийн теллерийн "Шинэ харилцагч" form энэ endpoint-оор
нэвтрэх эрхийг зэрэг үүсгэдэг.

Request:

```json
{
  "username": "batbold",
  "password": "demo1234",
  "displayName": "Bat Bold",
  "role": "VIEWER"
}
```

- `username`: 3-80 тэмдэгт, зөвхөн `A-Za-z0-9._-`; хадгалагдахдаа lowercase болно.
- `password`: 8-72 тэмдэгт (BCrypt hash-лагдана).
- `role`: `ADMIN` / `SECURITY` / `VIEWER`.
- Давхардсан username → `400` `"Username already exists: ..."`.
- Амжилттай бол `201` + `{id, username, displayName, role, enabled}`, audit-д
  `USER_CREATED` action бичигдэнэ.

## Systems

```text
GET    /api/systems
POST   /api/systems
GET    /api/systems/{id}
PUT    /api/systems/{id}
DELETE /api/systems/{id}
```

All `/api/systems` endpoints require a Bearer token.

Mutating endpoints require `ADMIN` role:

```text
POST /api/systems
PUT /api/systems/{id}
DELETE /api/systems/{id}
```

### `GET /api/systems`

Query params:

```text
keyword
type        CARD | CORE | INTERNAL | DIGITAL
developer
inUse      true | false
status     ACTIVE | INACTIVE | UNKNOWN | DOWN
page
size
```

Response:

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "page": 0,
  "size": 20
}
```

### `POST /api/systems`

Request:

```json
{
  "systemKey": "banking-transfer-service",
  "name": "Banking Transfer Service",
  "type": "CORE",
  "valuationMnt": 15000000,
  "description": "Харилцах данс хооронд гүйлгээ хийх сервис.",
  "developerName": "Demo Developer",
  "developerTeam": "Core Banking Team",
  "startDate": "2026-07-06",
  "endDate": null,
  "inUse": true,
  "environment": "DEV",
  "baseUrl": "http://localhost:8084",
  "healthUrl": "http://localhost:8084/actuator/health",
  "swaggerUrl": "http://localhost:8084/swagger-ui/index.html",
  "repoUrl": "https://example.com/banking-transfer-service.git",
  "status": "ACTIVE",
  "relatedSystems": [
    {
      "targetSystemId": 2,
      "relationType": "CALLS",
      "description": "Calls card service"
    }
  ]
}
```

Response status:

```text
201 Created
```

### `DELETE /api/systems/{id}`

This endpoint does not hard-delete the row. It sets:

```text
inUse = false
status = INACTIVE
```

## Security Checks

```text
GET /api/security-controls
GET /api/systems/{id}/security-checks
PUT /api/systems/{id}/security-checks
GET /api/systems/{id}/security-score
```

All Security Checks endpoints require a Bearer token.

Mutating endpoint requires `ADMIN` or `SECURITY` role:

```text
PUT /api/systems/{id}/security-checks
```

Result enum:

```text
PASS | WARNING | FAIL | NOT_CHECKED
```

Score rule:

```text
PASS = full weight
WARNING = half weight
FAIL = 0
NOT_CHECKED = 0
score = round(earnedWeight / totalWeight * 100)
```

### `GET /api/security-controls`

Response:

```json
[
  {
    "id": 1,
    "controlKey": "HTTPS_ENABLED",
    "title": "HTTPS enabled",
    "description": "Service must expose production traffic over HTTPS.",
    "weight": 15,
    "required": true,
    "automated": false,
    "standardRef": "SEC-01"
  }
]
```

### `GET /api/systems/{id}/security-checks`

Response:

```json
[
  {
    "controlId": 1,
    "controlKey": "HTTPS_ENABLED",
    "title": "HTTPS enabled",
    "description": "Service must expose production traffic over HTTPS.",
    "weight": 15,
    "required": true,
    "automated": false,
    "standardRef": "SEC-01",
    "result": "NOT_CHECKED",
    "evidence": null,
    "checkedBy": null,
    "checkedAt": null
  }
]
```

### `PUT /api/systems/{id}/security-checks`

Request:

```json
{
  "checks": [
    {
      "controlId": 1,
      "result": "PASS",
      "evidence": "Uses HTTPS in production"
    },
    {
      "controlId": 2,
      "result": "WARNING",
      "evidence": "Auth enabled, MFA not included yet"
    }
  ]
}
```

Response:

```text
200 OK
```

Body is the same shape as `GET /api/systems/{id}/security-checks`.

Validation:

```text
Duplicate controlId -> 400 Bad Request
Unknown systemId -> 404 Not Found
Unknown controlId -> 404 Not Found
```

### `GET /api/systems/{id}/security-score`

Response:

```json
{
  "systemId": 1,
  "score": 23,
  "earnedWeight": 22.5,
  "totalWeight": 100,
  "passCount": 1,
  "failCount": 1,
  "warningCount": 1,
  "notCheckedCount": 5
}
```

## Audit

```text
GET /api/audit-logs
```

All Audit endpoints require a Bearer token.

`GET /api/audit-logs` requires `ADMIN` or `SECURITY` role.

Query params:

```text
page
size
```

Response:

```json
{
  "content": [
    {
      "id": 1,
      "action": "SYSTEM_CREATED",
      "targetType": "SYSTEM",
      "targetId": 1,
      "message": "System created: Banking Transfer Service",
      "metadataJson": "{\"systemKey\":\"banking-transfer-service\"}",
      "actorUserId": 1,
      "actorUsername": "admin",
      "actorDisplayName": "System Admin",
      "createdAt": "2026-07-06T19:09:23.948+08:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

Current action values:

```text
LOGIN_SUCCESS
LOGIN_FAILURE
SYSTEM_CREATED
SYSTEM_UPDATED
SYSTEM_DISABLED
SECURITY_CHECK_UPDATED
```
