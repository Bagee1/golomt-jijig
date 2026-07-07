# Шаардлагын шинжилгээний баримт (Requirements Analysis Document)

| Талбар                        | Утга                                                                                                                                                                                                 |
| ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Төсөл                          | System Registry & Security Compliance Platform (`service-registry-platform`)                                                                                                                           |
| Хувилбар                    | 1.0                                                                                                                                                                                                      |
| Огноо                          | 2026-07-08                                                                                                                                                                                               |
| Эх даалгавар             | Голомт банк, МТГ — Програм хөгжүүлэлтийн хэлтэс, «Системийн бүртгэл хийх» (`анхны даалгавар/Даалгавар5.14_1.pdf`) |
| Боловсруулсан арга | Анхны даалгавар +`docs/` баримтууд + бодит кодын шинжилгээг нэгтгэв                                                                                  |

Энэ баримт нь **юу шаардлагатай байсан**, **түүнийг ямар дүрмээр хэрэгжүүлсэн**, **хэрхэн баталгаажуулсан** гэсэн гурван асуултад кодоор нотлогдсон, нэгдсэн хариулт өгнө. Кодоос шууд гаргасан утгууд (лимит, томьёо, төлөвийн шилжилт гэх мэт) файлын замтай хамт өгөгдсөн тул хамгаалалт/танилцуулгад шууд иш татаж болно.

---

## 1. Танилцуулга

### 1.1. Зорилго

Анхны даалгаврын шаардлага болон түүнийг өргөтгөсөн хэрэгжилтийн хооронд бүрэн мөрдөлт (traceability) тогтоож, функциональ, функциональ бус, өгөгдлийн болон интерфэйсийн шаардлагуудыг нэг баримтад системтэйгээр тодорхойлох.

### 1.2. Хамрах хүрээ

Платформ нь дараах 4 бүрэлдэхүүнтэй:

| Бүрэлдэхүүн               | Порт | Үүрэг                                                                                                                          |
| ------------------------------------ | -------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `backend/platform-api`             | 8080     | Нэвтрэлт (JWT олгогч), системийн бүртгэл, аюулгүй байдлын checklist, аудит         |
| `backend/banking-transfer-service` | 8084     | Банкны шилжүүлгийн демо сервис (каталогт бүртгэгдсэн «жинхэнэ» систем) |
| `frontend/` (Registry Portal)      | 5173     | Бүртгэлийн портал SPA                                                                                               |
| `frontend-banking/` (Banking App)  | 5174     | Банкны бие даасан SPA (харилцагч + теллер)                                                            |

Өгөгдлийн сан: PostgreSQL 16 хоёр тусдаа instance (5432 — `service_registry`, 5433 — `banking_transfer`), схемийг Flyway удирдана.

### 1.3. Нэр томьёо

| Нэр томьёо   | Тайлбар                                                                                                                             |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Систем (System) | Байгууллагын каталогт бүртгэгдэх програм хангамжийн систем                            |
| Control               | Аюулгүй байдлын стандартын нэг шалгах зүйл (жин бүхий)                                        |
| Теллер          | `ADMIN` эрхтэй хэрэглэгч банкны апп дээр back-office үүрэг гүйцэтгэнэ                         |
| Харилцагч    | `ADMIN` биш хэрэглэгч; `customers.username` ↔ JWT subject-ээр банкны харилцагчид холбогдоно |
| Ledger                | Давхар бичилтийн (DEBIT/CREDIT) гүйлгээний бүртгэл                                                         |
| Idempotency-Key       | Нэг шилжүүлгийг давхардуулахгүй байлгах header түлхүүр                                          |

### 1.4. Эх сурвалж баримтууд

- `анхны даалгавар/Даалгавар5.14_1.pdf` — анхны шаардлага
- `төслийн concept/даалгаврын нийцлийн үнэлгээ.md` — шаардлагын анхны mapping
- `docs/assignment-summary.md`, `docs/architecture.md`
- `docs/api-contract.md`, `docs/banking-api-contract.md` — интерфэйсийн гэрээ
- `docs/database-relations-and-dbrd.md` — өгөгдлийн сангийн DBRD
- `docs/development-checklist.md` — хэрэгжилтийн явц, шийдвэрийн бүртгэл, баталгаажуулалтын лог
- Бодит код: Java/TypeScript эх кодоос гаргасан утгууд (файл замууд текст дотор)

---

## 2. Системийн ерөнхий тодорхойлолт

### 2.1. Архитектур

```text
                    ┌─────────────────────┐      ┌──────────────────────┐
                    │ frontend (5173)     │      │ frontend-banking     │
                    │ Registry Portal     │      │ (5174) Banking App   │
                    └─────────┬───────────┘      └────┬──────────┬──────┘
                              │ REST + JWT      login │          │ REST + JWT
                              ▼                       ▼          ▼
                    ┌─────────────────────┐      ┌──────────────────────┐
                    │ platform-api (8080) │      │ banking-transfer-    │
                    │ JWT олгогч          │      │ service (8084)       │
                    │ Systems/Security/   │      │ JWT шалгагч (resource│
                    │ Audit               │      │ server, олгодоггүй)  │
                    └─────────┬───────────┘      └──────────┬───────────┘
                              ▼                             ▼
                    PostgreSQL 5432                PostgreSQL 5433
                    (service_registry)             (banking_transfer)
```

Гол зарчим: **нэг нэвтрэлт, хоёр сервис**. Хоёр сервис ижил `JWT_SECRET`-тэй (HS256); banking сервис token олгодоггүй, зөвхөн шалгана (`banking-transfer-service/.../config/SecurityConfig.java`).

