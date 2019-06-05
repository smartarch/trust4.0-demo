package trust40.enforcer;

public enum PrivacyLevel {
    ANY("any"),
    SENSITIVE("sensitive");

    private final String name;
    PrivacyLevel(String name) {
        this.name = name;
    }
}
