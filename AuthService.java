package foodmenu;
import java.sql.*;
public class AuthService {
    public boolean registerCustomer(
            String username, String password, String fullName)
            throws SQLException {
        String sql = "INSERT INTO customers " + "(username, password, full_name) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            return ps.executeUpdate() == 1;
        }
    }
    public User login(String username, String password, boolean customerLogin) throws SQLException {
        String sql;
        if (customerLogin) {
            sql = "SELECT customer_id, username, full_name " +
                    "FROM customers " +
                    "WHERE username = ? AND password = ? AND active = TRUE";
        }
        else {
            sql = "SELECT employee_id, username, full_name, role " +
                    "FROM employees " +
                    "WHERE username = ? AND password = ? AND active = TRUE";
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                if (customerLogin) {
                    return new User(rs.getInt("customer_id"), rs.getString("username"),
                            rs.getString("full_name"), "CUSTOMER");
                }
                return new User(rs.getInt("employee_id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("role"));
            }
        }
    }
    public void ensureStarterManager() {
        String check = "SELECT employee_id FROM employees " + "WHERE username = 'admin'";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(check)) {
            if (rs.next()) {
                return;
            }
        }
        catch (SQLException e) {
            System.out.println("Database setup error: " + e.getMessage());
            return;
        }
        String sql = "INSERT INTO employees " + "(username, password, full_name, role) " + "VALUES (?, ?, ?, 'ADMIN')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, "Admin@123");
            ps.setString(3, "System Administrator");
            ps.executeUpdate();
            System.out.println("Administrator account created.");
            System.out.println("Username: admin");
            System.out.println("Password: Admin@123");
        }
        catch (SQLException e) {
            System.out.println(
                    "Could not create administrator: " + e.getMessage());
        }
    }
}