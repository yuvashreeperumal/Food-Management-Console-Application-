package foodmenu;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
public class OrderService {
    private final Scanner sc;
    public OrderService(Scanner sc) {

        this.sc = sc;
    }
    public void placeOrder(User customer) {
        Map<Integer, Integer> cart = new LinkedHashMap<>();
        while (true) {
            System.out.print("Enter menu item ID (0 to checkout): ");
            int id;
            try {
                id = Integer.parseInt(sc.nextLine().trim());
            }
            catch (NumberFormatException e) {
                System.out.println("Enter a number.");
                continue;
            }
            if (id == 0) {
                break;
            }
            System.out.print("Quantity: ");
            int qty;
            try {
                qty = Integer.parseInt(sc.nextLine().trim());
            }
            catch (NumberFormatException e) {
                System.out.println("Invalid quantity.");
                continue;
            }
            if (qty < 1 || qty > 50) {
                System.out.println("Quantity must be 1-50.");
                continue;
            }
            cart.put(id, cart.getOrDefault(id, 0) + qty);
        }
        if (cart.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }
        System.out.println("Order type: 1 Dine-in, 2 Takeaway, 3 Delivery");
        String type;
        switch (sc.nextLine().trim()) {
            case "1":
                type = "DINE_IN";
                break;
            case "2":
                type = "TAKEAWAY";
                break;
            case "3":
                type = "DELIVERY";
                break;
            default:
                System.out.println("Invalid order type.");
                return;
        }
        String select = "SELECT item_name, price FROM menu_items " + "WHERE item_id = ? AND available = TRUE";
        String insertOrder = "INSERT INTO orders " + "(customer_id, order_type, total_amount) VALUES (?, ?, ?)";
        String insertLine = "INSERT INTO order_items " + "(order_id, item_id, item_name_snapshot, unit_price, quantity, line_total) " + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                Map<Integer, String[]> itemInfo = new LinkedHashMap<>();
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    try (PreparedStatement ps = con.prepareStatement(select)) {
                        ps.setInt(1, entry.getKey());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException("Item " + entry.getKey() + " is unavailable or does not exist.");
                            }
                            BigDecimal price = rs.getBigDecimal("price");
                            BigDecimal line = price.multiply(BigDecimal.valueOf(entry.getValue()));
                            total = total.add(line);
                            itemInfo.put(entry.getKey(), new String[]{rs.getString("item_name"), price.toPlainString()});
                        }
                    }
                }
                int orderId;
                try (PreparedStatement ps = con.prepareStatement(
                        insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customer.getId());
                    ps.setString(2, type);
                    ps.setBigDecimal(3, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not get order ID.");
                        }
                        orderId = keys.getInt(1);
                    }
                }
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    String[] info = itemInfo.get(entry.getKey());
                    BigDecimal price = new BigDecimal(info[1]);
                    BigDecimal line = price.multiply(BigDecimal.valueOf(entry.getValue()));
                    try (PreparedStatement ps = con.prepareStatement(insertLine)) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, entry.getKey());
                        ps.setString(3, info[0]);
                        ps.setBigDecimal(4, price);
                        ps.setInt(5, entry.getValue());
                        ps.setBigDecimal(6, line);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                System.out.println("Order placed successfully. Order ID: " + orderId + " | Total: " + total);
            }
            catch (Exception e) {
                try {
                    con.rollback();
                }
                catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                System.out.println("Order was not placed: " + e.getMessage());
            }
            finally {
                try {
                    con.setAutoCommit(true);
                }
                catch (SQLException e) {
                    System.out.println("Could not restore connection settings: " + e.getMessage());
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Order error: " + e.getMessage());
        }
    }
    public void viewMyOrders(User customer) {
        String sql = "SELECT order_id, order_type, total_amount, status, created_at " + "FROM orders WHERE customer_id = ? " +
                        "ORDER BY created_at DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customer.getId());
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-8s %-12s %-12s %-12s %-22s%n", "Order ID", "Type", "Total", "Status", "Created");
                while (rs.next()) {
                    System.out.printf("%-8d %-12s %-12s %-12s %-22s%n", rs.getInt("order_id"), rs.getString("order_type"),
                            rs.getBigDecimal("total_amount"), rs.getString("status"), rs.getTimestamp("created_at"));
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Could not retrieve orders: " + e.getMessage());
        }
    }
    public void viewActiveOrders() {
        String sql = "SELECT o.order_id, c.full_name, o.order_type, " + "o.total_amount, o.status, o.created_at " +
                "FROM orders o " + "JOIN customers c ON o.customer_id = c.customer_id " + "WHERE o.status NOT " +
                "IN ('SERVED', 'CANCELLED') " + "ORDER BY o.created_at";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-8s %-20s %-12s %-10s %-12s %-22s%n", "Order", "Customer", "Type", "Total", "Status", "Created");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-8d %-20s %-12s %-10s %-12s %-22s%n", rs.getInt("order_id"), rs.getString("full_name"),
                        rs.getString("order_type"), rs.getBigDecimal("total_amount"), rs.getString("status"),
                        rs.getTimestamp("created_at"));
            }
            if (!found) {
                System.out.println("No active orders found.");
            }
        }
        catch (SQLException e) {
            System.out.println("Could not retrieve active orders: " + e.getMessage());
        }
    }
    public void updateOrderStatus() {
        try {
            System.out.print("Order ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.println("Status: 1 RECEIVED, 2 PREPARING, " + "3 READY, 4 SERVED, 5 CANCELLED");
            String status;
            switch (sc.nextLine().trim()) {
                case "1":
                    status = "RECEIVED";
                    break;
                case "2":
                    status = "PREPARING";
                    break;
                case "3":
                    status = "READY";
                    break;
                case "4":
                    status = "SERVED";
                    break;
                case "5":
                    status = "CANCELLED";
                    break;
                default:
                    System.out.println("Invalid status.");
                    return;
            }
            String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                System.out.println(ps.executeUpdate() == 1 ? "Order status updated." : "Order not found.");
            }
        }
        catch (Exception e) {System.out.println("Status update failed: " + e.getMessage());
        }
    }
}