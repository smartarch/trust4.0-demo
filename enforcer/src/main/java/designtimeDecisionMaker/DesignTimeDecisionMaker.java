package designtimeDecisionMaker;

import java.util.Collection;

public interface DesignTimeDecisionMaker {

    Collection<ReasonedAllowRule> validatePolicies(Collection<AllowRule> allowRules, Collection<DenyRule> denyRules);
}
