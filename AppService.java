import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

public final class AppService {
    public static final String[] STATUSES = {"Active", "Inactive"};
    public static final String[] SUPPLIER_TYPES = {"Sound System", "Lighting",
        "Stage Equipment", "Transportation/Logistics", "Catering"};

    public enum Kind {
        ARTIST("artist", "genre", "artist_name", "genre", "country",
                "debut_year", "active_status"),
        VENUE("venue", "city", "venue_name", "street_address", "city",
                "venue_capacity"),
        SUPPLIER("supplier", "supplier_type", "supplier_name", "supplier_type",
                "contact_person", "email_address", "contact_number",
                "business_address", "service_fee", "supplier_status");

        final String table, filter, id;
        final String[] fields;
        Kind(String table, String filter, String... fields) {
            this.table = table;
            this.filter = filter;
            this.id = table + "_id";
            this.fields = fields;
        }

        String select() {
            String columns = String.join(", ", fields);
            if (this == ARTIST)
                columns = columns.replace("debut_year",
                        "CAST(debut_year AS UNSIGNED) AS debut_year");
            return "SELECT " + id + ", " + columns + " FROM " + table;
        }
    }

    public enum Report { ARTIST, VENUE, GENRE, TICKET }

    public record Item(int id, String name) {
        public String toString() { return name + " (#" + id + ")"; }
    }

    public record Concert(String name, int artist, int venue, LocalDate date,
        LocalTime start, LocalTime end, int capacity, List<Integer> suppliers) {}

    public DataAccess.Rows records(Kind kind, String filter) throws SQLException {
        String sql = kind.select();
        if (filter == null || filter.isBlank())
            return DataAccess.query(sql + " ORDER BY " + kind.id);
        return DataAccess.query(sql + " WHERE " + kind.filter +
                " = ? ORDER BY " + kind.id, filter.trim());
    }

    public Object[] record(Kind kind, int id) throws SQLException {
        return first(DataAccess.query(kind.select() + " WHERE " + kind.id + " = ?", id));
    }

    public List<String> filterValues(Kind kind) throws SQLException {
        return strings("SELECT DISTINCT " + kind.filter + " FROM " + kind.table +
                " ORDER BY " + kind.filter);
    }

    public void saveRecord(Kind kind, Integer id, String[] input) throws SQLException {
        Object[] args = new Object[kind.fields.length];
        for (int i = 0; i < args.length; i++)
            args[i] = field(kind.fields[i], input[i]);
        DataAccess.transaction(con -> {
            if (id == null) {
                String placeholders = String.join(", ", Collections.nCopies(args.length, "?"));
                DataAccess.insert(con, "INSERT INTO " + kind.table + " (" +
                        String.join(", ", kind.fields) + ") VALUES (" + placeholders + ")", args);
            } else {
                first(DataAccess.query(con, "SELECT " + kind.id + " FROM " + kind.table +
                        " WHERE " + kind.id + " = ? FOR UPDATE", id));
                if (kind == Kind.VENUE && (Integer) args[3] < number(DataAccess.query(con,
                        "SELECT COALESCE(MAX(ticket_capacity),0) FROM concert WHERE venue_id = ?", id)))
                    throw new IllegalArgumentException("Capacity is below an existing concert capacity.");
                StringJoiner sets = new StringJoiner(", ");
                for (String col : kind.fields) sets.add(col + " = ?");
                Object[] values = Arrays.copyOf(args, args.length + 1);
                values[args.length] = id;
                DataAccess.update(con, "UPDATE " + kind.table + " SET " + sets +
                        " WHERE " + kind.id + " = ?", values);
            }
            return null;
        });
    }

    public void deleteRecord(Kind kind, int id) throws SQLException {
        DataAccess.transaction(con -> {
            if (DataAccess.update(con, "DELETE FROM " + kind.table +
                    " WHERE " + kind.id + " = ?", id) == 0)
                throw new IllegalArgumentException("Record no longer exists.");
            return null;
        });
    }

    private static Object field(String name, String raw) {
        String value = raw.trim();
        if (name.equals("debut_year")) {
            if (value.isEmpty()) return null;
            int year = Integer.parseInt(value);
            if (year < 1901 || year > 2155)
                throw new IllegalArgumentException("Debut year must be 1901–2155.");
            return year;
        }
        if (name.equals("venue_capacity")) {
            int capacity = Integer.parseInt(value);
            if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive.");
            return capacity;
        }
        if (name.equals("service_fee")) {
            BigDecimal fee = new BigDecimal(value);
            if (fee.signum() < 0 || fee.stripTrailingZeros().scale() > 2 ||
                    fee.compareTo(new BigDecimal("99999999.99")) > 0)
                throw new IllegalArgumentException("Invalid service fee (maximum 99999999.99).");
            return fee;
        }
        if (name.endsWith("_status")) return choice(value, STATUSES);
        if (name.equals("supplier_type")) return choice(value, SUPPLIER_TYPES);
        int limit = switch (name) {
            case "genre" -> 50;
            case "country", "city" -> 60;
            case "street_address" -> 150;
            case "business_address" -> 200;
            case "contact_number" -> 20;
            default -> 100;
        };
        return text(value, limit, name.replace('_', ' '));
    }