### 2.2. Хэрэглэгчийн дүрүүд (Actors)

| Дүр       | Портал (8080/5173)                                    | Банкны апп (8084/5174)                                                             |
| ------------ | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `ADMIN`    | Бүх CRUD, checklist засвар, аудит унших  | Теллер: бүх данс/гүйлгээ/харилцагч, буцаалт, аудит |
| `SECURITY` | Унших + checklist засвар + аудит унших | Харилцагчийн эрхтэй адил (username холболттой бол)       |
| `VIEWER`   | Зөвхөн унших                                     | Харилцагч: зөвхөн өөрийн данс/гүйлгээ                       |

Демо хэрэглэгчид: `admin/admin123` (ADMIN), `batbold/demo123` (VIEWER, CUST-0001, данс 100000001), `sarnai/demo123` (VIEWER, CUST-0002, данс 100000002). Харилцагч хэрэглэгчид зөвхөн dev seed (`db/seed-dev`)-д байдаг.

### 2.3. Таамаглал ба хамаарал

- JDK 17, Maven 3.9+, Docker Desktop, Node.js 20+ шаардлагатай.
- Нууц утгууд (DB нууц үг, `JWT_SECRET`) орчны хувьсагчаар ирнэ; байхгүй бол сервис асахгүй (fail-fast).
- Prod горимд `FLYWAY_LOCATIONS=classpath:db/migration` тавьж demo seed-ийг хасна.

---

## 3. Анхны даалгаврын шаардлагын шинжилгээ

PDF даалгаврын шаардлага бүр → хэрэгжилт:

| #    | PDF шаардлага                                                           | Хэрэгжилт                                                                                                          | Байдал            |
| ---- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| R1   | Хэрэглэгч нэвтрэх                                                | `POST /api/auth/login` — JWT Bearer, 60 мин хүчинтэй                                                          | ✅ Бүрэн           |
| R2   | Хэрэглэгчийн нэр, нууц үг хадгалах                  | `users` хүснэгт, нууц үг **BCrypt hash** (plain text хадгалахгүй)                           | ✅ Бүрэн           |
| R3   | Системийн бүртгэл                                                | `POST/PUT /api/systems` + порталын форм                                                                       | ✅ Бүрэн           |
| R3.1 | Системийн нэр                                                        | `systems.name` (≤160, заавал)                                                                                      | ✅                      |
| R3.2 | Төрөл: Карт/Коре/Дотоод/Дижитал                        | enum`CARD/CORE/INTERNAL/DIGITAL` (DB check + UI монгол нэршил)                                                | ✅                      |
| R3.3 | Үнэлгээ/төг                                                            | `valuation_mnt numeric(18,2) >= 0`, заавал                                                                          | ✅                      |
| R3.4 | Тайлбар                                                                   | `description text` (форм дээр ≤5000)                                                                             | ✅                      |
| R3.5 | Холбоотой системүүд                                            | `system_relations` self-referencing many-to-many (`DEPENDS_ON/CALLS/INTEGRATES_WITH`)                                   | ✅                      |
| R3.6 | Хөгжүүлэгч                                                             | `developer_name`, `developer_team`                                                                                      | ✅                      |
| R3.7 | Хугацаа                                                                   | `start_date`, `end_date` + `start_date <= end_date` дүрэм                                                        | ✅                      |
| R3.8 | Ашиглагдаж байгаа эсэх                                       | `in_use boolean`; `in_use=false` → статус INACTIVE болно                                                    | ✅                      |
| R4   | Мэдээллийн аюулгүй байдлын стандарт шалгах | 8 control-той checklist (OWASP ASVS/API Security ref), PASS/WARNING/FAIL/NOT_CHECKED үр дүн, жинлэсэн score | ✅ Өргөтгөсөн |
| R5   | Жагсаалтаар харах, хайлт хийх                           | `GET /api/systems` — keyword/type/developer/inUse/status шүүлт, pagination, `createdAt desc`                      | ✅ Бүрэн           |

Өргөтгөсөн scope (даалгаврын «шаардлагын дагуу даалгавар боловсруулж хөгжүүлэх» хэсгийн хүрээнд нэмсэн): аудит лог, dashboard, банкны шилжүүлгийн демо сервис (бүртгэгдсэн системийн бодит жишээ), эрхийн түвшин (RBAC), нэвтрэлтийн түгжээ.

---

## 4. Функциональ шаардлага

Тэмдэглэгээ: **FR-…** = функциональ шаардлага. «Эх» багана — хэрэгжилтийг нотлох гол файл.

### 4.1. Нэвтрэлт ба эрхийн удирдлага (platform-api)

