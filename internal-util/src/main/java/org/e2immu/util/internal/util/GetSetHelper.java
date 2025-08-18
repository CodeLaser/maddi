package org.e2immu.util.internal.util;

public class GetSetHelper {
    public static String fieldName(String methodName) {
        String extractedName;
        int length = methodName.length();
        boolean set = methodName.startsWith("set");
        boolean has = methodName.startsWith("has");
        boolean get = methodName.startsWith("get");
        boolean is = methodName.startsWith("is");
        if (length >= 4 && (set || has || get) && Character.isUpperCase(methodName.charAt(3))) {
            extractedName = methodName.substring(3);
        } else if (length >= 3 && is && Character.isUpperCase(methodName.charAt(2))) {
            extractedName = methodName.substring(2);
        } else {
            extractedName = methodName;
        }
        return Character.toLowerCase(extractedName.charAt(0)) + extractedName.substring(1);
    }

    public static String setterName(String fieldName) {
        return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    public static String getterName(String fieldName, boolean isBoolean) {
        String prefix = isBoolean ? "is" : "get";
        return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
}
