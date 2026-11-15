SET @report_month = 11;
SET @report_year = 2026;

SET @period_start = STR_TO_DATE(
    CONCAT(@report_year, '-', LPAD(@report_month, 2, '0'), '-01'),
    '%Y-%m-%d'
);
SET @period_end = DATE_ADD(@period_start, INTERVAL 1 MONTH);

SELECT
    c.concert_id,
    c.concert_name,
    c.concert_date,
    COALESCE(SUM(p.quantity), 0) AS total_tickets_sold,
    COALESCE(SUM(p.quantity * p.unit_price), 0.00) AS total_ticket_revenue
FROM concert AS c
LEFT JOIN ticket_purchase AS p
    ON p.concert_id = c.concert_id
WHERE c.concert_date >= @period_start
  AND c.concert_date < @period_end
GROUP BY c.concert_id, c.concert_name, c.concert_date
ORDER BY c.concert_date, c.concert_name, c.concert_id;
