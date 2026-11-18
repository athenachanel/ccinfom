USE concert_management;

SELECT
    c.concert_id,
    c.concert_name,
    c.artist_id,
    a.artist_name,
    c.venue_id,
    v.venue_name,
    c.concert_date,
    c.start_time,
    c.end_time,
    c.ticket_capacity
FROM concert AS c
JOIN artist AS a ON a.artist_id = c.artist_id
JOIN venue AS v ON v.venue_id = c.venue_id
ORDER BY c.concert_date, c.start_time, c.concert_id;
