package trust40.enforcer.sdq.rules;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class Operation {
    private final String name;
    private final String[] parameters;

    public Operation(String name, String... parameters) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(parameters);
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String[] getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operation operation = (Operation) o;
        return name.equals(operation.name) &&
                Arrays.equals(parameters, operation.parameters);
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
