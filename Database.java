import java.sql.*;

public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("MYSQL_URL");
        String user = System.getenv("MYSQL_USER");
        String password = System.getenv("MYSQL_PASSWORD");
        if (url == null || url.isBlank())
            url = "jdbc:mysql://127.0.0.1:3306/concert_management";
        if (user == null || user.isBlank() || password == null)
            throw new SQLException("Set MYSQL_USER and MYSQL_PASSWORD.");
        return DriverManager.getConnection(url, user, password);
    }
}
