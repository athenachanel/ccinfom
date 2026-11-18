JOIN artist AS a ON a.artist_id = c.artist_id
JOIN venue AS v ON v.venue_id = c.venue_id
WHERE c.concert_id = @selected_concert_id;

-- The write examples use one demo concert and roll back at the end.
-- Run this section together. If execution stops early, run ROLLBACK.
START TRANSACTION;

-- 4. Add a concert and link its suppliers.
SET @concert_name = 'Sample Concert';
SET @artist_id = 1;
SET @venue_id = 1;
SET @concert_date = '2026-11-25';
SET @start_time = '18:00:00';
SET @end_time = '21:00:00';
SET @ticket_capacity = 1000;
SET @supplier_ids = JSON_ARRAY(1, 2);
SET @demo_concert_id = NULL;

-- The JOINs check that the artist and venue exist. The supplier count
-- check requires 1 to 10 existing supplier IDs without duplicates.
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
  AND CAST(@start_time AS TIME) >= TIME '00:00:00'
  AND CAST(@end_time AS TIME) <= TIME '23:59:59'
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

SET @suppliers_linked = ROW_COUNT();
SELECT @demo_concert_id AS new_concert_id, @suppliers_linked AS suppliers_linked;

-- 5. Edit the demo concert.
SET @concert_name = 'Sample Concert (Updated)';
SET @ticket_capacity = 1200;

UPDATE concert AS c
SET concert_name = TRIM(@concert_name),
    artist_id = @artist_id,
    venue_id = @venue_id,
    concert_date = @concert_date,
    start_time = @start_time,
    end_time = @end_time,
    ticket_capacity = @ticket_capacity
WHERE c.concert_id = @demo_concert_id
  AND @suppliers_linked = JSON_LENGTH(@supplier_ids)
  AND CHAR_LENGTH(TRIM(@concert_name)) BETWEEN 1 AND 150
  AND CAST(@start_time AS TIME) >= TIME '00:00:00'
  AND CAST(@end_time AS TIME) <= TIME '23:59:59'
  AND CAST(@start_time AS TIME) < CAST(@end_time AS TIME)
  AND EXISTS (
      SELECT 1
      FROM venue AS v
      WHERE v.venue_id = @venue_id
        AND @ticket_capacity BETWEEN 1 AND v.venue_capacity
  )
  AND @ticket_capacity >= (
      SELECT COALESCE(SUM(p.quantity), 0)
      FROM ticket_purchase AS p
      WHERE p.concert_id = c.concert_id
  );

SELECT ROW_COUNT() AS concerts_updated;

SELECT concert_id, concert_name, ticket_capacity
FROM concert
WHERE concert_id = @demo_concert_id;

-- 6. Delete the demo concert if it has no ticket purchases.
-- Remove supplier links first because they reference the concert.
DELETE FROM concert_supplier
WHERE concert_id = @demo_concert_id
  AND NOT EXISTS (
      SELECT 1
      FROM ticket_purchase AS p
      WHERE p.concert_id = @demo_concert_id
  );

DELETE FROM concert
WHERE concert_id = @demo_concert_id
  AND NOT EXISTS (
      SELECT 1
      FROM ticket_purchase AS p
      WHERE p.concert_id = @demo_concert_id
  );

SELECT ROW_COUNT() AS concerts_deleted;

ROLLBACK;
