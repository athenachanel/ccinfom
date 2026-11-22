import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String URL =
            "jdbc:mysql://127.0.0.1:3306/concert_management";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        String user = System.getenv("MYSQL_USER");
        String password = System.getenv("MYSQL_PASSWORD");

        if (user == null || user.isBlank() || password == null) {
            throw new SQLException("Set MYSQL_USER and MYSQL_PASSWORD before running the app.");
        }

        return DriverManager.getConnection(URL, user, password);
    }
}