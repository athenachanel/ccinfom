SELECT * FROM venue;
START TRANSACTION;

SET @venue_id = (
    SELECT COALESCE(MAX(venue_id), 0) + 1
    FROM venue
);

INSERT INTO venue (venue_id, venue_name, street_address, city, venue_capacity)
VALUES (@venue_id,'Sample Concert Hall','123 Sample Street','Pasay',5000);

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
