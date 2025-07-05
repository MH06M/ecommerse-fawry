import java.util.*;

interface Shippable {
    String getName();
    double getWeight();
}

abstract class Product {
    String name;
    double price;
    int quantity;

    Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    abstract boolean isExpired();

    boolean isAvailable(int requestedQty) {
        return requestedQty <= quantity && !isExpired();
    }

    void reduceQuantity(int qty) {
        quantity -= qty;
    }
}

class PerishableProduct extends Product implements Shippable {
    double weight;
    Date expiryDate;

    PerishableProduct(String name, double price, int quantity, double weight, Date expiryDate) {
        super(name, price, quantity);
        this.weight = weight;
        this.expiryDate = expiryDate;
    }

    @Override
    boolean isExpired() {
        return new Date().after(expiryDate);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getWeight() {
        return weight;
    }
}

class NonPerishableProduct extends Product {
    boolean requiresShipping;
    double weight;

    NonPerishableProduct(String name, double price, int quantity, boolean requiresShipping, double weight) {
        super(name, price, quantity);
        this.requiresShipping = requiresShipping;
        this.weight = weight;
    }

    @Override
    boolean isExpired() {
        return false;
    }

    boolean isShippable() {
        return requiresShipping;
    }

    Shippable asShippable() {
        return new Shippable() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public double getWeight() {
                return weight;
            }
        };
    }
}

class CartItem {
    Product product;
    int quantity;

    CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
}

class Cart {
    List<CartItem> items = new ArrayList<>();

    void add(Product product, int quantity) {
        if (!product.isAvailable(quantity)) {
            throw new IllegalArgumentException("Product unavailable or expired.");
        }
        items.add(new CartItem(product, quantity));
    }

    boolean isEmpty() {
        return items.isEmpty();
    }
}

class Customer {
    double balance;

    Customer(double balance) {
        this.balance = balance;
    }

    void pay(double amount) {
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
        balance -= amount;
    }
}

class ShippingService {
    void ship(List<Shippable> items) {
        System.out.println("** Shipment notice **");
        double totalWeight = 0;
        for (Shippable s : items) {
            System.out.printf("%s %s g\n", s.getName(), s.getWeight() * 1000);
            totalWeight += s.getWeight();
        }
        System.out.printf("Total package weight %.1f kg\n", totalWeight);
    }
}

public class ECommerceSystem {
    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }

        double subtotal = 0;
        double shipping = 0;
        List<Shippable> toShip = new ArrayList<>();

        for (CartItem item : cart.items) {
            if (!item.product.isAvailable(item.quantity)) {
                throw new IllegalArgumentException("One or more products unavailable or expired.");
            }
            subtotal += item.product.price * item.quantity;

            if (item.product instanceof PerishableProduct) {
                toShip.add((PerishableProduct) item.product);
            } else if (item.product instanceof NonPerishableProduct) {
                NonPerishableProduct np = (NonPerishableProduct) item.product;
                if (np.isShippable()) {
                    toShip.add(np.asShippable());
                }
            }
            item.product.reduceQuantity(item.quantity);
        }

        shipping = toShip.isEmpty() ? 0 : 30; // Flat shipping fee
        double total = subtotal + shipping;

        if (customer.balance < total) {
            throw new IllegalArgumentException("Insufficient balance.");
        }

        if (!toShip.isEmpty()) {
            new ShippingService().ship(toShip);
        }

        customer.pay(total);

        System.out.println("\n** Checkout receipt **");
        for (CartItem item : cart.items) {
            System.out.printf("%dx %s %s\n", item.quantity, item.product.name, item.product.price * item.quantity);
        }
        System.out.printf("Subtotal %s\n", subtotal);
        System.out.printf("Shipping %s\n", shipping);
        System.out.printf("Amount %s\n", total);
        System.out.printf("Customer balance after payment: %.2f\n", customer.balance);
    }

    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1); // expires tomorrow

        PerishableProduct cheese = new PerishableProduct("Cheese", 100, 5, 0.2, cal.getTime());
        PerishableProduct biscuits = new PerishableProduct("Biscuits", 150, 3, 0.7, cal.getTime());
        NonPerishableProduct tv = new NonPerishableProduct("TV", 200, 3, true, 10);
        NonPerishableProduct scratchCard = new NonPerishableProduct("ScratchCard", 50, 10, false, 0);

        Customer customer = new Customer(1000);
        Cart cart = new Cart();

        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(tv, 3);
        cart.add(scratchCard, 1);

        checkout(customer, cart);
    }
}
