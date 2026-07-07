# Төслийн файлын бүтэц

Энэ баримт нь **System Registry & Security Compliance Platform** төслийн хавтас, файлын бүтцийг тайлбарлана. Төслийг шинээр харж буй хөгжүүлэгч аль код хаана байгааг, ямар файл ямар үүрэгтэйг хурдан олоход зориулагдсан.

## Ерөнхий тойм

```text
service-registry-platform/
├── backend/
│   ├── platform-api/               # Үндсэн API — системийн бүртгэл, аюулгүй байдал, audit (порт 8080)
│   └── banking-transfer-service/   # Банкны гүйлгээний жишээ микросервис (порт 8084)
├── frontend/                       # React админ портал (порт 5173)
├── docs/                           # Төслийн баримт бичгүүд
├── docker-compose.yml              # PostgreSQL өндийлгөх Docker тохиргоо
├── .env.example                    # Орчны хувьсагчийн загвар (нууц утгагүй)
├── .gitignore                      # Git-д оруулахгүй файлуудын жагсаалт
├── .github/                        # GitHub-тай холбоотой тохиргоо
└── README.md                       # Ажиллуулах заавар, одоогийн scope
```

---

## Backend-ийн давхаргын загвар

Хоёр backend сервис хоёулаа **Spring Boot** бөгөөд модуль (feature) бүр ижил давхаргатай:

```text
HTTP хүсэлт
   ↓
Controller      # REST endpoint-уудыг тодорхойлно (@RestController)
   ↓
Service         # Бизнес логик, шалгалт, тооцоолол
   ↓
Repository      # Өгөгдлийн сантай харьцах (Spring Data JPA)
   ↓
Entity          # Өгөгдлийн сангийн хүснэгттэй харгалзах класс (@Entity)
```

Мөн модуль бүрт:

- **`dto/`** — Request/Response классууд. Entity-г шууд гаргахгүй, API-ийн оролт/гаралтыг тусад нь тодорхойлдог.
- **Enum файлууд** (жишээ нь `SystemStatus`, `TransferStatus`) — төлөв, төрлийн тогтмол утгууд.

Тохиргооны файлууд модулиас гадуур байрладаг:

- `src/main/resources/application.yml` — порт, өгөгдлийн сангийн холболт, JWT тохиргоо.
- `src/main/resources/db/migration/` — **Flyway** миграци. `V1__`, `V2__`... гэсэн дугаартай SQL файлууд сервис асахад дарааллаараа ажиллаж өгөгдлийн сангийн бүтцийг үүсгэнэ.
- `src/test/` — JUnit тестүүд (`application-test.yml` нь тестийн тусдаа тохиргоо).

---

## backend/platform-api

Үндсэн API. Java багцын зам: `src/main/java/mn/golomt/registry/`

| Багц | Үүрэг | Гол файлууд |
| --- | --- | --- |
| `auth/` | Нэвтрэлт: JWT токен олгох, одоогийн хэрэглэгчийн мэдээлэл | `AuthController`, `AuthService`, `JwtTokenService`, `DatabaseUserDetailsService`, `LoginRequest/LoginResponse` |
| `users/` | Хэрэглэгчийн entity ба эрхийн түвшин | `User`, `UserRepository`, `UserRole` |
| `systems/` | Системийн бүртгэл — CRUD, хайлт, шүүлт | `SystemController`, `SystemService`, `SystemEntity`, `SystemRepository`, `SystemSpecifications` (динамик шүүлт), `dto/` |
| `relations/` | Системүүд хоорондын холбоо (аль систем алинтай холбогддог) | `SystemRelation`, `SystemRelationRepository`, `RelationType` |
| `securitycheck/` | Аюулгүй байдлын checklist ба score тооцоолол | `SecurityCheckController`, `SecurityCheckService`, `SecurityControl` (шалгах стандартууд), `SecurityCheckResult` (систем бүрийн үр дүн), `dto/` |
| `audit/` | Үйлдлийн бүртгэл (хэн, юу, хэзээ хийсэн) | `AuditLogController`, `AuditLogService`, `AuditLog`, `AuditAction` |
| `common/` | Бүх модульд хамаарах алдааны боловсруулалт | `GlobalExceptionHandler`, `ErrorResponse`, `PageResponse`, `BadRequestException`, `ResourceNotFoundException` |
| `config/` | Spring Security ба JWT-ийн тохиргоо | `SecurityConfig`, `JwtEncoderConfig` |

Багцын гадна `PlatformApiApplication.java` — аппликейшнийг асаах entry point.

**resources:**

- `application.yml` — порт 8080, PostgreSQL (5432) холболт.
- `db/migration/V1__init_schema.sql` — бүх хүснэгтийн анхны бүтэц.
- `db/migration/V2__seed_security_controls.sql` — аюулгүй байдлын шалгах стандартуудын анхны өгөгдөл.
- `db/migration/V3__seed_demo_data.sql` — демо систем, хэрэглэгчийн өгөгдөл.

**test:** `auth`, `systems`, `securitycheck`, `audit` модуль бүрийн Controller тест.

---

## backend/banking-transfer-service

Платформ дээр бүртгэгддэг «жинхэнэ» сервисийн жишээ — данс хоорондын гүйлгээ хийдэг. Java багцын зам: `src/main/java/mn/golomt/banking/`

