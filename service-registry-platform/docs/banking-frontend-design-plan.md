# Banking Transfer Service — Frontend Design Plan

Огноо: 2026-07-06

> **Шинэчлэл (2026-07-07):** Энэ баримтын хэрэгжилт өргөжсөн. Banking UI одоо портал доторх
> хэсэг ч биш, build-mode switch ч биш — **бүрэн тусдаа `frontend-banking/` Vite төсөл (порт 5174)**.
> Backend-ийн API олон дахин өргөжсөн (status lifecycle, reversal, statement, эзэмшил, лимит,
> харилцагч/данс удирдлага, audit) — бүрэн contract-ыг [banking-api-contract.md](banking-api-contract.md)-аас харна.
> Нэмэгдсэн дэлгэцүүд: Хуулга (`/accounts/:no/statement`), теллерийн Харилцагчид/Данс удирдлага/Аудит лог
> (`/admin/...`), гүйлгээ буцаах товч. Route-ууд `/banking` prefix-гүй root түвшинд шилжсэн.
> Доорх агуулга нь анхны (2026-07-06) загварын түүхэн баримт хэвээр.

Энэ баримт нь `backend/banking-transfer-service`-д зориулсан frontend-ийн загвар. Ерөнхий өнгө, typography, компонентын хэв маяг нь [frontend-design-plan.md](frontend-design-plan.md)-д тодорхойлсонтой ижил байна — энд зөвхөн banking-д хамаарах дэлгэц, компонент, API холболтыг тодорхойлно.

## 1. Backend-ийн бодит API (энэ загварын хязгаар)

Frontend-ийг зөвхөн одоо байгаа endpoint-уудад тааруулж загварласан:

| Endpoint | Оролт | Гаралт |
| --- | --- | --- |
| `GET /api/accounts/{accountNo}` | Дансны дугаар (path) | `AccountResponse` — accountNo, customerNo, customerName, currency, balance, status, createdAt, updatedAt |
| `POST /api/transfers` | `TransferRequest` — fromAccountNo, toAccountNo, amount (≥ 0.01, 2 орон), description (≤ 500 тэмдэгт); нэмэлт `Idempotency-Key` header (≤ 80 тэмдэгт) | Шинэ бол `201` + `TransferResponse` + `ledgerEntries[]`; ижил key давтвал `200` + анхны transfer (дахин гүйцэтгэхгүй) |
| `GET /api/transfers?page=&size=` | Хуудаслалт (size ≤ 100, createdAt DESC) | `PageResponse<TransferResponse>` |
| `GET /api/transfers/{id}` | Гүйлгээний id | `TransferResponse` + `ledgerEntries[]` |

`LedgerEntryResponse`: accountNo, entryType (`DEBIT`/`CREDIT`), amount, balanceAfter, createdAt.

**Загварт нөлөөлөх хязгаарлалтууд:**

- Дансны **жагсаалтын endpoint байхгүй** — зөвхөн дугаараар нэг данс татна. Тиймээс «бүх дансны table» биш, **данс хайх (lookup)** хэлбэрээр загварлана.
- `TransferStatus` одоогоор зөвхөн `SUCCESS` — алдаатай гүйлгээ HTTP error-оор буцдаг тул status шүүлт хэрэггүй.
- Banking service **platform-api-ийн JWT-г шаарддаг** (2026-07-07-оос, shared `JWT_SECRET`) — эдгээр хуудсыг platform-ийн `ProtectedRoute` дотор байрлуулж, banking хүсэлт бүрд мөн `Authorization: Bearer` header явуулна.
- Демо өгөгдөл: `100000001` (Bat Bold, 1,000,000 MNT), `100000002` (Sarnai Erdene, 500,000 MNT).

## 2. Байршуулах хэлбэр

Тусдаа апп хийхгүй — **одоо байгаа frontend портал дотор «Banking Demo» хэсэг** болгож нэмнэ:

- `AppShell` sidebar-д шинэ nav бүлэг: **Banking Demo** (icon: `Landmark` эсвэл `ArrowLeftRight`).
- Одоогийн `Chips`, `States`, `PageHeader`, `MetricCard` компонентуудыг дахин ашиглана.
- API клиент тусдаа: banking service өөр порт (8084) дээр тул `httpClient.ts`-ээс тусдаа `bankingHttpClient.ts` үүсгэнэ (base URL: `http://localhost:8084`); platform-тай ижил JWT token-ийг `Authorization` header-ээр явуулна, transfer form нь давхар илгээлтээс хамгаалж `Idempotency-Key` үүсгэж дамжуулна.

