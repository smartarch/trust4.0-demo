package trust40.enforcer.sdq;

import scala.Enumeration;
import trust40.enforcer.sdq.io.DataTypeMapping;
import trust40.enforcer.sdq.io.PrivacyLoader;
import trust40.enforcer.sdq.rules.*;
import trust40.k4case.AllowPermission;
import trust40.k4case.DenyPermission;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class DesignTimeDecisionMakerImpl implements DesignTimeDecisionMaker {
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final String DEFAULT_FOLDER = "/home/majuwa/test.csv";
    private String pathToPrivacyLevels = "";
    private PrivacyTable privacyTable;
    private Map<String, String> datatypeMapping;


    /**
     * @param path contains the path to the privacy level file
     * @throws IOException
     */
    public DesignTimeDecisionMakerImpl(String path) throws IOException {

        PrivacyLoader privacyLoader = new PrivacyLoader(path);
        privacyTable = privacyLoader.getPrivacyTable();
        datatypeMapping = new DataTypeMapping(path).getTypeMapping();
        pathToPrivacyLevels = path;

    }

    public DesignTimeDecisionMakerImpl() throws IOException {
        this(DEFAULT_FOLDER);
    }

    @Override
    public List<AllowPermission> validatePolicies(List<AllowPermission> allowRules,
                                                  List<DenyPermission> denyRules, Map<AllowPermission,List<DenyPermission>> deniedRules) {

        List<AllowRule> rules = allowRules.stream().map(e -> {
            return new AllowRule(mapObject(e.subj()), parseOperation(e.verb()), mapObject(e.obj()));
        }).collect(Collectors.toList());
        Enumeration.Value test = denyRules.get(0).lvl();
        List<DenyRule> denyRulesOwn = denyRules.stream().map(e -> {
            return new DenyRule(mapObject(e.subj()), parseOperation(e.ver()), mapObject(e.obj()), convertPrivacyLevel(e.lvl()));
        }).collect(Collectors.toList());

        /* contains allow rules where a corresponding deny rule exist */
        List<AllowRule> allowRulesWithDeny = rules.stream().filter(
                allow -> denyRulesOwn.stream().
                        anyMatch(deny ->
                                deny.getObject().equals(allow.getObject()) &&
                                        deny.getSubject().equals(allow.getSubject()) &&
                                        deny.getOperation().equals(allow.getOperation())))
                .collect(Collectors.toList());
        List<AllowRule> removeRules = new ArrayList<>();
        for(AllowRule rule:rules){
            //mitigation by Deny Rule
            Optional<PrivacyLevel> privacyLevel = privacyTable.getPrivacyLevel(rule.getSubject(),rule.getOperation(),rule.getObject());
            if(!privacyLevel.isPresent())
                continue;
            Collection<DenyRule> deny = getDenyRules(rule,denyRulesOwn);
            EnumSet<PrivacyLevel> test123 = EnumSet.range(PrivacyLevel.PUBLIC,PrivacyLevel.HIGHLY_SENSITIVE);
            List<DenyPermission> applicableDenyRules = deny.stream().
                    filter(e -> EnumSet.range(e.getPrivacyLevel(),PrivacyLevel.HIGHLY_SENSITIVE).
                            contains(privacyLevel.get())).
                    map(e->e.getScalaPermission()).collect(Collectors.toList());
            if(applicableDenyRules.size()>0){
            removeRules.add(rule);
            deniedRules.put(rule.getScalaPermission(),applicableDenyRules);
            }
        }
        rules.removeAll(removeRules);

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
        allowRules.forEach(e ->
                System.out.println(e.subj() + " " + e.verb() + " " + e.obj()));
        System.out.println("############### Deny ###################");
        denyRules.forEach(e ->
                System.out.println(e.subj() + " " + e.ver() + " " + e.obj() + " " + e.lvl())
        );
        System.out.println("################################################");
        return rules.stream().map(e-> e.getScalaPermission()).collect(Collectors.toList());

    }

    private Collection<DenyRule> getDenyRules(AllowRule allowRule, Collection<DenyRule> denyRules){
        return denyRules.stream().filter(e-> e.equalRule(allowRule)).collect(Collectors.toList());
    }
    private PrivacyLevel convertPrivacyLevel(Enumeration.Value privacyLevel) {
        return PrivacyLevel.valueOf(privacyLevel.toString().toUpperCase());
    }

    public static  Operation parseOperation(String verb) {
        if(verb.contains("(")) {
            String operation = verb.substring(0, verb.indexOf('('));
            String[] parameters = verb.substring(verb.indexOf('(') + 1,verb.indexOf(')')).split(",");
            return new Operation(operation, parameters);
        }
        return new Operation(verb);
    }

    private DataObject mapObject(String value) {
        return new DataObject(datatypeMapping.get(value), value);
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
