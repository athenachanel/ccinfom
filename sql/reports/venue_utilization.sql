USE concert_management;

SET @month_start = '2026-11-01';

SELECT v.venue_name,
       v.city,
       COUNT(c.concert_id) AS concerts_held,
       COALESCE(SUM(c.ticket_capacity), 0) AS total_ticket_capacity
FROM venue AS v
LEFT JOIN concert AS c
    ON c.venue_id = v.venue_id
    AND c.concert_date >= @month_start
    AND c.concert_date < DATE_ADD(@month_start, INTERVAL 1 MONTH)
GROUP BY v.venue_id, v.venue_name, v.city
ORDER BY v.venue_name;
