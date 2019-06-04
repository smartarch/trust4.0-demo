package designtimeDecisionMaker;

public class ReasonedAllowRule extends AllowRule {
    private final String reason;

    public ReasonedAllowRule(final String subject, final String verb, final String object, final String reason) {
        super(subject, verb, object);
        this.reason = reason;
    }

    public final String getReason() {
        return reason;
    }
}
