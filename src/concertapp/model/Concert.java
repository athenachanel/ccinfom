package concertapp.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** One row of the concert table, plus the artist and venue names for display. */
public class Concert {

    private int concertId;
    private String concertName;
    private int artistId;
    private String artistName;     // filled when reading, ignored when saving
    private int venueId;
    private String venueName;      // filled when reading, ignored when saving
    private LocalDate concertDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int ticketCapacity;

    /** Used when adding: every concert needs at least one supplier (UML 1..*). */
    private List<Integer> supplierIds = new ArrayList<>();

    public int getConcertId() { return concertId; }
    public void setConcertId(int concertId) { this.concertId = concertId; }

    public String getConcertName() { return concertName; }
    public void setConcertName(String concertName) { this.concertName = concertName; }

    public int getArtistId() { return artistId; }
    public void setArtistId(int artistId) { this.artistId = artistId; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public int getVenueId() { return venueId; }
    public void setVenueId(int venueId) { this.venueId = venueId; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public LocalDate getConcertDate() { return concertDate; }
    public void setConcertDate(LocalDate concertDate) { this.concertDate = concertDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getTicketCapacity() { return ticketCapacity; }
    public void setTicketCapacity(int ticketCapacity) { this.ticketCapacity = ticketCapacity; }

    public List<Integer> getSupplierIds() { return supplierIds; }
    public void setSupplierIds(List<Integer> supplierIds) { this.supplierIds = supplierIds; }

    @Override
    public String toString() {
        return concertId + " | " + concertName + " | " + artistName + " | " + venueName
                + " | " + concertDate + " " + startTime + "-" + endTime + " | cap " + ticketCapacity;
    }
}
