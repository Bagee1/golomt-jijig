# Development Checklist and Work Log

Огноо: 2026-07-06

Энэ файл нь төслийн хөгжүүлэлтийн алхам, хийсэн ажлын тэмдэглэл, дараагийн хийх зүйлсийг нэг дор хөтөлнө.

## 1. Хөтлөх дүрэм

Ажил бүрийн дараа энэ файлыг шинэчилнэ.

Дүрэм:

- Дууссан ажлыг `[x]` болгоно.
- Дуусаагүй ажлыг `[ ]` хэвээр үлдээнэ.
- Асуудалтай ажлыг `Blocked` хэсэгт бичнэ.
- Шийдвэр гаргасан бол `Decision log` хэсэгт тэмдэглэнэ.
- Test/build ажиллуулсан бол `Verification log` хэсэгт command болон үр дүнг бичнэ.
- DB schema өөрчлөгдвөл DBRD document-ийг давхар шинэчилнэ.

## 2. Current status

Одоогийн checkpoint:

```text
platform-api бүрэн: Auth + System CRUD/list/search + Security checklist + Audit log API, 33 тест ногоон.
banking-transfer-service "жинхэнэ банк" түвшинд өргөжсөн: status lifecycle (SUCCESS/FAILED/REVERSED),
  reversal, дансны хуулга, эзэмшлийн шалгалт (customers.username ↔ JWT subject), гүйлгээний лимит,
  харилцагч/данс удирдлагын ADMIN API, өөрийн bank_audit_logs, ErrorCode-той алдааны формат — 44 тест ногоон.
Frontend ХОЁР тусдаа апп болсон: frontend/ (registry portal, 5173) ба frontend-banking/ (банкны апп, 5174).
  Banking апп: харилцагчийн нүүр (өөрийн данс), хуулга, шилжүүлэг; теллерийн харилцагч/данс/аудит/буцаалт.
Demo нэвтрэлт: admin/admin123 (теллер), batbold/demo123 + sarnai/demo123 (харилцагчид).
Git түүх үргэлжилж байгаа, ажил бүрт commit хийж байгаа.
Хоёр service жинхэнэ Postgres дээр шалгагдсан: migration OK, health UP, e2e transfer demo амжилттай.
banking-transfer-service platform-api-ийн JWT-г шаарддаг (shared JWT_SECRET, resource server).
```

Одоогийн гол blocker:

```text
Идэвхтэй blocker байхгүй.
(Өмнөх Docker Desktop blocker 2026-07-06 шөнө шийдэгдсэн — Blocked list харах.)
```

## 3. Done checklist

### 3.1. Planning and documentation

- [x] Анхны PDF даалгаврыг уншиж шаардлагыг гаргасан.
- [x] `хөгжүүлэлт эхлэх судалгаа.md` баримт үүсгэсэн.
- [x] Даалгавартай нийцлийн үнэлгээ гаргасан.
- [x] Хөгжүүлэлтийн нарийн дараалал ба AI дүрэм гаргасан.
- [x] DB relation болон DBRD document гаргасан.
- [x] Frontend component, screen/slide, color design plan гаргасан.

Тэмдэглэл:

- Анхны даалгаврын гол scope нь login, system registry, security standard check, list/search.
- Enterprise өргөтгөлөөр Banking Transfer Service, health check, Swagger, audit, Gateway/Eureka төлөвлөсөн.

### 3.2. Root project setup

- [x] `service-registry-platform/` repo folder үүсгэсэн.
- [x] Git repository initialized.
- [x] Root `README.md` нэмсэн.
- [x] Root `.gitignore` нэмсэн.
- [x] `.env.example` нэмсэн.
- [x] `docker-compose.yml` нэмсэн.
- [x] `docs/` folder нэмсэн.

Тэмдэглэл:

- Project root: `C:\Users\basba\OneDrive\Desktop\ajliin neg heseg\service-registry-platform`
- Docker Compose-д PostgreSQL service тодорхойлсон.
- Docker Desktop асаагүй байсан тул container ажиллуулах checkpoint хийгдээгүй.

### 3.3. Backend skeleton

- [x] `backend/platform-api` Spring Boot project үүсгэсэн.
- [x] Spring Boot version-ийг `3.5.16` болгож зассан.
- [x] Java version `17` болгож тохируулсан.
- [x] Maven build амжилттай болсон.
- [x] H2 test dependency нэмсэн.
- [x] `application.yml` нэмсэн.
- [x] `application-test.yml` нэмсэн.

Тэмдэглэл:

- Default `java` command Java 8 руу зааж байгаа.
- Build/test хийхдээ `C:\Users\basba\.jdk\jdk-17.0.16` ашиглана.

### 3.4. Database schema

- [x] `V1__init_schema.sql` үүсгэсэн.
- [x] `users` table үүсгэсэн.
- [x] `systems` table үүсгэсэн.
- [x] `system_relations` table үүсгэсэн.
- [x] `security_controls` table үүсгэсэн.
- [x] `security_check_results` table үүсгэсэн.
- [x] `audit_logs` table үүсгэсэн.
- [x] Primary key, foreign key, unique, check constraints нэмсэн.
- [x] Search/filter-д хэрэгтэй эхний indexes нэмсэн.

Тэмдэглэл:

- `systems` table нь PDF-ийн exact талбаруудыг агуулна.
- `system_relations` нь холбоотой системүүдийг self-referencing relation байдлаар хадгална.
- `security_check_results` нь system + security control unique pair байна.

### 3.5. Seed data

- [x] `V2__seed_security_controls.sql` үүсгэсэн.
- [x] 8 security control seed хийсэн.
- [x] `V3__seed_demo_data.sql` үүсгэсэн.
- [x] Admin user seed хийсэн.
- [x] Admin password-г BCrypt hash болгож хадгалсан.
- [x] Demo systems seed хийсэн.
- [x] Demo system relations seed хийсэн.

Тэмдэглэл:

- Seed admin username/password: `admin / admin123`.
- DB дээр password plain text биш BCrypt hash байдлаар хадгалагдана.

### 3.6. JPA entity and repository skeleton

- [x] `User` entity үүсгэсэн.
- [x] `UserRole` enum үүсгэсэн.
- [x] `UserRepository` үүсгэсэн.
- [x] `SystemEntity` entity үүсгэсэн.
- [x] `SystemType` enum үүсгэсэн.
- [x] `SystemStatus` enum үүсгэсэн.
- [x] `SystemRepository` үүсгэсэн.
- [x] `SystemRelation` entity үүсгэсэн.
- [x] `RelationType` enum үүсгэсэн.
- [x] `SystemRelationRepository` үүсгэсэн.
- [x] `SecurityControl` entity үүсгэсэн.
- [x] `SecurityCheckResult` entity үүсгэсэн.
- [x] `SecurityCheckResultStatus` enum үүсгэсэн.
- [x] `SecurityControlRepository` үүсгэсэн.
- [x] `SecurityCheckResultRepository` үүсгэсэн.
- [x] `AuditLog` entity үүсгэсэн.
- [x] `AuditLogRepository` үүсгэсэн.

Тэмдэглэл:

- Entity mapping нь Flyway schema-тай таарч байгаа.
- `mvn test` дээр Hibernate validation давсан.

### 3.7. Authentication API

- [x] Spring Security custom config хийсэн.
- [x] Default generated Spring password warning арилсан.
- [x] `PasswordEncoder` bean нэмсэн.
- [x] DB-backed `UserDetailsService` хийсэн.
- [x] JWT resource server dependency нэмсэн.
- [x] JWT encoder/decoder config хийсэн.
- [x] `POST /api/auth/login` хийсэн.
- [x] `GET /api/auth/me` хийсэн.
- [x] Auth API test нэмсэн.
- [x] API error response format эхний хувилбараар нэг болгосон.

Тэмдэглэл:

- Login seed user: `admin / admin123`.
- Login response нь Bearer JWT token, expiry seconds, user info буцаана.
- JWT token дотор `userId`, `displayName`, `role`, `authorities` claim орно.
- `/api/auth/login`, `/actuator/health`, `/actuator/info` public.
- Бусад endpoint token шаардана.

### 3.8. System Registry CRUD API

