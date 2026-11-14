USE concert_management;

INSERT INTO artist (artist_name, genre, country, debut_year, active_status)
VALUES ('Sample Artist', 'Pop', 'Philippines', 2021, 'Active');

SET @artist_id = LAST_INSERT_ID();

UPDATE artist
SET artist_name = 'Sample Artist',
    genre = 'Pop',
    country = 'Philippines',
    debut_year = 2021,
    active_status = 'Inactive'
WHERE artist_id = @artist_id;

SELECT artist_id, artist_name, genre, country, debut_year, active_status
FROM artist
ORDER BY artist_name, artist_id;

SET @artist_genre = 'Pop';

SELECT artist_id, artist_name, genre, country, debut_year, active_status
FROM artist
WHERE genre = @artist_genre
ORDER BY artist_name, artist_id;

DELETE FROM artist
WHERE artist_id = @artist_id
  AND NOT EXISTS (
      SELECT 1
      FROM concert
      WHERE concert.artist_id = artist.artist_id
  );
