CREATE DATABASE IF NOT EXISTS concert_management;
USE concert_management;

-- 1. ARTIST
CREATE TABLE IF NOT EXISTS artist (
    artist_id INT AUTO_INCREMENT PRIMARY KEY,
    artist_name VARCHAR(100) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    country VARCHAR(60) NOT NULL,
    debut_year YEAR,
    active_status ENUM('Active', 'Inactive')
        NOT NULL DEFAULT 'Active'
);

-- 2. VENUE
CREATE TABLE IF NOT EXISTS venue (
    venue_id INT AUTO_INCREMENT PRIMARY KEY,
    venue_name VARCHAR(100) NOT NULL,
    street_address VARCHAR(150) NOT NULL,
    city VARCHAR(60) NOT NULL,
    venue_capacity INT NOT NULL,
    CHECK (venue_capacity > 0)
);

-- 3. SUPPLIER
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
    CHECK (service_fee >= 0)
);

-- 4. ATTENDEE
CREATE TABLE IF NOT EXISTS attendee (
    attendee_id INT AUTO_INCREMENT PRIMARY KEY,
    attendee_name VARCHAR(100) NOT NULL,
    email_address VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20) NOT NULL
);

-- 5. CONCERT
CREATE TABLE IF NOT EXISTS concert (
    concert_id INT AUTO_INCREMENT PRIMARY KEY,
    concert_name VARCHAR(150) NOT NULL,
    artist_id INT NOT NULL,
    venue_id INT NOT NULL,
    concert_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    ticket_capacity INT NOT NULL,

    FOREIGN KEY (artist_id)
        REFERENCES artist(artist_id),

    FOREIGN KEY (venue_id)
        REFERENCES venue(venue_id),

    CHECK (ticket_capacity > 0)
);

-- 6. CONCERT SUPPLIER
CREATE TABLE IF NOT EXISTS concert_supplier (
    concert_id INT NOT NULL,
    supplier_id INT NOT NULL,

    PRIMARY KEY (concert_id, supplier_id),

    FOREIGN KEY (concert_id)
        REFERENCES concert(concert_id),

    FOREIGN KEY (supplier_id)
        REFERENCES supplier(supplier_id)
);

-- 7. TICKET PURCHASE
CREATE TABLE IF NOT EXISTS ticket_purchase (
    purchase_id INT AUTO_INCREMENT PRIMARY KEY,
    concert_id INT NOT NULL,
    attendee_id INT NOT NULL,
    purchase_date DATE NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,

    FOREIGN KEY (concert_id)
        REFERENCES concert(concert_id),

    FOREIGN KEY (attendee_id)
        REFERENCES attendee(attendee_id),

    CHECK (quantity > 0),
    CHECK (unit_price >= 0)
);
