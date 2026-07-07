# Систем бүртгэж хянах хэсгийн үнэлгээ ба шинэ сервисийг хянах бэлтгэл

| Талбар | Утга |
| --- | --- |
| Огноо | 2026-07-08 |
| Шалтгаан | Шинэ хадгаламжийн сервис (deposit-service) нэмэгдэж banking-transfer-service-тэй холбогдох гэж байгаа тул бүртгэл-хяналтын хэсэг (platform-api + portal) үүнийг зөв зохицуулж чадах эсэхийг үнэлэх |
| Арга | Кодын шинжилгээ (platform-api, frontend) + анхны концепцтой харьцуулалт |
| Үр дүн | Хийх зүйлс → `docs/development-checklist.md` §4.13 |

---

## 1. Судалгааны асуулт

1. Систем бүртгэж хянах хэсэг маань **одоо яг зөв ажиллаж байна уу?**
2. Шинээр нэмэгдэх deposit-service-ийг **хэрхэн хянах вэ, ямар засвар хэрэгтэй вэ?**

## 2. Одоогийн чадварын үнэлгээ

### 2.1. Зөв ажиллаж байгаа хэсэг ✅

| Чадвар | Байдал | Нотолгоо |
| --- | --- | --- |
| Систем бүртгэх (бүх талбар, unique key, огнооны дүрэм) | Бүрэн ажиллана, 7 тесттэй | `systems/SystemService.java`, `SystemControllerTests` |
| Жагсаалт/хайлт/шүүлт/pagination | Бүрэн ажиллана | `systems/SystemSpecifications.java` |
| Холбоотой системүүд (`CALLS`/`DEPENDS_ON`/`INTEGRATES_WITH`) | Бүрэн ажиллана — deposit→banking хамаарлыг бүртгэхэд бэлэн | `relations/SystemRelation.java` |
| Security checklist + жинлэсэн score | Бүрэн ажиллана, 9 тесттэй | `securitycheck/SecurityCheckService.java` |
| Аудит (бүртгэл/засвар/чеклистийн өөрчлөлт) | Бүрэн ажиллана | `audit/AuditLogService.java` |
| Эрхийн хяналт (ADMIN бичих, SECURITY checklist) | Бүрэн ажиллана, RBAC 7 тесттэй | `config/SecurityConfig.java` |

**Дүгнэлт:** шинэ сервисийг *бүртгэхэд* платформын код өөрчлөх шаардлагагүй — ADMIN формоор бүртгээд relations, checklist-ээ бөглөхөд л хангалттай.

### 2.2. Нэрийн төдий байгаа хэсэг ⚠️ — «хянах» функц

| Асуудал | Бодит байдал | Нотолгоо |
| --- | --- | --- |
| `health_url` хэзээ ч дуудагддаггүй | Талбар зөвхөн хадгалагдаж, DTO-оор буцдаг. platform-api-д `RestTemplate`/`WebClient`/`@Scheduled` огт байхгүй | grep: `SystemService.java:115,133` зөвхөн setter; HTTP client код 0 |
| `status` (ACTIVE/DOWN…) нь ажиглалт биш, гар мэдүүлэг | Админ формын `<select>`-ээс сонгодог; систем унасан ч DOWN болохгүй | `SystemFormPage.tsx:308` |
| Dashboard-ын «идэвхтэй/унасан» тоолол хуурамч амьд байдал харуулна | Гар статусын тоололт л хийдэг | `DashboardPage.tsx:19-21` |
| Health/Swagger нь энгийн гадаад линк | Дарж очиж болно, гэхдээ платформ өөрөө шалгадаггүй | `SystemDetailPage.tsx:146-147` |
| `security_controls.automated=true` хэрэгждэггүй | `HTTPS_ENABLED` automated гэж тэмдэглэгдсэн ч автомат шалгалт байхгүй, бүх үр дүн гараар | `V2__seed_security_controls.sql:3` |
| Health түүх хадгалагддаггүй | `system_health_checks` хүснэгт DBRD §11-д «ирээдүйд» гэж туссан, хийгдээгүй | `docs/database-relations-and-dbrd.md` |

Анхны концепц (`төслийн concept/гол агуулга`) «Бүртгэсэн Service болгон `GET /actuator/health` дууддаг, React дээр 🟢 UP / 🔴 DOWN харагдана» гэж заасан — энэ хэсэг **хэрэгжээгүй**.

### 2.3. Ерөнхий дүгнэлт

**Бүртгэх** хэсэг зөв, тесттэй ажиллаж байна. **Хянах** хэсэг одоогоор өөрөө-мэдүүлсэн статус дээр тогтдог тул: (а) banking-transfer-service унасан ч каталог ACTIVE гэж харуулсаар байна; (б) deposit-service нэмэгдэхэд «хянана» гэдэг нь бодит утгагүй хэвээр үлдэнэ. Deposit-service нь платформын хяналтын үнэ цэнийг харуулах 3 дахь бодит сервис болох тул яг одоо энэ цоорхойг хаах нь зөв цаг.

---

## 3. Хийх засварууд (registry талд)

### 3.1. R1 — On-demand health check (эхний алхам, deposit нэмэгдэхээс өмнө banking дээр туршина)

- Шинэ endpoint: `POST /api/systems/{id}/health-check` (ADMIN/SECURITY).
- Backend нь хадгалсан `health_url` руу server-side HTTP GET хийж actuator JSON-ы `status` утгыг уншина (`UP` → UP, бусад/алдаа/timeout → DOWN).
- Үр дүн: шинэ талбаруудад бичигдэнэ (§3.2), audit-д `HEALTH_CHECKED` action бичнэ, response-оор шууд буцаана.
- Portal: System detail дээр «Шалгах» товч + үр дүнгийн chip; Systems list дээр runtime chip.
- Хамгаалалт (админ оруулсан URL ч гэсэн): зөвхөн `http/https` scheme, connect/read timeout 2-3 сек, redirect дагахгүй, хариуг ≤ жижиг хэмжээгээр унших. Тохиргоогоор host allowlist (`app.health.allowed-hosts`, default localhost) — SSRF-ээс сэргийлнэ.

