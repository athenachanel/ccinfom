CREATE DATABASE IF NOT EXISTS concert_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE concert_management;

CREATE TABLE IF NOT EXISTS artist (
    artist_id INT AUTO_INCREMENT PRIMARY KEY,
    artist_name VARCHAR(100) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    country VARCHAR(60) NOT NULL,
    debut_year YEAR,
    active_status ENUM('Active', 'Inactive')
        NOT NULL DEFAULT 'Active'
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS venue (
    venue_id INT AUTO_INCREMENT PRIMARY KEY,
    venue_name VARCHAR(100) NOT NULL,
    street_address VARCHAR(150) NOT NULL,
    city VARCHAR(60) NOT NULL,
    venue_capacity INT NOT NULL,

    CONSTRAINT chk_venue_capacity
        CHECK (venue_capacity > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS supplier (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(100) NOT NULL,
    supplier_type ENUM(
        'Sound System',
        'Lighting',
        'Stage Equipment',
        'Transportation/Logistics',
        'Catering'
    ) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    email_address VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    business_address VARCHAR(200) NOT NULL,
    service_fee DECIMAL(10,2) NOT NULL,
    supplier_status ENUM('Active', 'Inactive')
        NOT NULL DEFAULT 'Active',

    CONSTRAINT chk_supplier_service_fee
        CHECK (service_fee >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attendee (
    attendee_id INT AUTO_INCREMENT PRIMARY KEY,
    attendee_name VARCHAR(100) NOT NULL,
    email_address VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20) NOT NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS concert (
    concert_id INT AUTO_INCREMENT PRIMARY KEY,
    concert_name VARCHAR(150) NOT NULL,
    artist_id INT NOT NULL,
    venue_id INT NOT NULL,
    concert_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    ticket_capacity INT NOT NULL,

    INDEX idx_concert_date (concert_date),

    INDEX idx_concert_artist_date
        (artist_id, concert_date),

    INDEX idx_concert_venue_date
        (venue_id, concert_date),

    CONSTRAINT fk_concert_artist
        FOREIGN KEY (artist_id)
        REFERENCES artist(artist_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_concert_venue
        FOREIGN KEY (venue_id)
        REFERENCES venue(venue_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_concert_ticket_capacity
        CHECK (ticket_capacity > 0),

    CONSTRAINT chk_concert_time
        CHECK (
            start_time >= '00:00:00'
            AND end_time <= '23:59:59'
            AND end_time > start_time
        )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS concert_supplier (
    concert_id INT NOT NULL,
    supplier_id INT NOT NULL,

    PRIMARY KEY (concert_id, supplier_id),

    CONSTRAINT fk_concert_supplier_concert
        FOREIGN KEY (concert_id)
        REFERENCES concert(concert_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_concert_supplier_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES supplier(supplier_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ticket_purchase (
    purchase_id INT AUTO_INCREMENT PRIMARY KEY,
    concert_id INT NOT NULL,
    attendee_id INT NOT NULL,
    purchase_date DATE NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,

    CONSTRAINT fk_ticket_purchase_concert
        FOREIGN KEY (concert_id)
        REFERENCES concert(concert_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ticket_purchase_attendee
        FOREIGN KEY (attendee_id)
        REFERENCES attendee(attendee_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_ticket_purchase_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_ticket_purchase_unit_price
        CHECK (unit_price >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

SHOW TABLES;
