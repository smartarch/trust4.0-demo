package trust40.enforcer.sdq;

import trust40.enforcer.sdq.rules.AllowRule;
import trust40.enforcer.sdq.rules.DenyRule;
import trust40.enforcer.sdq.rules.ReasonedAllowRule;
import trust40.k4case.AllowPermission;
import trust40.k4case.DenyPermission;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface DesignTimeDecisionMaker {
    /*
    Sample input:

    allowRules: [
        0: ["A-foreman", "enter", "factory"]
        1: ["A-worker-001", "enter", "factory"]
        2: ["A-worker-002", "enter", "factory"]
        3: ["A-worker-003", "enter", "factory"]
        4: ["A-worker-001", "use", "dispenser"]
        5: ["A-worker-002", "use", "dispenser"]
        6: ["A-worker-003", "use", "dispenser"]
        7: ["C-foreman", "enter", "factory"]
        8: ["C-worker-001", "enter", "factory"]
        9: ["C-worker-002", "enter", "factory"]
        10: ["C-worker-003", "enter", "factory"]
        11: ["C-worker-001", "use", "dispenser"]
        12: ["C-worker-002", "use", "dispenser"]
        13: ["C-worker-003", "use", "dispenser"]
        14: ["B-foreman", "enter", "factory"]
        15: ["B-worker-001", "enter", "factory"]
        16: ["B-worker-002", "enter", "factory"]
        17: ["B-worker-003", "enter", "factory"]
        18: ["B-worker-001", "use", "dispenser"]
        19: ["B-worker-002", "use", "dispenser"]
        20: ["B-worker-003", "use", "dispenser"]

        21: ["C-foreman", "read", "C-worker-001.personalData.phoneNo"]
        22: ["C-foreman", "read", "C-worker-001.distanceToWorkPlace"]
    ]

    denyRules: [
        0: ["C-foreman", "read", "A-worker-001.personalData", ANY]
        1: ["C-foreman", "read", "C-worker-001.personalData", SENSITIVE]
    ]
 */

    /**
     * This methods calculates the current validate allow rules. Therefore it takes the a Collection of the current {@link AllowRule}s and a Collection of the current {@link DenyRule}s
     * @param allowRules Collectionf of {@link AllowRule}
     * @param denyRules Collection of {@link DenyRule}
     * @return Collection of {@link ReasonedAllowRule} or null in case of an internal error
     */
    List<AllowPermission> validatePolicies(List<AllowPermission> allowRules, List<DenyPermission> denyRules, Map<AllowPermission,List<DenyPermission>> deniedRules) throws NullPointerException;

    /**
     * Reloads the privacyLevel File
     * @throws IOException
     */
    void reload() throws IOException;
    /**
     * Sets a new PrivacyLevel file. It does not automatically reloads it
     * @param path Path to the PrivacyLevel file
     */
    void setPrivacyLevelFile(String path);
}