| ID        | Шаардлага                                                               | Нарийвчлал                                                                                                                                                                                                  | Эх                              |
| --------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| FR-AUTH-1 | Хэрэглэгч нэр/нууц үгээр нэвтэрч JWT авна        | `POST /api/auth/login` → `{accessToken, tokenType:"Bearer", expiresInSeconds, user}`                                                                                                                             | `auth/AuthService.java`         |
| FR-AUTH-2 | Нэвтэрсэн хэрэглэгч өөрийн мэдээллийг авна | `GET /api/auth/me` → `{id, username, displayName, role}`                                                                                                                                                         | `auth/AuthController.java`      |
| FR-AUTH-3 | Brute-force хамгаалалт                                                 | **5** удаа дараалан буруу → **15 минут** түгжээ → HTTP **423 Locked**; амжилттай нэвтрэлт тоолуурыг тэглэнэ. In-memory (нэг node) | `auth/LoginAttemptService.java` |
| FR-AUTH-4 | Нууц үг зөвхөн hash хэлбэрээр хадгалагдана      | BCrypt; бүртгэл/нууц үг солих API одоогоор байхгүй (seed-ээр үүснэ)                                                                                                          | `config/SecurityConfig.java`    |
| FR-AUTH-5 | JWT агуулга                                                               | HS256; claims:`iss=service-registry-platform`, `sub`=username, `exp` (default 60 мин, `JWT_EXPIRATION_MINUTES`), `userId`, `displayName`, `role`, `authorities`                                    | `auth/JwtTokenService.java`     |
| FR-AUTH-6 | RBAC                                                                             | Дүрүүд:`ADMIN`, `SECURITY`, `VIEWER`. Public: `/api/auth/login`, `/actuator/health`, `/actuator/info`; бусад бүх endpoint token шаардана                                            | `config/SecurityConfig.java`    |

### 4.2. Системийн бүртгэл (platform-api)

| ID       | Шаардлага                                         | Нарийвчлал                                                                                                                                               | Эх                                                                     |
| -------- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------ |
| FR-SYS-1 | Систем бүртгэх                                | `POST /api/systems` (ADMIN) → 201. Заавал: name, type, valuationMnt (≥0). URL талбарууд `@URL` + ≤500                                        | `systems/SystemService.java`, `systems/dto/SystemCreateRequest.java` |
| FR-SYS-2 | Жагсаалт + хайлт                              | `GET /api/systems?keyword&type&developer&inUse&status&page&size` — бүх нэвтэрсэн хэрэглэгчид; sort `createdAt desc`; size 1–100 clamp | `systems/SystemSpecifications.java`                                    |
| FR-SYS-3 | Дэлгэрэнгүй харах                          | `GET /api/systems/{id}` — холбоотой системүүдтэй хамт                                                                                  | `systems/SystemController.java`                                        |
| FR-SYS-4 | Засварлах                                         | `PUT /api/systems/{id}` (ADMIN); relations жагсаалтыг бүрэн сольж бичнэ                                                                 | `systems/SystemService.java`                                           |
| FR-SYS-5 | Устгал = зөөлөн идэвхгүйжүүлэлт | `DELETE /api/systems/{id}` (ADMIN) → 204; мөр устахгүй, `inUse=false` + `status=INACTIVE`                                                        | `systems/SystemService.java`                                           |
| FR-SYS-6 | Холбоотой системийн дүрэм           | Өөртэйгөө холбогдохгүй;`(target, relationType)` давхардахгүй; төрөл: `DEPENDS_ON/CALLS/INTEGRATES_WITH`                  | мөн тэнд                                                          |
| FR-SYS-7 | `systemKey`                                              | Өгвөл unique шалгана; өгөхгүй бол нэрээс slug үүсгэнэ (давхардвал`-2`, `-3`…)                                    | `systems/SystemService.java`                                           |

### 4.3. Аюулгүй байдлын шалгалт (platform-api)

| ID       | Шаардлага                           | Нарийвчлал                                                                                                                                      | Эх                                            |
| -------- | -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- |
| FR-SEC-1 | Стандартын master жагсаалт | `GET /api/security-controls` — 8 control (V2 seed), OWASP ASVS / OWASP API Security ref, нийт жин **100**                                 | `db/migration/V2__seed_security_controls.sql` |
| FR-SEC-2 | Систем бүрийн checklist          | `GET /api/systems/{id}/security-checks`; үр дүнгүй control → `NOT_CHECKED`                                                                   | `securitycheck/SecurityCheckService.java`     |
| FR-SEC-3 | Checklist шинэчлэх                   | `PUT /api/systems/{id}/security-checks` (ADMIN эсвэл SECURITY); давхардсан controlId → 400; үл мэдэгдэх system/control → 404 | мөн тэнд                                 |
| FR-SEC-4 | Score тооцох                           | `GET /api/systems/{id}/security-score` → `{score, earnedWeight, totalWeight, pass/fail/warning/notChecked тоо}`                                   | мөн тэнд                                 |
| FR-SEC-5 | Batch score                                  | `GET /api/security-scores` — бүх системийн score нэг хүсэлтээр (N+1 арилгасан, dashboard-д)                          | `securitycheck/SecurityCheckController.java`  |

Control-ууд (жин): HTTPS_ENABLED 15, AUTHENTICATION_ENABLED 15, ROLE_BASED_ACCESS 15, AUDIT_LOG_ENABLED 10, SECRETS_NOT_IN_CODE 15, SWAGGER_PROTECTED 10, CORS_RESTRICTED 10, INPUT_VALIDATION 10.

### 4.4. Аудит лог (platform-api)

| ID       | Шаардлага          | Нарийвчлал                                                                                                                                                                                                                             | Эх                              |
| -------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------- |
| FR-AUD-1 | Үйлдэл бүртгэх | Actions:`LOGIN_SUCCESS`, `LOGIN_FAILURE`, `SYSTEM_CREATED`, `SYSTEM_UPDATED`, `SYSTEM_DISABLED`, `SECURITY_CHECK_UPDATED`. Login failure нь `REQUIRES_NEW` transaction-оор exception гарсан ч хадгалагдана | `audit/AuditLogService.java`    |
| FR-AUD-2 | Аудит үзэх         | `GET /api/audit-logs` (ADMIN/SECURITY), `createdAt desc, id desc`, pagination                                                                                                                                                                | `audit/AuditLogController.java` |