- [x] `SystemCreateRequest` DTO үүсгэсэн.
- [x] `SystemUpdateRequest` DTO үүсгэсэн.
- [x] `SystemResponse` DTO үүсгэсэн.
- [x] Relation request/response DTO үүсгэсэн.
- [x] `SystemService` үүсгэсэн.
- [x] `SystemController` үүсгэсэн.
- [x] `POST /api/systems` хийсэн.
- [x] `GET /api/systems` хийсэн.
- [x] `GET /api/systems/{id}` хийсэн.
- [x] `PUT /api/systems/{id}` хийсэн.
- [x] `DELETE /api/systems/{id}` хийсэн.
- [x] Delete хийхдээ hard delete биш `inUse=false`, `status=INACTIVE` болгодог болгосон.
- [x] `keyword`, `type`, `developer`, `inUse`, `status` filter хийсэн.
- [x] Pagination болон `createdAt desc` default sort хийсэн.
- [x] Related systems create/update/detail response хийсэн.
- [x] Өөрийгөө өөртэй нь холбох validation хийсэн.
- [x] Давхардсан relation үүсгэхгүй validation хийсэн.
- [x] System CRUD/search/relation tests нэмсэн.

Тэмдэглэл:

- `POST`, `PUT`, `DELETE` нь `ADMIN` role шаардана.
- `GET /api/systems`, `GET /api/systems/{id}` нь authenticated user шаардана.
- `systemKey` request дээр ирвэл unique эсэхийг шалгана.
- `systemKey` ирэхгүй бол `name`-ээс slug үүсгэнэ.
- `startDate > endDate` үед `400 Bad Request` буцаана.
- `DELETE` нь record устгахгүй, идэвхгүй болгоно.

### 3.9. Security Checklist API

- [x] `SecurityControlResponse` DTO үүсгэсэн.
- [x] `SecurityCheckItemRequest` DTO үүсгэсэн.
- [x] `SecurityCheckUpdateRequest` DTO үүсгэсэн.
- [x] `SecurityCheckResultResponse` DTO үүсгэсэн.
- [x] `SecurityScoreResponse` DTO үүсгэсэн.
- [x] `SecurityCheckService` үүсгэсэн.
- [x] `SecurityCheckController` үүсгэсэн.
- [x] `GET /api/security-controls` хийсэн.
- [x] `GET /api/systems/{id}/security-checks` хийсэн.
- [x] `PUT /api/systems/{id}/security-checks` хийсэн.
- [x] `GET /api/systems/{id}/security-score` хийсэн.
- [x] Score calculation service хийсэн.
- [x] PASS/WARNING/FAIL/NOT_CHECKED weight logic test хийсэн.
- [x] Security checklist controller tests нэмсэн.
- [x] Security control response дарааллыг `id asc` болгож тогтвортой болгосон.

Тэмдэглэл:

- Security control жагсаалт нь `V2__seed_security_controls.sql` дээрх 8 control-оос уншина.
- System бүрийн checklist нь control бүр дээр нэг result-тэй байна.
- Result байхгүй control-ууд response дээр `NOT_CHECKED` гэж буцна.
- `PUT /api/systems/{id}/security-checks` нь `ADMIN` эсвэл `SECURITY` role шаардана.
- Score logic: `PASS = full weight`, `WARNING = half weight`, `FAIL/NOT_CHECKED = 0`.
- Score нь `earnedWeight / totalWeight * 100` томьёогоор round хийж integer болгоно.

### 3.10. Audit Log API

- [x] `AuditAction` enum үүсгэсэн.
- [x] `AuditLogResponse` DTO үүсгэсэн.
- [x] `AuditLogService` үүсгэсэн.
- [x] `AuditLogController` үүсгэсэн.
- [x] `GET /api/audit-logs` хийсэн.
- [x] Login success/fail audit бичдэг болгосон.
- [x] System created audit бичдэг болгосон.
- [x] System updated audit бичдэг болгосон.
- [x] System disabled audit бичдэг болгосон.
- [x] Security check updated audit бичдэг болгосон.
- [x] Audit API болон action write tests нэмсэн.

Тэмдэглэл:

- `GET /api/audit-logs` нь `ADMIN` эсвэл `SECURITY` role шаардана.
- Audit жагсаалт нь `createdAt desc`, `id desc` дарааллаар pagination-той буцна.
- Login failure audit нь authentication exception гарсан ч `REQUIRES_NEW` transaction-оор хадгалагдана.
- Audit metadata-г JSON string хэлбэрээр `metadata_json` талбарт хадгална.

### 3.11. Frontend mock UI skeleton

- [x] `frontend/` Vite React TypeScript project үүсгэсэн.
- [x] `react-router-dom` route setup хийсэн.
- [x] `lucide-react` icon ашигласан.
- [x] Screenshot reference-тэй нийцсэн dark sidebar + topbar + content layout хийсэн.
- [x] Dashboard mock UI хийсэн.
- [x] Systems list mock UI хийсэн.
- [x] Audit log mock UI хийсэн.
- [x] Responsive collapse/scroll эхний хувилбараар хийсэн.
- [x] `npm.cmd run build` PASS.
- [x] `npm.cmd run lint` PASS.

Тэмдэглэл:

- Одоогоор frontend нь mock data ашиглаж байна.
- Backend API холболт, login flow, create/edit/detail/security tab дараагийн task-д үлдсэн.
- `agent-browser` CLI энэ орчинд callable биш байсан тул Playwright CLI screenshot fallback ашиглаж visual check хийсэн.

### 3.12. Frontend API integration ба login flow

- [x] `httpClient.ts` fetch wrapper + `ApiError` хийсэн.
- [x] `authApi`, `systemsApi`, `securityApi`, `auditApi` модулиуд хийсэн.
- [x] `AuthContext` + `ProtectedRoute`-той JWT login flow хийсэн.
- [x] Login page хийсэн (`/api/auth/login` + `/api/auth/me` холбогдсон).
- [x] Dashboard, Systems, Audit log дэлгэцүүдийг live API data руу шилжүүлсэн.
- [x] `useSystemsWithScores` hook-оор систем бүрийн security score харуулдаг болсон.

Тэмдэглэл:

- Axios биш төрөлжүүлсэн fetch wrapper ашигласан (Decision log харах).
- `mockData.ts` хэрэглэгдэхгүй болсон — устгах ажил 4.11-д орсон.
- Token одоогоор localStorage-д; 401 auto-logout, expiry шалгалт хийгдээгүй — 4.10-д орсон.

### 3.13. Banking Transfer Service

- [x] `backend/banking-transfer-service` Spring Boot project үүсгэсэн.
- [x] `V1__banking_schema.sql`: customers, accounts, transfers, ledger_entries.
- [x] `V2__seed_demo_customers.sql` demo customers/accounts seed хийсэн.
- [x] Customer, Account, Transfer, LedgerEntry entity/repository хийсэн.
- [x] `POST /api/transfers` + balance validation хийсэн.
- [x] Double-entry ledger (DEBIT + CREDIT) atomic transaction хийсэн.
- [x] PESSIMISTIC_WRITE lock-ийг account-number sorted дарааллаар авч deadlock-гүй болгосон.
- [x] Insufficient balance + rollback, same-account validation тестүүд хийсэн.
- [x] Swagger UI (springdoc) нэмсэн.
- [x] Platform каталогт V3 seed-ээр бүртгэсэн.

Тэмдэглэл:

- Service нь тусдаа Postgres (port 5433, `banking_transfer` DB) ашигладаг — docker-compose-д нэмэгдсэн (3.14 харах).
- Authentication болон idempotency key одоогоор байхгүй — 4.10-д орсон.

### 3.14. Review-ийн дараах эхний сайжруулалтууд (2026-07-06 орой)

Инфра:

- [x] docker-compose-д `banking-postgres` (5433) service, volume, healthcheck нэмсэн.
- [x] `.env.example`-д `BANKING_*` хувьсагчид нэмсэн.
- [x] `.github/workflows/ci.yml` нэмсэн: platform-api/banking `mvn test` + frontend `npm ci/lint/build` (remote push хийсний дараа идэвхжинэ).
- [x] Docker Desktop асааж `docker compose up -d` — хоёр Postgres healthy болсон.

Жинхэнэ Postgres дээрх шалгалт:

- [x] Хоёр service жинхэнэ Postgres дээр ассан, Flyway бүх migration success (platform V1-V3, banking V1-V2).
- [x] Login → systems list → security score endpoint-ууд live шалгагдсан (каталогт 3 систем, banking орсон).
- [x] E2E transfer demo: 25,000 MNT шилжүүлэг SUCCESS, DEBIT/CREDIT ledger зөв (975,000/525,000), дутуу үлдэгдэлд 400 буцаасан.
- [x] Port 8080 дээр үлдсэн хуучин dev process-ийг цэвэрлэсэн.

