# Deposit Service — API Contract

Огноо: 2026-07-08

`deposit-service` (порт 8085, өөрийн Postgres 5434) нь хугацаатай хадгаламжийн demo service.
Онцлог нь **banking-transfer-service-ийг жинхэнэ HTTP-ээр дууддаг** — хадгаламж нээхэд харилцагчийн
данснаас settlement данс руу, хаахад settlement данснаас харилцагч руу мөнгө шилжүүлнэ. Ингэснээр
системийн бүртгэлийн `deposit-service CALLS banking-transfer-service` хамаарал бодит болдог.

## 1. Эрхийн загвар

- Бүх endpoint platform-api-ийн JWT шаарддаг (shared `JWT_SECRET`, HS256 resource server).
- Харилцагч (VIEWER) зөвхөн **өөрийн** хадгаламжийг харж, нээж, хаана (JWT subject ↔ `deposits.customer_username`).
- ADMIN (теллер) бүх хадгаламжийг харна; `GET /api/deposits`, `GET /api/audit-logs` нь ADMIN.
- **svc-deposit** (service account, VIEWER): banking талд settlement данс `900000001`-ийн эзэн
  (`customers.username='svc-deposit'`). Хүн биш — зөвхөн эргэн төлөлтийн гүйлгээнд ашиглагдана.
  ADMIN эрхгүй тул least-privilege.

## 2. Мөнгөний урсгал (settlement загвар)

```
Нээх (fund):   харилцагчийн данс ──[banking POST /api/transfers, ХАРИЛЦАГЧИЙН token]──▶ settlement 900000001
Хаах (payout): settlement 900000001 ──[banking POST /api/transfers, svc-deposit token]──▶ харилцагчийн данс
```

- Нээхэд харилцагчийн өөрийн JWT-г banking руу дамжуулна → banking-ийн «зөвхөн өөрийн данснаас»
  дүрэм зөвшөөрлийг өөрөө шалгана.
- Хаахад svc-deposit нь settlement дансны эзэн тул тэр л данснаас гаргаж чадна.
- Idempotency: нээх нь `dep-{depositNo}-fund`, хаах нь `dep-{depositNo}-payout` — banking-ийн
  200-replay-ээр timeout/давталтад at-most-once баталгаа.

## 3. Статусын машин

```
FUNDING ──banking OK──▶ OPEN ──close (matured)──▶ CLOSED         (үндсэн + хүү)
   │                      │    ──close (early)────▶ CLOSED_EARLY  (зөвхөн үндсэн, хүү 0)
   │ banking 4xx          │ payout амжилтгүй
   ▼                      ▼
CANCELLED            PAYOUT_PENDING ──close дахин (ижил key, хадгалсан дүн)──▶ CLOSED*
   ▲
FUNDING ──banking хүрэхгүй──▶ FUNDING хэвээр (502) ──POST /{id}/retry-funding──▶ OPEN / CANCELLED
```

Чухал: анхны close дээр `interest_amount`, `payout_amount`, `close_type`-ийг банк дуудахаас **өмнө**
хадгална. Retry нь хадгалсан дүнг дахин ашиглах тул maturity хил давсан ч banking-ийн replay-тай зөрөхгүй.

## 4. Хүүгийн томьёо

```
interest = principal × annualRate × termDays / (365 × 100),  HALF_UP, 2 орон
termDays = нээсэн өдрөөс maturityDate хүртэл (maturityDate = openedDate + termMonths сар)
```

Жишээ: 1,000,000 @ 12.5% / 12 сар (365 хоног) → 125,000.00. Хугацаанаас өмнө хаавал хүү **0**.

## 5. Endpoint-ууд

| Method | Path | Эрх | Тайлбар |
| --- | --- | --- | --- |
| GET | `/api/deposit-products` | authenticated | Бүтээгдэхүүн (3/6/12 сар, жилийн хүү, min/max) |
| POST | `/api/deposits` | owner | Хадгаламж нээх. Header `Idempotency-Key` (≤80, заавал биш): шинэ бол 201, давтвал 200 |
| POST | `/api/deposits/{id}/retry-funding` | owner\|ADMIN | FUNDING төлөвт байгаа хадгаламжийн санхүүжилтийг дахин оролдох |
| POST | `/api/deposits/{id}/close` | owner\|ADMIN | Хаах (matured→CLOSED хүүтэй, эрт→CLOSED_EARLY хүүгүй); PAYOUT_PENDING-д дахин оролдоно |
| GET | `/api/deposits/my` | authenticated | Өөрийн хадгаламжууд (paginated) |
| GET | `/api/deposits/{id}` | owner\|ADMIN | Нэг хадгаламж |
| GET | `/api/deposits?username=&page=&size=` | ADMIN | Бүх хадгаламж, username-ээр шүүх |
| GET | `/api/audit-logs?page=&size=` | ADMIN | Хадгаламжийн аудит лог |