## 3. Дэлгэцүүд

| # | Дэлгэц | Route | Зорилго |
| ---: | --- | --- | --- |
| 1 | Banking overview | `/banking` | Демо 2 дансны үлдэгдэл + сүүлийн гүйлгээнүүд + «New transfer» товч |
| 2 | Account lookup | `/banking/accounts` | Дансны дугаараар хайж үлдэгдэл, эзэмшигч харах |
| 3 | New transfer | `/banking/transfers/new` | Шилжүүлгийн form + амжилтын үр дүн |
| 4 | Transfers list | `/banking/transfers` | Хуудаслалттай гүйлгээний түүх |
| 5 | Transfer detail | `/banking/transfers/:id` | Нэг гүйлгээ + давхар бичилтийн ledger харагдац |

### 3.1 Banking overview (`/banking`)

```text
┌──────────────────────────────────────────────────────┐
│ Banking Transfer Demo            [ + New transfer ]  │
├──────────────────────────────────────────────────────┤
│ ┌──────────────────┐  ┌──────────────────┐           │
│ │ 100000001        │  │ 100000002        │           │
│ │ Bat Bold         │  │ Sarnai Erdene    │           │
│ │ 1,000,000 MNT    │  │ 500,000 MNT      │           │
│ │ ● ACTIVE         │  │ ● ACTIVE         │           │
│ └──────────────────┘  └──────────────────┘           │
├──────────────────────────────────────────────────────┤
│ Recent transfers (сүүлийн 5)                         │
│ Ref | From → To | Amount | Status | Date             │
└──────────────────────────────────────────────────────┘
```

- Дансны картууд: демо 2 дансыг `GET /api/accounts/100000001`, `/100000002`-оор зэрэг татна (дансны дугааруудыг config/constant-д хадгална).
- Recent transfers: `GET /api/transfers?size=5`.
- Гүйлгээ хийсний дараа энэ хуудас руу буцахад үлдэгдэл шинэчлэгдсэн харагдана — demo-гийн гол «үзүүлэх мөч».

### 3.2 Account lookup (`/banking/accounts`)

- `SearchInput`: дансны дугаар оруулаад Enter → `AccountBalanceCard` харуулна.
- Олдохгүй бол (404) `ErrorState`: «Данс олдсонгүй».
- Картан дээр: accountNo, customerName, customerNo, balance (`MoneyDisplay`), currency, status chip, updatedAt.

### 3.3 New transfer (`/banking/transfers/new`)

Form талбарууд (backend validation-тай яг тааруулсан):

| Талбар | Компонент | Validation |
| --- | --- | --- |
| From account | Text input + «Check» товч (үлдэгдэл урьдчилж харуулах) | Хоосон биш |
| To account | Text input + «Check» товч | Хоосон биш, From-оос ялгаатай (client талд) |
| Amount | Number input, MNT suffix | ≥ 0.01, дээд тал нь 2 орны бутархай |
| Description | Textarea | ≤ 500 тэмдэгт, заавал биш |

Урсгал:

1. From/To дансыг «Check» дарж баталгаажуулбал дансны эзний нэр, үлдэгдэл form дотор харагдана (`GET /api/accounts/{no}`).
2. Submit → `POST /api/transfers`.
3. Амжилттай бол **TransferSuccessPanel**: transferRef, amount, шинэ ledger бичилтүүд (DEBIT улаан, CREDIT ногоон, balanceAfter-тай) + «View transfer», «Back to overview» товчнууд.
4. Алдаа (үлдэгдэл хүрэлцэхгүй, данс идэвхгүй г.м.) → backend-ийн `ErrorResponse.message`-ийг form дээр `AuthErrorAlert`-тай ижил хэв маягийн alert-аар харуулна.

### 3.4 Transfers list (`/banking/transfers`)

Table багана:

```text
Transfer ref
From → To
Amount (MNT)
Description
Status
Created at
```

- Эрэмбэ backend-ээс createdAt DESC ирдэг тул sort UI хэрэггүй.
- `PaginationControls` — page/size (`size` дээд тал нь 100).
- Мөр дарвал `/banking/transfers/:id` руу орно.
- Empty state: «Гүйлгээ алга. New transfer дарж эхлүүлнэ үү.»

