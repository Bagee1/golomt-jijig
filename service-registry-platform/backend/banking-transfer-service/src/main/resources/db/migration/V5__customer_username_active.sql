-- Links a banking customer to a platform-api user (JWT subject). Nullable: not every
-- customer has portal access, and not every platform user is a bank customer.
alter table customers add column username varchar(80);
alter table customers add constraint ux_customers_username unique (username);

alter table customers add column active boolean not null default true;
