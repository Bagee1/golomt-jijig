-- Deposit-service integration seed:
-- 1) svc-deposit — deposit-service-ийн service account (password: svc-deposit-demo-123).
--    Зориуд VIEWER: banking талд settlement дансны эзэн (customers.username='svc-deposit')
--    тул зөвхөн өөрийн данснаас эргэн төлөлт хийж чадна — least privilege.
insert into users (username, password_hash, display_name, role, enabled)
values ('svc-deposit', '$2a$10$qaWLHXS9cXEDg0tcIinb/.1XDvzTe.iopKdZfo/C07Zyg.sNo.Uw.', 'Deposit Service Account', 'VIEWER', true);

-- 2) deposit-service-ийг системийн каталогт бүртгэнэ (systems.type-д DEPOSIT байхгүй тул CORE).
insert into systems (system_key, name, type, valuation_mnt, description, developer_name,
    developer_team, start_date, end_date, in_use, environment, base_url, health_url, swagger_url, status, created_by)
values ('deposit-service', 'Deposit Service', 'CORE', 10000000,
    'Хугацаатай хадгаламжийн demo service — banking transfer API-г жинхэнэ HTTP-ээр дууддаг.',
    'Demo Developer', 'Core Banking Team', '2026-07-08', null, true, 'DEV',
    'http://localhost:8085', 'http://localhost:8085/actuator/health',
    'http://localhost:8085/swagger-ui/index.html', 'UNKNOWN', 1);

-- 3) Хамаарлууд: deposit→banking нь БОДИТ inter-service дуудлага, digital→deposit нь каталогийн жишээ.
insert into system_relations (source_system_id, target_system_id, relation_type, description)
values
    ((select id from systems where system_key = 'deposit-service'),
     (select id from systems where system_key = 'banking-transfer-service'),
     'CALLS', 'Хадгаламжийн санхүүжилт/эргэн төлөлт transfer API-аар (жинхэнэ inter-service дуудлага).'),
    ((select id from systems where system_key = 'digital-banking-service'),
     (select id from systems where system_key = 'deposit-service'),
     'CALLS', 'Дижитал банкны суваг хадгаламжийн API-г дуудна (каталогийн жишээ).');
