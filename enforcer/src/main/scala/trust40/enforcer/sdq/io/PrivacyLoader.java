package trust40.enforcer.sdq.io;

import trust40.enforcer.sdq.PrivacyLevel;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class for loading the privacy level
 */
public class PrivacyLoader extends CSVLoader {

    public PrivacyLoader(String path) {
        super(path);
    }

    private Map<String, PrivacyLevel> createMap(String[][] privacyMap) {
        if(privacyMap == null)
        	throw new IllegalArgumentException("Privacy Map can't be null");
        if (privacyMap[0].length != 2)
            throw new IllegalArgumentException("Size of 2d dimension needs to be 2");
        return Arrays.stream(privacyMap)
                .collect(Collectors.toMap(e -> e[0], e -> PrivacyLevel.valueOf(e[1].toUpperCase())));
    }

    /**
     * Loads the privacy level and returns a map with the object id as key and the privacy level as value
     * @return Map with key object id and value {@link PrivacyLevel}
     * @throws IOException If there are errors during the file reading
     */
    public Map<String, PrivacyLevel> getPrivacyMap() throws IOException {
        String[][] privacyStrings = super.loadCSVFile(2);
        return createMap(privacyStrings);
    }
}
