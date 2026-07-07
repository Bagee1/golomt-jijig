# Banking Transfer Service — API Contract

Base URL: `http://localhost:8084`

Бүх endpoint (health/swagger-аас бусад) platform-api-ийн олгосон **Bearer JWT** шаардана.
Хоёр service ижил `JWT_SECRET`-тэй байх ёстой. Banking service өөрөө token олгодоггүй.

## Эрхийн загвар

| Дүр | Утга |
| --- | --- |
| `ADMIN` | Банкны теллер / back-office: бүх данс, гүйлгээ, харилцагч, буцаалт, аудит |
| Бусад (`VIEWER`, `SECURITY`) | Энгийн харилцагч: `customers.username` = JWT subject таарсан данснууд дээр л ажиллана |

Холболт: `customers.username` багана (V5 migration) нь platform хэрэглэгчийн username-тай тохирно.
Demo: `batbold` → CUST-0001 (данс 100000001), `sarnai` → CUST-0002 (данс 100000002), нууц үг `demo123`.

## Гүйлгээний төлөвийн lifecycle

```text
(шинэ) ──валидаци OK──► SUCCESS ──reversal──► REVERSED
   │
   └──бизнес дүрэм зөрчигдвөл──► FAILED (failureReason-той, ledger бичилтгүй, үлдэгдэл хөндөгдөөгүй)
```

- `PENDING` enum-д ирээдүйн async урсгалд зориулж нөөцлөгдсөн — sync урсгал хэзээ ч хадгалдаггүй.
- FAILED мөр нь аудитын зорилготой: INSUFFICIENT_FUNDS, ACCOUNT_INACTIVE, CURRENCY_MISMATCH,
  LIMIT_EXCEEDED шалтгаанаар л үүснэ. Данс олдоогүй / ижил данс / эзэмшлийн алдаа мөр үүсгэхгүй.
- FAILED мөрөнд Idempotency-Key хадгалагдахгүй — шалтгааныг засаад ижил key-ээр дахин оролдож болно.

## Лимитүүд (application.yml / env)

| Тохиргоо | Env | Default |
| --- | --- | --- |
| Нэг удаагийн дээд дүн | `BANKING_MAX_PER_TRANSFER` | 5,000,000.00 |
| Өдрийн нийт зарлага (данс тус бүр) | `BANKING_DAILY_OUTGOING_TOTAL` | 10,000,000.00 |

Өдрийн лимит SUCCESS + REVERSED гүйлгээг тооцно (буцаалт лимитийг тэглэдэггүй);
reversal гүйлгээ өөрөө лимит шалгалтад орохгүй (back-office засвар).

## Endpoints

### Transfers

| Method | Path | Эрх | Тайлбар |
| --- | --- | --- | --- |
| POST | `/api/transfers` | Нэвтэрсэн; non-ADMIN зөвхөн өөрийн данснаас | Optional `Idempotency-Key` header (≤80 тэмдэгт). Шинэ → 201, давталт → 200 + анхны transfer |
| GET | `/api/transfers?page=&size=` | ADMIN бүгд; бусад өөрийн оролцоотой | `createdAt desc` |
| GET | `/api/transfers/{id}` | ADMIN эсвэл оролцогч | `reversedByTransferId` талбартай |
| POST | `/api/transfers/{id}/reversal` | ADMIN | Зөвхөн SUCCESS гүйлгээг буцаана → 201 + буцаалтын transfer; давхар буцаалт → 409 `TRANSFER_NOT_REVERSIBLE` |

`TransferResponse` нэмэлт талбарууд: `failureReason` (ErrorCode нэр эсвэл null),
`reversalOfTransferId`, `reversedByTransferId`.

### Accounts

