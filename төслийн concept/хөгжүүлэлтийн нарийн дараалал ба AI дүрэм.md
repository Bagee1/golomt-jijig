# Хөгжүүлэлтийн нарийн дараалал ба AI ашиглах дүрэм

Огноо: 2026-07-06

Хамаарах баримтууд:

- `төслийн concept/гол агуулга`
- `төслийн concept/хөгжүүлэлт эхлэх судалгаа.md`
- `төслийн concept/даалгаврын нийцлийн үнэлгээ.md`
- `анхны даалгавар/Даалгавар5.14_1.pdf`

## 1. Хамгийн чухал чиглэл

Энэ төслийг хийхдээ хоёр давхар зорилготой явна.

1. **PDF даалгаврыг 100% биелүүлэх**

   - Login.
   - Системийн бүртгэл.
   - PDF дээрх бүх талбар.
   - Мэдээллийн аюулгүй байдлын стандарт шалгах.
   - Жагсаалт болон хайлт.
2. **Enterprise болгож өргөтгөх**

   - Service Catalog.
   - Health check.
   - Swagger/OpenAPI link.
   - Audit log.
   - Banking Transfer Service demo.
   - Дараагийн шатанд Gateway/Eureka.

Хөгжүүлэлтийн үндсэн дүрэм:

> Эхлээд даалгаврын гол шаардлагыг ажилладаг болгоно. Дараа нь enterprise нэмэлтүүдийг давхарлаж нэмнэ.

## 2. Эхний барих ёстой MVP

Эхний хамгаалалт эсвэл demo-д заавал ажиллаж байх ёстой хамгийн бага хувилбар:

```text
React frontend
Spring Boot platform-api
PostgreSQL

Login
System CRUD
System list/search/filter
Security checklist
Audit log
```

Энэ MVP дотор PDF-ийн бүх талбар орсон байна:

| PDF талбар                           | Implementation талбар             |
| ------------------------------------------ | --------------------------------------- |
| Системийн нэр                  | `name`                                |
| Төрөл                                 | `type`: CARD, CORE, INTERNAL, DIGITAL |
| Үнэлгээ/төг                      | `valuationMnt`                        |
| Тайлбар                             | `description`                         |
| Холбоотой системүүд      | `serviceRelations`                    |
| Хөгжүүлэгч                       | `developerName`, `developerTeam`    |
| Хугацаа                             | `startDate`, `endDate`              |
| Ашиглагдаж байгаа эсэх | `inUse`                               |

Эхний MVP-д заавал ажиллах screen:

- Login page.
- Dashboard.
- System list page.
- Create system page.
- Edit system page.
- System detail page.
- Security checklist tab.
- Audit log page.

Эхний MVP-д заавал ажиллах backend:

- JWT login.
- Password hash.
- System CRUD API.
- Search/filter API.
- Security checklist API.
- Audit log API.

## 3. Хөгжүүлэх дарааллын overview

```text
Phase 0  - Ажлын орчин ба repo structure
Phase 1  - Database design ба migration
Phase 2  - Backend skeleton
Phase 3  - Auth/Login
Phase 4  - System Registry CRUD
Phase 5  - System relations
Phase 6  - Security checklist
Phase 7  - Audit log
Phase 8  - Frontend skeleton
Phase 9  - Frontend CRUD/list/search
Phase 10 - Dashboard
Phase 11 - Banking Transfer Service demo
Phase 12 - Health/Swagger enterprise fields
Phase 13 - Gateway/Eureka optional
Phase 14 - Testing, seed data, presentation
```

Хамгийн зөв checkpoint:

- Phase 0-10 дуусвал PDF даалгавар биелсэн гэж үзнэ.
- Phase 11-13 дуусвал enterprise нэмэлттэй хүчтэй demo болно.

## 4. Санал болгож буй root file structure

Эхэндээ repo-г дараах байдлаар үүсгэнэ:

