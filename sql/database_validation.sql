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
    IF NEW.ticket_capacity > (
        SELECT venue_capacity FROM venue
        WHERE venue_id = NEW.venue_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ticket capacity exceeds venue capacity';
    END IF;
END$$

CREATE TRIGGER trg_concert_before_update
BEFORE UPDATE ON concert
FOR EACH ROW
BEGIN
    IF NEW.ticket_capacity > (
        SELECT venue_capacity FROM venue
        WHERE venue_id = NEW.venue_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ticket capacity exceeds venue capacity';
    END IF;

    IF NEW.ticket_capacity < (
        SELECT COALESCE(SUM(quantity), 0)
        FROM ticket_purchase
        WHERE concert_id = OLD.concert_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Capacity cannot be below tickets sold';
    END IF;
END$$

CREATE TRIGGER trg_venue_before_update
BEFORE UPDATE ON venue
FOR EACH ROW
BEGIN
    IF NEW.venue_capacity < (
        SELECT COALESCE(MAX(ticket_capacity), 0)
        FROM concert
        WHERE venue_id = OLD.venue_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Capacity is below existing concert capacity';
    END IF;
END$$

CREATE TRIGGER trg_ticket_before_insert
BEFORE INSERT ON ticket_purchase
FOR EACH ROW
BEGIN
    DECLARE max_capacity INT;

    SELECT ticket_capacity INTO max_capacity
    FROM concert
    WHERE concert_id = NEW.concert_id
    FOR UPDATE;

    IF NEW.quantity + (
        SELECT COALESCE(SUM(quantity), 0)
        FROM ticket_purchase
        WHERE concert_id = NEW.concert_id
    ) > max_capacity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Not enough tickets available';
    END IF;
END$$

CREATE TRIGGER trg_ticket_before_update
BEFORE UPDATE ON ticket_purchase
FOR EACH ROW
BEGIN
    DECLARE max_capacity INT;

    SELECT ticket_capacity INTO max_capacity
    FROM concert
    WHERE concert_id = NEW.concert_id
    FOR UPDATE;

    IF NEW.quantity + (
        SELECT COALESCE(SUM(quantity), 0)
        FROM ticket_purchase
        WHERE concert_id = NEW.concert_id
          AND purchase_id <> OLD.purchase_id
    ) > max_capacity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Not enough tickets available';
    END IF;
END$$

DELIMITER ;
