USE concert_management;

SELECT artist_id, artist_name FROM artist ORDER BY artist_name;
SELECT venue_id, venue_name FROM venue ORDER BY venue_name;
SELECT supplier_id, supplier_name FROM supplier ORDER BY supplier_name;
SELECT DISTINCT YEAR(concert_date) AS concert_year
FROM concert ORDER BY concert_year;

START TRANSACTION;

SET @concert_name = 'Sample Concert';
SET @artist_id = 1;
SET @venue_id = 1;
SET @concert_date = '2026-11-25';
SET @start_time = '18:00:00';
SET @end_time = '21:00:00';
SET @ticket_capacity = 1000;
SET @supplier_ids = JSON_ARRAY(1, 2);

INSERT INTO concert (
    concert_name, artist_id, venue_id, concert_date,
    start_time, end_time, ticket_capacity
)
SELECT TRIM(@concert_name), a.artist_id, v.venue_id, @concert_date,
       @start_time, @end_time, @ticket_capacity
FROM artist AS a
CROSS JOIN venue AS v
WHERE a.artist_id = @artist_id
  AND v.venue_id = @venue_id
  AND CHAR_LENGTH(TRIM(@concert_name)) BETWEEN 1 AND 150
  AND CAST(@start_time AS TIME) >= '00:00:00'
  AND CAST(@end_time AS TIME) <= '23:59:59'
  AND CAST(@start_time AS TIME) < CAST(@end_time AS TIME)
  AND @ticket_capacity BETWEEN 1 AND v.venue_capacity
  AND JSON_LENGTH(@supplier_ids) BETWEEN 1 AND 10
  AND JSON_LENGTH(@supplier_ids) = (
      SELECT COUNT(DISTINCT s.supplier_id)
      FROM JSON_TABLE(@supplier_ids, '$[*]'
          COLUMNS (supplier_id INT PATH '$')) AS requested
      JOIN supplier AS s ON s.supplier_id = requested.supplier_id
  );

SET @demo_concert_id = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);

INSERT INTO concert_supplier (concert_id, supplier_id)
SELECT @demo_concert_id, requested.supplier_id
FROM JSON_TABLE(@supplier_ids, '$[*]'
    COLUMNS (supplier_id INT PATH '$')) AS requested
WHERE @demo_concert_id IS NOT NULL;

SELECT c.concert_id, c.concert_name, a.artist_name, v.venue_name,
       c.concert_date, c.start_time, c.end_time, c.ticket_capacity
FROM concert AS c
JOIN artist AS a ON a.artist_id = c.artist_id
JOIN venue AS v ON v.venue_id = c.venue_id
WHERE c.concert_id = @demo_concert_id;

SELECT s.supplier_id, s.supplier_name, s.supplier_type
FROM concert_supplier AS cs
JOIN supplier AS s ON s.supplier_id = cs.supplier_id
WHERE cs.concert_id = @demo_concert_id;

SELECT c.concert_id, c.concert_name, a.artist_name, v.venue_name,
       c.concert_date, c.start_time, c.end_time, c.ticket_capacity
FROM concert AS c
JOIN artist AS a ON a.artist_id = c.artist_id
JOIN venue AS v ON v.venue_id = c.venue_id
ORDER BY c.concert_date, c.start_time;

SET @month = 11;
SET @year = 2026;

SELECT c.concert_id, c.concert_name, a.artist_name, v.venue_name,
       c.concert_date, c.start_time, c.end_time, c.ticket_capacity
FROM concert AS c
JOIN artist AS a ON a.artist_id = c.artist_id
JOIN venue AS v ON v.venue_id = c.venue_id
WHERE MONTH(c.concert_date) = @month
  AND YEAR(c.concert_date) = @year
ORDER BY c.concert_date, c.start_time;

SET @concert_name = 'Sample Concert (Updated)';
SET @ticket_capacity = 1200;
SET @new_supplier_ids = JSON_ARRAY(2, 3);

SET @valid_update = (
    SELECT COUNT(*)
    FROM concert AS c
    JOIN venue AS v ON v.venue_id = @venue_id
    JOIN artist AS a ON a.artist_id = @artist_id
    WHERE c.concert_id = @demo_concert_id
      AND CHAR_LENGTH(TRIM(@concert_name)) BETWEEN 1 AND 150
      AND CAST(@start_time AS TIME) >= '00:00:00'
      AND CAST(@end_time AS TIME) <= '23:59:59'
      AND CAST(@start_time AS TIME) < CAST(@end_time AS TIME)
      AND @ticket_capacity BETWEEN 1 AND v.venue_capacity
      AND @ticket_capacity >= (
          SELECT COALESCE(SUM(p.quantity), 0)
          FROM ticket_purchase AS p
          WHERE p.concert_id = c.concert_id
      )
      AND JSON_LENGTH(@new_supplier_ids) BETWEEN 1 AND 10
      AND JSON_LENGTH(@new_supplier_ids) = (
          SELECT COUNT(DISTINCT s.supplier_id)
          FROM JSON_TABLE(@new_supplier_ids, '$[*]'
              COLUMNS (supplier_id INT PATH '$')) AS requested
          JOIN supplier AS s ON s.supplier_id = requested.supplier_id
      )
);

UPDATE concert
SET concert_name = TRIM(@concert_name),
    artist_id = @artist_id,
    venue_id = @venue_id,
    concert_date = @concert_date,
    start_time = @start_time,
    end_time = @end_time,
    ticket_capacity = @ticket_capacity
WHERE concert_id = @demo_concert_id
  AND @valid_update = 1;

DELETE FROM concert_supplier
WHERE concert_id = @demo_concert_id
  AND @valid_update = 1;

INSERT INTO concert_supplier (concert_id, supplier_id)
SELECT @demo_concert_id, requested.supplier_id
FROM JSON_TABLE(@new_supplier_ids, '$[*]'
    COLUMNS (supplier_id INT PATH '$')) AS requested
WHERE @valid_update = 1;

DELETE FROM concert_supplier
WHERE concert_id = @demo_concert_id
  AND NOT EXISTS (
      SELECT 1 FROM ticket_purchase
      WHERE concert_id = @demo_concert_id
  );

DELETE FROM concert
WHERE concert_id = @demo_concert_id
  AND NOT EXISTS (
      SELECT 1 FROM ticket_purchase
      WHERE concert_id = @demo_concert_id
  );

ROLLBACK;