### 4.5. Банкны шилжүүлгийн сервис (banking-transfer-service)

| ID        | Шаардлага                               | Нарийвчлал                                                                                                                                                                                                                                                                                                                                                                                                                         | Эх                                                       |
| --------- | ------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| FR-BNK-1  | Шилжүүлэг хийх                      | `POST /api/transfers`: amount ≥0.01, бутархай ≤2 орон, ижил данс хориотой, хоёр данс ACTIVE, валют ижил, үлдэгдэл хүрэлцэх. Дансуудыг accountNo-ийн эрэмбээр `PESSIMISTIC_WRITE` түгжинэ (deadlock-гүй). Амжилт → DEBIT+CREDIT хоёр ledger бичилт                                                                          | `transfer/TransferService.java`                          |
| FR-BNK-2  | Идемпотентлог                       | `Idempotency-Key` header (≤80 тэмдэгт): шинэ → 201, давталт → 200 + анхны гүйлгээ (дахин гүйцэтгэхгүй); зэрэгцээ давхардлыг unique constraint + catch-refetch шийднэ                                                                                                                                                                                             | `transfer/TransferController.java`                       |
| FR-BNK-3  | Төлөвийн lifecycle                       | `SUCCESS → REVERSED`; бизнес дүрэм зөрчигдвөл → `FAILED` мөр хадгалагдана (`failureReason`-той, ledger/үлдэгдэл хөндөгдөхгүй, idempotency key хадгалахгүй — `noRollbackFor`). `PENDING` ирээдүйн async урсгалд нөөцлөгдсөн. Данс олдоогүй/ижил данс/эзэмшлийн алдаа мөр үүсгэхгүй | `transfer/TransferStatus.java`, `TransferService.java` |
| FR-BNK-4  | Лимит                                       | Нэг удаагийн max:`app.limits.max-per-transfer` (default **5,000,000.00₮**); өдрийн зарлагын нийт: `app.limits.daily-outgoing-total` (default **10,000,000.00₮**), данс тус бүрээр, түгжээтэй хэсэгт committed data-аас тооцно. SUCCESS+REVERSED тооцогдоно, reversal гүйлгээ өөрөө лимитээс чөлөөтэй               | `config/TransferLimitsProperties.java`                   |
| FR-BNK-5  | Гүйлгээ харах                        | `GET /api/transfers`, `GET /api/transfers/{id}`: ADMIN бүгдийг, харилцагч зөвхөн өөрийн оролцоотойг                                                                                                                                                                                                                                                                                               | `transfer/TransferController.java`                       |
| FR-BNK-6  | Гүйлгээ буцаах                      | `POST /api/transfers/{id}/reversal` (зөвхөн ADMIN) → 201. Зөвхөн SUCCESS буцаана (давхар → 409); transfer мөрийг эхэлж түгжинэ + `reversal_of_transfer_id` unique backstop; хүлээн авагчийн үлдэгдэл хүрэлцэхгүй бол 400                                                                                                                                | `transfer/TransferService.reverse`                       |
| FR-BNK-7  | Данс харах                              | `GET /api/accounts/my` (өөрийн), `GET /api/accounts/{no}` (ADMIN эсвэл эзэмшигч)                                                                                                                                                                                                                                                                                                                                      | `account/AccountController.java`                         |
| FR-BNK-8  | Дансны хуулга                        | `GET /api/accounts/{no}/statement?from&to` (default сүүлийн 30 хоног): opening/closing balance, орлого/зарлагын нийлбэр, counterparty-тэй бичилтүүд                                                                                                                                                                                                                                           | `account/StatementService.java`                          |
| FR-BNK-9  | Данс удирдах (теллер)           | ADMIN: нээх (дугаар`account_no_seq`-ээс), block (ACTIVE→BLOCKED), unblock (BLOCKED→ACTIVE), close (үлдэгдэл 0 үед л; CLOSED эцсийн төлөв)                                                                                                                                                                                                                                                            | `account/AccountController.java`                         |
| FR-BNK-10 | Харилцагч удирдах (теллер) | ADMIN: хайх/үзэх/үүсгэх (`CUST-%04d`, sequence 1001-ээс)/засах/идэвхгүй болгох. Username давхардвал 409 `USERNAME_TAKEN`. Deactivate нь бүртгэлийн шинжтэй — гүйлгээг дансны төлөв л хаана                                                                                                                                                    | `customer/CustomerController.java`                       |
| FR-BNK-11 | Эзэмшлийн загвар                  | JWT`sub` ↔ `customers.username`; ADMIN = теллер (бүгдийг), бусад = харилцагч (зөвхөн өөрийнх). Зөрчил → 403 `FORBIDDEN_ACCOUNT`                                                                                                                                                                                                                                                        | `common/AuthFacade.java`                                 |
| FR-BNK-12 | Банкны аудит                          | 10 action (`TRANSFER_CREATED/FAILED/REVERSED`, `ACCOUNT_OPENED/BLOCKED/UNBLOCKED/CLOSED`, `CUSTOMER_CREATED/UPDATED/DEACTIVATED`); actor нь JWT snapshot (FK-гүй); `REQUIRES_NEW` тул FAILED гүйлгээний аудит ч хадгалагдана; `GET /api/audit-logs` (ADMIN)                                                                                                                                    | `audit/BankAuditService.java`                            |
| FR-BNK-13 | Нэвтрэлт                                 | Өөрөө login-гүй; platform-api-ийн JWT-г ижил`JWT_SECRET`-ээр шалгадаг resource server. Health/info/Swagger public, бусад бүгд authenticated                                                                                                                                                                                                                                                            | `config/SecurityConfig.java`                             |

