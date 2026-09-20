package code.behavioral.template_method;

public final class PaymentDemo {
    private PaymentDemo() {
    }

    private static final class CardPayment extends PaymentFlow {
        private CardPayment(int amountInCents) {
            super(amountInCents);
        }

        @Override
        protected void debit() {
            System.out.println("authorized card for " + amountInCents() + " cents");
        }

        @Override
        public void sendMoney() {
            System.out.println("captured card payment");
        }
    }

    private static final class WalletPayment extends PaymentFlow {
        private WalletPayment(int amountInCents) {
            super(amountInCents);
        }

        @Override
        protected void debit() {
            System.out.println("debited wallet by " + amountInCents() + " cents");
        }

        @Override
        public void sendMoney() {
            System.out.println("transferred wallet funds");
        }
    }

    public static void main(String[] args) {
        new CardPayment(2_500).pay();
        new WalletPayment(1_200).pay();
    }
}
