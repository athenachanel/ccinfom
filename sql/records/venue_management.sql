USE concert_management;

SELECT * FROM venue;
START TRANSACTION;

INSERT INTO venue (venue_name, street_address, city, venue_capacity)
VALUES ('Sample Concert Hall', '123 Sample Street', 'Pasay', 5000);

SET @venue_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);

SELECT * FROM venue
WHERE venue_id = @venue_id;

UPDATE venue
SET
    venue_name = 'Sample Music Arena',
    street_address = '456 Sample Avenue',
    city = 'Pasay',
    venue_capacity = 7000
WHERE venue_id = @venue_id;

SELECT * FROM venue
WHERE venue_id = @venue_id;

SELECT * FROM venue
ORDER BY venue_id;

SET @selected_city = 'Pasay';

SELECT *
FROM venue
WHERE city = @selected_city
ORDER BY venue_name;

DELETE FROM venue
WHERE venue_id = @venue_id;

SELECT * FROM venue
WHERE venue_id = @venue_id;

ROLLBACK;
