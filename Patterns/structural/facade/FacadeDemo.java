package code.structural.facade;

import java.util.Objects;

public final class FacadeDemo {
    private FacadeDemo() {
    }

    private record OrderRequest(String sku, int quantity, int amountInCents) {
        private OrderRequest {
            Objects.requireNonNull(sku, "sku");
            if (quantity <= 0 || amountInCents <= 0) {
                throw new IllegalArgumentException("quantity and amount must be positive");
            }
        }
    }

    private record Receipt(String paymentId, String shipmentId) {
    }

    private static final class InventoryService {
        void reserve(String sku, int quantity) {
            System.out.printf("reserved %d of %s%n", quantity, sku);
        }
    }

    private static final class PaymentService {
        String charge(int amountInCents) {
            return "payment-" + amountInCents;
        }
    }

    private static final class ShippingService {
        String createShipment(String sku, int quantity) {
            return "shipment-" + sku + "-" + quantity;
        }
    }

    private static final class CheckoutFacade {
        private final InventoryService inventory = new InventoryService();
        private final PaymentService payments = new PaymentService();
        private final ShippingService shipping = new ShippingService();

        Receipt placeOrder(OrderRequest request) {
            inventory.reserve(request.sku(), request.quantity());
            String paymentId = payments.charge(request.amountInCents());
            String shipmentId = shipping.createShipment(request.sku(), request.quantity());
            return new Receipt(paymentId, shipmentId);
        }
    }

    public static void main(String[] args) {
        Receipt receipt = new CheckoutFacade().placeOrder(new OrderRequest("BOOK-17", 2, 3_998));
        System.out.println(receipt);
    }
}
