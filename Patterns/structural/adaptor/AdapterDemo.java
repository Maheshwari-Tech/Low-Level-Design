package code.structural.adaptor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class AdapterDemo {
    private AdapterDemo() {
    }

    private interface PaymentGateway {
        String pay(BigDecimal amount);
    }

    private static final class LegacyBankApi {
        String chargeInCents(long cents) {
            return "legacy-charge-" + cents;
        }
    }

    private static final class LegacyBankAdapter implements PaymentGateway {
        private final LegacyBankApi adaptee;

        private LegacyBankAdapter(LegacyBankApi adaptee) {
            this.adaptee = Objects.requireNonNull(adaptee, "adaptee");
        }

        @Override
        public String pay(BigDecimal amount) {
            long cents = amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
            return adaptee.chargeInCents(cents);
        }
    }

    public static void main(String[] args) {
        PaymentGateway gateway = new LegacyBankAdapter(new LegacyBankApi());
        System.out.println(gateway.pay(new BigDecimal("19.95")));
    }
}
