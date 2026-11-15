CREATE TABLE supplier (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(120) NOT NULL,
    supplier_type ENUM(
        'Sound System',
        'Lighting',
        'Stage Equipment',
        'Transportation/Logistics',
        'Catering'
    ) NOT NULL,
    contact_person VARCHAR(120) NOT NULL,
    email_address VARCHAR(254) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    business_address VARCHAR(255) NOT NULL,
    service_fee DECIMAL(12,2) NOT NULL CHECK (service_fee >= 0),
    supplier_status ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active'
);

INSERT INTO supplier (
    supplier_name, supplier_type, contact_person, email_address,
    contact_number, business_address, service_fee, supplier_status
) VALUES (
    'Sample Sound Supplier', 'Sound System', 'Sample Contact',
    'supplier@example.com', '09000000000',
    'Sample Business Address', 15000.00, 'Active'
);

SET @supplier_id = LAST_INSERT_ID();

UPDATE supplier
SET service_fee = 16000.00,
    supplier_status = 'Inactive'
WHERE supplier_id = @supplier_id;

SELECT *
FROM supplier
ORDER BY supplier_name, supplier_id;

SET @supplier_type = 'Sound System';

SELECT *
FROM supplier
WHERE supplier_type = @supplier_type
ORDER BY supplier_name, supplier_id;
