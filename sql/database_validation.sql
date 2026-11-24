USE concert_management;

DROP TRIGGER IF EXISTS trg_concert_before_insert;
DROP TRIGGER IF EXISTS trg_concert_before_update;
DROP TRIGGER IF EXISTS trg_venue_before_update;
DROP TRIGGER IF EXISTS trg_ticket_before_insert;
DROP TRIGGER IF EXISTS trg_ticket_before_update;

DELIMITER $$

CREATE TRIGGER trg_concert_before_insert
BEFORE INSERT ON concert
FOR EACH ROW
BEGIN

    DECLARE max_capacity INT DEFAULT NULL;

    SELECT venue_capacity
    INTO max_capacity
    FROM venue
    WHERE venue_id = NEW.venue_id;

    IF CHAR_LENGTH(TRIM(NEW.concert_name)) = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Concert name cannot be empty';
    END IF;

    IF NEW.start_time < '00:00:00'
       OR NEW.end_time > '23:59:59'
       OR NEW.end_time <= NEW.start_time THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid concert start or end time';

    END IF;

    IF max_capacity IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Selected venue does not exist';
    END IF;

    IF NEW.ticket_capacity <= 0
       OR NEW.ticket_capacity > max_capacity THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid concert ticket capacity';

    END IF;

END$$

CREATE TRIGGER trg_concert_before_update
BEFORE UPDATE ON concert
FOR EACH ROW
BEGIN

    DECLARE max_capacity INT DEFAULT NULL;
    DECLARE tickets_sold BIGINT DEFAULT 0;

    SELECT venue_capacity
    INTO max_capacity
    FROM venue
    WHERE venue_id = NEW.venue_id;

    SELECT COALESCE(SUM(quantity), 0)
    INTO tickets_sold
    FROM ticket_purchase
    WHERE concert_id = OLD.concert_id;

    IF CHAR_LENGTH(TRIM(NEW.concert_name)) = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Concert name cannot be empty';
    END IF;

    IF NEW.start_time < '00:00:00'
       OR NEW.end_time > '23:59:59'
       OR NEW.end_time <= NEW.start_time THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid concert start or end time';

    END IF;

    IF max_capacity IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Selected venue does not exist';
    END IF;

    IF NEW.ticket_capacity <= 0
       OR NEW.ticket_capacity > max_capacity THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid concert ticket capacity';

    END IF;

    IF NEW.ticket_capacity < tickets_sold THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Ticket capacity cannot be below tickets already sold';

    END IF;

END$$

CREATE TRIGGER trg_venue_before_update
BEFORE UPDATE ON venue
FOR EACH ROW
BEGIN

    DECLARE required_capacity INT DEFAULT 0;

    SELECT COALESCE(MAX(ticket_capacity), 0)
    INTO required_capacity
    FROM concert
    WHERE venue_id = OLD.venue_id;

    IF NEW.venue_capacity <= 0 THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Venue capacity must be greater than zero';

    END IF;

    IF NEW.venue_capacity < required_capacity THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Venue capacity is below an existing concert capacity';

    END IF;

END$$

CREATE TRIGGER trg_ticket_before_insert
BEFORE INSERT ON ticket_purchase
FOR EACH ROW
BEGIN

    DECLARE max_capacity INT DEFAULT NULL;
    DECLARE tickets_sold BIGINT DEFAULT 0;

    SELECT ticket_capacity
    INTO max_capacity
    FROM concert
    WHERE concert_id = NEW.concert_id
    FOR UPDATE;

    SELECT COALESCE(SUM(quantity), 0)
    INTO tickets_sold
    FROM ticket_purchase
    WHERE concert_id = NEW.concert_id;

    IF max_capacity IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Selected concert does not exist';
    END IF;

    IF NEW.quantity <= 0 THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Ticket quantity must be greater than zero';

    END IF;

    IF NEW.unit_price < 0 THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ticket price cannot be negative';

    END IF;

    IF tickets_sold + NEW.quantity > max_capacity THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Not enough tickets available';

    END IF;

END$$

CREATE TRIGGER trg_ticket_before_update
BEFORE UPDATE ON ticket_purchase
FOR EACH ROW
BEGIN

    DECLARE max_capacity INT DEFAULT NULL;
    DECLARE tickets_sold BIGINT DEFAULT 0;

    SELECT ticket_capacity
    INTO max_capacity
    FROM concert
    WHERE concert_id = NEW.concert_id
    FOR UPDATE;

    SELECT COALESCE(SUM(quantity), 0)
    INTO tickets_sold
    FROM ticket_purchase
    WHERE concert_id = NEW.concert_id
      AND purchase_id <> OLD.purchase_id;

    IF max_capacity IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Selected concert does not exist';
    END IF;

    IF NEW.quantity <= 0 THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
            'Ticket quantity must be greater than zero';

    END IF;

    IF NEW.unit_price < 0 THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ticket price cannot be negative';

    END IF;

    IF tickets_sold + NEW.quantity > max_capacity THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Not enough tickets available';

    END IF;

END$$

DELIMITER ;

SHOW TRIGGERS;
