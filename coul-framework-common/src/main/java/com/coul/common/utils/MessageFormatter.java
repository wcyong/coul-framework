package com.coul.common.utils;

public class MessageFormatter {
    static final char         DELIM_START = '{';
    static final char         DELIM_STOP  = '}';
    private static final char ESCAPE_CHAR = '\\';
    
    /**
     * Performs single argument substitution for the 'messagePattern' passed as
     * parameter.
     * <p>
     * For example,
     * 
     * <pre>
     * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;);
     * </pre>
     * 
     * will return the string "Hi there.".
     * <p>
     * 
     * @param messagePattern
     *          The message pattern which will be parsed and formatted
     * @param argument
     *          The argument to be substituted in place of the formatting anchor
     * @return The formatted message
     */
    public static String format(String messagePattern, Object arg) {
        return arrayFormat(messagePattern, new Object[] {arg });
    }
    
    /**
     * 
     * Performs a two argument substitution for the 'messagePattern' passed as
     * parameter.
     * <p>
     * For example,
     * 
     * <pre>
     * MessageFormatter.format(&quot;Hi {}. My name is {}.&quot;, &quot;Alice&quot;, &quot;Bob&quot;);
     * </pre>
     * 
     * will return the string "Hi Alice. My name is Bob.".
     * 
     * @param messagePattern
     *          The message pattern which will be parsed and formatted
     * @param arg1
     *          The argument to be substituted in place of the first formatting
     *          anchor
     * @param arg2
     *          The argument to be substituted in place of the second formatting
     *          anchor
     * @return The formatted message
     */
    public static String format(String messagePattern, Object arg1, Object arg2) {
        return arrayFormat(messagePattern, new Object[] {arg1, arg2 });
    }
    
    /**
     * Same principle as the {@link #format(String, Object)} and
     * {@link #format(String, Object, Object)} methods except that any number of
     * arguments can be passed in an array.
     * 
     * @param messagePattern
     *          The message pattern which will be parsed and formatted
     * @param argArray
     *          An array of arguments to be substituted in place of formatting
     *          anchors
     * @return The formatted message
     */
    public static String arrayFormat(String messagePattern, Object[] argArray) {
        if (messagePattern == null) {
            return null;
        }
        int i = 0;
        int len = messagePattern.length();
        int j = messagePattern.indexOf(DELIM_START);
        
        if (argArray == null) {
            return messagePattern;
        }
        
        StringBuffer sbuf = new StringBuffer(messagePattern.length() + 50);
        
        for (int L = 0; L < argArray.length; L++) {
            
            j = messagePattern.indexOf(DELIM_START, i);
            
            if (j == -1 || (j + 1 == len)) {
                // no more variables
                if (i == 0) { // this is a simple string
                    return messagePattern;
                } else { // add the tail string which contains no variables and return
                    // the result.
                    sbuf.append(messagePattern.substring(i, messagePattern.length()));
                    return sbuf.toString();
                }
            } else {
                char delimStop = messagePattern.charAt(j + 1);
                
                if (isEscapedDelimeter(messagePattern, j)) {
                    if (!isDoubleEscaped(messagePattern, j)) {
                        L--; // DELIM_START was escaped, thus should not be incremented
                        sbuf.append(messagePattern.substring(i, j - 1));
                        sbuf.append(DELIM_START);
                        i = j + 1;
                    } else {
                        // The escape character preceding the delemiter start is
                        // itself escaped: "abc x:\\{}"
                        // we have to consume one backward slash
                        sbuf.append(messagePattern.substring(i, j - 1));
                        sbuf.append(argArray[L]);
                        i = j + 2;
                    }
                } else if ((delimStop != DELIM_STOP)) {
                    // invalid DELIM_START/DELIM_STOP pair
                    sbuf.append(messagePattern.substring(i, messagePattern.length()));
                    return sbuf.toString();
                } else {
                    // normal case
                    sbuf.append(messagePattern.substring(i, j));
                    sbuf.append(argArray[L]);
                    i = j + 2;
                }
            }
        }
        // append the characters following the last {} pair.
        sbuf.append(messagePattern.substring(i, messagePattern.length()));
        return sbuf.toString();
    }
    
    static boolean isEscapedDelimeter(String messagePattern, int delimeterStartIndex) {
        
        if (delimeterStartIndex == 0) {
            return false;
        }
        char potentialEscape = messagePattern.charAt(delimeterStartIndex - 1);
        if (potentialEscape == ESCAPE_CHAR) {
            return true;
        } else {
            return false;
        }
    }
    
    static boolean isDoubleEscaped(String messagePattern, int delimeterStartIndex) {
        if (delimeterStartIndex >= 2
            && messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR) {
            return true;
        } else {
            return false;
        }
    }
}
