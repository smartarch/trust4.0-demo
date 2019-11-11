package trust40.enforcer.sdq.rules;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class Operation {
    private final String name;
    private final String[] parameters;
    private boolean allQuantified;

    public Operation(String name, String... parameters) {
        this.name = Objects.requireNonNull(name);
        this.parameters = parameters;
        allQuantified = Arrays.stream(parameters).anyMatch(e -> e.equals("*"));
    }

    public String getName() {
        return name;
    }

    public String[] getParameters() {
        return parameters;
    }

    public boolean isAllQuantified(){
        return allQuantified;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operation operation = (Operation) o;
        return name.equals(operation.name) &&
                Arrays.equals(parameters, operation.parameters);
    }
    public boolean equalOperation(Operation o){
        if (o.equals(this))
            return  true;
        return o.name.equals(this.name) && (this.allQuantified || o.allQuantified);
    }
    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }

    @Override
    public String toString() {
        return name  + Arrays.stream(parameters).collect(Collectors.joining(",","(",")"));
    }
}