### 4.6. Хэрэглэгчийн интерфэйс

**Registry Portal (`frontend/`, 5173)** — localStorage: `service-registry-*`:

| Хуудас                                                           | Route                                   | Эрх                                                             |
| ---------------------------------------------------------------------- | --------------------------------------- | ------------------------------------------------------------------ |
| Нэвтрэх                                                         | `/login`                              | Public                                                             |
| Dashboard (нийт систем, score дундаж)                  | `/dashboard`                          | Нэвтэрсэн                                                 |
| Системүүд (хайлт 300ms debounce, шүүлт, pagination) | `/systems`                            | Нэвтэрсэн; «Систем нэмэх» зөвхөн ADMIN |
| Систем үүсгэх/засах форм                          | `/systems/new`, `/systems/:id/edit` | ADMIN                                                              |
| Системийн дэлгэрэнгүй + Security checklist tab     | `/systems/:id`                        | Унших бүгд; checklist засвар ADMIN/SECURITY         |
| Аудит лог                                                      | `/audit-logs`                         | Нэвтэрсэн (API нь ADMIN/SECURITY)                       |

**Banking App (`frontend-banking/`, 5174)** — login нь 8080 руу (SSO), localStorage: `banking-app-*`:

| Хуудас                                                                                                                                             | Route                                                  | Эрх                                                                          |
| -------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------- |
| Нүүр (өөрийн данснууд)                                                                                                                 | `/`                                                  | Нэвтэрсэн                                                              |
| Данс хайх, Хуулга (огнооны шүүлт, opening/closing картууд, running balance)                                             | `/accounts`, `/accounts/:no/statement`             | Нэвтэрсэн (эзэмшлийн хяналт API талд)               |
| Гүйлгээнүүд, Шинэ шилжүүлэг (өөрийн дансны dropdown,`crypto.randomUUID()` idempotency key), Дэлгэрэнгүй | `/transfers`, `/transfers/new`, `/transfers/:id` | Нэвтэрсэн                                                              |
| Харилцагчид, Харилцагчийн форм, Данс удирдлага, Аудит лог                                                | `/admin/...`                                         | `AdminRoute` — зөвхөн ADMIN, цэс нь бусдад нуугдана |

Алдааны монголчлол: `utils/bankingErrors.ts` — 17 ErrorCode бүрд монгол мессеж (жишээ: `INSUFFICIENT_FUNDS` → «Дансны үлдэгдэл хүрэлцэхгүй байна»).

---

## 5. Бизнесийн дүрмүүд (Business Rules)

| ID    | Дүрэм                                   | Утга/Томьёо                                                                                                                                                                                                                                                                       |
| ----- | -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| BR-1  | Нэвтрэлтийн түгжээ          | 5 дараалсан алдаа → 15 минут түгжээ (423); амжилт тоолуур тэглэнэ                                                                                                                                                                             |
| BR-2  | Security score                               | `PASS` = бүтэн жин, `WARNING` = жингийн 50%, `FAIL`/`NOT_CHECKED` = 0; `score = round(earnedWeight × 100 / totalWeight)`                                                                                                                                          |
| BR-3  | Системийн статус              | `inUse=false` → статус хүчээр `INACTIVE`; DELETE = зөөлөн идэвхгүйжүүлэлт                                                                                                                                                                             |
| BR-4  | Хугацааны дүрэм                | `startDate ≤ endDate`, зөрчвөл 400                                                                                                                                                                                                                                                |
| BR-5  | Шилжүүлгийн дүн                | `0.01 ≤ amount`, бутархай ≤2 орон, нэг удаад ≤ 5,000,000₮ (тохируулгатай)                                                                                                                                                                            |
| BR-6  | Өдрийн лимит                      | Дансны өдрийн зарлага (SUCCESS+REVERSED, reversal-ыг эс тооцно) + шинэ дүн ≤ 10,000,000₮ (тохируулгатай)                                                                                                                                 |
| BR-7  | FAILED гүйлгээ                        | Зөвхөн`INSUFFICIENT_FUNDS`/`ACCOUNT_INACTIVE`/`CURRENCY_MISMATCH`/`LIMIT_EXCEEDED` шалтгаанаар үүснэ; ledger бичилтгүй, үлдэгдэл өөрчлөхгүй, idempotency key хадгалахгүй (дахин оролдох боломжтой) |
| BR-8  | Буцаалт                               | Зөвхөн ADMIN, зөвхөн SUCCESS гүйлгээг, нэг л удаа; лимит шалгалтад орохгүй; CLOSED данс оролцвол хориглоно                                                                                                            |
| BR-9  | Хуулгын opening balance               | `opening = одоогийн үлдэгдэл − Σ(орлого) + Σ(зарлага)` (хүрээний эхнээс хойш) — seed үлдэгдэл ledger-гүйг зөв тооцно                                                                                              |
| BR-10 | Дансны төлөвийн шилжилт | ACTIVE↔BLOCKED (block/unblock); → CLOSED зөвхөн үлдэгдэл 0 үед; CLOSED = эцсийн                                                                                                                                                                                    |
| BR-11 | Эзэмшил                               | ADMIN биш хэрэглэгч зөвхөн`customers.username` = JWT subject таарсан данс/гүйлгээн дээр ажиллана                                                                                                                                         |
| BR-12 | Дугаарлалт                         | Харилцагч:`CUST-%04d` (seq 1001-ээс), данс: `account_no_seq` (100000101-ээс); demo seed: CUST-0001/0002, данс 100000001/100000002                                                                                                                                |
| BR-13 | Идемпотент давталт          | Ижил key → анхны гүйлгээг буцаана (body харьцуулдаггүй — мэдэгдэж буй хязгаарлалт); өөр харилцагчийн key = олдсонгүй мэт                                                                           |