| Багц | Үүрэг | Гол файлууд |
| --- | --- | --- |
| `customer/` | Харилцагчийн entity | `Customer`, `CustomerRepository` |
| `account/` | Данс — үлдэгдэл, төлөв, хайлт | `AccountController`, `AccountService`, `Account`, `AccountStatus`, `dto/AccountResponse` |
| `transfer/` | Данс хоорондын шилжүүлэг | `TransferController`, `TransferService`, `Transfer`, `TransferStatus`, `dto/` |
| `ledger/` | Давхар бичилтийн (double-entry) журнал — гүйлгээ бүр дебит/кредит хоёр бичилт үүсгэнэ | `LedgerEntry`, `LedgerEntryType`, `LedgerEntryRepository`, `dto/LedgerEntryResponse` |
| `common/` | Алдааны боловсруулалт (platform-api-тай ижил загвар) | `GlobalExceptionHandler`, `ErrorResponse`, `PageResponse` |

**resources:** `application.yml` (порт 8084, тусдаа PostgreSQL 5433), `db/migration/V1__banking_schema.sql`, `V2__seed_demo_customers.sql`.

**test:** `TransferControllerTests` — шилжүүлгийн API-ийн тест.

---

## frontend

Vite + React + TypeScript. Бүх код `src/` дотор:

```text
frontend/src/
├── main.tsx            # Entry point — React app-ийг DOM-д холбоно
├── App.tsx             # Route-уудын тодорхойлолт (доорх хүснэгтийг үз)
├── styles.css          # Глобал стиль
├── api/                # Backend-тэй харьцах давхарга
│   ├── httpClient.ts   # Суурь HTTP клиент — хүсэлт бүрт JWT токен хавсаргана
│   ├── authApi.ts      # Нэвтрэлтийн API дуудлагууд
│   ├── systemsApi.ts   # Системийн бүртгэлийн API дуудлагууд
│   ├── securityApi.ts  # Аюулгүй байдлын checklist/score API дуудлагууд
│   └── auditApi.ts     # Audit log-ийн API дуудлагууд
├── auth/               # Нэвтрэлтийн төлөв (state)
│   ├── AuthContext.tsx       # Токен, хэрэглэгчийн мэдээллийг хадгалах React Context
│   ├── authContextValue.ts   # Context-ийн төрөл, анхны утга
│   └── useAuth.ts            # Context-оос уншдаг hook
├── components/         # Дахин ашиглагдах компонентууд
│   ├── AppShell.tsx          # Нийтлэг layout — sidebar, header
│   ├── ProtectedRoute.tsx    # Нэвтрээгүй хэрэглэгчийг /login руу чиглүүлнэ
│   ├── PageHeader.tsx        # Хуудасны гарчгийн хэсэг
│   ├── MetricCard.tsx        # Dashboard-ын тоон үзүүлэлтийн карт
│   ├── Chips.tsx             # Статус/төрлийн жижиг badge-ууд
│   └── States.tsx            # Loading / error / empty төлвийн харагдац
├── pages/              # Хуудас бүр нэг route
│   ├── LoginPage.tsx         # /login
│   ├── DashboardPage.tsx     # /dashboard — нийт үзүүлэлтүүд
│   ├── SystemsPage.tsx       # /systems — системийн жагсаалт, хайлт
│   └── AuditLogPage.tsx      # /audit-logs — үйлдлийн түүх
├── hooks/
│   └── useSystemsWithScores.ts  # Систем + security score-ийг хамт татдаг hook
├── types/
│   └── api.ts          # Backend-ийн response-уудын TypeScript төрлүүд
├── utils/
│   └── format.ts       # Огноо, тоо форматлах туслах функцууд
├── data/
│   └── mockData.ts     # Хөгжүүлэлтийн үеийн жишээ өгөгдөл
└── assets/             # Зураг, icon (hero.png, react.svg, vite.svg)
```

**Route-ууд** ([App.tsx](../frontend/src/App.tsx)):

| Зам | Хуудас | Хамгаалалт |
| --- | --- | --- |
| `/login` | LoginPage | Нээлттэй |
| `/dashboard` | DashboardPage | Нэвтэрсэн байх шаардлагатай |
| `/systems` | SystemsPage | Нэвтэрсэн байх шаардлагатай |
| `/audit-logs` | AuditLogPage | Нэвтэрсэн байх шаардлагатай |
| бусад бүх зам | `/dashboard` руу чиглүүлнэ | — |

`src/`-ийн гадна: `package.json` (сангууд, скриптүүд), `vite.config.ts` (Vite тохиргоо), `tsconfig*.json` (TypeScript тохиргоо), `index.html` (HTML суурь).

---

## docs/

| Файл | Агуулга |
| --- | --- |
| `architecture.md` | Архитектурын товч тойм — давхаргууд, модулиуд |
| `api-contract.md` | Backend API endpoint-уудын гэрээ (зам, оролт, гаралт) |
| `database.md` | Өгөгдлийн сангийн хүснэгтүүдийн тайлбар |
| `database-relations-and-dbrd.md` | Хүснэгт хоорондын холбоо, ERD |
| `assignment-summary.md` | Даалгаврын шаардлагын товчлол |
| `development-checklist.md` | Хөгжүүлэлтийн ажлын жагсаалт, ахиц (гол tracking файл) |
| `frontend-design-plan.md` | Frontend-ийн дизайн, хуудсуудын төлөвлөгөө |
| `ai-rules.md` | AI-тай ажиллах дүрэм |
| `file-structure.md` | Энэ баримт |

---

## Автоматаар үүсдэг зүйлс (гараар засахгүй)

| Зам | Юу вэ |
| --- | --- |
| `backend/*/target/` | Maven-ий build-ийн үр дүн — `mvn clean`-ээр устгаж болно |
| `frontend/node_modules/` | npm сангууд — `npm install`-аар дахин үүснэ |
| `frontend/dist/` | `npm run build`-ийн үр дүн |
| `*.log`, `*.err.log` | Сервис ажиллах үеийн лог файлууд |

Эдгээр нь `.gitignore`-д орсон тул git-д хадгалагдахгүй.
