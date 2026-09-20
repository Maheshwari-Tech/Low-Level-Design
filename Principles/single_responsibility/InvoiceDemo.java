import java.util.Objects;

public final class InvoiceDemo {
    private InvoiceDemo() {
    }

    private record Marker(String name, int unitPriceInCents) {
        private Marker {
            Objects.requireNonNull(name, "name");
            if (unitPriceInCents < 0) {
                throw new IllegalArgumentException("price cannot be negative");
            }
        }
    }

    private record Invoice(String id, Marker marker, int quantity) {
        private Invoice {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(marker, "marker");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
        }
    }

    private static final class InvoiceCalculator {
        int totalInCents(Invoice invoice) {
            return Math.multiplyExact(invoice.marker().unitPriceInCents(), invoice.quantity());
        }
    }

    private static final class InvoicePrinter {
        String format(Invoice invoice, int totalInCents) {
            return "%s: %d x %s = %d cents".formatted(
                    invoice.id(), invoice.quantity(), invoice.marker().name(), totalInCents);
        }
    }

    private static final class InvoiceRepository {
        void save(Invoice invoice) {
            System.out.println("saved invoice " + invoice.id());
        }
    }

    public static void main(String[] args) {
        Invoice invoice = new Invoice("INV-42", new Marker("Blue marker", 125), 4);
        int total = new InvoiceCalculator().totalInCents(invoice);
        System.out.println(new InvoicePrinter().format(invoice, total));
        new InvoiceRepository().save(invoice);
    }
}