```text
service-registry-platform/
  README.md
  docker-compose.yml
  .env.example
  .gitignore

  docs/
    assignment-summary.md
    architecture.md
    api-contract.md
    database.md
    demo-script.md
    ai-rules.md

  backend/
    pom.xml

    platform-api/
      pom.xml
      src/
        main/
          java/
            mn/
              golomt/
                registry/
                  PlatformApiApplication.java
                  common/
                  config/
                  auth/
                  users/
                  systems/
                  relations/
                  securitycheck/
                  audit/
                  dashboard/
          resources/
            application.yml
            application-local.yml
            db/
              migration/
                V1__init_schema.sql
                V2__seed_security_controls.sql
                V3__seed_demo_data.sql
        test/
          java/
            mn/
              golomt/
                registry/

    banking-transfer-service/
      pom.xml
      src/
        main/
          java/
            mn/
              golomt/
                banking/
                  BankingTransferApplication.java
                  customer/
                  account/
                  transfer/
                  ledger/
                  notification/
          resources/
            application.yml
            db/
              migration/
                V1__banking_schema.sql
                V2__seed_demo_customers.sql
        test/
          java/
            mn/
              golomt/
                banking/

    gateway-service/
      pom.xml
      src/
        main/
          java/
            mn/
              golomt/
                gateway/
          resources/
            application.yml

    discovery-server/
      pom.xml
      src/
        main/
          java/
            mn/
              golomt/
                discovery/
          resources/
            application.yml

  frontend/
    package.json
    vite.config.ts
    tsconfig.json
    index.html
    src/
      main.tsx
      App.tsx
      routes/
      layouts/
      pages/
        LoginPage.tsx
        DashboardPage.tsx
        SystemsListPage.tsx
        SystemCreatePage.tsx
        SystemEditPage.tsx
        SystemDetailPage.tsx
        AuditLogPage.tsx
      components/
        common/
        systems/
        security/
        dashboard/
      api/
        httpClient.ts
        authApi.ts
        systemsApi.ts
        securityApi.ts
        auditApi.ts
        dashboardApi.ts
      types/
        auth.ts
        system.ts
        security.ts
        audit.ts
      hooks/
      utils/
      styles/
```

## 5. Эхний шатанд яг барих backend structure

Эхний хөгжүүлэлтэд `platform-api` л заавал хэрэгтэй. `banking-transfer-service`, `gateway-service`, `discovery-server`-ийг folder placeholder байдлаар үлдээж болно.

`platform-api` module доторх package:

```text
mn.golomt.registry
  common/
    ApiResponse.java
    PageResponse.java
    ErrorResponse.java
    GlobalExceptionHandler.java
    ValidationException.java

  config/
    SecurityConfig.java
    JwtConfig.java
    OpenApiConfig.java
    CorsConfig.java

  auth/
    AuthController.java
    AuthService.java
    LoginRequest.java
    LoginResponse.java
    JwtTokenProvider.java
    CurrentUser.java

  users/
    User.java
    UserRepository.java
    UserRole.java
    UserService.java

  systems/
    SystemEntity.java
    SystemType.java
    SystemStatus.java
    SystemRepository.java
    SystemService.java
    SystemController.java
    dto/
      SystemCreateRequest.java
      SystemUpdateRequest.java
      SystemResponse.java
      SystemSearchRequest.java

  relations/
    SystemRelation.java
    RelationType.java
    SystemRelationRepository.java
    SystemRelationService.java

  securitycheck/
    SecurityControl.java
    SecurityCheckResult.java
    SecurityCheckResultStatus.java
    SecurityControlRepository.java
    SecurityCheckRepository.java
    SecurityCheckService.java
    SecurityCheckController.java
    dto/
      SecurityCheckUpdateRequest.java
      SecurityScoreResponse.java

  audit/
    AuditLog.java
    AuditAction.java
    AuditLogRepository.java
    AuditLogService.java
    AuditLogController.java

  dashboard/
    DashboardController.java
    DashboardService.java
    DashboardSummaryResponse.java
```

## 6. Database migration нарийвчилсан дараалал

### V1__init_schema.sql

Үүсгэх хүснэгтүүд:

```text
users
systems
system_relations
security_controls
security_check_results
audit_logs
```

`users`:

```sql
id
username
password_hash
display_name
role
enabled
created_at
updated_at
```

`systems`:

```sql
id
system_key
name
type
valuation_mnt
description
developer_name
developer_team
start_date
end_date
in_use
environment
base_url
health_url
swagger_url
repo_url
status
created_by
created_at
updated_at
```

`system_relations`:

