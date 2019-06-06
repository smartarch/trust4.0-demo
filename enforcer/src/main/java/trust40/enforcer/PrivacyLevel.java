package trust40.enforcer;

/**
 * Enum for handling the privacy level
 */
public enum PrivacyLevel {
    /* The order of appearing describes the order of the enumeration */
    PUBLIC("public"), SENSITIVE("sensitive"), OFFICIAL("official"), PRIVATE("private");
    private final String name;

    PrivacyLevel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