Security/frontend жижиг засварууд:

- [x] 401 хариунд auto-logout хийдэг болгосон (`UNAUTHORIZED_EVENT` — httpClient → AuthContext).
- [x] Token expiry-г localStorage-д хадгалж, хугацаа дууссан session-ийг ачаалахад цэвэрлэдэг болгосон.
- [x] Login form-ын admin/admin123 урьдчилсан бөглөлтийг арилгаж, `required` + хоосон үед submit идэвхгүй болгосон.
- [x] Хэрэглэгдэхгүй `mockData.ts`-г устгасан.

Backend жижиг засварууд:

- [x] Pagination clamp: page>=0, 1<=size<=100 (systems, audit-logs, transfers).
- [x] platform-api handler-т `AccessDeniedException`(403), `IllegalArgumentException`(400), generic(500+log) нэмсэн.
- [x] banking handler-т `ArithmeticException`(400), `DataIntegrityViolationException`(409), generic(500+log) нэмсэн.

### 3.15. Banking service authentication (2026-07-07)

- [x] `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` dependency нэмсэн.
- [x] `mn/golomt/banking/config/SecurityConfig.java` — platform-api-ийн HS256 JWT-г мөн `JWT_SECRET`-ээр validate хийдэг resource server тохиргоо (token олгохгүй, зөвхөн шалгана).
- [x] `/actuator/health`, `/actuator/info`, Swagger UI/api-docs нээлттэй; бусад бүх endpoint authenticated.
- [x] CORS: frontend (5173)-д Authorization + Idempotency-Key header-тэй хандалт зөвшөөрсөн (Banking Demo UI-д бэлтгэл).
- [x] `application.yml`-д `app.jwt.secret`, `app.cors.allowed-origins`; test profile-д тусдаа test secret.
- [x] Тестүүд: бүх request `jwt()` post-processor-той болсон + token-гүй хандалтад 401 буцаах 2 шинэ тест (нийт 8 ногоон).
- [x] `.env.example`-д JWT_SECRET хоёр service-д ижил байх ёстойг тайлбарласан.

### 3.16. Transfer idempotency key (2026-07-07)

- [x] `V3__add_transfer_idempotency_key.sql` — `transfers.idempotency_key varchar(80)` + unique constraint (partial index биш — H2 дэмждэггүй; nullable unique нь key-гүй transfer-үүдэд саадгүй).
- [x] `POST /api/transfers`-д нэмэлт `Idempotency-Key` header: шинэ бол 201, ижил key давтвал 200 + анхны transfer (дахин гүйцэтгэхгүй).
- [x] Зэрэгцээ давхардлыг unique constraint + `DataIntegrityViolationException` catch-ээр шийдэж ялагчийн transfer-ийг буцаадаг (service-ийн transaction хилийн гадна catch хийсэн тул rollback-only асуудалгүй).
- [x] 80-аас урт key-д 400 буцаана; давтагдсан request-ийн body анхныхтай ижил эсэхийг шалгадаггүй (хязгаарлалт гэж тэмдэглэв).
- [x] Тестүүд: ижил key ×2 → үлдэгдэл 1 удаа хасагдана, өөр key → тусдаа гүйлгээ, урт key → 400 (нийт 11 ногоон).
- [x] `banking-frontend-design-plan.md`-ийн API хүснэгт болон auth тэмдэглэлийг шинэчилсэн.

### 3.17. Banking Demo UI (2026-07-07)

- [x] `types/banking.ts`, `api/bankingHttpClient.ts` (8084 + JWT + алдааны монгол мессеж), `api/bankingApi.ts` үүсгэсэн.
- [x] 5 хуудас: `/banking` overview (2 демо дансны карт + сүүлийн 5 гүйлгээ), `/banking/accounts` данс хайх, `/banking/transfers/new` шилжүүлгийн форм + амжилтын дэлгэц, `/banking/transfers` жагсаалт (pagination-той), `/banking/transfers/:id` ledger дэлгэрэнгүй.
- [x] Компонентууд: `AccountBalanceCard`, `TransfersTable`, `LedgerEntriesTable` + `LedgerTypeChip` (DEBIT улаан −, CREDIT ногоон +).
- [x] Шилжүүлгийн форм: данс урьдчилж шалгах товч, ижил данс/дүнгийн client validation, backend алдааны message харуулах, `crypto.randomUUID()` idempotency key (нэг бөглөлтөд нэг key — давхар дарахад давхар гүйлгээ гарахгүй).
- [x] Sidebar-д «Banking Demo» nav, `formatExactMnt` мянгачилсан форматлагч, banking CSS хэсэг нэмсэн.

### 3.18. Secret fallback-уудыг арилгасан (2026-07-07)

- [x] Хоёр `application.yml`-аас `JWT_SECRET`, `POSTGRES_PASSWORD`/`BANKING_DB_PASSWORD`-ын default утгыг устгасан — env байхгүй бол startup дээр fail fast.
- [x] Host/port/DB нэр/username-ийн default-ууд хэвээр (нууц биш тул хөгжүүлэлтэд эвтэйхэн).
- [x] platform-api test profile-д тусдаа `app.jwt.secret` нэмсэн (тест env-ээс хамааралгүй, CI-д secret хэрэггүй).
- [x] `.env.example`-д бүх утга заавал гэдгийг, README-д env тохируулах зааврыг нэмсэн.

### 3.19. Banking backend — банкны pattern-ууд (2026-07-07)

Migrations (V4-V6 + seed-dev V7, H2/Postgres хоёуланд шалгагдсан):

- [x] `V4__transfer_lifecycle.sql`: status CHECK (PENDING/SUCCESS/FAILED/REVERSED), `failure_reason`, `reversal_of_transfer_id` + unique (давхар буцаалтын race backstop).
- [x] `V5__customer_username_active.sql`: `customers.username` unique (platform хэрэглэгчтэй холбоно), `active`.
- [x] `V6__account_mgmt_and_bank_audit.sql`: `bank_audit_logs` хүснэгт, `customer_no_seq`/`account_no_seq` sequences.
- [x] seed-dev `V7__seed_customer_usernames.sql`: CUST-0001→batbold, CUST-0002→sarnai; platform-api seed-dev `V5` нь batbold/sarnai VIEWER хэрэглэгчид (demo123).

Transfer lifecycle:

- [x] Бизнес дүрмийн алдаа (INSUFFICIENT_FUNDS/ACCOUNT_INACTIVE/CURRENCY_MISMATCH/LIMIT_EXCEEDED) FAILED мөр болж хадгалагдана — `@Transactional(noRollbackFor=BadRequestException)`, ledger бичилтгүй, үлдэгдэл хөндөгдөхгүй, idempotency key хадгалахгүй (retry боломжтой).
- [x] `POST /api/transfers/{id}/reversal` (ADMIN): transfer мөрийг эхэлж түгжээд хоёр дансыг sorted дарааллаар түгжинэ; зөвхөн SUCCESS буцаана (давхар → 409), хүлээн авагчийн үлдэгдэл хүрэлцэхгүй бол 400; эсрэг чиглэлийн transfer + 2 ledger entry, original → REVERSED.
- [x] `TransferResponse`-д `failureReason`, `reversalOfTransferId`, `reversedByTransferId`.

Эзэмшил ба лимит:

- [x] Non-ADMIN зөвхөн өөрийн (username таарсан) данснаас шилжүүлж, өөрийн оролцоотой гүйлгээ/дансаа харна; ADMIN = теллер бүгдийг. Идемпотент replay бусдын гүйлгээг задруулахгүй.
- [x] `app.limits.*` тохиргоо: нэг удаагийн max (default 5 сая) + өдрийн нийт зарлага (10 сая), түгжээтэй хэсэг дотор committed data-аас тооцно; REVERSED тооцогдоно, reversal өөрөө лимитэд орохгүй; injectable `Clock`.

Удирдлага ба аудит:

