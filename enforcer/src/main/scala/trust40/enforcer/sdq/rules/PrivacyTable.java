package trust40.enforcer.sdq.rules;

import trust40.enforcer.sdq.DesignTimeDecisionMakerImpl;
import trust40.enforcer.sdq.PrivacyLevel;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class PrivacyTable {
    public static int SUBJECT = 0;
    public static int OPERATION = 1;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrivacyTable that = (PrivacyTable) o;
        return Arrays.deepEquals(privacyTable, that.privacyTable);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(privacyTable);
    }

    public static int OBJECT = 2;
    public static int PRIVACY_LEVEL = 3;
    public String[][] privacyTable;

    public PrivacyTable (String [][] table){
        privacyTable = Objects.requireNonNull(table);
        if(table.length == 0)
            throw new IllegalArgumentException("Table must be >0");
        if(table[0].length != 4){
            throw  new IllegalArgumentException("Table needs 4 columns");
        }
    }

    public Optional<PrivacyLevel> getPrivacyLevel(DataObject subject, Operation operation, DataObject object){
        return Arrays.stream(privacyTable).filter(e-> {
            Operation checkOperation = DesignTimeDecisionMakerImpl.parseOperation(e[OPERATION]);
            boolean operationExpression = checkOperation.equalOperation(operation);
            boolean subjectExpression = e[SUBJECT].equals(subject.getType());
            boolean objectExpression =  e[OBJECT].equals(object.getType());
            return operationExpression && subjectExpression && objectExpression;
        }).map(e-> PrivacyLevel.valueOf(e[PRIVACY_LEVEL].toUpperCase())).findAny();
    }

}
