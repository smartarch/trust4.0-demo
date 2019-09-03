package trust40.enforcer.sdq.rules;

import java.util.Objects;

public class ReasonedAllowRule extends AllowRule {
    private final String reason;

    public ReasonedAllowRule(final String subject, final String rename, final String object, final String reason) {
        super(subject, rename, object);
        this.reason = reason;
    }

    public final String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        ReasonedAllowRule that = (ReasonedAllowRule) o;
        return Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), reason);
    }

}
