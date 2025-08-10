
package org.JayGamerz;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class ColorManager {
    
    // Constants for better maintainability
    private static final char COLOR_CHAR = '§';
    
    // Precompiled regex patterns for better performance
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9a-fA-FklmnorKLMNOR])");
    
    private ColorManager() {
        throw new UnsupportedOperationException("This class is not meant to be instantiated.");
    }

    /**
     * Translates color codes in the given text.
     * Supports both legacy (&a, &b, etc.) and hex colors (&#ffffff).
     * 
     * @param textToTranslate The text to colorize
     * @return The colorized text with § color codes
     */
    public static String colorize(String textToTranslate) {
        if (textToTranslate == null || textToTranslate.isEmpty()) {
            return textToTranslate;
        }
        
        // Process hex colors first (&#ffffff format)
        String result = processHexColors(textToTranslate);
        
        // Process legacy colors (&a, &b, etc.)
        result = processLegacyColors(result);
        
        return result;
    }
    
    /**
     * Processes hex color codes in the format &#ffffff
     */
    private static String processHexColors(String text) {
        if (!text.contains("&#")) {
            return text;
        }
        
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder();
            replacement.append(COLOR_CHAR).append('x');
            
            // Convert each hex digit to individual color codes
            for (char c : hexCode.toCharArray()) {
                replacement.append(COLOR_CHAR).append(c);
            }
            
            matcher.appendReplacement(result, replacement.toString());
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Processes legacy color codes in the format &a, &b, etc.
     */
    private static String processLegacyColors(String text) {
        if (!text.contains("&")) {
            return text;
        }
        
        Matcher matcher = LEGACY_PATTERN.matcher(text);
        return matcher.replaceAll(COLOR_CHAR + "$1");
    }
    
    /**
     * Strips all color codes from the given text
     * 
     * @param text The text to strip colors from
     * @return The text without color codes
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Remove all § color codes
        String result = text.replaceAll("§[0-9a-fA-FklmnorKLMNORx]", "");
        
        // Remove hex color patterns
        result = result.replaceAll("§x(§[0-9a-fA-F]){6}", "");
        
        return result;
    }
}
