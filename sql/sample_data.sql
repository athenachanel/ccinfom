USE concert_management;

START TRANSACTION;

INSERT INTO artist (
    artist_id, artist_name, genre,
    country, debut_year, active_status
)
VALUES
(1, 'Neon Horizon', 'Pop', 'Philippines', 2021, 'Active'),
(2, 'Midnight Avenue', 'Rock', 'Philippines', 2018, 'Active'),
(3, 'Lunar Echo', 'Indie', 'Philippines', 2020, 'Active'),
(4, 'Velvet Sky', 'R&B', 'Philippines', 2019, 'Active'),
(5, 'Golden Rhythm', 'Pop', 'South Korea', 2022, 'Active'),
(6, 'Pacific Lights', 'Rock', 'Japan', 2017, 'Active'),
(7, 'Silver Notes', 'Jazz', 'United States', 2016, 'Active'),
(8, 'Urban Pulse', 'Hip-Hop', 'Philippines', 2020, 'Active'),
(9, 'Ocean Waves', 'Indie', 'Australia', 2015, 'Inactive'),
(10, 'Crimson Melody', 'Pop', 'Philippines', 2019, 'Inactive');

INSERT INTO venue (
    venue_id, venue_name, street_address,
    city, venue_capacity
)
VALUES
(1, 'Grand Horizon Arena', '100 Sample Avenue', 'Quezon City', 15000),
(2, 'Metro Music Arena', '200 Sample Avenue', 'Pasay', 12000),
(3, 'South City Events Hall', '300 Sample Avenue', 'Taguig', 8000),
(4, 'Manila Music Theater', '400 Sample Avenue', 'Manila', 5000),
(5, 'Central Concert Grounds', '500 Sample Avenue', 'Makati', 10000),
(6, 'North Star Arena', '600 Sample Avenue', 'Quezon City', 18000),
(7, 'Bayview Events Center', '700 Sample Avenue', 'Pasay', 6000),
(8, 'Eastside Performance Hall', '800 Sample Avenue', 'Pasig', 4000),
(9, 'Skyline Concert Center', '900 Sample Avenue', 'Mandaluyong', 9000),
(10, 'Harbor Music Hall', '1000 Sample Avenue', 'Manila', 3000);

INSERT INTO supplier (
    supplier_id, supplier_name, supplier_type,
    contact_person, email_address, contact_number,
    business_address, service_fee, supplier_status
)
VALUES
(1, 'Prime Audio Services', 'Sound System',
 'Alex Reyes', 'audio@example.com', '09000000001',
 'Quezon City', 50000.00, 'Active'),

(2, 'Spectrum Lighting', 'Lighting',
 'Jamie Cruz', 'lighting@example.com', '09000000002',
 'Pasay', 45000.00, 'Active'),

(3, 'Elevate Stage Solutions', 'Stage Equipment',
 'Morgan Santos', 'stage@example.com', '09000000003',
 'Manila', 75000.00, 'Active'),

(4, 'Metro Event Logistics', 'Transportation/Logistics',
 'Casey Garcia', 'logistics@example.com', '09000000004',
 'Makati', 35000.00, 'Active'),

(5, 'Harmony Catering', 'Catering',
 'Taylor Ramos', 'catering@example.com', '09000000005',
 'Taguig', 25000.00, 'Active'),

(6, 'ClearSound Productions', 'Sound System',
 'Jordan Flores', 'clearsound@example.com', '09000000006',
 'Pasig', 55000.00, 'Active'),

(7, 'Luminous Events', 'Lighting',
 'Cameron Diaz', 'luminous@example.com', '09000000007',
 'Mandaluyong', 40000.00, 'Active'),

(8, 'NextStage Equipment', 'Stage Equipment',
 'Robin Mendoza', 'nextstage@example.com', '09000000008',
 'Quezon City', 65000.00, 'Active'),

(9, 'Swift Transport Services', 'Transportation/Logistics',
 'Sam Rivera', 'swift@example.com', '09000000009',
 'Pasay', 30000.00, 'Inactive'),

(10, 'Premier Event Catering', 'Catering',
 'Drew Lopez', 'premier@example.com', '09000000010',
 'Manila', 28000.00, 'Inactive');

INSERT INTO attendee (
    attendee_id, attendee_name,
    email_address, contact_number
)
VALUES
(1, 'Attendee One', 'attendee1@example.com', '09100000001'),
(2, 'Attendee Two', 'attendee2@example.com', '09100000002'),
(3, 'Attendee Three', 'attendee3@example.com', '09100000003'),
(4, 'Attendee Four', 'attendee4@example.com', '09100000004'),
(5, 'Attendee Five', 'attendee5@example.com', '09100000005'),
(6, 'Attendee Six', 'attendee6@example.com', '09100000006'),
(7, 'Attendee Seven', 'attendee7@example.com', '09100000007'),
(8, 'Attendee Eight', 'attendee8@example.com', '09100000008'),
(9, 'Attendee Nine', 'attendee9@example.com', '09100000009'),
(10, 'Attendee Ten', 'attendee10@example.com', '09100000010');