- [x] Customer CRUD + deactivate (ADMIN, `CUST-%04d` sequence, USERNAME_TAKEN 409).
- [x] Account нээх/блоклох/идэвхжүүлэх/хаах (ADMIN; хаахад үлдэгдэл 0, CLOSED эцсийн төлөв), `GET /api/accounts/my`, admin жагсаалт customerNo filter-тэй.
- [x] Дансны хуулга `GET /api/accounts/{no}/statement`: огнооны хүрээ, opening = одоогийн үлдэгдэл − эхлэлээс хойшхи signed нийлбэр (seed үлдэгдэл ledger-гүйг зөв тооцно), closing/totalDebit/totalCredit, counterparty-тэй хуудасласан бичилтүүд.
- [x] `bank_audit_logs` + `BankAuditService` (REQUIRES_NEW, actor snapshot JWT-ээс), 10 action, `GET /api/audit-logs` (ADMIN).
- [x] `ErrorResponse`-д `code` талбар + `ErrorCode` enum (17 код), `AccessDeniedException`→403 FORBIDDEN handler.
- [x] Тестүүд: 44 ногоон (өмнө 11) — ownership/limit/reversal/statement/account-admin/customer-admin/audit тусдаа class-ууд; хуучин insufficient-balance тест FAILED мөр + `$.code` шалгадаг болсон.
- [x] platform-api `RoleAccessTests`-ийн DB бохирдлыг зассан (security_check_results цэвэрлэдэг @Sql) — бүрэн suite 33 ногоон.

### 3.20. frontend-banking тусдаа апп + portal цэвэрлэгээ (2026-07-07)

- [x] `frontend-banking/` тусдаа Vite React 19 TS төсөл (порт 5174) — build mode switch-ийг орлосон. Login platform-api (8080) руу хэвээр (SSO), localStorage түлхүүр `banking-app-*`.
- [x] `bankingHttpClient` алдааны body-оос `code` уншиж `utils/bankingErrors.ts`-ээр монгол мессеж болгоно (`BankApiError`).
- [x] Харилцагчийн хуудсууд: Нүүр (`GET /api/accounts/my` — hardcoded demo данс устсан), Данс хайх, Хуулга (огнооны шүүлт, opening/closing/орлого/зарлага картууд, running balance хүснэгт), Гүйлгээнүүд (статус chip-тэй), Шинэ шилжүүлэг (өөрийн данснуудын dropdown, ADMIN гараар бичнэ), Гүйлгээний дэлгэрэнгүй (failureReason, буцаалтын хөндлөн холбоосууд).
- [x] Теллерийн (ADMIN) хуудсууд: Харилцагчид (хайлт/жагсаалт), Харилцагчийн форм (үүсгэх/засах/идэвхгүй болгох + данснууд), Данс удирдлага (нээх/блоклох/идэвхжүүлэх/хаах ConfirmDialog-той), Аудит лог; `AdminRoute` + sidebar-ын admin цэс role-оор нуугдана.
- [x] Гүйлгээ буцаах товч (ADMIN, зөвхөн SUCCESS) ConfirmDialog-той.
- [x] Portal (frontend/) цэвэрлэгдсэн: appMode/banking файлууд устсан, portal-only routes/nav, `dev:banking`/`build:banking` script хасагдсан. Хоёр апп build + oxlint цэвэр.

## 4. Next checklist

### 4.1. Immediate next tasks

- [x] Spring Security custom config хийх.
- [x] Default generated Spring password warning арилгах.
- [x] `PasswordEncoder` bean нэмэх.
- [x] `UserDetailsService` эсвэл auth user loading service хийх.
- [x] JWT dependency нэмэх эсэхийг шийдэх.
- [x] `POST /api/auth/login` хийх.
- [x] `GET /api/auth/me` хийх.
- [x] Auth API test нэмэх.
- [x] API error response format нэг болгох.

### 4.2. System CRUD

- [x] `SystemCreateRequest` DTO үүсгэх.
- [x] `SystemUpdateRequest` DTO үүсгэх.
- [x] `SystemResponse` DTO үүсгэх.
- [x] `SystemSearchRequest` эсвэл query params загвар гаргах.
- [x] `SystemService` үүсгэх.
- [x] `SystemController` үүсгэх.
- [x] `POST /api/systems` хийх.
- [x] `GET /api/systems` хийх.
- [x] `GET /api/systems/{id}` хийх.
- [x] `PUT /api/systems/{id}` хийх.
- [x] `DELETE /api/systems/{id}` хийх.
- [x] Delete хийхдээ hard delete биш `inUse=false` эсвэл `status=INACTIVE` болгох.
- [x] System CRUD tests нэмэх.

### 4.3. Search and filter

- [x] `keyword` search хийх.
- [x] `type` filter хийх.
- [x] `developer` filter хийх.
- [x] `inUse` filter хийх.
- [x] `status` filter хийх.
- [x] Pagination нэмэх.
- [x] Default sort `createdAt desc` болгох.
- [x] Search/filter tests нэмэх.

### 4.4. Related systems

- [x] Create/update request дээр related system list авах.
- [x] Relation handling logic хийх.
- [x] Өөрийгөө өөртэй нь холбох validation хийх.
- [x] Давхардсан relation үүсгэхгүй байх.
- [x] Detail response дээр холбоотой системүүд буцаах.

### 4.5. Security checklist

- [x] `GET /api/security-controls` хийх.
- [x] `GET /api/systems/{id}/security-checks` хийх.
- [x] `PUT /api/systems/{id}/security-checks` хийх.
- [x] `GET /api/systems/{id}/security-score` хийх.
- [x] Score calculation service хийх.
- [x] PASS/WARNING/FAIL/NOT_CHECKED weight logic test хийх.

### 4.6. Audit log

- [x] `AuditLogService` хийх.
- [x] `GET /api/audit-logs` хийх.
- [x] Login success/fail audit бичих.
- [x] System created audit бичих.
- [x] System updated audit бичих.
- [x] System disabled audit бичих.
- [x] Security check updated audit бичих.

### 4.7. Frontend

- [x] `frontend/` Vite React TypeScript project үүсгэх.
- [x] UI сангийн шийдвэр гаргах: Material UI хэрэглэхгүй, custom CSS + lucide-react (Decision log 2026-07-06).
- [x] React Router тохируулах.
- [x] API client тохируулах (Axios биш fetch wrapper — Decision log 2026-07-06).
- [x] Login page хийх.
- [x] Dashboard page хийх.
- [x] Systems list page хийх.
- [x] System create/edit form хийх.
- [x] System detail page хийх.
- [x] Security checklist tab хийх.
- [x] Audit log page хийх.

### 4.8. Banking Transfer Service

- [x] `backend/banking-transfer-service` project үүсгэх.
- [x] Banking DB schema гаргах.
- [x] Customer table үүсгэх.
- [x] Account table үүсгэх.
- [x] Transfer table үүсгэх.
- [x] Ledger entries table үүсгэх.
- [x] Demo customers/accounts seed хийх.
- [x] `POST /api/transfers` хийх.
- [x] Balance validation хийх.
- [x] Transaction rollback test хийх.
- [x] Banking service-г platform catalog дээр бүртгэх (V3 seed-ээр).
- [x] Banking transfer end-to-end demo хийх (2026-07-06: жинхэнэ Postgres дээр амжилттай, 3.14 харах).

### 4.9. Process ба инфра (2026-07-06 code review-ээс)

- [x] Git эхний commit хийх.
- [ ] Remote repository (GitHub) үүсгэж push хийх.
- [ ] Ажил бүрийн дараа commit хийх дүрмийг мөрдөх (үргэлжилж байгаа).
- [x] CI pipeline нэмэх (GitHub Actions: `mvn test` + `npm run build/lint`) — remote push хийсний дараа идэвхжинэ.
- [x] docker-compose-д banking Postgres (5433) service нэмэх.
- [x] `.env.example`-д `BANKING_DB_*` хувьсагчид нэмэх.
- [x] Docker Desktop асааж хоёр service-ийг жинхэнэ Postgres дээр бүрэн шалгах.
- [ ] Remote push хийсний дараа төслийг OneDrive-ын гаднах folder руу нүүлгэх.
- [x] Бүх service-ийг Docker-жуулсан (2026-07-07): backend 2-т multi-stage Maven→JRE Dockerfile, frontend 2-т Node build→nginx (SPA fallback-тай), docker-compose-д 4 app service нэмэгдэж `docker compose up -d --build` нэг командаар бүтэн stack асдаг болсон. Шинэ төхөөрөмжид зөвхөн Docker Desktop хэрэгтэй.

### 4.10. Security hardening (2026-07-06 code review-ээс)

