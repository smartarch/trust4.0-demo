package trust40.enforcer;

public interface PrivacyChecker {
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
    PrivacyCheckResult validateRules(AllowRule[] allowRules, DenyRule[] denyRules);
}
