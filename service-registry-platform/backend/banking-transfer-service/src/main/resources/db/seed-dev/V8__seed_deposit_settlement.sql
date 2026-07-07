-- Deposit settlement account: term-deposit principals are parked here and paid back
-- (with interest) from here. Owned by the deposit-service's service user via
-- customers.username = 'svc-deposit' — banking's own-account transfer rule then
-- authorizes settlement payouts without granting the service account ADMIN rights.
-- The large balance is the demo interest pool (documented simplification).
-- Numbers are outside the sequences (customer_no_seq starts 1001, account_no_seq 100000101).
insert into customers (customer_no, first_name, last_name, phone, email, username)
values ('CUST-9000', 'Deposit', 'Settlement', null, null, 'svc-deposit');

insert into accounts (account_no, customer_id, currency, balance, status)
values ('900000001', (select id from customers where customer_no = 'CUST-9000'), 'MNT', 100000000.00, 'ACTIVE');
