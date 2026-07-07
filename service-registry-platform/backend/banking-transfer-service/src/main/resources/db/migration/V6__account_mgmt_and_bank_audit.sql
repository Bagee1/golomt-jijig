-- Actor fields are a snapshot from the JWT: users live in the platform-api database,
-- so no foreign key is possible here.
create table bank_audit_logs (
    id bigserial primary key,
    actor_username varchar(80),
    actor_display_name varchar(160),
    actor_role varchar(20),
    action varchar(80) not null,
    target_type varchar(80) not null,
    target_id bigint,
    message text not null,
    metadata_json text,
    created_at timestamp with time zone not null default now()
);

create index idx_bank_audit_logs_created_at on bank_audit_logs(created_at desc);

-- Number generators for admin-created customers/accounts; start values clear of the
-- seeded demo rows (CUST-0001/0002, 100000001/100000002).
create sequence customer_no_seq start with 1001;
create sequence account_no_seq start with 100000101;
