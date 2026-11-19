package foodmenu;
import java.sql.*;
import java.util.Scanner;
public class EmployeeService {
    private final Scanner sc;
    public EmployeeService(Scanner sc) {
        this.sc = sc;
    }
    public void addEmployee() {
        try {
            String username = read("Create username: ");
            String fullName = read("Employee full name: ");
            String password = read("Temporary password: ");
            System.out.println("Role: 1 Manager, 2 Kitchen Staff, 3 Waitstaff");
            String role;
            switch (read("Choose role: ")) {
                case "1":
                    role = "MANAGER";
                    break;
                case "2":
                    role = "KITCHEN_STAFF";
                    break;
                case "3":
                    role = "WAITSTAFF";
                    break;
                default:
                    System.out.println("Invalid role.");
                    return;
            }
            String sql = "INSERT INTO employees " + "(username, password, full_name, role) VALUES (?, ?, ?, ?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, fullName);
                ps.setString(4, role);
                System.out.println(ps.executeUpdate() == 1 ? "Employee created successfully." : "Employee creation failed.");
            }
        }
        catch (SQLException e) {
            System.out.println("Could not create employee: " + e.getMessage());
        }
    }
    public void listEmployees() {
        String sql = "SELECT employee_id, username, full_name, role, " + "active, created_at FROM employees ORDER BY employee_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-5s %-18s %-24s %-18s %-8s %-20s%n", "ID", "Username", "Name", "Role", "Active", "Created");
            while (rs.next()) {
                System.out.printf("%-5d %-18s %-24s %-18s %-8s %-20s%n", rs.getInt("employee_id"), rs.getString("username"),
                        rs.getString("full_name"), rs.getString("role"), rs.getBoolean("active"),
                        rs.getTimestamp("created_at"));
            }
        }
        catch (SQLException e) {
            System.out.println("Employee list failed: " + e.getMessage());
        }
    }
    public void updateEmployee() {
        try {
            int id = Integer.parseInt(read("Employee ID: "));
            String fullName = read("New full name: ");
            System.out.println("New role: 1 Manager, 2 Kitchen Staff, 3 Waitstaff");
            String role;
            switch (read("Choose role: ")) {
                case "1":
                    role = "MANAGER";
                    break;
                case "2":
                    role = "KITCHEN_STAFF";
                    break;
                case "3":
                    role = "WAITSTAFF";
                    break;
                default:
                    System.out.println("Invalid role.");
                    return;
            }
            String sql = "UPDATE employees SET full_name = ?, role = ? " + "WHERE employee_id = ? AND role <> 'ADMIN'";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, fullName);
                ps.setString(2, role);
                ps.setInt(3, id);
                System.out.println(ps.executeUpdate() == 1 ? "Employee updated successfully." : "Employee not found or protected account.");
            }
        }
        catch (NumberFormatException e) {
            System.out.println("Please enter a valid employee ID.");
        }
        catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }
    public void deleteEmployee() {
        try {
            int id = Integer.parseInt(read("Employee ID to deactivate: "));
            String sql = "UPDATE employees SET active = FALSE " + "WHERE employee_id = ? AND role <> 'ADMIN'";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                System.out.println(ps.executeUpdate() == 1 ? "Employee account deactivated." : "Employee not found or protected account.");
            }
        }
        catch (NumberFormatException e) {
            System.out.println("Please enter a valid employee ID.");
        }
        catch (SQLException e) {
            System.out.println("Deactivation failed: " + e.getMessage());
        }
    }
    private String read(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
}