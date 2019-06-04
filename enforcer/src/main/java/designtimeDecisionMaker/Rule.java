package designtimeDecisionMaker;

import java.util.Objects;

public abstract class Rule {
    private final String subject;
    private final String verb;
    private final String object;

    public Rule(String subject, String verb, String object) {
        Objects.isNull(subject);
        Objects.isNull(verb);
        Objects.isNull(object);
        this.subject = subject;
        this.verb = verb;
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return subject.equals(rule.subject) &&
                verb.equals(rule.verb) &&
                object.equals(rule.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, verb, object);
    }

    public final String getSubject() {
        return subject;
    }

    public final String getVerb() {
        return verb;
    }

    public final String getObject() {
        return object;
    }

}
