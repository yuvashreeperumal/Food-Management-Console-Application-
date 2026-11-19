package foodmenu;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public final class DBConnection {
    private static final String url = "jdbc:mysql://localhost/food_menu_db";
    private static final String username = "root";
    private static final String password = "yuvashree"; // Change to your MySQL password.
    private DBConnection() {}
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
