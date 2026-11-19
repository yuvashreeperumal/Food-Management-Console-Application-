package foodmenu;
import java.sql.SQLException;
import java.util.Scanner;
public class Main {
    private final Scanner sc = new Scanner(System.in);
    private final AuthService auth = new AuthService();
    private final FoodMenu menu = new FoodMenu(sc);
    private final EmployeeService employees = new EmployeeService(sc);
    private final OrderService orders = new OrderService(sc);
    public static void main(String[] args) {
        Main app = new Main();
        app.auth.ensureStarterManager();
        app.run();
    }
    private void run() {
        while (true) {
            System.out.println("\n========== FOOD MENU MANAGEMENT ==========");
            System.out.println("1. Customer");
            System.out.println("2. Employee");
            System.out.println("0. Exit");
            String choice = read("Choose: ");
            switch (choice) {
                case "1": customerEntry(); break;
                case "2": employeeEntry(); break;
                case "0": System.out.println("Thank you. Goodbye!");
                return;
                default: System.out.println("Invalid choice.");
            }
        }
    }
    private void customerEntry() {
        while (true) {
            System.out.println("\n========== CUSTOMER ============");
            System.out.println("1. Register new customer");
            System.out.println("2. Login existing customer");
            System.out.println("0. Back");
            String choice = read("Choose: ");
            if ("1".equals(choice)) {
                String fullName = read("Full name: ");
                String username = read("Create username: ");
                String password = read("Create password: ");
                try {
                    System.out.println(auth.registerCustomer(username, password, fullName) ? "Registration successful. You can now log in." : "Registration failed.");
                }
                catch (SQLException e) { System.out.println("Registration failed: " + e.getMessage()); }
            }
            else if ("2".equals(choice)) {
                User user = login(true);
                if (user != null) customerDashboard(user);
            }
            else if ("0".equals(choice)) return;
            else System.out.println("Invalid choice.");
        }
    }
    private void employeeEntry() {
        User user = login(false);
        if (user == null) return;
        employeeDashboard(user);
    }
    private User login(boolean customer) {
        System.out.println(customer ? "\nCustomer Login" : "\nEmployee Login");
        String username = read("Username: ");
        String password = read("Password: ");
        try {
            User user = auth.login(username, password, customer);
            if (user == null) System.out.println("Invalid username/password or account type.");
            return user;
        }
        catch (SQLException e) {
            System.out.println("Login failed. Check database setup: " + e.getMessage());
            return null;
        }
    }
    private void customerDashboard(User user) {
        while (true) {
            System.out.println("\n--- CUSTOMER DASHBOARD: " + user.getFullName() + " ---");
            System.out.println("1. Browse available menu");
            System.out.println("2. Place order");
            System.out.println("3. View my orders / tracking status");
            System.out.println("0. Logout");
            switch (read("Choose: ")) {
                case "1": menu.showMenu(false); break;
                case "2": menu.showMenu(false); orders.placeOrder(user); break;
                case "3": orders.viewMyOrders(user); break;
                case "0": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }
    private void employeeDashboard(User user) {
        while (true) {
            System.out.println("\n--- " + user.getRole() + " DASHBOARD: " + user.getFullName() + " ---");
            if (user.getRole().equals("ADMIN") || user.getRole().equals("MANAGER")) {
                System.out.println("1. View all menu items");
                System.out.println("2. Add menu item");
                System.out.println("3. Update menu item / price");
                System.out.println("4. Delete menu item");
                System.out.println("5. Add employee account");
                System.out.println("6. View employees");
                System.out.println("7. Update employee details/role");
                System.out.println("8. Deactivate employee account");
                System.out.println("9. View active orders");
                System.out.println("10. Update order status");
                System.out.println("0. Logout");
                switch (read("Choose: ")) {
                    case "1": menu.showMenu(true); break;
                    case "2": menu.addItem(); break;
                    case "3": menu.updateItem(); break;
                    case "4": menu.deleteItem(); break;
                    case "5": if (user.getRole().equals("ADMIN")) employees.addEmployee(); else System.out.println("Only ADMIN can create employee accounts in this starter app."); break;
                    case "6": employees.listEmployees(); break;
                    case "7": if (user.getRole().equals("ADMIN")) employees.updateEmployee(); else System.out.println("Only ADMIN can update employee accounts."); break;
                    case "8": if (user.getRole().equals("ADMIN")) employees.deleteEmployee(); else System.out.println("Only ADMIN can deactivate employee accounts."); break;
                    case "9": orders.viewActiveOrders(); break;
                    case "10": orders.updateOrderStatus(); break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            }
            else if (user.getRole().equals("KITCHEN_STAFF")) {
                System.out.println("1. View menu");
                System.out.println("2. Toggle item availability");
                System.out.println("3. View active orders");
                System.out.println("4. Update order status");
                System.out.println("0. Logout");
                switch (read("Choose: ")) {
                    case "1": menu.showMenu(true); break;
                    case "2": menu.toggleAvailability(); break;
                    case "3": orders.viewActiveOrders(); break;
                    case "4": orders.updateOrderStatus(); break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            }
            else if (user.getRole().equals("WAITSTAFF")) {
                System.out.println("1. View menu");
                System.out.println("2. View active orders");
                System.out.println("3. Update order status");
                System.out.println("0. Logout");
                switch (read("Choose: ")) {
                    case "1": menu.showMenu(false); break;
                    case "2": orders.viewActiveOrders(); break;
                    case "3": orders.updateOrderStatus(); break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            }
            else {
                System.out.println("This employee role has no dashboard.");
                return;
            }
        }
    }
    private String read(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
}