    private static String choice(String value, String[] allowed) {
        if (!Arrays.asList(allowed).contains(value))
            throw new IllegalArgumentException("Invalid selection.");
        return value;
    }

    private static String text(String value, int max, String label) {
        String clean = value.trim();
        if (clean.isEmpty() || clean.codePointCount(0, clean.length()) > max)
            throw new IllegalArgumentException(label + " must contain 1–" + max + " characters.");
        return clean;
    }

    private static Object[] first(DataAccess.Rows rows) {
        if (rows.values().isEmpty()) throw new IllegalArgumentException("Record not found.");
        return rows.values().get(0);
    }

    private static long number(DataAccess.Rows rows) {
        return ((Number) first(rows)[0]).longValue();
    }

    private static List<String> strings(String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        for (Object[] row : DataAccess.query(sql).values()) values.add(row[0].toString());
        return values;
    }

    public List<Item> items(Kind kind) throws SQLException {
        List<Item> items = new ArrayList<>();
        for (Object[] row : DataAccess.query("SELECT " + kind.id + ", " +
                kind.fields[0] + " FROM " + kind.table + " ORDER BY " + kind.fields[0]).values())
            items.add(new Item(((Number) row[0]).intValue(), row[1].toString()));
        return items;
    }

    private static final String CONCERT_LIST = """
        SELECT c.concert_id, c.concert_name, a.artist_name, v.venue_name,
               c.concert_date, c.start_time, c.end_time, c.ticket_capacity
        FROM concert c JOIN artist a ON a.artist_id = c.artist_id
        JOIN venue v ON v.venue_id = c.venue_id
        """;

    public DataAccess.Rows concerts(YearMonth month) throws SQLException {
        String order = " ORDER BY c.concert_date, c.start_time, c.concert_id";
        if (month == null) return DataAccess.query(CONCERT_LIST + order);
        return DataAccess.query(CONCERT_LIST +
                " WHERE c.concert_date >= ? AND c.concert_date < ?" + order,
                month.atDay(1), month.plusMonths(1).atDay(1));
    }

    public Concert concert(int id) throws SQLException {
        Object[] r = first(DataAccess.query("""
            SELECT concert_name, artist_id, venue_id, concert_date,
                   start_time, end_time, ticket_capacity
            FROM concert WHERE concert_id = ?
            """, id));
        List<Integer> suppliers = new ArrayList<>();
        for (Object[] row : DataAccess.query("SELECT supplier_id FROM concert_supplier " +
                "WHERE concert_id = ? ORDER BY supplier_id", id).values())
            suppliers.add(((Number) row[0]).intValue());
        return new Concert(r[0].toString(), ((Number) r[1]).intValue(),
                ((Number) r[2]).intValue(), LocalDate.parse(r[3].toString()),
                LocalTime.parse(r[4].toString()), LocalTime.parse(r[5].toString()),
                ((Number) r[6]).intValue(), suppliers);
    }

    public DataAccess.Rows concertDetails(int id) throws SQLException {
        return DataAccess.query(CONCERT_LIST + " WHERE c.concert_id = ?", id);
    }

    public DataAccess.Rows concertSuppliers(int id) throws SQLException {
        return DataAccess.query("""
            SELECT s.supplier_id, s.supplier_name, s.supplier_type
            FROM concert_supplier cs JOIN supplier s ON s.supplier_id = cs.supplier_id
            WHERE cs.concert_id = ? ORDER BY s.supplier_name
            """, id);
    }

