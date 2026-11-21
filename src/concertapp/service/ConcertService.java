package concertapp.service;

import concertapp.dao.ConcertDAO;
import concertapp.model.Concert;
import concertapp.model.LookupItem;
import concertapp.model.VenueUtilizationRow;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * APPLICATION TIER (logic) for the Concert/Event module.
 * The UI calls this class. It checks the rules, calls ConcertDAO, and turns
 * database trouble into a ConcertException with a message the UI can show.
 */
public class ConcertService {

    private final ConcertDAO dao = new ConcertDAO();

    // ------------------------------------------------------------------
    // Record management
    // ------------------------------------------------------------------

    /** Adds the concert (with its suppliers) and returns the new concert id. */
    public int addConcert(Concert c) throws ConcertException {
        try {
            validateConcert(c);
            validateSuppliers(c.getSupplierIds());
            return dao.addConcert(c);
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public void updateConcert(Concert c) throws ConcertException {
        try {
            if (dao.findById(c.getConcertId()) == null) {
                throw new ConcertException("No concert with ID " + c.getConcertId() + ".");
            }
            validateConcert(c);
            dao.updateConcert(c);
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public void deleteConcert(int concertId) throws ConcertException {
        try {
            if (dao.findById(concertId) == null) {
                throw new ConcertException("No concert with ID " + concertId + ".");
            }
            // Sold tickets are sales records, so a concert that has any cannot be deleted.
            int purchases = dao.countTicketPurchases(concertId);
            if (purchases > 0) {
                throw new ConcertException("This concert has " + purchases
                        + " ticket purchase record(s), so it cannot be deleted.");
            }
            dao.deleteConcert(concertId);
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public Concert getConcert(int concertId) throws ConcertException {
        try {
            Concert c = dao.findById(concertId);
            if (c == null) {
                throw new ConcertException("No concert with ID " + concertId + ".");
            }
            return c;
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public List<Concert> listAllConcerts() throws ConcertException {
        try {
            return dao.findAll();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    /** The filtered view: concerts in a given month (1-12) and year. */
    public List<Concert> listConcertsByMonthYear(int month, int year) throws ConcertException {
        checkMonthYear(month, year);
        try {
            return dao.findByMonthYear(month, year);
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    // ------------------------------------------------------------------
    // Report
    // ------------------------------------------------------------------

    public List<VenueUtilizationRow> venueUtilizationReport(int month, int year) throws ConcertException {
        checkMonthYear(month, year);
        try {
            return dao.venueUtilization(month, year);
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    // ------------------------------------------------------------------
    // Lists for the drop-downs
    // ------------------------------------------------------------------

    public List<LookupItem> getArtists() throws ConcertException {
        try {
            return dao.listArtists();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public List<LookupItem> getVenues() throws ConcertException {
        try {
            return dao.listVenues();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public List<LookupItem> getSuppliers() throws ConcertException {
        try {
            return dao.listSuppliers();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public List<Integer> getConcertYears() throws ConcertException {
        try {
            return dao.listConcertYears();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    // ------------------------------------------------------------------
    // Rules
    // ------------------------------------------------------------------

    private void validateConcert(Concert c) throws ConcertException, SQLException {
        if (c.getConcertName() == null || c.getConcertName().trim().isEmpty()) {
            throw new ConcertException("Concert name cannot be empty.");
        }
        c.setConcertName(c.getConcertName().trim());
        if (c.getConcertName().length() > 150) {
            throw new ConcertException("Concert name can be at most 150 characters.");
        }
        if (c.getConcertDate() == null) {
            throw new ConcertException("Please choose the concert date.");
        }
        if (c.getStartTime() == null || c.getEndTime() == null) {
            throw new ConcertException("Please enter both the start time and the end time.");
        }
        // Simple rule: a concert ends on the same day it starts.
        if (!c.getEndTime().isAfter(c.getStartTime())) {
            throw new ConcertException("End time must be later than the start time.");
        }
        if (c.getTicketCapacity() <= 0) {
            throw new ConcertException("Ticket capacity must be greater than 0.");
        }
        if (!dao.artistExists(c.getArtistId())) {
            throw new ConcertException("Artist ID " + c.getArtistId() + " does not exist.");
        }
        Integer venueCapacity = dao.findVenueCapacity(c.getVenueId());
        if (venueCapacity == null) {
            throw new ConcertException("Venue ID " + c.getVenueId() + " does not exist.");
        }
        if (c.getTicketCapacity() > venueCapacity) {
            throw new ConcertException("Ticket capacity (" + c.getTicketCapacity()
                    + ") cannot be more than the venue's capacity (" + venueCapacity + ").");
        }
    }

    /** A concert needs at least one supplier (UML 1..*), each one real and listed once. */
    private void validateSuppliers(List<Integer> supplierIds) throws ConcertException, SQLException {
        if (supplierIds == null || supplierIds.isEmpty()) {
            throw new ConcertException("A concert needs at least one supplier.");
        }
        Set<Integer> seen = new HashSet<>();
        for (int id : supplierIds) {
            if (!seen.add(id)) {
                throw new ConcertException("Supplier ID " + id + " is listed more than once.");
            }
            if (!dao.supplierExists(id)) {
                throw new ConcertException("Supplier ID " + id + " does not exist.");
            }
        }
    }

    private void checkMonthYear(int month, int year) throws ConcertException {
        if (month < 1 || month > 12) {
            throw new ConcertException("Month must be from 1 to 12.");
        }
        if (year < 1900 || year > 2100) {
            throw new ConcertException("Year must be between 1900 and 2100.");
        }
    }

    private ConcertException databaseError(SQLException e) {
        return new ConcertException("Database error: " + e.getMessage(), e);
    }
}
