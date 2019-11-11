package trust40.enforcer.sdq.rules;

import trust40.k4case.AllowPermission;

/**
 * DTO for allow rules
 */
public class AllowRule extends Rule {


    public AllowRule(DataObject subject, Operation operation, DataObject object) {
        super(subject, operation, object);
    }

    public AllowPermission getScalaPermission(){
        return new AllowPermission(getSubject().getValue(),getOperation().toString(),getObject().getValue());
    }
}
