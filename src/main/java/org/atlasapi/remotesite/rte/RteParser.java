package org.atlasapi.remotesite.rte;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;


public class RteParser {
    
    private final static String CANONICAL_URI_PREFIX = "http://rte.ie/shows/";
    private final static Pattern ID_PATTERN = Pattern.compile(".*:(\\d+)$");

    public static String canonicalUriFrom(String id) {
        checkArgument(!Strings.isNullOrEmpty(id), "Cannot build canonical uri from empty or null uri");
        
        return buildCanonicalUriFromId(id);
    }
    
    private static String buildCanonicalUriFromId(String id) {
        Matcher matcher = ID_PATTERN.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Failed to parse ID " + id);
        }
        return CANONICAL_URI_PREFIX + matcher.group(1);
    }

    public static String titleParser(String originalTitle) {
        String title = originalTitle.substring(6, (originalTitle.length() - 7));
        if (!title.contains("Season")) {
            return title;
        }

        String[] titleArray = title.split(" ");
        String newTitle = "";

        for (int i = 0; i < titleArray.length; i++) {
            if (titleArray[i].equals("Season")) {
                return newTitle;
            }
            newTitle += titleArray[i] + " ";
        }

        return title;
    }
    
}