```sql
id
source_system_id
target_system_id
relation_type
description
created_at
```

`security_controls`:

```sql
id
control_key
title
description
weight
required
automated
standard_ref
created_at
```

`security_check_results`:

```sql
id
system_id
control_id
result
evidence
checked_by
checked_at
```

`audit_logs`:

```sql
id
actor_user_id
action
target_type
target_id
message
metadata_json
created_at
```

### V2__seed_security_controls.sql

Эхний checklist:

```text
HTTPS_ENABLED
AUTHENTICATION_ENABLED
ROLE_BASED_ACCESS
AUDIT_LOG_ENABLED
SECRETS_NOT_IN_CODE
SWAGGER_PROTECTED
CORS_RESTRICTED
INPUT_VALIDATION
```

### V3__seed_demo_data.sql

Эхний өгөгдөл:

```text
admin user
Banking Transfer Service
Card Service
Core Banking Adapter
Digital Banking Service
CRM Service
HR Service
```

## 7. Backend хөгжүүлэлтийн нарийн дараалал

### Phase 0 - Project setup

Хийх зүйл:

1. `service-registry-platform` folder үүсгэх.
2. Git init хийх.
3. `.gitignore` нэмэх.
4. `.env.example` нэмэх.
5. `docker-compose.yml` дээр PostgreSQL нэмэх.
6. Root `README.md` үүсгэх.
7. `docs/` folder үүсгэж architecture/API/database бичих.

Дууссан гэж үзэх шалгуур:

- PostgreSQL container асна.
- README дээр run command байна.
- Git дээр initial commit хийхэд бэлэн.

### Phase 1 - Spring Boot platform-api skeleton

Хийх зүйл:

1. Spring Boot project үүсгэх.
2. Dependency:
   - Spring Web.
   - Spring Security.
   - Spring Data JPA.
   - PostgreSQL Driver.
   - Validation.
   - Flyway.
   - Lombok.
   - springdoc-openapi.
   - Actuator.
3. `application-local.yml` дээр DB config.
4. `/actuator/health` шалгах.
5. `/swagger-ui.html` эсвэл `/swagger-ui/index.html` шалгах.

Дууссан гэж үзэх шалгуур:

- Backend асна.
- Health endpoint `UP`.
- Swagger UI нээгдэнэ.
- Flyway migration ажиллана.

### Phase 2 - User/Auth

Хийх зүйл:

1. `User` entity.
2. `UserRepository`.
3. `UserRole` enum.
4. Seed admin user.
5. Password BCrypt hash.
6. Login API:
   - `POST /api/auth/login`
7. JWT token үүсгэх.
8. `GET /api/auth/me`.
9. Protected API test хийх.

Дууссан гэж үзэх шалгуур:

- Admin login амжилттай.
- Tokenгүй үед protected API 401 өгнө.
- Tokenтой үед API ажиллана.
- Password plain text хадгалагдахгүй.

### Phase 3 - System Registry CRUD

Хийх зүйл:

1. `SystemEntity` entity.
2. Enum:
   - `SystemType`: CARD, CORE, INTERNAL, DIGITAL.
   - `SystemStatus`: ACTIVE, INACTIVE, UNKNOWN, DOWN.
3. DTO:
   - `SystemCreateRequest`.
   - `SystemUpdateRequest`.
   - `SystemResponse`.
4. Validation:
   - name required.
   - type required.
   - valuationMnt >= 0.
   - startDate <= endDate.
   - URL fields valid if present.
5. CRUD endpoint:
   - `GET /api/systems`
   - `POST /api/systems`
   - `GET /api/systems/{id}`
   - `PUT /api/systems/{id}`
   - `DELETE /api/systems/{id}`
6. Delete эхэндээ hard delete биш `inUse=false` эсвэл `status=INACTIVE` болгох.

Дууссан гэж үзэх шалгуур:

- PDF-ийн бүх талбарыг API авч хадгална.
- Create/update validation ажиллана.
- System detail зөв буцна.

### Phase 4 - Search/filter

Хийх зүйл:

1. List API query param:
   - `keyword`
   - `type`
   - `developer`
   - `inUse`
   - `status`
   - `page`
   - `size`
2. Repository query эсвэл Specification.
3. Pagination response.
4. Sorting:
   - createdAt desc default.

