/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.util;

public class GetSetNames {
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
