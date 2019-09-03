package trust40.enforcer.sdq.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import trust40.enforcer.sdq.DesignTimeDecisionMaker;
import trust40.enforcer.sdq.DesignTimeDecisionMakerImpl;
import trust40.enforcer.sdq.PrivacyLevel;
import trust40.enforcer.sdq.rules.AllowRule;
import trust40.enforcer.sdq.rules.DenyRule;
import trust40.enforcer.sdq.rules.ReasonedAllowRule;

class TestPrivacyMerger {
	static DesignTimeDecisionMaker decision;
	static Path t;

	@BeforeAll
	static void init() {
		t = Paths.get("test.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(t, Charset.forName("UTF-8"))) {
			writer.write("Data00;public\n");
			writer.write("Data01;sensitive\n");
			writer.close();
			decision = new DesignTimeDecisionMakerImpl("test.csv");
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO-Error");
		}
	}

	@Test
	void testForemanDenyHighlySensitive() {
		List<AllowRule> listAllow = Stream.of(new AllowRule("Foreman", "read", "Data00"),
				new AllowRule("Test", "read", "Data00"), new AllowRule("Foreman", "read", "Data01"))
				.collect(Collectors.toList());
		List<DenyRule> listDeny = Stream
				.of(new DenyRule("Foreman", "read", "Data00", PrivacyLevel.PUBLIC),
						new DenyRule("Foreman", "read", "Data01", PrivacyLevel.HIGHLY_SENSITIVE))
				.collect(Collectors.toList());
		Collection<ReasonedAllowRule> rulesexpected = Stream.of(
				new ReasonedAllowRule("Test", "read","Data00", null),
				new ReasonedAllowRule("Foreman", "read","Data01", null)
				).collect(Collectors.toList());
		Collection<ReasonedAllowRule> rules = decision.validatePolicies(listAllow, listDeny);
		
		assertThat(rules, is(rulesexpected) );
	}
	@Test
	void testForemanDenySensitive() {
		List<AllowRule> listAllow = Stream.of(
				new AllowRule("Test", "read", "Data00"), new AllowRule("Foreman", "read", "Data01"))
				.collect(Collectors.toList());
		List<DenyRule> listDeny = Stream
				.of(new DenyRule("Foreman", "read", "Data01", PrivacyLevel.SENSITIVE))
				.collect(Collectors.toList());
		Collection<ReasonedAllowRule> rulesexpected = Stream.of(new ReasonedAllowRule("Test", "read","Data00", null)).collect(Collectors.toList());
		Collection<ReasonedAllowRule> rules = decision.validatePolicies(listAllow, listDeny);
		
		assertThat(rules, is(rulesexpected) );
	}
	@Test
	void testFormanDenyInternalUse() {
		List<AllowRule> listAllow = Stream.of(new AllowRule("Test", "read", "Data00"), new AllowRule("Foreman", "read", "Data01"))
				.collect(Collectors.toList());
		List<DenyRule> listDeny = Stream
				.of(new DenyRule("Foreman", "read", "Data01", PrivacyLevel.INTERNAL_USE))
				.collect(Collectors.toList());
		Collection<ReasonedAllowRule> rulesexpected = Stream.of(new ReasonedAllowRule("Test", "read","Data00", null)).collect(Collectors.toList());
		Collection<ReasonedAllowRule> rules = decision.validatePolicies(listAllow, listDeny);
		
		assertThat(rules, is(rulesexpected) );
	}
	@Test
	void testFormanDenyPublic() {
		List<AllowRule> listAllow = Stream.of(new AllowRule("Test", "read", "Data00"), new AllowRule("Foreman", "read", "Data01"))
				.collect(Collectors.toList());
		List<DenyRule> listDeny = Stream
				.of(new DenyRule("Foreman", "read", "Data01", PrivacyLevel.PUBLIC))
				.collect(Collectors.toList());
		Collection<ReasonedAllowRule> rulesexpected = Stream.of(new ReasonedAllowRule("Test", "read","Data00", null)).collect(Collectors.toList());
		Collection<ReasonedAllowRule> rules = decision.validatePolicies(listAllow, listDeny);
		
		assertThat(rules, is(rulesexpected) );
	}
	
	@Test
	void testFormanDenyPublic2() {
		List<AllowRule> listAllow = Stream.of(new AllowRule("Foreman", "read", "Data01"))
				.collect(Collectors.toList());
		List<DenyRule> listDeny = Stream
				.of(new DenyRule("Foreman", "read", "Data01", PrivacyLevel.PUBLIC))
				.collect(Collectors.toList());
		Collection<ReasonedAllowRule> rulesexpected = new ArrayList<ReasonedAllowRule>();
		Collection<ReasonedAllowRule> rules = decision.validatePolicies(listAllow, listDeny);
		
		assertThat(rules, is(rulesexpected) );
	}

	@AfterAll
	static void clean() {
		try {
			Files.delete(t);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO-Error: can't delete privacy File");
		}
	}

}