- [x] `JWT_SECRET` болон DB password-ын default fallback-уудыг application.yml-ээс арилгах (env байхгүй бол fail fast).
- [x] Demo seed-ийг dev-only болгож салгасан: platform V3 нь зөвхөн admin, demo systems нь `db/seed-dev/V4`; banking demo customers нь `db/seed-dev/V2`. Prod-д `FLYWAY_LOCATIONS=classpath:db/migration` тохируулна (README харах).
- [x] Login form-ын admin/admin123 урьдчилсан бөглөлтийг арилгах.
- [x] banking-transfer-service-д authentication нэмэх (JWT resource server).
- [x] Transfer-т idempotency key нэмэх (давхар шилжүүлгээс хамгаалах).
- [x] Frontend-д 401 auto-logout + token expiry шалгалт нэмэх.
- [x] Login brute-force хамгаалалт нэмсэн: 5 удаа буруу оролдвол 15 минут түгждэг (423 Locked), in-memory тул single-node хязгаартай.
- [x] User бүртгэх API нэмсэн (2026-07-07): `POST /api/users` (ADMIN-only) — username unique + normalize, BCrypt, 8+ тэмдэгт нууц үг, `USER_CREATED` audit; teller-ийн "Шинэ харилцагч" form нэвтрэх эрхийг нэг алхамд зэрэг үүсгэдэг болсон. (Бүрэн user CRUD — жагсаалт/засах/идэвхгүй болгох — ирээдүйд.)

### 4.11. Frontend үндсэн функц (даалгаврын шаардлага)

- [x] System create/edit form хийх.
- [x] System detail page хийх.
- [x] Security checklist tab хийх (даалгаврын гол шаардлага — үр дүн/нотолгоо засварлаж хадгалдаг, score panel-тай).
- [x] Pagination UI нэмэх (Systems/Audit хоёулаа size=20, хуудас шилжих товчтой).
- [x] Systems хайлтад debounce нэмэх (300ms).
- [x] Security score-г batch endpoint-оор авах (`GET /api/security-scores` нэмж N+1 арилгасан).
- [x] Хэрэглэгдэхгүй болсон `mockData.ts`-г устгах.
- [x] Ажиллагаагүй товчнуудыг холбох эсвэл нуух (Систем нэмэх/View/Edit/Disable холбогдсон; орчин filter, bell, help нуугдсан; audit үйлдлийн filter ажилладаг болсон).

### 4.12. Код чанар ба тест (2026-07-06 code review-ээс)

- [x] RBAC deny тохиолдлын тестүүд нэмсэн (`RoleAccessTests`: jwt() post-processor-оор VIEWER/SECURITY 403/200 — 7 тест).
- [ ] Frontend-д Vitest + Testing Library суурилуулж эхний тестүүд бичих.
- [ ] Banking concurrency automated тест (Testcontainers) нэмэх — 2026-07-07-нд жинхэнэ Postgres дээр 12 зэрэгцээ гүйлгээний live burst-ээр lock баталгаажсан (Verification log).
- [x] Systems list-ийн relation N+1-ийг batch query болгосон (`findBySourceSystemIdIn` + groupingBy).
- [x] Transfers list-ийн ledger N+1-ийг fetch join болгосон (`findAllWithAccounts` + `findByTransferIdIn` join fetch).
- [x] Pagination `size`-д дээд хязгаар тавих (clamp: 1-100).
- [x] GlobalExceptionHandler-т `AccessDeniedException`/`IllegalArgumentException`/generic handler нэмэх (banking-д мөн 409/ArithmeticException).
- [x] `TransferStatus` бүрэн lifecycle болсон: PENDING/SUCCESS/FAILED/REVERSED (3.19 харах; өмнөх SUCCESS-only шийдвэрийг 2026-07-07-ны "банк шиг болгох" хүсэлтээр өөрчилсөн).
- [ ] frontend-banking-д Vitest тестүүд нэмэх (portal-тай хамт).
- [ ] frontend/frontend-banking хоёрын давхардсан код (styles.css, auth/, httpClient)-ыг shared package болгох — одоогоор давхардал зөвшөөрөгдсөн (Decision log 2026-07-07).

### 4.13. Хяналтын функцийг бодит болгох (deposit-service нэмэгдэхийн бэлтгэл, 2026-07-08 судалгаа)

Судалгаа: `docs/registry-monitoring-research.md`. Гол олдвор: `health_url` хэзээ ч дуудагддаггүй,
`status` нь гар мэдүүлэг тул «хянах» хэсэг одоогоор нэрийн төдий; шинэ хадгаламжийн сервисийг
бүртгэхэд кодын өөрчлөлт хэрэггүй ч бодитоор хянахын тулд доорх засварууд хэрэгтэй.

R1 — On-demand health check (эхэлж хийх, banking дээр туршина):

- [ ] `POST /api/systems/{id}/health-check` endpoint (ADMIN/SECURITY) — health_url руу server-side GET, actuator `status` уншина.
- [ ] SSRF хамгаалалт: http/https scheme л, timeout 2-3с, redirect дагахгүй, `app.health.allowed-hosts` allowlist (default localhost).
- [ ] `HEALTH_CHECKED` audit action нэмэх.
- [ ] Portal: System detail дээр «Шалгах» товч + үр дүнгийн chip, Systems list дээр runtime chip.
- [ ] Тестүүд: MockRestServiceServer-ээр UP/DOWN/timeout/буруу JSON; RBAC (VIEWER 403).

R2 — Статусын загварыг салгах:

- [ ] Flyway V6 (platform): `systems.runtime_status` (UP/DOWN/UNKNOWN), `last_health_check_at`, `last_health_error`.
- [ ] `status`-ыг бүртгэлийн төлөв болгож формоос DOWN/UNKNOWN сонголтыг хасах; runtime_status-ыг зөвхөн health check бичнэ.
- [ ] Dashboard: lifecycle ба runtime (UP/DOWN/UNKNOWN) тусдаа тоолол.

R3 — Тогтмол хяналт + түүх:

- [ ] `@Scheduled` poller (`app.health.poll-interval`, default 60с, 0=унтраах; тестэд унтраана) — in_use=true, health_url-тай системүүд.
- [ ] `system_health_checks` түүхийн хүснэгт (DBRD §11) + detail хуудсанд сүүлийн шалгалтууд.
- [ ] UP→DOWN, DOWN→UP шилжилтэд audit бичих.

R4 — Automated security checks (сонголттой):

- [ ] `automated=true` control-уудыг health check-тэй хамт автоматаар үнэлэх (HTTPS scheme, health хүрэгдэх, swagger хамгаалалт); гар үр дүнг дарж бичихгүй.

Deposit-service онбординг (registry талаас, кодгүй — сервис бэлэн болсны дараа):

- [ ] Каталогт бүртгэх: нэр/type CORE/base-health-swagger URL (8085)/хөгжүүлэгч/үнэлгээ/inUse.
- [ ] Relation: Deposit Service `CALLS` Banking Transfer Service.
- [ ] Security checklist бөглөж score авах; аудит бичлэгүүд үүссэнийг нягтлах.
- [ ] Health check ногоон эсэхийг R1/R3-аар шалгах.

### 4.14. Deposit Service — хугацаатай хадгаламжийн 3 дахь microservice (2026-07-08 төлөвлөгөө)

Гол шийдвэрүүд: banking-ийг ЖИНХЭНЭ HTTP-ээр дууддаг (registry-ийн CALLS бодит болно); settlement данс 900000001-ийн эзэн нь `svc-deposit` (VIEWER!) тул banking-ийн эзэмшлийн дүрэм эргэн төлөлтийг өөрөө зөвшөөрнө; нээхдээ хэрэглэгчийн JWT-г дамжуулна; хүү = principal × rate × termDays / (365×100), эрт хаавал 0; статус: FUNDING→OPEN→PAYOUT_PENDING→CLOSED/CLOSED_EARLY, FUNDING→CANCELLED.