### 3.2. R2 — Статусын загварыг салгах (бүртгэлийн төлөв ≠ ажиллагааны төлөв)

Одоогийн `status` нь lifecycle (ACTIVE/INACTIVE — админ шийддэг) болон runtime (UP/DOWN — ажиглагдах ёстой) хоёрыг хольсон. Засвар:

- Flyway V6 (platform): `systems`-д `runtime_status varchar(10)` (`UP/DOWN/UNKNOWN`, default UNKNOWN), `last_health_check_at timestamptz`, `last_health_error varchar(500)` нэмнэ.
- `status`-ыг бүртгэлийн төлөв болгож үлдээнэ (формын сонголтоос `DOWN/UNKNOWN`-г хасах); runtime_status-ыг зөвхөн health check бичнэ.
- Dashboard: «Ашиглалтад байгаа» (lifecycle) ба «Амьд байдал UP/DOWN/UNKNOWN» (runtime) тусдаа тоололтой болно.

### 3.3. R3 — Тогтмол хяналт + түүх

- `@Scheduled` poller (интервал `app.health.poll-interval`, default 60с, унтраах боломжтой) health_url-тай, `in_use=true` бүх системийг шалгана.
- `system_health_checks` хүснэгт (DBRD §11-ийн санал): system FK, шалгасан огноо, үр дүн, хариу хугацаа (ms), алдааны товч. Detail хуудсанд сүүлийн N шалгалтын түүх.
- Төлөв өөрчлөгдөх үед (UP→DOWN, DOWN→UP) audit бичих — «Notification» концепцийн хамгийн хямд эхлэл.

### 3.4. R4 — Automated security checks (сонголттой, гуравдугаарт)

`automated=true` control-уудыг health check-тэй хамт автоматаар бөглөх: `HTTPS_ENABLED` (health_url scheme https эсэх), health endpoint хүрэгдэж буй эсэх, `SWAGGER_PROTECTED` (swagger_url token-гүй 200 буцаавал FAIL). Гар үр дүнг дарж бичихгүй — зөвхөн `NOT_CHECKED` байгаа үед санал болгох эсвэл evidence-д тэмдэглэх.

### 3.5. Хамрах хүрээний тодруулга

Registry нь **сервисийн түвшний** хяналт (амьд эсэх, стандарт хангаж буй эсэх) хийнэ. Deposit↔banking хоорондын **бизнес түвшний** хяналт (шилжүүлэг амжилттай эсэх, тооцооны дансны үлдэгдэл) нь сервисүүдийн өөрсдийн аудит/ErrorCode-ын ажил — registry-д давхардуулахгүй.

---

## 4. Deposit-service-ийг хянах онбординг (кодгүй, процесс)

Deposit-service бэлэн болмогц registry талд хийх алхмууд:

1. **Бүртгэх** (ADMIN формоор): нэр «Deposit Service», type `CORE`, base `http://localhost:8085`, health `http://localhost:8085/actuator/health`, swagger `http://localhost:8085/swagger-ui/index.html`, хөгжүүлэгч/үнэлгээ/хугацаа, `inUse=true`.
2. **Хамаарал:** Deposit Service `CALLS` Banking Transfer Service (мөнгөн урсгал), шаардлагатай бол `INTEGRATES_WITH` Digital Banking.
3. **Checklist:** 8 control-ыг бөглөж score авах (deposit-service нь banking-тай ижил хэв маягаар JWT resource server, аудит, env-secret хийвэл өндөр score гарахаар).
4. **Хяналт:** R1/R3 хэрэгжсэн байвал health check ногоон эсэхийг шалгах; UP/DOWN chip ажиглагдана.
5. **Аудит:** бүртгэлийн `SYSTEM_CREATED`, checklist-ийн `SECURITY_CHECK_UPDATED` бичлэгүүд үүссэнийг нягтлах.

Энэ бүхэлдээ платформын кодыг өөрчлөхгүй — §3-ын засварууд нь deposit-д тусгайлсан биш, **бүх** бүртгэлтэй системийн хяналтыг бодит болгож байгаа юм (banking дээр шууд туршиж болно).

---

## 5. Ажлын жагсаалт ба дараалал

| Үе шат | Агуулга | Хамаарал |
| --- | --- | --- |
| R1 | On-demand health check + UI товч/chip | Юунаас ч хамаарахгүй, banking дээр туршина |
| R2 | runtime_status салгах (Flyway V6) + dashboard засвар | R1-тэй хамт хийвэл зүгээр |
| R3 | Scheduled poller + `system_health_checks` түүх + төлөв өөрчлөлтийн audit | R1/R2 дууссаны дараа |
| R4 | Automated security checks | Сонголттой |
| Онбординг | §4-ийн процесс | deposit-service бэлэн болсны дараа |

Тестийн төлөвлөгөө: `MockRestServiceServer`-ээр UP/DOWN/timeout/алдаатай JSON хариуг дуурайлгах; H2 дээр V6 migration; RBAC (VIEWER health-check дуудаж чадахгүй) тест; poller-ийг тестэд унтраах (`app.health.poll-interval=0` эсвэл profile).

Дэлгэрэнгүй task жагсаалт: `docs/development-checklist.md` §4.13.