---

## 6. Өгөгдлийн шаардлага

### 6.1. platform-api (`service_registry` DB)

| Хүснэгт             | Үүрэг                                                         | Гол хязгаарлалт                                                                            |
| -------------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------- |
| `users`                  | Нэвтрэх хэрэглэгч                                  | `username` unique; `role in (ADMIN, SECURITY, VIEWER)`; BCrypt hash                                  |
| `systems`                | Системийн каталог (PDF-ийн бүх талбар) | `system_key` unique; type/environment/status check; `valuation_mnt ≥ 0`; `start_date ≤ end_date` |
| `system_relations`       | Холбоотой системүүд                              | `source ≠ target`; `(source, target, type)` unique; cascade delete                                  |
| `security_controls`      | Стандартын master (8 мөр seed)                        | `control_key` unique; `weight > 0`                                                                   |
| `security_check_results` | Шалгалтын үр дүн                                     | `(system_id, control_id)` unique; result check                                                         |
| `audit_logs`             | Аудит                                                         | `target_id` polymorphic (FK биш); `created_at` индекстэй                                 |

Flyway: `V1__init_schema`, `V2__seed_security_controls`, `V3__seed_admin_user`; dev-only `db/seed-dev`: `V4__seed_demo_systems`, `V5__seed_demo_customer_users`.

### 6.2. banking-transfer-service (`banking_transfer` DB)

| Хүснэгт      | Үүрэг                | Гол хязгаарлалт                                                                                                                                                                                                  |
| ------------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `customers`       | Харилцагч        | `customer_no` unique; `username` unique nullable (platform холболт); `active`                                                                                                                                     |
| `accounts`        | Данс                  | `account_no` unique; **`balance ≥ 0` DB check** (overdraw боломжгүй); status: ACTIVE/BLOCKED/CLOSED                                                                                                        |
| `transfers`       | Гүйлгээ            | `transfer_ref` unique (`TRF-` + 12 hex); `idempotency_key` unique nullable; `amount > 0` check; status: PENDING/SUCCESS/FAILED/REVERSED; `reversal_of_transfer_id` unique (давхар буцаалтын backstop) |
| `ledger_entries`  | Давхар бичилт | transfer FK cascade; DEBIT/CREDIT;`balance_after` snapshot                                                                                                                                                                   |
| `bank_audit_logs` | Банкны аудит   | Actor snapshot (username/displayName/role) — FK-гүй                                                                                                                                                                        |

Flyway: `V1__banking_schema`, `V3__add_transfer_idempotency_key`, `V4__transfer_lifecycle`, `V5__customer_username_active`, `V6__account_mgmt_and_bank_audit` (+ sequences); dev-only `db/seed-dev`: `V2__seed_demo_customers`, `V7__seed_customer_usernames`. Нэг дугаарлалтын цуваа — дугаар дахин ашиглахгүй.

---

## 7. Гадаад интерфэйсийн шаардлага

### 7.1. Алдааны формат

platform-api: `{timestamp, status, error, message, path}`.
banking: нэмэлт машин-уншигдах **`code`** талбартай: `{timestamp, status, error, code, message, path}`.

### 7.2. Банкны ErrorCode (17 утга)

| Code                          | HTTP | Хэзээ                                                          | UI монгол мессеж                                                                  |
| ----------------------------- | ---- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `VALIDATION_ERROR`          | 400  | Validation, огнооны хүрээ, бутархай >2 орон | Оруулсан мэдээлэл буруу байна                                       |
| `SAME_ACCOUNT`              | 400  | Илгээгч = хүлээн авагч                            | Илгээгч болон хүлээн авагч данс ижил байж болохгүй |
| `INSUFFICIENT_FUNDS`        | 400  | Үлдэгдэл хүрэлцэхгүй                             | Дансны үлдэгдэл хүрэлцэхгүй байна                               |
| `ACCOUNT_INACTIVE`          | 400  | BLOCKED/CLOSED данс                                             | Данс идэвхгүй байна                                                          |
| `CURRENCY_MISMATCH`         | 400  | Валют зөрсөн                                             | Данснуудын валют зөрж байна                                           |
| `LIMIT_EXCEEDED`            | 400  | Лимит хэтэрсэн                                         | Гүйлгээний лимит хэтэрсэн байна                                   |
| `ACCOUNT_NOT_EMPTY`         | 400  | Үлдэгдэлтэй данс хаах                            | (мөн адил монголчлогдсон)                                                |
| `INVALID_STATUS_TRANSITION` | 400  | Буруу төлөвийн шилжилт                          | —                                                                                            |
| `FORBIDDEN_ACCOUNT`         | 403  | Бусдын данс/гүйлгээ                                | Энэ данс руу хандах эрхгүй байна                                   |
| `FORBIDDEN`                 | 403  | Role хүрэлцэхгүй                                         | —                                                                                            |
| `ACCOUNT_NOT_FOUND`         | 404  | —                                                                  | —                                                                                            |
| `CUSTOMER_NOT_FOUND`        | 404  | —                                                                  | —                                                                                            |
| `TRANSFER_NOT_FOUND`        | 404  | —                                                                  | —                                                                                            |
| `TRANSFER_NOT_REVERSIBLE`   | 409  | SUCCESS биш гүйлгээ буцаах                          | —                                                                                            |
| `USERNAME_TAKEN`            | 409  | Username давхардсан                                       | —                                                                                            |
| `DUPLICATE_REQUEST`         | 409  | Зэрэгцээ unique зөрчил                                | —                                                                                            |
| `INTERNAL_ERROR`            | 500  | —                                                                  | —                                                                                            |

