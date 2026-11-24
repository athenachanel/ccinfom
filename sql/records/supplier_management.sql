USE concert_management;

START TRANSACTION;

INSERT INTO supplier (
    supplier_name, supplier_type, contact_person, email_address,
    contact_number, business_address, service_fee, supplier_status
) VALUES (
    'Sample Sound Supplier', 'Sound System', 'Sample Contact',
    'supplier@example.com', '09000000000',
    'Sample Business Address', 15000.00, 'Active'
);

SET @supplier_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);

UPDATE supplier
SET supplier_name = 'Updated Sound Supplier',
    supplier_type = 'Sound System',
    contact_person = 'Updated Contact',
    email_address = 'supplier@example.com',
    contact_number = '09000000000',
    business_address = 'Sample Business Address',
    service_fee = 16000.00,
    supplier_status = 'Inactive'
WHERE supplier_id = @supplier_id;

SELECT *
FROM supplier
WHERE supplier_id = @supplier_id;

SELECT *
FROM supplier
ORDER BY supplier_name, supplier_id;

SET @supplier_type = 'Sound System';

SELECT *
FROM supplier
WHERE supplier_type = @supplier_type
ORDER BY supplier_name, supplier_id;

DELETE FROM supplier
WHERE supplier_id = @supplier_id
  AND NOT EXISTS (
      SELECT 1 FROM concert_supplier AS cs
      WHERE cs.supplier_id = supplier.supplier_id
  );

ROLLBACK;