Дууссан гэж үзэх шалгуур:

- Нэрээр хайна.
- Төрлөөр filter хийнэ.
- Хөгжүүлэгчээр хайна.
- Ашиглагдаж байгаа эсэхээр filter хийнэ.

### Phase 5 - Related systems

Хийх зүйл:

1. `SystemRelation` entity.
2. `RelationType` enum:
   - DEPENDS_ON.
   - CALLS.
   - INTEGRATES_WITH.
3. Create/update system request дээр related system IDs авах.
4. Detail response дээр холбоотой системүүдийг буцаах.
5. Өөрийгөө өөртэй нь холбохыг хориглох.

Дууссан гэж үзэх шалгуур:

- A system B system-тэй холбоотой гэж бүртгэгдэнэ.
- Detail дээр холбоотой системүүд харагдана.
- Search/list дээр related count харагдаж болно.

### Phase 6 - Security checklist

Хийх зүйл:

1. `SecurityControl` seed хийх.
2. `SecurityCheckResult` entity.
3. Endpoint:
   - `GET /api/security-controls`
   - `GET /api/systems/{id}/security-checks`
   - `PUT /api/systems/{id}/security-checks`
   - `GET /api/systems/{id}/security-score`
4. Score calculation:
   - PASS weight нэмнэ.
   - FAIL нэмэхгүй.
   - WARNING 50% weight.
   - NOT_CHECKED нэмэхгүй.
5. UI-д хэрэгтэй summary response гаргах.

Дууссан гэж үзэх шалгуур:

- System бүр checklist-тэй.
- PASS/FAIL/WARNING/NOT_CHECKED хадгалагдана.
- Score 0-100 гарна.

### Phase 7 - Audit log

Хийх зүйл:

1. `AuditLog` entity.
2. `AuditAction` enum:
   - LOGIN_SUCCESS.
   - SYSTEM_CREATED.
   - SYSTEM_UPDATED.
   - SYSTEM_DISABLED.
   - SECURITY_CHECK_UPDATED.
3. System create/update/delete дээр audit бичих.
4. Security check update дээр audit бичих.
5. Endpoint:
   - `GET /api/audit-logs`

Дууссан гэж үзэх шалгуур:

- Хэн, хэзээ, юу өөрчилсөн харагдана.
- System detail дээр тухайн system-ийн audit history харуулж болно.

## 8. Frontend хөгжүүлэлтийн нарийн дараалал

### Phase 8 - Frontend skeleton

Хийх зүйл:

1. Vite React TypeScript project үүсгэх.
2. Material UI суулгах.
3. React Router тохируулах.
4. Axios client үүсгэх.
5. Token storage strategy:
   - эхний хувилбарт localStorage байж болно,
   - дараа нь httpOnly cookie болгох боломжтой.
6. Layout:
   - Sidebar.
   - Topbar.
   - Content area.

Дууссан гэж үзэх шалгуур:

- App асна.
- Login route, dashboard route байна.
- API base URL `.env`-ээс уншина.

### Phase 9 - Login UI

Хийх зүйл:

1. Login form.
2. Username/password validation.
3. Login API дуудах.
4. Token хадгалах.
5. Authenticated route хамгаалах.
6. Logout хийх.

Дууссан гэж үзэх шалгуур:

- Login амжилттай.
- Tokenгүй бол dashboard руу оруулахгүй.
- Logout ажиллана.

### Phase 10 - System list/search UI

Хийх зүйл:

1. `SystemsListPage`.
2. Table columns:
   - name.
   - type.
   - valuation.
   - developer.
   - in use.
   - security score.
   - status.
   - actions.
3. Search input.
4. Type filter.
5. In use filter.
6. Pagination.
7. Detail/edit buttons.

Дууссан гэж үзэх шалгуур:

- List API-тай холбогдоно.
- Search/filter ажиллана.
- Empty state байна.
- Loading/error state байна.

### Phase 11 - Create/Edit form UI

Хийх зүйл:

1. `SystemCreatePage`.
2. `SystemEditPage`.
3. Form fields:
   - name.
   - type select.
   - valuationMnt number input.
   - description textarea.
   - relatedSystems multi-select.
   - developerName.
   - developerTeam.
   - startDate.
   - endDate.
   - inUse switch.
   - baseUrl.
   - healthUrl.
   - swaggerUrl.
   - repoUrl.