- [x] Д1: Skeleton — pom (banking-ийн хуулбар), SecurityConfig (ижил JWT_SECRET resource server), common (AuthFacade + `bearerToken()`, ErrorCode 17, GlobalExceptionHandler + 502), application.yml (8085, DB 5434, app.deposit/banking/platform блок), V1__deposit_schema.sql (deposits + deposit_no_seq 5001 + deposit_audit_logs), 3 тест ногоон.
- [x] Д2: Seeds — banking V8 settlement (CUST-9000/900000001/100М), platform V6 (svc-deposit VIEWER + systems мөр + deposit→banking CALLS, digital→deposit CALLS).
- [x] Д3: Domain — Deposit entity/repository (nextval, findByIdForUpdate lock), DepositProperties (3/6/12 сар, min/max), InterestCalculator (365 хоногийн суурь, HALF_UP), GET /api/deposit-products (9 тест ногоон).
- [x] Д4: BankingClient (RestClient, timeout, `{code,message}`→BankingApiException, 401→Unauthorized, IO→Unavailable) + PlatformAuthClient + ServiceTokenProvider (кэш+60с skew+invalidate); MockRestServiceServer-ээр 6 тест.
- [x] Д5: Нээх урсгал — POST /api/deposits (client Idempotency-Key replay 200, `dep-{no}-fund`, хэрэглэгчийн token дамжуулна), retry-funding, /my, /{id} (owner|ADMIN), admin list, deposit audit (REQUIRES_NEW). Business error → CANCELLED + code pass-through, банк унтарсан → FUNDING хэвээр + 502. 8 шинэ тест (нийт 23).
- [x] Д6: Хаах — POST /api/deposits/{id}/close: prepareClose (PESSIMISTIC lock, дүн/close_type-ийг банк дуудахаас ӨМНӨ хадгална), payout нь svc-deposit token-оор (`dep-{no}-payout`), 401-д invalidate+retry once. Matured→CLOSED+хүү, эрт→CLOSED_EARLY+хүү 0, банк унтарсан→PAYOUT_PENDING+502→retry ижил дүн, давхар хаах 409, өөр хэрэглэгч 403. 6 шинэ тест (нийт 29).
- [x] Д7: Frontend — types/deposit, depositHttpClient (VITE_DEPOSIT_API_URL:8085, DepositApiError), depositErrors (pass-through нь bankErrorMessage-руу fallback), depositApi (8 функц), format+Chips (DepositStatusChip), nav «Хадгаламж»+admin «Хадгаламжийн бүртгэл», DepositsPage (бүтээгдэхүүний картууд + миний хадгаламжууд), /deposits route. tsc+lint+build цэвэр.
- [ ] Д8: Frontend — NewDepositPage (урьдчилсан хүү), DepositDetailPage (хаах/retry товч), DepositsAdminPage.
- [ ] Д9: Infra — Dockerfile, compose (deposit-postgres 5434 + deposit-service 8085 + VITE_DEPOSIT_API_URL), .env.example, CI (deposit + frontend-banking job).
- [ ] Д10: Docs — deposit-api-contract.md, DBRD, README.
- [ ] Д11: E2E demo (нээх→үлдэгдэл буурах→эрт/хугацаатай хаах→502 сэргэлт→registry CALLS) + Verification log.

## 5. Blocked list

| Огноо | Асуудал | Нөлөө | Дараагийн алхам |
| --- | --- | --- | --- |
| 2026-07-06 | Docker Desktop асаагүй | Хоёр service-ийн migration/lock жинхэнэ Postgres дээр шалгагдаагүй | Шийдэгдсэн (2026-07-06 шөнө): Docker ассан, 2 Postgres compose-оор healthy, хоёр service жинхэнэ PG дээр UP, e2e transfer demo амжилттай |

## 6. Decision log

| Огноо | Шийдвэр | Шалтгаан |
| --- | --- | --- |
| 2026-07-06 | Spring Boot `3.5.16` ашиглах | JDK 17-тэй тогтвортой, Maven Central дээр artifact байгаа |
| 2026-07-06 | Test profile дээр H2 ашиглах | Docker Desktop унтраалттай үед migration/JPA mapping шалгах боломжтой |
| 2026-07-06 | Admin password-г BCrypt hash болгож seed хийх | Plain text password хадгалахгүй байх security rule |
| 2026-07-06 | `system_relations` self-referencing table ашиглах | PDF-ийн “Холбоотой системүүд” шаардлагыг зөв relational model болгох |
| 2026-07-06 | `audit_logs.target_id` foreign key биш polymorphic reference байхаар үлдээх | Audit log олон төрлийн entity дээр ашиглагдана |
| 2026-07-06 | JWT Bearer auth ашиглах | Frontend/API хооронд stateless authentication хийхэд тохиромжтой |
| 2026-07-06 | Spring OAuth2 Resource Server JWT decoder ашиглах | Protected endpoint дээр Bearer token validate хийх стандарт Spring Security flow |
| 2026-07-06 | System delete-г soft disable болгох | Бүртгэлийн түүх, relation/security data алдагдуулахгүй |
| 2026-07-06 | System relation logic-г эхний хувилбарт `SystemService` дотор хийх | Scope жижиг байлгаж, CRUD flow хурдан дуусгах |
| 2026-07-06 | Security score-д `PASS=full`, `WARNING=half`, `FAIL/NOT_CHECKED=0` weight ашиглах | Checklist-ийн partial compliance-ийг тооцоолох энгийн, тайлбарлахад ойлгомжтой дүрэм |
| 2026-07-06 | Frontend-д Axios биш төрөлжүүлсэн fetch wrapper ашиглах | Гадны dependency багасгаж, жижиг client хангалттай |
| 2026-07-06 | Material UI хэрэглэхгүй custom CSS + lucide-react ашиглах | Screenshot reference дизайнтай яг нийцүүлэхэд шууд CSS илүү уян хатан |
| 2026-07-06 | Banking service тусдаа Postgres (5433, `banking_transfer`) ашиглах | Service тус бүр өөрийн DB-тэй байх зарчим |
| 2026-07-06 | Бүх ажлыг логик бүлгээр commit хийж git түүхийг эхлүүлэх | Commit-гүй ажиллах эрсдэлийг зогсоох; цаашид task бүрт commit хийнэ |
| 2026-07-07 | Banking auth-д тусдаа login биш platform-api-ийн JWT-г shared `JWT_SECRET`-ээр validate хийх | Нэг нэвтрэлтээр хоёр service ашиглах боломж; банк token олгохгүй тул хамгийн бага код |
| 2026-07-07 | Idempotency-г тусдаа хүснэгт биш `transfers.idempotency_key` unique багана + catch-and-refetch-ээр шийдэх | Демо scope-д хангалттай; H2/Postgres хоёуланд ажилладаг хамгийн энгийн найдвартай хэлбэр |
| 2026-07-07 | Demo seed-ийг `db/seed-dev` Flyway location-д салгаж, default-оор оруулж, prod-д `FLYWAY_LOCATIONS` env-ээр хасах | Dev/test UX хэвээр, prod clean; profile файл нэмэхгүй env-first загвартай нийцнэ |
| 2026-07-07 | Login lockout-ийг in-memory (5 fail / 15 мин, 423 Locked) хийх | Нэг node-той demo-д хангалттай; scale-out үед shared store руу шилжүүлэхээр тэмдэглэсэн |
| 2026-07-07 | `TransferStatus`-ийг SUCCESS-only хэвээр үлдээх | Татгалзсан гүйлгээ HTTP error-оор буцдаг, rollback хийгддэг тул demo scope-д PENDING/FAILED төлөв шаардлагагүй; async flow нэмэгдвэл эргэж харна |
| 2026-07-07 | (Дээрх шийдвэрийг өөрчилсөн) `TransferStatus` бүрэн lifecycle: FAILED мөрийг `noRollbackFor`-оор хадгална | "Банк шиг" болгох шаардлагаар татгалзсан оролдлого аудитлагдах ёстой; validation үлдэгдэл хөндөхөөс өмнө тул commit аюулгүй. Idempotency key FAILED мөрөнд хадгалагдахгүй — key нь at-most-once SUCCESS баталгаа |
| 2026-07-07 | PENDING enum/CHECK-д үлдэнэ гэхдээ sync урсгал хэзээ ч хадгалахгүй | Ирээдүйн async flow-д хямд forward-compat; хуурамч төлөвийн машин demo-д утгагүй |
| 2026-07-07 | Эзэмшлийг `customers.username` (nullable unique) ↔ JWT subject-ээр холбох | Cross-DB FK, шинэ token claim хэрэггүй хамгийн бага өөрчлөлт; VIEWER/SECURITY = харилцагч, ADMIN = теллер |
| 2026-07-07 | Эрхийн hybrid загвар: цэвэр role хаалт `@PreAuthorize`, өгөгдлөөс хамаарах шалгалт service доторх `AuthFacade` | Role хаалт declarative/тесттэй; эзэмшлийн шалгалтад ачаалагдсан entity хэрэгтэй тул service давхаргад зөв |
| 2026-07-07 | Reversal идемпотент: transfer мөрийг PESSIMISTIC_WRITE-ээр эхэлж түгжиж status шалгах + `ux_transfers_reversal_of` unique backstop | Зэрэгцээ давхар буцаалт транзакцын түвшинд цуваарладаг; unique constraint нь race-ийн эцсийн хамгаалалт |
| 2026-07-07 | Өдрийн лимитийг from-account түгжээтэй хэсэгт committed data-аас тооцох; SUCCESS+REVERSED тооцно, reversal transfer-ийг хасна | Түгжээ зэрэгцээ зарлагыг цуваарладаг тул race-гүй; буцаалт лимит тэглэдэг цоорхойг хаана |
| 2026-07-07 | Хуулгын opening balance = одоогийн үлдэгдэл − эхлэлээс хойшхи signed нийлбэр | Seed-ээр орсон үлдэгдэл ledger бичилтгүй тул зөвхөн энэ томьёо бүх тохиолдолд зөв |
| 2026-07-07 | Bank audit-ыг REQUIRES_NEW + JWT actor snapshot-оор (user FK-гүй) хийх | Хэрэглэгчид platform DB-д тул FK боломжгүй; FAILED гүйлгээний аудит exception-оос үл хамааран хадгалагдана |
| 2026-07-07 | Banking UI-г нэг кодын сангийн build mode биш тусдаа `frontend-banking/` төсөл болгох | Хэрэглэгчийн сонголт: жинхэнэ тусдаа систем шиг байх; давхардсан styles/auth-ыг shared package болгох ажил 4.12-т нэмэгдсэн |
| 2026-07-08 | deposit-service: тусдаа Postgres 5434, customers хүснэгт ДАВХАРДУУЛАХГҮЙ — deposits мөрөнд customer_username + linked_account_no snapshot | Database-per-service хадгалагдана; cross-DB FK боломжгүй тул banking-тай ижил soft-link загвар |
| 2026-07-08 | deposit-service нь banking-ийг ЖИНХЭНЭ HTTP-ээр (Spring RestClient) дуудна; нээхэд хэрэглэгчийн JWT дамжуулж, эргэн төлөлтөд svc-deposit-ийн token ашиглана | Registry-ийн CALLS хамаарал бодит болно; хэрэглэгчийн token дамжуулснаар banking-ийн эзэмшлийн дүрэм санхүүжилтийн зөвшөөрлийг өөрөө шалгана |
| 2026-07-08 | svc-deposit нь ADMIN биш VIEWER — settlement данс 900000001-ийн эзэн (customers.username='svc-deposit') тул banking-ийн өөрийн-данс дүрмээр эргэн төлөлт хийж чадна | Least privilege: service account гүйлгээ буцаах/харилцагч удирдах эрхгүй |
| 2026-07-08 | deposit-service-д global mvn (wrapper-гүй), flyway locations зөвхөн db/migration (seed-dev хавтасгүй) | Banking-тай ижил хэв маяг; deposit-д demo мөр шаардлагагүй |

