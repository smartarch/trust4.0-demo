package trust40.enforcer.sdq.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import trust40.enforcer.sdq.PrivacyLevel;
import trust40.enforcer.sdq.rules.AllowRule;
import trust40.enforcer.sdq.rules.DenyRule;
import trust40.enforcer.sdq.rules.ReasonedAllowRule;

class TestRule {

	@Test
	@DisplayName("Tests if Exceptions are thrown with null")
	void testEmptyConstructorRule() {
		assertAll("null check",
				() -> assertThrows(IllegalArgumentException.class, () -> new AllowRule(null, null, null)),
				() -> assertThrows(IllegalArgumentException.class, () -> new AllowRule(null, "", "")),
				() -> assertThrows(IllegalArgumentException.class, () -> new AllowRule("", null, "")),
				() -> assertThrows(IllegalArgumentException.class, () -> new AllowRule("", "", null)),

				() -> assertThrows(IllegalArgumentException.class, () -> new DenyRule(null, null, null, null)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new DenyRule(null, "", "", PrivacyLevel.PUBLIC)),
				() -> assertThrows(IllegalArgumentException.class,
						() -> new DenyRule("", null, "", PrivacyLevel.PUBLIC)),
				() -> assertThrows(IllegalArgumentException.class, () -> new DenyRule("", "", "", null)),

				() -> assertThrows(IllegalArgumentException.class, () -> new ReasonedAllowRule(null, null, null, null)),
				() -> assertThrows(IllegalArgumentException.class, () -> new ReasonedAllowRule(null, "", "", "")),
				() -> assertThrows(IllegalArgumentException.class, () -> new ReasonedAllowRule("", null, "", "")),
				() -> assertThrows(IllegalArgumentException.class, () -> new ReasonedAllowRule("", "", null, ""))

		);
	}

	@Nested
	@DisplayName("Tests Different Equality")
	class testEqualsRule {
		AllowRule allowRule;
		DenyRule denyRule;

		@BeforeEach
		void initial() {
			allowRule = new AllowRule("test123", "test234", "test345");
			denyRule = new DenyRule("test123", "test234", "test345", PrivacyLevel.HIGHLY_SENSITIVE);
		}

		@Test
		void testEqualsWithDenyAndAllowRule() {
			assertFalse(allowRule.equals(denyRule));
		}

		@Test
		void testEqualsAllowRule() {
			assertTrue(allowRule
					.equals(new AllowRule(allowRule.getSubject(), allowRule.getAction(), allowRule.getObject())));
		}

		@Test
		void testEqualsDenyRule() {
			assertTrue(denyRule.equals(new DenyRule(denyRule.getSubject(), denyRule.getAction(), denyRule.getObject(),
					denyRule.getPrivacyLevel())));
		}

		@Test
		void testSameRule() {
			assertTrue(allowRule.equalRule(denyRule));
		}
	}

}