4. Client validation.
5. Save/cancel.
6. Error display.

Дууссан гэж үзэх шалгуур:

- PDF-ийн бүх талбар UI дээр байна.
- Create/update ажиллана.
- Validation ойлгомжтой гарна.

### Phase 12 - Detail + security UI

Хийх зүйл:

1. `SystemDetailPage`.
2. Tabs:
   - Overview.
   - Related systems.
   - Security checklist.
   - Audit.
3. Security checklist:
   - control title.
   - result select.
   - evidence input.
   - save button.
   - score progress.
4. Swagger link button.
5. Health URL display.

Дууссан гэж үзэх шалгуур:

- Detail page дээр бүх мэдээлэл харагдана.
- Security result update ажиллана.
- Score update болж харагдана.

### Phase 13 - Dashboard UI

Хийх зүйл:

1. Summary cards:
   - Total systems.
   - Active systems.
   - Inactive systems.
   - Average security score.
2. Type distribution.
3. Recent audit logs.
4. Low security systems.

Дууссан гэж үзэх шалгуур:

- Dashboard backend API-тай холбогдоно.
- Seed data дээр утгатай харагдана.

## 9. Banking Transfer Service дараалал

Энэ бол enterprise bonus demo. Үндсэн даалгавар дууссаны дараа хийвэл зөв.

### Phase 14 - Banking service skeleton

Хийх зүйл:

1. `banking-transfer-service` Spring Boot app.
2. Dependency:
   - Spring Web.
   - Spring Data JPA.
   - PostgreSQL Driver.
   - Flyway.
   - Validation.
   - springdoc-openapi.
   - Actuator.
3. DB schema:
   - customers.
   - accounts.
   - transfers.
   - ledger_entries.
4. Seed:
   - Customer A.
   - Customer B.
   - Account A balance 1,000,000 MNT.
   - Account B balance 500,000 MNT.

### Phase 15 - Transfer API

Endpoint:

```text
POST /api/transfers
GET  /api/accounts/{accountNo}
GET  /api/transfers
GET  /api/transfers/{id}
```

Transfer validation:

- Sender account exists.
- Receiver account exists.
- Sender account active.
- Receiver account active.
- Amount > 0.
- Sender balance >= amount.
- Debit and credit must be one transaction.
- Error гарвал rollback.

Demo:

```json
{
  "fromAccountNo": "100000001",
  "toAccountNo": "100000002",
  "amount": 50000,
  "description": "Demo transfer"
}
```

Дууссан гэж үзэх шалгуур:

- 2 дансны balance өөрчлөгдөнө.
- Transfer history үүснэ.
- Ledger debit/credit бичилт үүснэ.
- `/actuator/health` UP.
- Swagger UI байна.

### Phase 16 - Catalog дээр banking service бүртгэх

Хийх зүйл:

1. Platform дээр `Banking Transfer Service` бүртгэх.
2. `baseUrl`: `http://localhost:8084`.
3. `healthUrl`: `http://localhost:8084/actuator/health`.
4. `swaggerUrl`: `http://localhost:8084/swagger-ui/index.html`.
5. Security checklist бөглөх.
6. Dashboard дээр харах.

Дууссан гэж үзэх шалгуур:

- Banking service catalog дээр бүртгэлтэй.
- Health/Swagger/Security score харагдана.
- Transfer API demo ажиллана.

## 10. Optional enterprise дараалал

Эдгээрийг хугацаа байвал хий.

### Gateway

Хийх зүйл:

- `gateway-service` үүсгэх.
- Frontend зөвхөн gateway URL рүү дууддаг болгох.
- Route:
  - `/api/platform/**` -> platform-api.
  - `/api/banking/**` -> banking-transfer-service.

### Eureka

Хийх зүйл:

- `discovery-server` үүсгэх.
- platform-api, banking-transfer-service, gateway-service-г Eureka client болгох.
- Dashboard screenshot эсвэл demo дээр service instance харах.

### Config Server

Хийх зүйл:

- Зөвхөн architecture тайлбар эсвэл placeholder.
- Эхний demo-д хэрэгжүүлэх шаардлагагүй.

