-- Concert Management System (Group 8)
-- SQL for Concert/Event Record Management + Venue Utilization Report
--
-- HOW TO USE THIS FILE
--   * In MySQL Workbench you can run the whole file. It works inside a transaction that is
--     rolled back at the very end, so your data stays exactly as it was.
--   * Every "@something" is an input. In the Java app, replace each @something with a ?
--     and fill it in with a PreparedStatement (the ? are filled in the order they appear).
--   * ADD and DELETE each need several statements. In Java, run them as ONE transaction:
--     setAutoCommit(false) ... commit(), and rollback() if anything fails.
--   * Run this AFTER the schema and sample data scripts.

USE concert_management_system;
START TRANSACTION;


-- =====================================================================
-- DROP-DOWN HELPERS (fill the UI's lists)
-- =====================================================================
SELECT artist_id, artist_name   FROM artist   ORDER BY artist_name;
SELECT venue_id,  venue_name    FROM venue    ORDER BY venue_name;
SELECT supplier_id, supplier_name FROM supplier ORDER BY supplier_name;
SELECT DISTINCT YEAR(concert_date) AS concert_year FROM concert ORDER BY concert_year;


-- =====================================================================
-- 1. ADD A CONCERT
-- =====================================================================
SET @concert_name    = 'Test Show';
SET @artist_id       = 3;
SET @venue_id        = 3;
SET @concert_date    = '2026-12-01';
SET @start_time      = '18:00:00';
SET @end_time        = '21:00:00';
SET @ticket_capacity = 2000;

-- Check first (Java does this before inserting): ticket capacity must not exceed the venue's capacity.
SELECT venue_capacity FROM venue WHERE venue_id = @venue_id;

INSERT INTO concert (concert_name, artist_id, venue_id, concert_date, start_time, end_time, ticket_capacity)
VALUES (@concert_name, @artist_id, @venue_id, @concert_date, @start_time, @end_time, @ticket_capacity);

-- In Java, get this from getGeneratedKeys(). In SQL it is LAST_INSERT_ID().
SET @new_concert_id = LAST_INSERT_ID();

-- Every concert needs at least one supplier (UML 1..*). Run once per chosen supplier.
INSERT INTO concert_supplier (concert_id, supplier_id) VALUES (@new_concert_id, 1);
INSERT INTO concert_supplier (concert_id, supplier_id) VALUES (@new_concert_id, 2);


-- =====================================================================
-- 2. VIEW ONE CONCERT
-- =====================================================================
SELECT c.concert_id, c.concert_name, a.artist_name, v.venue_name,
       c.concert_date, c.start_time, c.end_time, c.ticket_capacity
FROM concert c
JOIN artist a ON a.artist_id = c.artist_id
JOIN venue  v ON v.venue_id  = c.venue_id
WHERE c.concert_id = @new_concert_id;


-- =====================================================================
-- 3. LIST ALL CONCERTS
-- =====================================================================
SELECT c.concert_id, c.concert_name, a.artist_name, v.venue_name,
       c.concert_date, c.start_time, c.end_time, c.ticket_capacity
FROM concert c
JOIN artist a ON a.artist_id = c.artist_id
JOIN venue  v ON v.venue_id  = c.venue_id
ORDER BY c.concert_date, c.start_time;


-- =====================================================================
-- 4. LIST CONCERTS BY MONTH AND YEAR (the filtered view)
-- =====================================================================
SET @month = 12;
SET @year  = 2026;

SELECT c.concert_id, c.concert_name, a.artist_name, v.venue_name,
       c.concert_date, c.start_time, c.end_time, c.ticket_capacity
FROM concert c
JOIN artist a ON a.artist_id = c.artist_id
JOIN venue  v ON v.venue_id  = c.venue_id
WHERE YEAR(c.concert_date) = @year
  AND MONTH(c.concert_date) = @month
ORDER BY c.concert_date, c.start_time;


-- =====================================================================
-- 5. UPDATE A CONCERT
-- =====================================================================
SET @concert_name    = 'Test Show Updated';
SET @ticket_capacity = 2500;

UPDATE concert
SET concert_name    = @concert_name,
    artist_id       = @artist_id,
    venue_id        = @venue_id,
    concert_date    = @concert_date,
    start_time      = @start_time,
    end_time        = @end_time,
    ticket_capacity = @ticket_capacity
WHERE concert_id = @new_concert_id;

SELECT concert_id, concert_name, ticket_capacity FROM concert WHERE concert_id = @new_concert_id;


-- =====================================================================
-- 6. REPORT: VENUE UTILIZATION (per venue, for a month and year)
--    LEFT JOIN keeps venues that had no concerts, with 0.
-- =====================================================================
SELECT v.venue_name,
       v.city,
       COUNT(c.concert_id)                  AS concerts_held,
       COALESCE(SUM(c.ticket_capacity), 0)  AS total_ticket_capacity
FROM venue v
LEFT JOIN concert c ON c.venue_id = v.venue_id
                   AND YEAR(c.concert_date) = @year
                   AND MONTH(c.concert_date) = @month
GROUP BY v.venue_id, v.venue_name, v.city
ORDER BY v.venue_name;


-- =====================================================================
-- 7. DELETE A CONCERT (several statements, one transaction in Java)
-- =====================================================================
-- Check first: a concert with ticket purchases must NOT be deleted. Must be 0 to continue.
SELECT COUNT(*) AS ticket_purchases FROM ticket_purchase WHERE concert_id = @new_concert_id;

-- The supplier links belong to the concert, so remove them first, then the concert.
DELETE FROM concert_supplier WHERE concert_id = @new_concert_id;
DELETE FROM concert          WHERE concert_id = @new_concert_id;


-- Undo everything this test file did.
ROLLBACK;
