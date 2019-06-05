package designtimeDecisionMaker;

import java.util.Objects;

public class DenyRule extends Rule {
    private String privacyLevel;
    public DenyRule(String subject, String verb, String object, String privacyLevel) {
        super(subject, verb, object);
        Objects.isNull(privacyLevel);
        this.privacyLevel = privacyLevel;
    }
    public String getPrivacyLevel(){
        return privacyLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DenyRule denyRule = (DenyRule) o;
        return privacyLevel.equals(denyRule.privacyLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), privacyLevel);
    }
}
