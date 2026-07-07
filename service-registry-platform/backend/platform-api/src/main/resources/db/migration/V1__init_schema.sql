create table users (
    id bigserial primary key,
    username varchar(80) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(160) not null,
    role varchar(30) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    constraint users_role_check check (role in ('ADMIN', 'SECURITY', 'VIEWER'))
);

create table systems (
    id bigserial primary key,
    system_key varchar(80) not null unique,
    name varchar(160) not null,
    type varchar(30) not null,
    valuation_mnt numeric(18, 2) not null default 0,
    description text,
    developer_name varchar(160),
    developer_team varchar(160),
    start_date date,
    end_date date,
    in_use boolean not null default true,
    environment varchar(30) not null default 'DEV',
    base_url varchar(500),
    health_url varchar(500),
    swagger_url varchar(500),
    repo_url varchar(500),
    status varchar(30) not null default 'UNKNOWN',
    created_by bigint references users(id),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    constraint systems_type_check check (type in ('CARD', 'CORE', 'INTERNAL', 'DIGITAL')),
    constraint systems_environment_check check (environment in ('DEV', 'TEST', 'UAT', 'PROD')),
    constraint systems_status_check check (status in ('ACTIVE', 'INACTIVE', 'UNKNOWN', 'DOWN')),
    constraint systems_valuation_check check (valuation_mnt >= 0),
    constraint systems_date_check check (end_date is null or start_date is null or start_date <= end_date)
);

create table system_relations (
    id bigserial primary key,
    source_system_id bigint not null references systems(id) on delete cascade,
    target_system_id bigint not null references systems(id) on delete cascade,
    relation_type varchar(30) not null,
    description text,
    created_at timestamp with time zone not null default now(),
    constraint system_relations_type_check check (relation_type in ('DEPENDS_ON', 'CALLS', 'INTEGRATES_WITH')),
    constraint system_relations_not_self_check check (source_system_id <> target_system_id),
    constraint system_relations_unique unique (source_system_id, target_system_id, relation_type)
);

create table security_controls (
    id bigserial primary key,
    control_key varchar(80) not null unique,
    title varchar(180) not null,
    description text,
    weight integer not null default 10,
    required boolean not null default true,
    automated boolean not null default false,
    standard_ref varchar(160),
    created_at timestamp with time zone not null default now(),
    constraint security_controls_weight_check check (weight > 0)
);

create table security_check_results (
    id bigserial primary key,
    system_id bigint not null references systems(id) on delete cascade,
    control_id bigint not null references security_controls(id) on delete cascade,
    result varchar(30) not null default 'NOT_CHECKED',
    evidence text,
    checked_by bigint references users(id),
    checked_at timestamp with time zone not null default now(),
    constraint security_check_results_result_check check (result in ('PASS', 'FAIL', 'WARNING', 'NOT_CHECKED')),
    constraint security_check_results_unique unique (system_id, control_id)
);

create table audit_logs (
    id bigserial primary key,
    actor_user_id bigint references users(id),
    action varchar(80) not null,
    target_type varchar(80) not null,
    target_id bigint,
    message text not null,
    metadata_json text,
    created_at timestamp with time zone not null default now()
);

create index idx_systems_name on systems(name);
create index idx_systems_type on systems(type);
create index idx_systems_in_use on systems(in_use);
create index idx_systems_status on systems(status);
create index idx_audit_logs_created_at on audit_logs(created_at desc);

