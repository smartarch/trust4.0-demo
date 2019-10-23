package trust40.enforcer.sdq;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import scala.Enumeration;
import sun.rmi.runtime.Log;
import trust40.enforcer.sdq.io.PrivacyLoader;
import trust40.enforcer.sdq.rules.AllowRule;
import trust40.enforcer.sdq.rules.DenyRule;
import trust40.enforcer.sdq.rules.Operation;
import trust40.enforcer.sdq.rules.ReasonedAllowRule;
import trust40.k4case.AllowPermission;
import trust40.k4case.DenyPermission;

public class DesignTimeDecisionMakerImpl implements DesignTimeDecisionMaker {
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private static final String DEFAULT_FOLDER = "";
	private String pathToPrivacyLevels = "";
	private Map<String, PrivacyLevel> privacyMapping;


	/**
	 * 
	 * @param path contains the path to the privacy level file
	 * @throws IOException
	 */
	public DesignTimeDecisionMakerImpl(String path) throws IOException {
		/*
		PrivacyLoader privacyLoader = new PrivacyLoader(path);
		privacyMapping = privacyLoader.getPrivacyMap();
		pathToPrivacyLevels = path;

		 */
	}

	public DesignTimeDecisionMakerImpl() throws IOException {
		this(DEFAULT_FOLDER);
	}

	@Override
	public List<AllowPermission> validatePolicies(List<AllowPermission> allowRules,
												  List<DenyPermission> denyRules) {
		List<AllowRule> rules = allowRules.stream().map(e-> {
			return new AllowRule(e.subj(),parseOperation(e.verb()),e.obj());
		}).collect(Collectors.toList());
		Enumeration.Value test = denyRules.get(0).lvl();
		List<DenyRule> denyRulesOwn = denyRules.stream().map(e-> {
			return new DenyRule(e.subj(),parseOperation(e.ver()),e.obj(),convertPrivacyLevel(e.lvl()));
		}).collect(Collectors.toList());


		/*
		lock.readLock().lock();
		try {
			Collection<DenyRule> applicableDenyRules = denyRules.parallelStream()
					.filter(e -> privacyMapping.containsKey(e.getObject())).collect(Collectors.toSet());
			Collection<AllowRule> removeableRules = allowRules.parallelStream()
					.filter(allowRule -> applicableDenyRules.stream().filter(denyRule -> allowRule.equalRule(denyRule))
							.anyMatch(e -> EnumSet.range(e.getPrivacyLevel(),PrivacyLevel.HIGHLY_SENSITIVE)
									.contains(privacyMapping.get(e.getObject()))))
					.collect(Collectors.toList());
			allowRules.removeAll(removeableRules);
			return allowRules.parallelStream().map(allowRule -> new ReasonedAllowRule(allowRule.getSubject(),
					allowRule.getAction(), allowRule.getObject(), null)).collect(Collectors.toList());
		} finally {
			lock.readLock().unlock();
		}
		 */
		System.out.println("################################################");
		allowRules.forEach(e->
				 System.out.println(e.subj() + " " + e.verb() + " " + e.obj()));
		System.out.println("############### Deny ###################");
		denyRules.forEach(e->
				System.out.println(e.subj() + " " + e.ver() + " "+ e.obj() + " " + e.lvl())
		);
		System.out.println("################################################");
		return allowRules;

	}
	private PrivacyLevel convertPrivacyLevel(Enumeration.Value privacyLevel){
		return PrivacyLevel.valueOf(privacyLevel.toString());
	}
	private Operation parseOperation(String verb){
		String operation = verb.substring(0,verb.indexOf('('));
		String[] parameters = verb.substring(verb.indexOf('('+1,')')-1).split(",");
		return new Operation(operation,parameters);
	}

	@Override
	public void reload() throws IOException {
		/*
		lock.writeLock().lock();
		try {
			PrivacyLoader privacyLoader = new PrivacyLoader(pathToPrivacyLevels);
			privacyMapping = privacyLoader.getPrivacyMap();
		} finally {
			lock.writeLock().unlock();
		}

		 */
	}

	@Override
	public void setPrivacyLevelFile(String path) {
		pathToPrivacyLevels = path;
	}
}
