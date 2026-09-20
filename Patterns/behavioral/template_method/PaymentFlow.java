package code.behavioral.template_method;

public abstract class PaymentFlow {
    private final int amountInCents;

    protected PaymentFlow(int amountInCents) {
        if (amountInCents <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.amountInCents = amountInCents;
    }

    public final void pay() {
        validate();
        debit();
        sendMoney();
        notifyCompletion();
    }

    protected void validate() {
        System.out.println("validated " + amountInCents + " cents");
    }

    protected abstract void debit();

    public abstract void sendMoney();

    protected final int amountInCents() {
        return amountInCents;
    }

    protected void notifyCompletion() {
        System.out.println("payment complete");
    }
}