## 7. Verification log

| Огноо | Command | Үр дүн | Тэмдэглэл |
| --- | --- | --- | --- |
| 2026-07-06 | `mvn -q -DskipTests package` | PASS | Spring Boot skeleton compile/package амжилттай |
| 2026-07-06 | `mvn test` | PASS | H2 test profile дээр Flyway 3 migration амжилттай |
| 2026-07-06 | `mvn -q test` | PASS | Entity/repository mapping нэмсний дараа schema validate амжилттай |
| 2026-07-06 | `docker compose up -d postgres` | FAIL | Docker Desktop daemon ажиллаагүй |
| 2026-07-06 | `mvn -q test` | PASS | DBRD docs нэмсний дараа backend хэвээр ногоон |
| 2026-07-06 | `mvn test` | PASS | Auth API нэмсний дараа 5 test pass: login, invalid password, protected `/me`, JWT current user |
| 2026-07-06 | `mvn -q test` | PASS | System CRUD/list/search/relation нэмсний дараа 12 test pass |
| 2026-07-06 | `mvn -q test` | PASS | Security checklist API нэмсний дараа 19 test pass |
| 2026-07-06 | `mvn -q test` | PASS | Audit log API нэмсний дараа 22 test pass |
| 2026-07-06 | `npm.cmd run build` | PASS | Frontend mock UI skeleton build амжилттай |
| 2026-07-06 | `npm.cmd run lint` | PASS | Frontend TSX/CSS lint алдаагүй |
| 2026-07-06 | `npx.cmd playwright screenshot` | PASS | Dashboard, Systems, Audit log route-ууд screenshot-оор visual check хийсэн |
| 2026-07-06 | `mvn -q test` (platform-api) | PASS | Эхний commit-ийн өмнөх шалгалт: 22 тест ногоон (H2) |
| 2026-07-06 | `mvn -q test` (banking-transfer-service) | PASS | Эхний commit-ийн өмнөх шалгалт: 6 тест ногоон (H2) |
| 2026-07-06 | `npm run build` + `npm run lint` | PASS | Vite production build 259KB bundle, oxlint алдаагүй |
| 2026-07-06 | `mvn -q test` x2 (сайжруулалтын дараа) | PASS | Exception handler + clamp өөрчлөлтийн дараа 22+6 тест ногоон |
| 2026-07-06 | `npx tsc -b` + build + lint (сайжруулалтын дараа) | PASS | 401/expiry/login өөрчлөлт, mockData устгалын дараа цэвэр |
| 2026-07-06 | `docker compose up -d` | PASS | 2 Postgres container healthy (5432, 5433) |
| 2026-07-06 | Хоёр service жинхэнэ Postgres дээр асаах | PASS | Flyway бүх migration success, health UP |
| 2026-07-06 | E2E transfer demo (curl) | PASS | 25,000 MNT SUCCESS + ledger зөв; insufficient balance 400; login/systems/score OK |
| 2026-07-07 | `mvn test` (banking, auth нэмсний дараа) | PASS | 8 тест ногоон: хуучин 6 нь jwt()-тэй, шинэ 401 тест 2 |
| 2026-07-07 | `mvn test` (banking, idempotency нэмсний дараа) | PASS | 11 тест ногоон: replay 200/нэг удаа гүйцэтгэл, өөр key тусдаа, урт key 400 |
| 2026-07-07 | `mvn test` ×2 (fallback устгасны дараа, env-гүй) | PASS | 22+11 тест ногоон — test profile өөрийн secret-тэй, CI-д env хэрэггүй |
| 2026-07-07 | `spring-boot:run` env-гүйгээр (platform-api) | PASS (fail-fast) | 3.6 сек дотор exit 1 — DB password placeholder шийдэгдэхгүй тул холбогдож чадахгүй, JWT secret нь placeholder exception шиддэг |
| 2026-07-07 | `mvn -q test` (batch scores endpoint нэмсний дараа) | PASS | platform-api 24 тест ногоон (шинэ 2 batch тест орсон) |
| 2026-07-07 | `npx tsc -b` + `npm run lint` (form/detail/pagination нэмсний дараа) | PASS | TypeScript, oxlint цэвэр |
| 2026-07-07 | 3 service асааж playwright authenticated screenshot | PASS | Login, Systems, Detail, Create form хуудсууд live баталгаажсан |
| 2026-07-07 | `npx tsc -b` + `npm run build` + `npm run lint` (Banking Demo UI нэмсний дараа) | PASS | 1816 модуль, TypeScript/oxlint цэвэр |
| 2026-07-07 | `mvn -q test` ×2 (seed салгалт, N+1, lockout, RBAC-ийн дараа) | PASS | platform-api 33 тест (RBAC 7 + lockout 2 шинэ), banking 11 тест ногоон |
| 2026-07-07 | Fresh Postgres (compose down -v) дээр Flyway | PASS | platform V1-V4 (V3=admin, V4=demo seed-dev), banking V1-V3 бүгд success |
| 2026-07-07 | Concurrency burst: 12 зэрэгцээ 100,000₮ гүйлгээ (үлдэгдэл 10-д л хүрэлцэнэ) | PASS | Яг 10 нь 201, 2 нь 400 Insufficient; үлдэгдэл 0/1,500,000 — overdraw-гүй, мөнгө алдагдаагүй |
| 2026-07-07 | `mvn test` (banking, lifecycle/reversal/statement/ownership/limits/mgmt/audit нэмсний дараа) | PASS | 44 тест ногоон (өмнө 11): 9 test class, шинэ 7 class |
| 2026-07-07 | `mvn test` (platform-api, demo user seed + RoleAccessTests засварын дараа) | PASS | 33 тест ногоон; өмнө нь RoleAccessTests-ийн DB бохирдол SecurityCheckControllerTests-ийг унагаж байсныг зассан |
| 2026-07-07 | `npm run build` + `npm run lint` (frontend-banking шинэ төсөл) | PASS | 1811 модуль, 287KB bundle, oxlint цэвэр |
| 2026-07-07 | `npm run build` + `npm run lint` (portal, banking цэвэрлэгээний дараа) | PASS | 1805 модуль, oxlint цэвэр; grep-ээр banking үлдэгдэл 2 (placeholder текст + CSS коммент) — хор хөнөөлгүй |
| 2026-07-07 | Fresh Postgres (`compose down -v`) + хоёр service асаалт | PASS | Banking Flyway V1→V7, platform V1→V5 цэвэр; хуучин порт эзэлсэн dev process-уудыг (8080/8084/5173/5174) цэвэрлэсэн |
| 2026-07-07 | E2E API suite (22 шалгалт, жинхэнэ Postgres) | PASS | batbold: өөрийн данс/шилжүүлэг/идемпотент replay/хуулга opening-closing; 403 FORBIDDEN_ACCOUNT (бусдын данс/хуулга), 400 LIMIT_EXCEEDED + FAILED мөр admin-д харагдана; reversal → балансууд сэргэсэн, давхар → 409; admin данс нээх/блоклох (400 ACCOUNT_INACTIVE)/unblock/close (400 ACCOUNT_NOT_EMPTY); audit 8+ бичилт, харилцагчид 403 |
| 2026-07-07 | Playwright screenshot (5174 banking, 5173 portal) | PASS | batbold нүүр (зөвхөн өөрийн данс, admin цэсгүй), хуулга (opening/orlogo/zarlaga/closing зөв), теллерийн Данс удирдлага (3 данс + үйлдлүүд), portal dashboard banking-гүй хэвийн |
| 2026-07-07 | `docker compose build` (4 image) | PASS | platform-api, banking, frontend ×2 — multi-stage build бүгд амжилттай |
| 2026-07-07 | Docker бүтэн stack smoke (6 container) | PASS | Health ×2, frontend ×2 (SPA deep-link 200), admin+batbold login, 3 систем seed-тэй, 5000₮ transfer SUCCESS + idempotency replay ижил ref, playwright screenshot nginx build зөв |
| 2026-07-07 | `mvn -q test` (user бүртгэх API нэмсний дараа) | PASS | platform-api 37 тест (шинэ 4: үүсгээд нэвтрэх, давхардал 400, богино нууц үг 400, VIEWER 403) |
| 2026-07-07 | User бүртгэлийн e2e (Docker stack) | PASS | POST /api/users (khulan) → CUST-1003 → данс 100000103 → khulan нэвтэрч өөрийн данс харав; audit-д USER_CREATED; давхардсан username 400; teller form screenshot зөв |
| 2026-07-08 | `mvn test` (deposit-service skeleton) | PASS | 3 тест ногоон: context, health public, token-гүй 401; Flyway V1 (deposits + deposit_audit_logs + deposit_no_seq) H2 дээр амжилттай |
| 2026-07-08 | `mvn test` ×2 (settlement V8 + platform V6 seed нэмсний дараа) | PASS | platform 37, banking 44 тест ногоон — шинэ seed-үүд H2 дээр цэвэр ачаалагдана |
| 2026-07-08 | `mvn test` (deposit domain нэмсний дараа) | PASS | 9 тест: хүүгийн 4 жишээ (бүтэн жил 125,000 / 92 хоног 20,164.38 / HALF_UP / өндөр жил 366 хоног), products 200+401 |
| 2026-07-08 | `mvn test` (deposit HTTP client нэмсний дараа) | PASS | 15 тест: transfer 201/200 амжилт + header, INSUFFICIENT_FUNDS code mapping, 401 Unauthorized; token кэш нэг login, invalidate шинэ login |
| 2026-07-08 | `mvn test` (deposit нээх урсгал нэмсний дараа) | PASS | 23 тест: нээх→OPEN (from=account/to=settlement/key/token баталгаажсан), INSUFFICIENT_FUNDS→CANCELLED, банк унтарсан→FUNDING+502→retry OPEN, client key replay 200 нэг л transfer, PRODUCT_NOT_FOUND/AMOUNT_OUT_OF_RANGE 400, эзэмшил 403, admin list, audit DEPOSIT_OPENED |
| 2026-07-08 | `mvn test` (deposit хаах урсгал нэмсний дараа) | PASS | 29 тест: matured→CLOSED хүү 125,000 (settlement→данс, svc token, key баталгаажсан), эрт→CLOSED_EARLY хүү 0, 502→PAYOUT_PENDING→retry CLOSED ижил дүн, 401→invalidate+retry, давхар 409, өөр хэрэглэгч 403 (SqlMergeMode.MERGE-ээр seed цэвэрлэгээ засав) |
| 2026-07-08 | `npx tsc -b` + `npm run lint` + `npm run build` (deposit frontend API+DepositsPage) | PASS | frontend-banking 1816 модуль, oxlint цэвэр |