(Бүх 17 код `frontend-banking/src/utils/bankingErrors.ts`-д монгол мессежтэй.)

### 7.3. Endpoint-уудын бүрэн жагсаалт

platform-api: `docs/api-contract.md`; banking: `docs/banking-api-contract.md`-д тодорхойлогдсон бөгөөд кодтой нийцэж буйг шинжилгээгээр баталгаажууллаа. Swagger UI зөвхөн banking сервис дээр (`/swagger-ui/index.html`, 8084).

---

## 8. Функциональ бус шаардлага

### 8.1. Аюулгүй байдал (NFR-SEC)

| ID        | Шаардлага                             | Хэрэгжилт                                                                                                                          |
| --------- | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| NFR-SEC-1 | Stateless нэвтрэлт                     | JWT HS256, session/CSRF байхгүй,`SessionCreationPolicy.STATELESS`                                                                  |
| NFR-SEC-2 | Нууц утгын сахилга             | `JWT_SECRET`, DB нууц үгэнд default fallback байхгүй — env байхгүй бол startup fail-fast; secret ≥32 байт |
| NFR-SEC-3 | CORS allowlist                                 | Зөвхөн 5173/5174 origin, Authorization + Idempotency-Key header зөвшөөрөгдсөн                                            |
| NFR-SEC-4 | Brute-force хамгаалалт               | FR-AUTH-3 (5/15мин/423)                                                                                                                  |
| NFR-SEC-5 | Demo өгөгдлийн тусгаарлалт | Demo seed`db/seed-dev`-д; prod-д `FLYWAY_LOCATIONS`-оор хасагдана                                                         |
| NFR-SEC-6 | Front-end хамгаалалт                 | 401 → auto-logout; token expiry localStorage шалгалт; login форм урьдчилан бөглөдөггүй                      |

### 8.2. Өгөгдлийн бүрэн бүтэн байдал (NFR-INT)

| ID        | Шаардлага                                                    | Хэрэгжилт                                                                                                                                                                                                                   |
| --------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| NFR-INT-1 | Зэрэгцээ гүйлгээнд мөнгө алдагдахгүй | Дансуудыг accountNo эрэмбээр`PESSIMISTIC_WRITE` түгжинэ; 12 зэрэгцээ гүйлгээний live burst-ээр баталгаажсан (яг 10 амжилт, 2 татгалзал, overdraw-гүй) |
| NFR-INT-2 | Давхар бичилт                                             | Гүйлгээ бүр DEBIT+CREDIT хос ledger бичилт, atomic transaction                                                                                                                                                    |
| NFR-INT-3 | DB түвшний хамгаалалт                                | `balance ≥ 0`, `amount > 0` check; idempotency/reversal unique constraint                                                                                                                                                       |
| NFR-INT-4 | Schema-код нийцэл                                            | `ddl-auto=validate` — схемийг зөвхөн Flyway өөрчилнө                                                                                                                                                         |

### 8.3. Гүйцэтгэл ба ажиглалт (NFR-PERF/OBS)

- Pagination бүх жагсаалтад, `size` 1–100 clamp.
- N+1 арилгасан: systems relations batch query, transfers ledger fetch join, batch score endpoint.
- Хайлтын debounce 300ms (portal).
- Actuator `health`/`info` (+liveness/readiness probes) хоёр сервис дээр public.

### 8.4. Тохиргоо (NFR-CONF)

| Хувьсагч                                                    | Default                                 | Заавал эсэх              |
| ------------------------------------------------------------------- | --------------------------------------- | ---------------------------------- |
| `POSTGRES_PASSWORD` (platform), `BANKING_DB_PASSWORD` (banking) | байхгүй                          | **Заавал** (fail-fast) |
| `JWT_SECRET` (хоёр сервист ижил)                   | байхгүй                          | **Заавал** (fail-fast) |
| `JWT_EXPIRATION_MINUTES`                                          | 60                                      | Сонголттой               |
| `BANKING_MAX_PER_TRANSFER`                                        | 5000000.00                              | Сонголттой               |
| `BANKING_DAILY_OUTGOING_TOTAL`                                    | 10000000.00                             | Сонголттой               |
| `FLYWAY_LOCATIONS`                                                | migration + seed-dev                    | Prod-д`classpath:db/migration`  |
| Портууд                                                      | 8080 / 8084 / 5432 / 5433 / 5173 / 5174 | Сонголттой               |

### 8.5. Нутагшуулалт (NFR-I18N)

UI монгол хэлээр; банкны бүх ErrorCode монгол мессежтэй; мөнгөн дүн мянгачилсан форматтай (`formatExactMnt`).

---

## 9. Баталгаажуулалт ба хүлээн авах шалгуур

### 9.1. Автомат тест (бүгд ногоон, H2 + Flyway)

