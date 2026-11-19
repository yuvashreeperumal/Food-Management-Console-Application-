package foodmenu;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Scanner;
public class FoodMenu {
    private final Scanner sc;
    public FoodMenu(Scanner sc) {
        this.sc = sc;
    }
    public void showMenu(boolean includeUnavailable) {
        String sql = "SELECT m.item_id, " + "COALESCE(c.category_name, 'Other') AS category_name, "
                + "m.item_name, m.description, m.price, " + "m.dietary_tags, m.available " +
                "FROM menu_items m " + "LEFT JOIN menu_categories c " + "ON m.category_id = c.category_id "
                + (includeUnavailable ? "" : "WHERE m.available = TRUE ") + "ORDER BY category_name," +
                " m.item_name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\n--- FOOD MENU ---");
            System.out.printf("%-5s %-18s %-25s %-10s %-12s %-10s%n", "ID", "Category", "Item", "Price",
                    "Available", "Tags");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-18s %-25s %-10s %-12s %-10s%n",
                        rs.getInt("item_id"),
                        clip(rs.getString("category_name"), 18),
                        clip(rs.getString("item_name"), 25),
                        rs.getBigDecimal("price"),
                        rs.getBoolean("available") ? "Yes" : "No",
                        clip(rs.getString("dietary_tags"), 10));
                String desc = rs.getString("description");
                if (desc != null && !desc.isBlank()) {
                    System.out.println("      " + desc);
                }
            }
            if (!found) {
                System.out.println("No menu items found.");
            }
        }
        catch (SQLException e) {
            System.out.println("Menu error: " + e.getMessage());
        }
    }
    public void addItem() {
        try {
            int categoryId = readInt(
                    "Category ID (1 Appetizers, 2 Main Course, " +
                            "3 Desserts, 4 Beverages): ");
            if (!categoryExists(categoryId)) {
                System.out.println("Invalid category ID.");
                return;
            }
            String name = readLine("Item name: ");
            if (name.isBlank()) {
                System.out.println("Item name cannot be empty.");
                return;
            }
            String description = readLine("Description: ");
            BigDecimal price = readPrice("Price: ");
            String tags = readLine("Dietary tags (e.g. Vegan,Spicy; blank if none): ");
            String sql = "INSERT INTO menu_items " + "(category_id, item_name, description, " +
                    "price, dietary_tags) " + "VALUES (?, ?, ?, ?, ?)";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, categoryId);
                ps.setString(2, name);
                ps.setString(3, description);
                ps.setBigDecimal(4, price);
                ps.setString(5, tags.isBlank() ? null : tags);
                System.out.println(ps.executeUpdate() == 1 ? "Menu item added." : "Add failed.");
            }
        }
        catch (Exception e) {
            System.out.println("Could not add item: " + e.getMessage());
        }
    }
    public void updateItem() {
        try {
            int id = readInt("Item ID to update: ");

            String name = readLine("New item name: ");

            if (name.isBlank()) {
                System.out.println("Item name cannot be empty.");
                return;
            }
            String description = readLine("New description: ");
            BigDecimal price = readPrice("New price: ");
            String tags = readLine("New dietary tags: ");
            int categoryId = readInt("Category ID: ");
            if (!categoryExists(categoryId)) {
                System.out.println("Invalid category ID.");
                return;
            }
            String sql = "UPDATE menu_items SET item_name = ?, " + "description = ?, price = ?," +
                    " dietary_tags = ?, " +  "category_id = ? WHERE item_id = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setBigDecimal(3, price);
                ps.setString(4, tags.isBlank() ? null : tags);
                ps.setInt(5, categoryId);
                ps.setInt(6, id);
                System.out.println(ps.executeUpdate() == 1 ? "Menu item updated." : "Item ID not found.");
            }
        }
        catch (Exception e) {
            System.out.println("Could not update item: " + e.getMessage());
        }
    }
    public void deleteItem() {
        try {
            int id = readInt("Item ID to delete: ");
            String sql = "DELETE FROM menu_items WHERE item_id = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                System.out.println(ps.executeUpdate() == 1 ? "Menu item deleted." : "Item ID not found.");
            }
        } catch (SQLException e) {
            System.out.println("Delete failed. The item may already be part " + "of an order. " + e.getMessage());
        }
    }
    public void toggleAvailability() {
        try {
            int id = readInt("Item ID: ");
            String sql = "UPDATE menu_items " + "SET available = NOT available WHERE item_id = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                System.out.println(ps.executeUpdate() == 1 ? "Availability toggled." : "Item ID not found.");
            }
        }
        catch (SQLException e) {
            System.out.println("Availability update failed: " + e.getMessage());
        }
    }
    private boolean categoryExists(int categoryId) throws SQLException {
        String sql = "SELECT 1 FROM menu_categories WHERE category_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    private int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(sc.nextLine().trim());
            }
            catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            }
        }
    }
    private BigDecimal readPrice(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);

                BigDecimal price =
                        new BigDecimal(sc.nextLine().trim());

                if (price.signum() > 0) {
                    return price;
                }

            }
            catch (NumberFormatException ignored) {
                System.out.println("Enter a valid price greater than zero.");
            }
        }
    }
    private String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
    private String clip(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }
}
