package trust40.enforcer;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import trust40.enforcer.io.PrivacyLoader;
import trust40.enforcer.rules.AllowRule;
import trust40.enforcer.rules.DenyRule;
import trust40.enforcer.rules.ReasonedAllowRule;

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
		PrivacyLoader privacyLoader = new PrivacyLoader(path);
		privacyMapping = privacyLoader.getPrivacyMap();
		pathToPrivacyLevels = path;
	}

	public DesignTimeDecisionMakerImpl() throws IOException {
		this(DEFAULT_FOLDER);
	}

	@Override
	public Collection<ReasonedAllowRule> validatePolicies(Collection<AllowRule> allowRules,
			Collection<DenyRule> denyRules) {
		lock.readLock().lock();
		try {
			Collection<DenyRule> applicableDenyRules = denyRules.parallelStream()
					.filter(e -> privacyMapping.containsKey(e.getObject())).collect(Collectors.toSet());
			Collection<AllowRule> removeableRules = allowRules.parallelStream()
					.filter(allowRule -> applicableDenyRules.stream().filter(denyRule -> allowRule.equalRule(denyRule))
							.anyMatch(e -> EnumSet.range(PrivacyLevel.PUBLIC, e.getPrivacyLevel())
									.contains(privacyMapping.get(e.getObject()))))
					.collect(Collectors.toList());
			allowRules.removeAll(removeableRules);
			return allowRules.parallelStream().map(allowRule -> new ReasonedAllowRule(allowRule.getSubject(),
					allowRule.getAction(), allowRule.getObject(), null)).collect(Collectors.toList());
		} finally {
			lock.readLock().unlock();
		}

	}

	@Override
	public void reload() throws IOException {
		lock.writeLock().lock();
		try {
			PrivacyLoader privacyLoader = new PrivacyLoader(pathToPrivacyLevels);
			privacyMapping = privacyLoader.getPrivacyMap();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void setPrivacyLevelFile(String path) {
		pathToPrivacyLevels = path;
	}
}