## 11. Testing дараалал

Backend test:

- AuthService login test.
- SystemService create/update validation test.
- System search/filter test.
- Security score calculation test.
- TransferService insufficient balance test.
- TransferService success rollback-safe test.

Frontend test:

- Login form validation.
- System form required fields.
- Search/filter smoke test.

Manual test checklist:

```text
1. PostgreSQL ассан эсэх
2. platform-api ассан эсэх
3. frontend ассан эсэх
4. admin login
5. system create
6. system edit
7. system search
8. related system add
9. security checklist update
10. audit log харах
11. banking-transfer-service асах
12. transfer хийх
13. banking service catalog дээр харах
```

## 12. Demo script

Хамгаалалт дээр хийх дараалал:

1. Login хийх.
2. Dashboard харуулах.
3. System list харуулах.
4. New system бүртгэх:
   - нэр: Banking Transfer Service.
   - төрөл: Коре.
   - үнэлгээ: 15000000.
   - хөгжүүлэгч: Demo Developer.
   - ашиглагдаж байгаа: true.
5. Detail page рүү орох.
6. Related systems нэмэх:
   - Core Banking Adapter.
   - Notification Service.
7. Security checklist бөглөх.
8. Score өөрчлөгдөж байгааг харуулах.
9. Audit log дээр action бүр бичигдсэн эсэхийг харуулах.
10. Banking transfer API-р 2 данс хооронд гүйлгээ хийх.
11. Banking service-ийн Swagger/health link харуулах.
12. “Энэ бол анхны систем бүртгэлийн даалгаврыг enterprise service catalog болгон өргөтгөсөн хувилбар” гэж тайлбарлах.

## 13. AI ашиглах үндсэн дүрэм

AI-г ашиглахдаа дараах зарчмыг баримтална:

> AI нь код бичих туслах хэрэгсэл. Харин business rule, security decision, scope decision-ийг хүн батална.

### 13.1. AI-д нэг дор өгөх task жижиг байх

Буруу:

```text
Бүх backend/frontend-ийг хий.
```

Зөв:

```text
platform-api дээр SystemEntity, SystemType enum, SystemRepository үүсгээд
PDF-ийн шаардлагын талбаруудыг тусга. Одоо байгаа package naming-ийг дага.
Migration файлд тохирох table нэм.
```

### 13.2. AI заавал context уншсаны дараа код бичих

AI-д өгөх prompt бүрд:

- Ямар файл засах.
- Ямар шаардлага биелүүлэх.
- Ямар файлд хүрэхгүй байх.
- Ямар test ажиллуулах.

Жишээ:

```text
Эхлээд backend/platform-api package structure-ийг унш.
Дараа нь зөвхөн systems package дээр System CRUD нэм.
Auth/security config-д өөрчлөлт хийх шаардлагатай бол тайлбарлаад хий.
Дууссаны дараа mvn test ажиллуул.
```

### 13.3. AI-д хориглох зүйл

AI дараах зүйлийг хийх ёсгүй:

- Нууц үгийг plain text хадгалах.
- `.env` эсвэл secret key-г commit хийх.
- User input URL рүү хамгаалалтгүй server-side request хийх.
- Бүх actuator endpoint-ийг public expose хийх.
- Existing code-г шалтгаангүй том refactor хийх.
- Даалгаврын exact талбаруудыг хасах.
- Business rule-ийг дур мэдэн өөрчлөх.
- Test алдаатай байхад “болсон” гэж хэлэх.

### 13.4. AI ашиглах workflow

Task бүр дээр:

```text
1. Context уншуулна.
2. Хийх жижиг task өгнө.
3. AI өөрчлөлт хийнэ.
4. Test/build ажиллуулна.
5. Diff шалгана.
6. Хэрэв зөв бол commit.
7. Дараагийн task руу орно.
```

### 13.5. AI prompt template

Backend prompt:

```text
Чи энэ repo дээр Spring Boot backend хөгжүүлнэ.
Одоо хийх task: [TASK_NAME].

Шаардлага:
- [REQ_1]
- [REQ_2]
- [REQ_3]

Хүрэх file/folder:
- backend/platform-api/src/main/java/...
- backend/platform-api/src/main/resources/db/migration/...

Хүрэхгүй:
- frontend/
- banking-transfer-service/

Дууссаны дараа:
- mvn test ажиллуул
- ямар file өөрчлөгдсөнийг товч тайлбарла
- үлдсэн эрсдэл байвал хэл
```

