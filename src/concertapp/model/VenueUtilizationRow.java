package concertapp.model;

/** One line of the Venue Utilization Report. */
public class VenueUtilizationRow {

    private final String venueName;
    private final String city;
    private final int concertsHeld;
    private final long totalTicketCapacity;

    public VenueUtilizationRow(String venueName, String city, int concertsHeld, long totalTicketCapacity) {
        this.venueName = venueName;
        this.city = city;
        this.concertsHeld = concertsHeld;
        this.totalTicketCapacity = totalTicketCapacity;
    }

    public String getVenueName() { return venueName; }
    public String getCity() { return city; }
    public int getConcertsHeld() { return concertsHeld; }
    public long getTotalTicketCapacity() { return totalTicketCapacity; }
}