| Method | Path | Эрх | Тайлбар |
| --- | --- | --- | --- |
| GET | `/api/accounts/my` | Нэвтэрсэн хэн ч | Өөрийн данснууд (холбоогүй бол хоосон жагсаалт) |
| GET | `/api/accounts/{accountNo}` | ADMIN эсвэл эзэмшигч | |
| GET | `/api/accounts/{accountNo}/statement?from=&to=&page=&size=` | ADMIN эсвэл эзэмшигч | ISO огноо, default сүүлийн 30 хоног. Opening balance = одоогийн үлдэгдэл − эхлэлээс хойшхи signed нийлбэр (seed үлдэгдэл ledger-гүйг зөв тооцно) |
| GET | `/api/accounts?customerNo=&page=&size=` | ADMIN | |
| POST | `/api/accounts` | ADMIN | `{customerNo, currency?, initialBalance?}` — дугаар sequence-ээс; initialBalance нь ledger-гүй demo shortcut |
| POST | `/api/accounts/{accountNo}/block` | ADMIN | ACTIVE → BLOCKED |
| POST | `/api/accounts/{accountNo}/unblock` | ADMIN | BLOCKED → ACTIVE |
| POST | `/api/accounts/{accountNo}/close` | ADMIN | Үлдэгдэл 0 үед л; CLOSED = эцсийн төлөв |

### Customers (бүгд ADMIN)

| Method | Path | Тайлбар |
| --- | --- | --- |
| GET | `/api/customers?q=&page=&size=` | Нэр/дугаараар хайлт |
| GET | `/api/customers/{id}` | Данснуудын хамт |
| POST | `/api/customers` | `customer_no` sequence-ээс (`CUST-1001`, ...); username давхардвал 409 `USERNAME_TAKEN` |
| PUT | `/api/customers/{id}` | |
| POST | `/api/customers/{id}/deactivate` | Устгал байхгүй. `active=false` нь бүртгэлийн шинжтэй — гүйлгээг дансны төлөв (BLOCKED/CLOSED) л хаана |

### Audit

| Method | Path | Эрх |
| --- | --- | --- |
| GET | `/api/audit-logs?page=&size=` | ADMIN |

Actions: `TRANSFER_CREATED/FAILED/REVERSED`, `ACCOUNT_OPENED/BLOCKED/UNBLOCKED/CLOSED`,
`CUSTOMER_CREATED/UPDATED/DEACTIVATED`. Actor нь JWT-ээс авсан snapshot
(username/displayName/role) — хэрэглэгчид platform DB-д байдаг тул FK байхгүй.
`REQUIRES_NEW` transaction тул FAILED transfer-ийн аудит ч хадгалагдана.

## Алдааны формат

```json
{
  "timestamp": "2026-07-07T12:00:00+08:00",
  "status": 400,
  "error": "Bad Request",
  "code": "INSUFFICIENT_FUNDS",
  "message": "Insufficient balance for account: 100000002",
  "path": "/api/transfers"
}
```

### ErrorCode ба HTTP статус

| Code | HTTP | Хэзээ |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Bean validation, огнооны хүрээ буруу, дүнгийн бутархай >2 орон |
| `SAME_ACCOUNT` | 400 | Илгээгч = хүлээн авагч |
| `INSUFFICIENT_FUNDS` | 400 | Үлдэгдэл хүрэлцэхгүй (шилжүүлэг ба reversal) |
| `ACCOUNT_INACTIVE` | 400 | BLOCKED/CLOSED данс оролцсон |
| `CURRENCY_MISMATCH` | 400 | Валют зөрсөн |
| `LIMIT_EXCEEDED` | 400 | Нэг удаагийн эсвэл өдрийн лимит |
| `ACCOUNT_NOT_EMPTY` | 400 | Үлдэгдэлтэй данс хаах гэсэн |
| `INVALID_STATUS_TRANSITION` | 400 | Буруу төлөвийн шилжилт |
| `FORBIDDEN_ACCOUNT` | 403 | Бусдын данс/гүйлгээ рүү хандсан |
| `FORBIDDEN` | 403 | Role хүрэлцэхгүй (method security) |
| `ACCOUNT_NOT_FOUND` / `CUSTOMER_NOT_FOUND` / `TRANSFER_NOT_FOUND` | 404 | |
| `TRANSFER_NOT_REVERSIBLE` | 409 | SUCCESS биш гүйлгээг буцаах гэсэн (давхар буцаалт орно) |
| `USERNAME_TAKEN` | 409 | Username өөр харилцагчид холбоотой |
| `DUPLICATE_REQUEST` | 409 | Unique constraint зөрчил (зэрэгцээ давхардал) |
| `INTERNAL_ERROR` | 500 | |
