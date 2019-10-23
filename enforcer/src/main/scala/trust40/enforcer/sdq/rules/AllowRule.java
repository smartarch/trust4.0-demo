package trust40.enforcer.sdq.rules;

/**
 * DTO for allow rules
 */
public class AllowRule extends Rule {


    public AllowRule(String subject, Operation operation, String object) {
        super(subject, operation, object);
    }
}