Frontend prompt:

```text
Чи энэ repo дээр React + Vite + TypeScript frontend хөгжүүлнэ.
Одоо хийх task: [TASK_NAME].

Шаардлага:
- Existing API types ашигла.
- Material UI ашигла.
- Loading, error, empty state заавал хий.
- Text overlap үүсгэхгүй responsive layout хий.

Хүрэх folder:
- frontend/src/pages/...
- frontend/src/components/...
- frontend/src/api/...
- frontend/src/types/...

Дууссаны дараа:
- npm run build ажиллуул
- UI flow-г товч тайлбарла
```

Review prompt:

```text
Энэ өөрчлөлтийг code review маягаар шалга.
Эхлээд bug/security/regression finding-уудыг жагсаа.
Дараа нь test gap хэл.
Эцэст нь зөвхөн шаардлагатай засвар санал болго.
```

## 14. AI code review checklist

AI-аар code review хийлгэхдээ дараахыг шалгуулна:

- PDF-ийн бүх талбар хадгалагдаж байна уу.
- Password hash ашиглаж байна уу.
- JWT expiry байгаа юу.
- Role-based access зөв үү.
- Validation backend дээр байгаа юу.
- Frontend validation дангаараа үлдээгүй юу.
- SQL constraint хангалттай юу.
- Audit log бүх чухал action дээр бичигдэж байна уу.
- Search/filter query зөв үү.
- Security score calculation зөв үү.
- Transfer дээр transaction rollback ажиллах уу.
- URL fetch хийх үед SSRF хамгаалалт байгаа юу.
- Actuator endpoint public биш үү.
- Test хангалттай юу.

## 15. Commit хийх дараалал

Commit-ийг жижиг, ойлгомжтой хийнэ.

Санал болгож буй commit order:

```text
chore: initialize project structure
chore: add docker compose and env example
feat(platform): add database schema migrations
feat(platform): add authentication
feat(platform): add system registry CRUD
feat(platform): add system search and filters
feat(platform): add related systems
feat(platform): add security checklist and score
feat(platform): add audit logging
feat(frontend): add app shell and routing
feat(frontend): add login flow
feat(frontend): add systems list and filters
feat(frontend): add system create and edit forms
feat(frontend): add system detail and security checklist
feat(frontend): add dashboard
feat(banking): add transfer service
docs: add demo script
```

## 16. Хамгийн эхний 10 task

Яг одоо код эхлүүлэхэд хийх хамгийн эхний дараалал:

1. `service-registry-platform/` repo folder үүсгэх.
2. `README.md`, `.gitignore`, `.env.example`, `docker-compose.yml` нэмэх.
3. PostgreSQL container асаах.
4. `backend/platform-api` Spring Boot project үүсгэх.
5. Flyway migration `V1__init_schema.sql` бичих.
6. Seed admin user болон security controls нэмэх.
7. Login API хийх.
8. System CRUD API хийх.
9. React frontend skeleton үүсгэх.
10. Login + System list/create form холбох.

Энэ 10 task дуусахад төсөл бодитоор хөдөлж эхэлнэ.

## 17. Scope хамгаалах дүрэм

Хөгжүүлэлтийн явцад scope нэмэгдэх эрсдэл өндөр. Тиймээс:

- PDF-ийн шаардлага дуусаагүй байхад Eureka хийхгүй.
- System CRUD дуусаагүй байхад Banking Transfer Service хийхгүй.
- Login дуусаагүй байхад dashboard гоёчлохгүй.
- Security checklist дуусаагүй байхад Prometheus/Grafana хийхгүй.
- Frontend form PDF-ийн бүх талбарыг аваагүй байхад enterprise нэмэлт field дээр төвлөрөхгүй.

Хэрэв хугацаа давчуу бол хамгийн түрүүнд үлдээх зүйл:

```text
Login
System CRUD
List/search
Security checklist
Audit log
```

Хэрэв хугацаа илүү байвал нэмэх зүйл:

```text
Banking Transfer Service
Health check
Swagger link
Gateway
Eureka
```