    public void saveConcert(Integer id, Concert c) throws SQLException {
        String name = text(c.name(), 150, "Concert name");
        if (c.date() == null || c.start() == null || c.end() == null ||
                !c.end().isAfter(c.start()) || c.capacity() <= 0)
            throw new IllegalArgumentException("Invalid date, schedule or capacity.");
        if (c.suppliers() == null || c.suppliers().isEmpty() || c.suppliers().size() > 10 ||
                new HashSet<>(c.suppliers()).size() != c.suppliers().size())
            throw new IllegalArgumentException("Select 1–10 different suppliers.");
        DataAccess.transaction(con -> {
            if (id != null)
                first(DataAccess.query(con, "SELECT concert_id FROM concert " +
                        "WHERE concert_id = ? FOR UPDATE", id));
            long limit = number(DataAccess.query(con, "SELECT venue_capacity FROM venue " +
                    "WHERE venue_id = ? FOR UPDATE", c.venue()));
            if (c.capacity() > limit)
                throw new IllegalArgumentException("Ticket capacity exceeds venue capacity.");
            if (id != null && c.capacity() < number(DataAccess.query(con,
                    "SELECT COALESCE(SUM(quantity),0) FROM ticket_purchase WHERE concert_id = ?", id)))
                throw new IllegalArgumentException("Capacity cannot be below tickets sold.");
            Object[] args = {name, c.artist(), c.venue(), c.date(), c.start(), c.end(), c.capacity()};
            int concertId;
            if (id == null) {
                concertId = DataAccess.insert(con, """
                    INSERT INTO concert (concert_name, artist_id, venue_id, concert_date,
                        start_time, end_time, ticket_capacity) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, args);
            } else {
                concertId = id;
                Object[] values = Arrays.copyOf(args, 8);
                values[7] = id;
                DataAccess.update(con, """
                    UPDATE concert SET concert_name=?, artist_id=?, venue_id=?, concert_date=?,
                        start_time=?, end_time=?, ticket_capacity=? WHERE concert_id=?
                    """, values);
                DataAccess.update(con, "DELETE FROM concert_supplier WHERE concert_id = ?", id);
            }
            for (Integer supplier : c.suppliers())
                DataAccess.update(con, "INSERT INTO concert_supplier VALUES (?, ?)", concertId, supplier);
            return null;
        });
    }

    public void deleteConcert(int id) throws SQLException {
        DataAccess.transaction(con -> {
            first(DataAccess.query(con, "SELECT concert_id FROM concert WHERE concert_id = ? FOR UPDATE", id));
            if (number(DataAccess.query(con, "SELECT COUNT(*) FROM ticket_purchase WHERE concert_id = ?", id)) > 0)
                throw new IllegalArgumentException("Concert has ticket purchases and cannot be deleted.");
            DataAccess.update(con, "DELETE FROM concert_supplier WHERE concert_id = ?", id);
            DataAccess.update(con, "DELETE FROM concert WHERE concert_id = ?", id);
            return null;
        });
    }

    public DataAccess.Rows report(Report report, YearMonth month, String genre) throws SQLException {
        LocalDate start = month.atDay(1), end = month.plusMonths(1).atDay(1);
        return switch (report) {
            case ARTIST -> DataAccess.query("""
                SELECT a.artist_id, a.artist_name, COUNT(c.concert_id) AS total_concerts
                FROM artist a LEFT JOIN concert c ON c.artist_id = a.artist_id
                AND c.concert_date >= ? AND c.concert_date < ?
                GROUP BY a.artist_id, a.artist_name ORDER BY total_concerts DESC, a.artist_name
                """, start, end);
            case VENUE -> DataAccess.query("""
                SELECT v.venue_name, v.city, COUNT(c.concert_id) AS concerts_held,
                       COALESCE(SUM(c.ticket_capacity),0) AS total_ticket_capacity
                FROM venue v LEFT JOIN concert c ON c.venue_id = v.venue_id
                AND c.concert_date >= ? AND c.concert_date < ?
                GROUP BY v.venue_id, v.venue_name, v.city ORDER BY v.venue_name
                """, start, end);
                case GENRE -> DataAccess.query("""
                SELECT a.genre, COUNT(c.concert_id) AS total_concerts,
                       COUNT(DISTINCT c.artist_id) AS unique_performing_artists
                FROM artist a LEFT JOIN concert c ON c.artist_id = a.artist_id
                AND c.concert_date >= ? AND c.concert_date < ?
                WHERE a.genre = ? GROUP BY a.genre
                """, start, end, text(Objects.toString(genre, ""), 50, "Genre"));
            case TICKET -> DataAccess.query("""
                SELECT c.concert_id, c.concert_name, c.concert_date,
                       COALESCE(SUM(p.quantity),0) AS total_tickets_sold,
                       COALESCE(SUM(p.quantity*p.unit_price),0.00) AS total_ticket_revenue
                FROM concert c LEFT JOIN ticket_purchase p ON p.concert_id = c.concert_id
                WHERE c.concert_date >= ? AND c.concert_date < ?
                GROUP BY c.concert_id, c.concert_name, c.concert_date
                ORDER BY c.concert_date, c.concert_name, c.concert_id
                """, start, end);
        };
    }
}
