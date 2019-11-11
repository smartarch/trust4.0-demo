package trust40.enforcer.sdq.rules;

import java.util.Objects;

/**
 * A dataobject consist of a type and a value. Dataobjects are semantical identical when the type is the same
 */
public class DataObject {
    private String type;
    private String value;

    public DataObject(String type, String value) {
        this.type = Objects.requireNonNull(type);
        this.value = value;
    }
    public DataObject(String type){
        this(type, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataObject that = (DataObject) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString(){
        return "[" + value + ":" + type + "]";
    }
}