### 3.5 Transfer detail (`/banking/transfers/:id`)

- Толгой: transferRef, status chip, createdAt, amount том үсгээр.
- From/To дансны мэдээлэл хоёр багана.
- **Ledger entries хүснэгт** — double-entry-г үзүүлэх гол хэсэг:

```text
Account    | Type   | Amount     | Balance after | Created at
100000001  | DEBIT  | -50,000    | 950,000       | ...
100000002  | CREDIT | +50,000    | 550,000       | ...
```

DEBIT мөрөнд дүнг `-` тэмдэгтэй улаанаар, CREDIT-ийг `+` ногооноор харуулна.

## 4. Шинэ компонентууд

| Компонент | Үүрэг |
| --- | --- |
| `AccountBalanceCard` | Данс: дугаар, эзэмшигч, үлдэгдэл, status chip |
| `TransferForm` | From/To/Amount/Description + inline account check |
| `TransferSuccessPanel` | Амжилттай гүйлгээний үр дүн + ledger |
| `TransfersTable` | Гүйлгээний жагсаалт |
| `LedgerEntriesTable` | DEBIT/CREDIT бичилтүүд, balanceAfter |
| `LedgerTypeChip` | DEBIT (улаан) / CREDIT (ногоон) |
| `MoneyDisplay` | MNT форматлагч — одоогийн `utils/format.ts`-д нэмнэ |

Дахин ашиглах: `PageHeader`, `MetricCard`, `States` (Loading/Error/Empty), `Chips`, `AppShell`, `ProtectedRoute`.

## 5. API ба төрлийн модулиуд

| Файл | Агуулга |
| --- | --- |
| `api/bankingHttpClient.ts` | Base URL `http://localhost:8084` (env хувьсагч `VITE_BANKING_API_URL`), JWT interceptor-гүй |
| `api/bankingApi.ts` | `getAccount(accountNo)`, `createTransfer(req)`, `listTransfers(page, size)`, `getTransfer(id)` |
| `types/banking.ts` | `AccountResponse`, `TransferRequest`, `TransferResponse`, `LedgerEntryResponse`, `PageResponse<T>` — backend record-уудтай нэг нэгээр тааруулна |
| `data/bankingDemo.ts` | Демо дансны дугаарууд (`100000001`, `100000002`) |

## 6. Route map (нэмэгдэх хэсэг)

| Route | Page |
| --- | --- |
| `/banking` | `BankingOverviewPage` |
| `/banking/accounts` | `AccountLookupPage` |
| `/banking/transfers` | `TransfersPage` |
| `/banking/transfers/new` | `NewTransferPage` |
| `/banking/transfers/:id` | `TransferDetailPage` |

Бүгд `ProtectedRoute` + `AppShell` дотор. Sidebar-д «Banking Demo» бүлэг нэмэгдэнэ.

## 7. Хийх дараалал

1. `types/banking.ts` + `api/bankingHttpClient.ts` + `api/bankingApi.ts` (backend аль хэдийн бэлэн тул эхлээд холболт).
2. `.env`-д `VITE_BANKING_API_URL` нэмэх; banking service-ийн CORS-д `127.0.0.1:5173`-ийг зөвшөөрөх эсэхийг шалгах.
3. `MoneyDisplay`/format нэмэлт, `LedgerTypeChip`.
4. Transfers list + detail (унших урсгал эхэлж — өгөгдөл харагдвал бусдыг шалгахад амар).
5. New transfer form + success panel.
6. Banking overview + account lookup.
7. Sidebar nav + route бүртгэл, empty/loading/error states шалгах.

## 8. Design checklist

- Мөнгөн дүн бүх газар мянгачилсан форматтай, MNT нэгжтэй.
- DEBIT/CREDIT өнгө (улаан/ногоон) бүх дэлгэц дээр зөрөхгүй.
- Гүйлгээ амжилттай болмогц үлдэгдлийн өөрчлөлт overview дээр шууд харагдана.
- Backend-ийн validation алдааны message form дээр ойлгомжтой гарна.
- Account check хийхгүйгээр ч шууд submit хийж болно (check нь заавал биш, туслах).
- Banking service унтарсан үед бүх banking хуудас `ErrorState`-тай, portal-ийн бусад хэсэг хэвийн ажиллана.