| Сервис             | Тест                   | Гол хамрал                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ------------------------ | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| platform-api             | 33 тест / 7 класс | Login/JWT/lockout (5 fail → 423, reset), RBAC deny матриц (VIEWER/SECURITY 403), System CRUD/хайлт/зөөлөн устгал/огнооны дүрэм/давхардсан relation, checklist + score + batch, аудит бичилт                                                                                                                                                                                                                                                                          |
| banking-transfer-service | 45 тест / 9 класс | Шилжүүлэг + ledger, үлдэгдэл хүрэлцэхгүй + rollback, идемпотентлог (нэг удаа гүйцэтгэл), эзэмшил (бусдын данс 403, мөр үүсэхгүй), лимитүүд (FAILED мөртэй), буцаалт (сэргээлт/давхар 409/зарцуулагдсан), данс удирдлага (block/unblock/close дүрэм), хуулга (opening/closing, хүрээ), харилцагч CRUD, аудит (FAILED дээр ч REQUIRES_NEW) |

CI: GitHub Actions — хоёр сервисийн `mvn test` + portal-ын lint/build (remote push хийгдсэний дараа идэвхжинэ).

### 9.2. Гар/E2E баталгаажуулалт (жинхэнэ PostgreSQL)

- Fresh DB дээр Flyway бүрэн цэвэр: platform V1–V5, banking V1–V7.
- 22 шалгалттай E2E API suite: харилцагчийн шилжүүлэг/идемпотент давталт/хуулга; 403 FORBIDDEN_ACCOUNT; 400 LIMIT_EXCEEDED + FAILED мөр; буцаалтаар үлдэгдэл сэргэсэн, давхар → 409; данс нээх/блоклох/хаах дүрмүүд; аудит бичилтүүд.
- Зэрэгцээ 12 гүйлгээний burst — overdraw-гүй.
- Playwright screenshot-оор хоёр апп-ын гол дэлгэцүүд баталгаажсан (харилцагчийн эрхийн ялгаа орсон).

---

## 10. Хязгаарлалт, нээлттэй ажлууд

### 10.1. Мэдэгдэж буй хязгаарлалтууд (зориуд хийсэн шийдвэрүүд)

| Хязгаарлалт                                                | Тайлбар                                                                               |
| --------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| Login lockout in-memory                                               | Нэг node-д л ажиллана; scale-out үед shared store хэрэгтэй           |
| Идемпотент давталт body харьцуулдаггүй | Ижил key + өөр body → анхны гүйлгээг л буцаана                  |
| Нууц үгийн complexity дүрэм байхгүй              | Хэрэглэгч seed-ээр үүсдэг тул одоогоор шаардлагагүй |
| Данс нээх initialBalance ledger-гүй                        | Demo shortcut; BR-9-ийн томьёо үүнийг зөв тооцдог                   |
| Portal-д user удирдлагын API байхгүй                | SECURITY/VIEWER хэрэглэгчид seed-ээр л үүснэ                             |
| `PENDING` төлөв ашиглагддаггүй                   | Ирээдүйн async урсгалд нөөцлөгдсөн                                 |

### 10.2. Хамрах хүрээнээс гадуур (дараагийн шатны өргөтгөл)

Gateway (Spring Cloud Gateway), Eureka Discovery, Config Server, Notification (Email/Slack), Prometheus/Grafana мониторинг, fraud detection — эдгээр нь концепцид туссан боловч эхний хамгаалалтад зориуд хойшлогдсон (`төслийн concept/даалгаврын нийцлийн үнэлгээ.md` §6).

### 10.3. Нээлттэй ажлын жагсаалт

1. GitHub remote үүсгэж push хийх, CI идэвхжүүлэх.
2. Хоёр frontend-д Vitest тестүүд нэмэх (CI-д frontend-banking-ийн lint/build мөн нэмэгдэх ёстой).
3. Давхардсан frontend кодыг (styles, auth, httpClient) shared package болгох.
4. Portal user-management API.
5. Push-ийн дараа төслийг OneDrive-ын гаднах хавтас руу нүүлгэх.

---

## 11. Мөрдөлтийн матриц (Traceability)

| PDF шаардлага                                      | FR            | Гол код                         | Тест                                         |
| ----------------------------------------------------------- | ------------- | ------------------------------------- | ------------------------------------------------ |
| Хэрэглэгч нэвтрэх                           | FR-AUTH-1,2,5 | `auth/AuthService.java`             | `AuthControllerTests` (5)                      |
| Нэр/нууц үг хадгалах                       | FR-AUTH-4     | `users/User.java`, V3 seed          | `AuthControllerTests`, `LoginLockoutTests`   |
| Системийн бүртгэл (бүх талбар)     | FR-SYS-1,4,7  | `systems/SystemService.java`        | `SystemControllerTests` (7)                    |
| Холбоотой системүүд                       | FR-SYS-6      | `relations/SystemRelation.java`     | `SystemControllerTests`                        |
| Аюулгүй байдлын стандарт шалгах | FR-SEC-1…5   | `securitycheck/*`                   | `SecurityCheckControllerTests` (9)             |
| Жагсаалт, хайлт                                | FR-SYS-2,3    | `systems/SystemSpecifications.java` | `SystemControllerTests`                        |
| (Өргөтгөл) Аудит                               | FR-AUD-1,2    | `audit/*`                           | `AuditLogControllerTests`, `RoleAccessTests` |
| (Өргөтгөл) Эрхийн түвшин                | FR-AUTH-6     | `config/SecurityConfig.java`        | `RoleAccessTests` (7)                          |
| (Өргөтгөл) Банкны демо систем       | FR-BNK-1…13  | `banking-transfer-service/*`        | 9 тест класс (45)                       |

---

*Энэ баримт нь 2026-07-08-ны байдлаарх кодын төлөвийг тусгасан. Код өөрчлөгдвөл §4–§9-ийн утгуудыг (лимит, тестийн тоо, migration жагсаалт) давхар шинэчилнэ.*
