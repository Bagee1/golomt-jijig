-- Idempotency key for POST /api/transfers: replaying the same key must not re-execute the transfer.
-- Plain unique constraint on a nullable column: both Postgres and H2 allow multiple NULLs,
-- so transfers created without a key stay unaffected. (No partial index — H2 does not support it.)
alter table transfers add column idempotency_key varchar(80);
alter table transfers add constraint ux_transfers_idempotency_key unique (idempotency_key);