## 8. Files created so far

Root:

```text
README.md
.gitignore
.env.example
docker-compose.yml
```

Docs:

```text
docs/assignment-summary.md
docs/architecture.md
docs/api-contract.md
docs/database.md
docs/database-relations-and-dbrd.md
docs/ai-rules.md
docs/development-checklist.md
docs/frontend-design-plan.md
```

Frontend:

```text
frontend/package.json
frontend/src/App.tsx
frontend/src/main.tsx
frontend/src/styles.css
frontend/src/data/mockData.ts
frontend/src/components/AppShell.tsx
frontend/src/components/PageHeader.tsx
frontend/src/components/Chips.tsx
frontend/src/components/MetricCard.tsx
frontend/src/pages/DashboardPage.tsx
frontend/src/pages/SystemsPage.tsx
frontend/src/pages/AuditLogPage.tsx
```

Backend:

```text
backend/platform-api/pom.xml
backend/platform-api/src/main/resources/application.yml
backend/platform-api/src/main/resources/db/migration/V1__init_schema.sql
backend/platform-api/src/main/resources/db/migration/V2__seed_security_controls.sql
backend/platform-api/src/main/resources/db/migration/V3__seed_demo_data.sql
backend/platform-api/src/test/resources/application-test.yml
```

Main Java packages:

```text
mn.golomt.registry.users
mn.golomt.registry.systems
mn.golomt.registry.relations
mn.golomt.registry.securitycheck
mn.golomt.registry.audit
mn.golomt.registry.auth
mn.golomt.registry.common
mn.golomt.registry.config
mn.golomt.registry.systems.dto
mn.golomt.registry.securitycheck.dto
```

Audit files:

```text
backend/platform-api/src/main/java/mn/golomt/registry/audit/AuditAction.java
backend/platform-api/src/main/java/mn/golomt/registry/audit/AuditLogController.java
backend/platform-api/src/main/java/mn/golomt/registry/audit/AuditLogService.java
backend/platform-api/src/main/java/mn/golomt/registry/audit/dto/AuditLogResponse.java
backend/platform-api/src/test/java/mn/golomt/registry/audit/AuditLogControllerTests.java
```

2026-07-06 нэмэлт (эхний commit-оос хойш файлын бүрэн жагсаалтын эх сурвалж нь git түүх болно):

Banking Transfer Service:

```text
backend/banking-transfer-service/pom.xml
backend/banking-transfer-service/src/main/resources/application.yml
backend/banking-transfer-service/src/main/resources/db/migration/V1__banking_schema.sql
backend/banking-transfer-service/src/main/resources/db/migration/V2__seed_demo_customers.sql
mn.golomt.banking.customer / account / transfer / ledger / common packages
backend/banking-transfer-service/src/test/java/mn/golomt/banking/transfer/TransferControllerTests.java
```

Frontend API integration нэмэлт:

```text
frontend/src/api/httpClient.ts, authApi.ts, systemsApi.ts, securityApi.ts, auditApi.ts
frontend/src/auth/AuthContext.tsx, authContextValue.ts, useAuth.ts
frontend/src/components/ProtectedRoute.tsx, States.tsx
frontend/src/hooks/useSystemsWithScores.ts
frontend/src/pages/LoginPage.tsx
frontend/src/types/api.ts
frontend/src/utils/format.ts
```

## 9. Next working note template

Дараагийн ажил бүрийн дараа доорх загвараар тэмдэглэл нэмнэ.

```text
Огноо:
Task:
Өөрчилсөн файлууд:
Юу хийсэн:
Яаж шалгасан:
Үр дүн:
Дараагийн алхам:
```
