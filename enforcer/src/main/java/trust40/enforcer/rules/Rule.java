package trust40.enforcer.rules;

import java.util.Objects;

public abstract class Rule {
    private final String subject;
    private final String action;
    private final String object;

    public Rule(String subject, String action, String object) {
        Objects.isNull(subject);
        Objects.isNull(action);
        Objects.isNull(object);
        this.subject = subject;
        this.action = action;
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Rule rule = (Rule) o;
        return subject.equals(rule.subject) && action.equals(rule.action) && object.equals(rule.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, action, object);
    }

    public final String getSubject() {
        return subject;
    }

    public final String getAction() {
        return action;
    }

    public final String getObject() {
        return object;
    }

    public final boolean equalRule(Rule f) {
        if (f == null)
            return false;
        if (this.equals(f))
            return true;
        return f.action.equals(this.action) && f.object.equals(this.object) && f.subject.equals(this.subject);
    }

    @Override
    public String toString() {
        return "[" + subject + " " + action + " " + object + "]";
    }

}
