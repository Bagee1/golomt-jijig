alter table transfers drop constraint transfers_status_check;
alter table transfers add constraint transfers_status_check
    check (status in ('PENDING', 'SUCCESS', 'FAILED', 'REVERSED'));

alter table transfers add column failure_reason varchar(40);
alter table transfers add column reversal_of_transfer_id bigint references transfers(id);

-- Plain unique on a nullable column: allows many NULLs on both H2 and Postgres,
-- but at most one reversal per original transfer (race backstop for double reversal).
alter table transfers add constraint ux_transfers_reversal_of unique (reversal_of_transfer_id);
