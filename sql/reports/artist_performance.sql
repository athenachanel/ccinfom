USE concert_management;

SET @selected_month = 11;
SET @selected_year = 2026;

SELECT a.artist_id,
       a.artist_name,
       COUNT(c.concert_id) AS total_concerts
FROM artist AS a
LEFT JOIN concert AS c
    ON a.artist_id = c.artist_id
    AND MONTH(c.concert_date) = @selected_month
    AND YEAR(c.concert_date) = @selected_year
GROUP BY a.artist_id, a.artist_name
ORDER BY total_concerts DESC, a.artist_name ASC;
