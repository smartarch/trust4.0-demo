package trust40.enforcer;

import trust40.enforcer.io.PrivacyLoader;
import trust40.enforcer.rules.AllowRule;
import trust40.enforcer.rules.DenyRule;
import trust40.enforcer.rules.ReasonedAllowRule;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

public class DesignTimeDecisionMakerImpl implements DesignTimeDecisionMaker {
    private static final String PATH_TO_PRIVACY_LEVELS = "";

    @Override
    public Collection<ReasonedAllowRule> validatePolicies(Collection<AllowRule> allowRules,
            Collection<DenyRule> denyRules) {
        PrivacyLoader privacyLoaders = new PrivacyLoader(PATH_TO_PRIVACY_LEVELS);
        try {
            Map<String, PrivacyLevel> privacyMapping = privacyLoaders.getPrivacyMap();
            Collection<DenyRule> applicableDenyRules = denyRules.parallelStream()
                    .filter(e -> privacyMapping.containsKey(e.getObject())).collect(Collectors.toSet());
            Collection<AllowRule> removeableRules = allowRules.parallelStream()
                    .filter(allowRule -> applicableDenyRules.stream().filter(denyRule -> allowRule.equalRule(denyRule))
                            .anyMatch(e -> EnumSet.range(PrivacyLevel.PUBLIC, e.getPrivacyLevel())
                                    .contains(privacyMapping.get(e.getObject())))).collect(Collectors.toList());
            allowRules.removeAll(removeableRules);
            return allowRules.parallelStream()
                    .map(allowRule -> new ReasonedAllowRule(allowRule.getSubject(), allowRule.getAction(),
                            allowRule.getObject(), null)).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
