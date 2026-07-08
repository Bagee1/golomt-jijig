-- Demo security-checklist results so the dashboard shows real weighted scores
-- instead of everything at 0/NOT_CHECKED. Weights total 100, so score == earned
-- weight (PASS = full, WARNING = half, FAIL/NOT_CHECKED = 0).
-- Target scores: deposit-service 88, banking-transfer-service 83 (well-built, green),
-- card-service 40, digital-banking-service 28 (catalog-only, "needs attention").
-- checked_by = admin.

-- deposit-service -> 88
insert into security_check_results (system_id, control_id, result, evidence, checked_by)
select s.id, c.id, v.result, v.evidence, (select id from users where username = 'admin')
from (values
    ('HTTPS_ENABLED', 'WARNING', 'Dev орчинд HTTP; prod-д TLS termination шаардлагатай.'),
    ('AUTHENTICATION_ENABLED', 'PASS', 'Platform JWT (HS256) resource server бүх endpoint дээр.'),
    ('ROLE_BASED_ACCESS', 'PASS', '@PreAuthorize + эзэмшлийн шалгалт (customer_username).'),
    ('AUDIT_LOG_ENABLED', 'PASS', 'deposit_audit_logs — нээх/хаах/алдаа бүрд actor snapshot.'),
    ('SECRETS_NOT_IN_CODE', 'PASS', 'JWT_SECRET/DB нууц үг env-ээс, fallback-гүй.'),
    ('SWAGGER_PROTECTED', 'WARNING', 'Swagger dev-д нээлттэй; prod-д хаах ёстой.'),
    ('CORS_RESTRICTED', 'PASS', 'Allowlist: зөвхөн 5173/5174 origin.'),
    ('INPUT_VALIDATION', 'PASS', 'Bean Validation (@Valid) + бизнес дүрмийн шалгалт.')
) as v (control_key, result, evidence)
join security_controls c on c.control_key = v.control_key
cross join systems s
where s.system_key = 'deposit-service';

-- banking-transfer-service -> 83
insert into security_check_results (system_id, control_id, result, evidence, checked_by)
select s.id, c.id, v.result, v.evidence, (select id from users where username = 'admin')
from (values
    ('HTTPS_ENABLED', 'WARNING', 'Dev орчинд HTTP; prod-д TLS шаардлагатай.'),
    ('AUTHENTICATION_ENABLED', 'PASS', 'Platform JWT resource server.'),
    ('ROLE_BASED_ACCESS', 'PASS', '@PreAuthorize + дансны эзэмшлийн шалгалт.'),
    ('AUDIT_LOG_ENABLED', 'PASS', 'bank_audit_logs — гүйлгээ/данс/харилцагчийн үйлдэл.'),
    ('SECRETS_NOT_IN_CODE', 'PASS', 'Fail-fast: нууц утга env-ээс.'),
    ('SWAGGER_PROTECTED', 'FAIL', 'Swagger public хэвээр — хаагаагүй.'),
    ('CORS_RESTRICTED', 'PASS', 'Allowlist origin.'),
    ('INPUT_VALIDATION', 'PASS', 'Bean Validation + идемпотентийн шалгалт.')
) as v (control_key, result, evidence)
join security_controls c on c.control_key = v.control_key
cross join systems s
where s.system_key = 'banking-transfer-service';

-- card-service -> 40 (catalog-only, backend байхгүй)
insert into security_check_results (system_id, control_id, result, evidence, checked_by)
select s.id, c.id, v.result, v.evidence, (select id from users where username = 'admin')
from (values
    ('HTTPS_ENABLED', 'FAIL', 'Endpoint HTTPS-гүй.'),
    ('AUTHENTICATION_ENABLED', 'WARNING', 'Хэсэгчилсэн нэвтрэлт, бүрэн биш.'),
    ('ROLE_BASED_ACCESS', 'WARNING', 'Role загвар тодорхойгүй.'),
    ('AUDIT_LOG_ENABLED', 'FAIL', 'Audit log байхгүй.'),
    ('SECRETS_NOT_IN_CODE', 'PASS', 'Нууц утга config-д ил биш.'),
    ('SWAGGER_PROTECTED', 'FAIL', 'Swagger нээлттэй.'),
    ('CORS_RESTRICTED', 'WARNING', 'CORS сул тохируулгатай.'),
    ('INPUT_VALIDATION', 'WARNING', 'Validation хэсэгчилсэн.')
) as v (control_key, result, evidence)
join security_controls c on c.control_key = v.control_key
cross join systems s
where s.system_key = 'card-service';

-- digital-banking-service -> 28 (catalog-only, хамгийн сул)
insert into security_check_results (system_id, control_id, result, evidence, checked_by)
select s.id, c.id, v.result, v.evidence, (select id from users where username = 'admin')
from (values
    ('HTTPS_ENABLED', 'FAIL', 'HTTPS-гүй.'),
    ('AUTHENTICATION_ENABLED', 'WARNING', 'Нэвтрэлт хэсэгчилсэн.'),
    ('ROLE_BASED_ACCESS', 'FAIL', 'Эрхийн түвшин байхгүй.'),
    ('AUDIT_LOG_ENABLED', 'FAIL', 'Audit log байхгүй.'),
    ('SECRETS_NOT_IN_CODE', 'PASS', 'Нууц утга ил биш.'),
    ('SWAGGER_PROTECTED', 'FAIL', 'Swagger нээлттэй.'),
    ('CORS_RESTRICTED', 'WARNING', 'CORS сул.'),
    ('INPUT_VALIDATION', 'FAIL', 'Validation байхгүй.')
) as v (control_key, result, evidence)
join security_controls c on c.control_key = v.control_key
cross join systems s
where s.system_key = 'digital-banking-service';
