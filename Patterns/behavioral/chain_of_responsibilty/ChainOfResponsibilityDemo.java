package code.behavioral.chain_of_responsibilty;

import java.util.List;
import java.util.Objects;

public final class ChainOfResponsibilityDemo {
    private ChainOfResponsibilityDemo() {
    }

    public record PurchaseRequest(boolean authenticated, String role, int amount) {
        public PurchaseRequest {
            Objects.requireNonNull(role, "role");
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }
    }

    private abstract static class Handler {
        private Handler next;

        final Handler linkWith(Handler nextHandler) {
            next = Objects.requireNonNull(nextHandler, "nextHandler");
            return nextHandler;
        }

        final boolean handle(PurchaseRequest request) {
            if (!check(request)) {
                return false;
            }
            return next == null || next.handle(request);
        }

        protected abstract boolean check(PurchaseRequest request);
    }

    private static final class AuthenticationHandler extends Handler {
        @Override
        protected boolean check(PurchaseRequest request) {
            return request.authenticated();
        }
    }

    private static final class RoleHandler extends Handler {
        @Override
        protected boolean check(PurchaseRequest request) {
            return "BUYER".equals(request.role());
        }
    }

    private static final class SpendingLimitHandler extends Handler {
        private final int limit;

        private SpendingLimitHandler(int limit) {
            this.limit = limit;
        }

        @Override
        protected boolean check(PurchaseRequest request) {
            return request.amount() <= limit;
        }
    }

    public static void main(String[] args) {
        Handler chain = new AuthenticationHandler();
        chain.linkWith(new RoleHandler()).linkWith(new SpendingLimitHandler(1_000));

        List<PurchaseRequest> requests = List.of(
                new PurchaseRequest(true, "BUYER", 750),
                new PurchaseRequest(false, "BUYER", 100),
                new PurchaseRequest(true, "BUYER", 1_500));

        requests.forEach(request ->
                System.out.printf("%s -> %s%n", request, chain.handle(request) ? "accepted" : "rejected"));
    }
}
