import java.sql.*;
import java.util.*;

public final class DataAccess {
    private DataAccess() {}

    public record Rows(String[] columns, List<Object[]> values) {}

    @FunctionalInterface
    public interface Transaction<T> {
        T execute(Connection con) throws SQLException;
    }

    public static <T> T transaction(Transaction<T> work) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = work.execute(con);
                con.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); }
                catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

private static void bind(PreparedStatement ps, Object... args)
            throws SQLException {
        for (int i = 0; i < args.length; i++)
            ps.setObject(i + 1, args[i]);
    }

    public static Rows query(String sql, Object... args) throws SQLException {
        try (Connection con = Database.getConnection()) {
            return query(con, sql, args);
        }
    }

    public static Rows query(Connection con, String sql, Object... args)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                String[] columns = new String[meta.getColumnCount()];
                for (int i = 0; i < columns.length; i++)
                    columns[i] = meta.getColumnLabel(i + 1);
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[columns.length];
                    for (int i = 0; i < row.length; i++)
                        row[i] = rs.getObject(i + 1);
                    rows.add(row);
                }
                return new Rows(columns, rows);
            }
        }
    }

    public static int update(Connection con, String sql, Object... args)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate();
        }
    }

    public static int insert(Connection con, String sql, Object... args)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, args);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("No generated ID.");
                return rs.getInt(1);
            }
        }
    }
}
