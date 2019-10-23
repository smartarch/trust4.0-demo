package trust40.enforcer.sdq.rules;

import java.util.Objects;

public abstract class Rule {
    private final String subject;
    private final Operation operation;
    private final String object;

    public Rule(String subject, Operation operation, String object) {
    	if(subject == null)
    		throw new IllegalArgumentException("subject is null");
    	if(operation == null)
    		throw new IllegalArgumentException("subject is null");
    	if(object == null)
    		throw new IllegalArgumentException("subject is null");
        this.subject = subject;
        this.operation = operation;
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Rule rule = (Rule) o;
        return subject.equals(rule.subject) && operation.equals(rule.operation) && object.equals(rule.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, operation, object);
    }

    public final String getSubject() {
        return subject;
    }

    public final Operation getOperation() {
        return operation;
    }

    public final String getObject() {
        return object;
    }

    public final boolean equalRule(Rule f) {
        if (f == null)
            return false;
        if (this.equals(f))
            return true;
        return f.operation.equals(this.operation) && f.object.equals(this.object) && f.subject.equals(this.subject);
    }

    @Override
    public String toString() {
        return "[" + subject + " " + operation + " " + object + "]";
    }

}
