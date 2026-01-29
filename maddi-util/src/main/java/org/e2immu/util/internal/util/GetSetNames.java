/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.util.internal.util;

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