INSERT INTO concert (
    concert_id, concert_name, artist_id,
    venue_id, concert_date, start_time,
    end_time, ticket_capacity
)
VALUES
(1, 'Neon Nights Live', 1, 1,
 '2026-11-05', '18:00:00', '22:00:00', 12000),

(2, 'Midnight Rock Festival', 2, 2,
 '2026-11-08', '19:00:00', '22:00:00', 10000),

(3, 'Lunar Acoustic Sessions', 3, 4,
 '2026-11-12', '17:00:00', '20:00:00', 4000),

(4, 'Velvet Sky Experience', 4, 3,
 '2026-11-15', '18:00:00', '21:00:00', 7000),

(5, 'Golden Rhythm World Tour', 5, 1,
 '2026-11-18', '19:00:00', '22:00:00', 14000),

(6, 'Pacific Rock Live', 6, 6,
 '2026-11-20', '18:00:00', '22:00:00', 16000),

(7, 'Silver Jazz Evening', 7, 8,
 '2026-12-03', '18:00:00', '21:00:00', 3000),

(8, 'Urban Pulse Live', 8, 5,
 '2026-12-10', '19:00:00', '22:00:00', 9000),

(9, 'Ocean Waves Acoustic', 9, 7,
 '2025-11-15', '17:00:00', '20:00:00', 5000),

(10, 'Crimson Melody Live', 10, 10,
 '2025-11-22', '18:00:00', '21:00:00', 2500),

(11, 'Neon Horizon Encore', 1, 2,
 '2026-11-23', '18:00:00', '22:00:00', 11000),

(12, 'Lunar December Showcase', 3, 9,
 '2026-12-20', '18:00:00', '21:00:00', 8000);

INSERT INTO concert_supplier (
    concert_id, supplier_id
)
VALUES
(1, 1), (1, 2),
(2, 3), (2, 4),
(3, 1), (3, 5),
(4, 6), (4, 7),
(5, 2), (5, 8),
(6, 3), (6, 6),
(7, 1), (7, 7),
(8, 4), (8, 5),
(9, 6), (9, 9),
(10, 8), (10, 10),
(11, 1), (11, 2),
(12, 3), (12, 7);

INSERT INTO ticket_purchase (
    purchase_id, concert_id, attendee_id,
    purchase_date, quantity, unit_price
)
VALUES
(1, 1, 1, '2026-10-15', 2, 2500.00),
(2, 1, 2, '2026-10-20', 3, 2500.00),
(3, 2, 3, '2026-10-22', 2, 1800.00),
(4, 2, 4, '2026-11-01', 4, 1800.00),
(5, 3, 5, '2026-11-02', 2, 1200.00),
(6, 3, 6, '2026-11-04', 1, 1200.00),
(7, 4, 7, '2026-11-05', 3, 2000.00),
(8, 4, 8, '2026-11-07', 2, 2000.00),
(9, 5, 9, '2026-11-08', 4, 3500.00),
(10, 5, 10, '2026-11-10', 2, 3500.00),
(11, 6, 1, '2026-11-11', 3, 2800.00),
(12, 6, 2, '2026-11-12', 2, 2800.00),
(13, 7, 3, '2026-11-15', 2, 1500.00),
(14, 8, 4, '2026-11-16', 3, 2200.00),
(15, 9, 5, '2025-11-01', 2, 1300.00),
(16, 10, 6, '2025-11-05', 2, 1000.00),
(17, 11, 7, '2026-11-17', 4, 2500.00),
(18, 11, 8, '2026-11-18', 3, 2500.00),
(19, 1, 9, '2026-11-01', 1, 2500.00),
(20, 5, 10, '2026-11-12', 2, 3500.00);

COMMIT;

SELECT 'artist' AS table_name, COUNT(*) AS total_records
FROM artist
UNION ALL
SELECT 'venue', COUNT(*) FROM venue
UNION ALL
SELECT 'supplier', COUNT(*) FROM supplier
UNION ALL
SELECT 'attendee', COUNT(*) FROM attendee
UNION ALL
SELECT 'concert', COUNT(*) FROM concert
UNION ALL
SELECT 'concert_supplier', COUNT(*) FROM concert_supplier
UNION ALL
SELECT 'ticket_purchase', COUNT(*) FROM ticket_purchase;
