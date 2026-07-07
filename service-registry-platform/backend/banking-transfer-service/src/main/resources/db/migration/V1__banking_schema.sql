create table customers (
    id bigserial primary key,
    customer_no varchar(30) not null unique,
    first_name varchar(80) not null,
    last_name varchar(80) not null,
    phone varchar(30),
    email varchar(160),
    created_at timestamp with time zone not null default now()
);

create table accounts (
    id bigserial primary key,
    account_no varchar(30) not null unique,
    customer_id bigint not null references customers(id),
    currency varchar(3) not null default 'MNT',
    balance numeric(18, 2) not null default 0,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    constraint accounts_status_check check (status in ('ACTIVE', 'BLOCKED', 'CLOSED')),
    constraint accounts_balance_check check (balance >= 0)
);

create table transfers (
    id bigserial primary key,
    transfer_ref varchar(40) not null unique,
    from_account_id bigint not null references accounts(id),
    to_account_id bigint not null references accounts(id),
    amount numeric(18, 2) not null,
    currency varchar(3) not null default 'MNT',
    description varchar(500),
    status varchar(20) not null default 'SUCCESS',
    created_at timestamp with time zone not null default now(),
    constraint transfers_amount_check check (amount > 0),
    constraint transfers_status_check check (status in ('SUCCESS'))
);

create table ledger_entries (
    id bigserial primary key,
    transfer_id bigint not null references transfers(id) on delete cascade,
    account_id bigint not null references accounts(id),
    entry_type varchar(20) not null,
    amount numeric(18, 2) not null,
    balance_after numeric(18, 2) not null,
    created_at timestamp with time zone not null default now(),
    constraint ledger_entries_type_check check (entry_type in ('DEBIT', 'CREDIT')),
    constraint ledger_entries_amount_check check (amount > 0)
);

create index idx_accounts_customer_id on accounts(customer_id);
create index idx_transfers_created_at on transfers(created_at desc);
create index idx_transfers_from_account_id on transfers(from_account_id);
create index idx_transfers_to_account_id on transfers(to_account_id);
create index idx_ledger_entries_transfer_id on ledger_entries(transfer_id);
