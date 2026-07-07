insert into customers (customer_no, first_name, last_name, phone, email)
values
    ('CUST-0001', 'Bat', 'Bold', '99110001', 'bat.bold@example.com'),
    ('CUST-0002', 'Sarnai', 'Erdene', '99110002', 'sarnai.erdene@example.com');

insert into accounts (account_no, customer_id, currency, balance, status)
values
    ('100000001', (select id from customers where customer_no = 'CUST-0001'), 'MNT', 1000000.00, 'ACTIVE'),
    ('100000002', (select id from customers where customer_no = 'CUST-0002'), 'MNT', 500000.00, 'ACTIVE');
