-- Term deposits. No local customers table: rows link to the platform user via
-- customer_username (JWT subject) and to the banking account via linked_account_no —
-- soft cross-service references, money itself lives in banking-transfer-service.
create table deposits (
    id bigserial primary key,
    deposit_no varchar(30) not null,
    client_request_key varchar(80),
    customer_username varchar(80) not null,
    linked_account_no varchar(30) not null,
    principal numeric(18, 2) not null,
    annual_rate numeric(5, 2) not null,
    term_months int not null,
    opened_at timestamp with time zone not null default now(),
    maturity_date date not null,
    status varchar(20) not null default 'FUNDING',
    close_type varchar(10),
    interest_amount numeric(18, 2),
    payout_amount numeric(18, 2),
    funding_transfer_ref varchar(40),
    payout_transfer_ref varchar(40),
    failure_reason varchar(40),
    closed_at timestamp with time zone,
    constraint ux_deposits_deposit_no unique (deposit_no),
    -- plain unique on a nullable column: multiple NULLs allowed on both Postgres and H2
    constraint ux_deposits_client_request_key unique (client_request_key),
    constraint deposits_status_check check (status in
        ('FUNDING', 'OPEN', 'PAYOUT_PENDING', 'CLOSED', 'CLOSED_EARLY', 'CANCELLED')),
    constraint deposits_close_type_check check (close_type in ('EARLY', 'MATURED')),
    constraint deposits_principal_check check (principal > 0),
    constraint deposits_term_check check (term_months > 0)
);

create index idx_deposits_username on deposits (customer_username);
create index idx_deposits_status on deposits (status);

create sequence deposit_no_seq start with 5001;

-- Actor fields are a JWT snapshot: users live in the platform DB, no FK possible.
create table deposit_audit_logs (
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

create index idx_deposit_audit_logs_created_at on deposit_audit_logs (created_at desc);