`DepositResponse`: id, depositNo, customerUsername, linkedAccountNo, principal, annualRate, termMonths,
openedAt, maturityDate, status, closeType, interestAmount, payoutAmount, **projectedInterest** (хугацаанд нь
хаавал авах хүү), **matured** (bool), fundingTransferRef, payoutTransferRef, failureReason, closedAt.

## 6. Идемпотентийн 3 түвшин

1. **Browser** — `POST /api/deposits`-ийн `Idempotency-Key` (нэг форм бөглөлтөд `crypto.randomUUID()`):
   давхар дарахад давхар хадгаламж нээгдэхгүй (`deposits.client_request_key` unique).
2. **Fund** — banking руу `dep-{no}-fund`: retry-funding давтахад давхар татахгүй.
3. **Payout** — banking руу `dep-{no}-payout`: close давтахад давхар төлөхгүй.

## 7. ErrorCode ↔ HTTP

| Code | HTTP | Тайлбар | Гарал |
| --- | --- | --- | --- |
| VALIDATION_ERROR | 400 | Талбарын шалгалт | deposit |
| PRODUCT_NOT_FOUND | 400 | Ийм хугацааны бүтээгдэхүүн алга | deposit |
| AMOUNT_OUT_OF_RANGE | 400 | Дүн min/max-аас гадуур | deposit |
| DEPOSIT_NOT_FOUND | 404 | Хадгаламж алга | deposit |
| DEPOSIT_ALREADY_CLOSED | 409 | Аль хэдийн хаагдсан | deposit |
| INVALID_STATUS_TRANSITION | 409 | Буруу төлөвөөс хаах/санхүүжүүлэх | deposit |
| DUPLICATE_REQUEST | 409 | client_request_key давхардал | deposit |
| FORBIDDEN | 403 | Өөрийн бус хадгаламж | deposit |
| BANKING_UNAVAILABLE | 502 | Банк хүрэхгүй (retry аюулгүй) | deposit |
| FUNDING_FAILED / PAYOUT_FAILED | 400 | Банк татгалзсан (тодорхойгүй) | deposit |
| INSUFFICIENT_FUNDS | 400 | Үлдэгдэл хүрэлцэхгүй | banking (pass-through) |
| ACCOUNT_INACTIVE | 400 | Данс идэвхгүй | banking (pass-through) |
| LIMIT_EXCEEDED | 400 | Гүйлгээний лимит | banking (pass-through) |
| FORBIDDEN_ACCOUNT | 403 | Тухайн данс руу эрхгүй | banking (pass-through) |

Pass-through кодуудыг banking-ийн хариунаас яг тэр хэвээр дамжуулна тул frontend-ийн орчуулга дахин ашиглагдана.

## 8. Тохиргоо (env)

| Хувьсагч | Default | Тайлбар |
| --- | --- | --- |
| `DEPOSIT_SERVICE_PORT` | 8085 | HTTP порт |
| `DEPOSIT_DB_*` | 5434/deposit_service | Өөрийн Postgres |
| `JWT_SECRET` | — | 3 service-д ижил, fallback-гүй |
| `BANKING_API_URL` | http://localhost:8084 | banking-transfer-service |
| `PLATFORM_API_URL` | http://localhost:8080 | platform-api (svc login) |
| `SVC_DEPOSIT_USERNAME/PASSWORD` | svc-deposit / demo | Settlement payout-ийн service account |
| `DEPOSIT_MIN_AMOUNT/MAX_AMOUNT` | 100,000 / 3,000,000 | Хадгаламжийн хязгаар |
| `app.deposit.products` | 3→8%, 6→10%, 12→12.5% | Бүтээгдэхүүн (yml) |

## 9. Demo-гийн хялбарчлал

- Хүү нь settlement дансны 100,000,000₮ demo сангаас гарна (бодит банкинд хүүгийн зардлын данс байх ёстой).
- deposit max default 3,000,000 — banking-ийн 5,000,000/гүйлгээ лимитэд үндсэн+хүү багтахын тулд.
- Эрт хаалт хүүг 0 болгодог (энгийн торгууль). Бууруулсан хувиар тооцох нь ирээдүйн ажил.
- ADMIN нь дурын данснаас хадгаламж нээж чадна (banking ADMIN-д зөвшөөрдөг); тухайн мөр ADMIN-ий
  username дор бүртгэгдэнэ — demo caveat.
