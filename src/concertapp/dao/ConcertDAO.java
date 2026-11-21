package concertapp.dao;

import concertapp.db.DBConnection;
import concertapp.model.Concert;
import concertapp.model.LookupItem;
import concertapp.model.VenueUtilizationRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DATA ACCESS for the Concert/Event module.
 * All the SQL for concert records and the Venue Utilization Report lives here.
 * This class only talks to the database. Rules and messages live in ConcertService.
 */
public class ConcertDAO {

    private static final String SELECT_CONCERT =
            "SELECT c.concert_id, c.concert_name, c.artist_id, a.artist_name, "
          + "c.venue_id, v.venue_name, c.concert_date, c.start_time, c.end_time, c.ticket_capacity "
          + "FROM concert c "
          + "JOIN artist a ON a.artist_id = c.artist_id "
          + "JOIN venue v ON v.venue_id = c.venue_id ";

    private static final String ORDER_CONCERT = " ORDER BY c.concert_date, c.start_time, c.concert_id";

    // ------------------------------------------------------------------
    // Record management
    // ------------------------------------------------------------------

    /** Saves the concert and its supplier links together (one transaction). Returns the new id. */
    public int addConcert(Concert c) throws SQLException {
        String insertConcert =
                "INSERT INTO concert (concert_name, artist_id, venue_id, concert_date, "
              + "start_time, end_time, ticket_capacity) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertLink = "INSERT INTO concert_supplier (concert_id, supplier_id) VALUES (?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int newId;
                try (PreparedStatement ps = con.prepareStatement(insertConcert, Statement.RETURN_GENERATED_KEYS)) {
                    bindConcertFields(ps, c);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The database did not return the new concert id.");
                        }
                        newId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(insertLink)) {
                    for (int supplierId : c.getSupplierIds()) {
                        ps.setInt(1, newId);
                        ps.setInt(2, supplierId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return newId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void updateConcert(Concert c) throws SQLException {
        String sql = "UPDATE concert SET concert_name = ?, artist_id = ?, venue_id = ?, "
                   + "concert_date = ?, start_time = ?, end_time = ?, ticket_capacity = ? "
                   + "WHERE concert_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindConcertFields(ps, c);
            ps.setInt(8, c.getConcertId());
            ps.executeUpdate();
        }
    }

    /** Removes the supplier links, then the concert (one transaction). */
    public void deleteConcert(int concertId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM concert_supplier WHERE concert_id = ?")) {
                    ps.setInt(1, concertId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM concert WHERE concert_id = ?")) {
                    ps.setInt(1, concertId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Returns null when there is no concert with that id. */
    public Concert findById(int concertId) throws SQLException {
        String sql = SELECT_CONCERT + "WHERE c.concert_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, concertId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapConcert(rs) : null;
            }
        }
    }

    public List<Concert> findAll() throws SQLException {
        String sql = SELECT_CONCERT + ORDER_CONCERT;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapConcerts(rs);
        }
    }

    /** The filtered view: concerts held in a given month and year. */
    public List<Concert> findByMonthYear(int month, int year) throws SQLException {
        String sql = SELECT_CONCERT
                   + "WHERE YEAR(c.concert_date) = ? AND MONTH(c.concert_date) = ?" + ORDER_CONCERT;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                return mapConcerts(rs);
            }
        }
    }

    // ------------------------------------------------------------------
    // Report
    // ------------------------------------------------------------------

    /** Per venue: concerts held and total ticket capacity for a month and year (0 if none). */
    public List<VenueUtilizationRow> venueUtilization(int month, int year) throws SQLException {
        String sql = "SELECT v.venue_name, v.city, COUNT(c.concert_id) AS concerts_held, "
                   + "COALESCE(SUM(c.ticket_capacity), 0) AS total_ticket_capacity "
                   + "FROM venue v "
                   + "LEFT JOIN concert c ON c.venue_id = v.venue_id "
                   + "AND YEAR(c.concert_date) = ? AND MONTH(c.concert_date) = ? "
                   + "GROUP BY v.venue_id, v.venue_name, v.city "
                   + "ORDER BY v.venue_name";
        List<VenueUtilizationRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new VenueUtilizationRow(
                            rs.getString("venue_name"),
                            rs.getString("city"),
                            rs.getInt("concerts_held"),
                            rs.getLong("total_ticket_capacity")));
                }
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // Checks used by the service before saving
    // ------------------------------------------------------------------

    public boolean artistExists(int artistId) throws SQLException {
        return exists("SELECT 1 FROM artist WHERE artist_id = ?", artistId);
    }

    public boolean supplierExists(int supplierId) throws SQLException {
        return exists("SELECT 1 FROM supplier WHERE supplier_id = ?", supplierId);
    }

    /** Returns null when the venue does not exist. */
    public Integer findVenueCapacity(int venueId) throws SQLException {
        String sql = "SELECT venue_capacity FROM venue WHERE venue_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, venueId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Integer.valueOf(rs.getInt(1)) : null;
            }
        }
    }

    public int countTicketPurchases(int concertId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ticket_purchase WHERE concert_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, concertId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // Lists for the drop-downs
    // ------------------------------------------------------------------

    public List<LookupItem> listArtists() throws SQLException {
        return lookup("SELECT artist_id, artist_name FROM artist ORDER BY artist_name");
    }

    public List<LookupItem> listVenues() throws SQLException {
        return lookup("SELECT venue_id, venue_name FROM venue ORDER BY venue_name");
    }

    public List<LookupItem> listSuppliers() throws SQLException {
        return lookup("SELECT supplier_id, supplier_name FROM supplier ORDER BY supplier_name");
    }

    /** Years that have at least one concert, for the report's year drop-down. */
    public List<Integer> listConcertYears() throws SQLException {
        String sql = "SELECT DISTINCT YEAR(concert_date) AS concert_year FROM concert ORDER BY concert_year";
        List<Integer> years = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                years.add(rs.getInt(1));
            }
        }
        return years;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void bindConcertFields(PreparedStatement ps, Concert c) throws SQLException {
        ps.setString(1, c.getConcertName());
        ps.setInt(2, c.getArtistId());
        ps.setInt(3, c.getVenueId());
        ps.setObject(4, c.getConcertDate());
        ps.setObject(5, c.getStartTime());
        ps.setObject(6, c.getEndTime());
        ps.setInt(7, c.getTicketCapacity());
    }

    private Concert mapConcert(ResultSet rs) throws SQLException {
        Concert c = new Concert();
        c.setConcertId(rs.getInt("concert_id"));
        c.setConcertName(rs.getString("concert_name"));
        c.setArtistId(rs.getInt("artist_id"));
        c.setArtistName(rs.getString("artist_name"));
        c.setVenueId(rs.getInt("venue_id"));
        c.setVenueName(rs.getString("venue_name"));
        c.setConcertDate(rs.getObject("concert_date", LocalDate.class));
        c.setStartTime(rs.getObject("start_time", LocalTime.class));
        c.setEndTime(rs.getObject("end_time", LocalTime.class));
        c.setTicketCapacity(rs.getInt("ticket_capacity"));
        return c;
    }

    private List<Concert> mapConcerts(ResultSet rs) throws SQLException {
        List<Concert> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapConcert(rs));
        }
        return list;
    }

    private boolean exists(String sql, int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<LookupItem> lookup(String sql) throws SQLException {
        List<LookupItem> items = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new LookupItem(rs.getInt(1), rs.getString(2)));
            }
        }
        return items;
    }
}
