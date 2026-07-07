-- Demo bank customers for the standalone banking app (password: demo123).
-- banking-transfer-service links them to seeded customers via customers.username.
insert into users (username, password_hash, display_name, role, enabled)
values
    ('batbold', '$2a$10$sUKuSsh939lY3GZFXN92JO2PgGyJMyIQ8/xFx6jErTmwO4JAlh46u', 'Bat Bold', 'VIEWER', true),
    ('sarnai', '$2a$10$FwbYwKyCK8akHap/U9oihOUaDtgVzcVPipsVu1J0LFNGP.qhQ49D6', 'Sarnai Erdene', 'VIEWER', true);
