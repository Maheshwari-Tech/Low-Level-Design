package com.example.lld.order_processing_system.demo;

import com.example.lld.order_processing_system.exception.ConfirmationException;
import com.example.lld.order_processing_system.model.Address;
import com.example.lld.order_processing_system.model.Money;
import com.example.lld.order_processing_system.model.Order;
import com.example.lld.order_processing_system.model.OrderLineRequest;
import com.example.lld.order_processing_system.model.OrderStatus;
import com.example.lld.order_processing_system.port.FulfilmentPort;
import com.example.lld.order_processing_system.port.IdGenerator;
import com.example.lld.order_processing_system.port.InventoryPort;
import com.example.lld.order_processing_system.port.PaymentPort;
import com.example.lld.order_processing_system.port.PromotionPort;
import com.example.lld.order_processing_system.service.OrderService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Executable scenarios; throws AssertionError if an invariant is broken. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T10:15:30Z"), ZoneOffset.UTC);
        AtomicInteger sequence = new AtomicInteger();
        IdGenerator ids = prefix -> prefix + '-' + sequence.incrementAndGet();
        FakeInventory inventory = new FakeInventory();
        FakePayments payments = new FakePayments();
        FakeFulfilment fulfilment = new FakeFulfilment();
        PromotionPort promotions = (customer, sku, quantity, price) ->
                "BOOK".equals(sku) ? Money.of("1.00", "USD") : Money.of("0.00", "USD");
        OrderService service = new OrderService(
                clock, ids, inventory, payments, promotions, fulfilment);

        Address address = new Address(
                "42 Domain Drive", "Bengaluru", "Karnataka", "560001", "IN");

        Order created = service.createOrder(
                "create-100", "customer-7", address,
                List.of(
                        new OrderLineRequest("BOOK", "Designing Objects", 2,
                                Money.of("25.00", "USD")),
                        new OrderLineRequest("MUG", "LLD Mug", 1,
                                Money.of("10.00", "USD"))));
        check(created.payableTotal().equals(Money.of("58.00", "USD")),
                "exact subtotal/discount calculation");
        Order sameCreation = service.createOrder(
                "create-100", "customer-7", address,
                List.of(
                        new OrderLineRequest("BOOK", "Designing Objects", 2,
                                Money.of("25.00", "USD")),
                        new OrderLineRequest("MUG", "LLD Mug", 1,
                                Money.of("10.00", "USD"))));
        check(created.orderId().equals(sameCreation.orderId()), "idempotent create");

        Order confirmed = service.confirmOrder("confirm-100", created.orderId());
        Order duplicateConfirmation = service.confirmOrder("confirm-100", created.orderId());
        check(confirmed.status() == OrderStatus.CONFIRMED, "successful confirmation");
        check(duplicateConfirmation == confirmed, "duplicate returns original immutable result");
        check(payments.chargeCount() == 1 && inventory.reserveCount() == 1,
                "duplicate confirm does not charge or reserve twice");

        String firstLine = confirmed.lines().get(0).lineId();
        String secondLine = confirmed.lines().get(1).lineId();
        Order partiallyFulfilled = service.recordFulfilment(
                "ship-100-a", confirmed.orderId(), firstLine, 1);
        Order continued = service.cancelLine(
                "cancel-100-b", confirmed.orderId(), secondLine, 1, "Customer changed mind");
        check(partiallyFulfilled.status() == OrderStatus.PARTIALLY_FULFILLED,
                "partial fulfilment is visible");
        check(continued.lines().get(1).cancelledQuantity() == 1,
                "one line can be cancelled while another continues");

        Order paymentFailure = service.createOrder(
                "create-200", "customer-8", address,
                List.of(new OrderLineRequest(
                        "PEN", "Pen", 1, Money.of("2.00", "USD"))));
        payments.failNextCharge();
        try {
            service.confirmOrder("confirm-200", paymentFailure.orderId());
            throw new AssertionError("payment failure should reject confirmation");
        } catch (ConfirmationException expected) {
            check(service.getOrder(paymentFailure.orderId()).status() == OrderStatus.PENDING,
                    "failed confirmation remains explainably pending");
            check(inventory.releaseCount() == 1,
                    "inventory is released after a payment failure");
        }
        Order retriedConfirmation = service.confirmOrder(
                "confirm-201", paymentFailure.orderId());
        Order fullyCancelled = service.cancelOrder(
                "cancel-200", retriedConfirmation.orderId(), "Customer cancelled order");
        check(fullyCancelled.status() == OrderStatus.CANCELLED,
                "a fresh command can recover and fully cancel after a failed attempt");

        Order lastUnit = service.createOrder(
                "create-300", "customer-9", address,
                List.of(new OrderLineRequest(
                        "LAST", "Last unit", 1, Money.of("3.00", "USD"))));
        Order lastUnitConfirmed = service.confirmOrder("confirm-300", lastUnit.orderId());
        String lastLine = lastUnitConfirmed.lines().get(0).lineId();
        int successfulConcurrentCommands = raceForLastOpenUnit(service, lastUnit.orderId(), lastLine);
        check(successfulConcurrentCommands == 1,
                "only one concurrent command can consume the final open quantity");
        check(service.getOrder(lastUnit.orderId()).status() == OrderStatus.FULFILLED,
                "the concurrency winner fulfils the order exactly once");

        System.out.println("Order Management System demo passed");
        System.out.println("  confirmed: " + confirmed);
        System.out.println("  failure history: "
                + service.getOrder(paymentFailure.orderId()).history().get(1).reason());
        System.out.println("  concurrent successes: " + successfulConcurrentCommands + "/2");
    }

    private static int raceForLastOpenUnit(
            OrderService service, String orderId, String lineId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Order> first = pool.submit(
                    () -> service.recordFulfilment("race-a", orderId, lineId, 1));
            Future<Order> second = pool.submit(
                    () -> service.recordFulfilment("race-b", orderId, lineId, 1));
            int successes = 0;
            for (Future<Order> result : List.of(first, second)) {
                try {
                    result.get();
                    successes++;
                } catch (ExecutionException expectedLoser) {
                    if (!(expectedLoser.getCause() instanceof RuntimeException)) {
                        throw expectedLoser;
                    }
                }
            }
            return successes;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** In-memory adapter standing in for an inventory integration. */
    private static final class FakeInventory implements InventoryPort {
        private final Map<String, InventoryHold> holdsByKey = new ConcurrentHashMap<>();
        private final Set<String> releases = ConcurrentHashMap.newKeySet();
        private final Set<String> cancellations = ConcurrentHashMap.newKeySet();
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public InventoryHold reserve(
                String orderId, Map<String, Integer> quantities, String idempotencyKey) {
            if (quantities.containsKey("OUT")) {
                throw new IllegalStateException("OUT is unavailable");
            }
            return holdsByKey.computeIfAbsent(
                    idempotencyKey, ignored -> new InventoryHold("hold-" + sequence.incrementAndGet()));
        }

        @Override
        public void release(String reservationId, String idempotencyKey) {
            releases.add(idempotencyKey);
        }

        @Override
        public void cancelCommitted(
                String reservationId,
                Map<String, Integer> quantities,
                String idempotencyKey) {
            cancellations.add(idempotencyKey);
        }

        int reserveCount() {
            return holdsByKey.size();
        }

        int releaseCount() {
            return releases.size();
        }
    }

    /** In-memory adapter standing in for a payment provider. */
    private static final class FakePayments implements PaymentPort {
        private final Map<String, PaymentReceipt> charges = new ConcurrentHashMap<>();
        private final Set<String> refunds = ConcurrentHashMap.newKeySet();
        private final AtomicInteger sequence = new AtomicInteger();
        private final AtomicBoolean failNext = new AtomicBoolean();

        @Override
        public PaymentReceipt charge(String orderId, Money amount, String idempotencyKey) {
            if (failNext.compareAndSet(true, false)) {
                throw new IllegalStateException("card declined");
            }
            return charges.computeIfAbsent(
                    idempotencyKey,
                    ignored -> new PaymentReceipt("payment-" + sequence.incrementAndGet()));
        }

        @Override
        public void refund(String paymentId, Money amount, String idempotencyKey) {
            refunds.add(idempotencyKey);
        }

        void failNextCharge() {
            failNext.set(true);
        }

        int chargeCount() {
            return charges.size();
        }
    }

    /** In-memory adapter standing in for a warehouse/fulfilment integration. */
    private static final class FakeFulfilment implements FulfilmentPort {
        private final Set<String> requests = ConcurrentHashMap.newKeySet();

        @Override
        public void request(
                String orderId,
                Address address,
                List<FulfilmentLine> lines,
                String idempotencyKey) {
            requests.add(idempotencyKey);
        }
    }
}
