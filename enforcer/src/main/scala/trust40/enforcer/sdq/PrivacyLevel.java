package trust40.enforcer.sdq;

/**
 * Enum for handling the privacy level
 */
public enum PrivacyLevel {
    /* The order of appearing describes the order of the enumeration */
    PUBLIC("public"), INTERNAL_USE("internal use"), SENSITIVE("sensitive"), HIGHLY_SENSITIVE("highly sensitive");
    private final String name;

    PrivacyLevel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
