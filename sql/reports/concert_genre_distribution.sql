USE concert_management;

SET @report_genre = 'Pop';
SET @report_month = 11;
SET @report_year = 2026;

SET @month_start = STR_TO_DATE(
    CONCAT(@report_year, '-', LPAD(@report_month, 2, '0'), '-01'),
    '%Y-%m-%d'
);

SET @next_month_start = DATE_ADD(@month_start, INTERVAL 1 MONTH);

SELECT a.genre,
       COUNT(c.concert_id) AS total_concerts,
       COUNT(DISTINCT c.artist_id) AS unique_performing_artists
FROM artist AS a
LEFT JOIN concert AS c
    ON c.artist_id = a.artist_id
    AND c.concert_date >= @month_start
    AND c.concert_date < @next_month_start
WHERE a.genre = @report_genre
GROUP BY a.genre;
