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

package io.codelaser.maddi.bytecode.java.asm;


import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.HashSet;
import java.util.Set;

public class ParameterNameFactory {
    private final Set<String> names = new HashSet<>();

    public String next(ParameterizedType type) {
        String base;
        if (type.typeInfo() != null) {
            // cheap way of finding out with high likelihood if we're dealing with a primitive
            // under no conditions can we trigger a type inspection here!
            if (type.typeInfo().fullyQualifiedName().indexOf('.') < 0) {
                base = firstLetterLowerCase(type.typeInfo().simpleName()).substring(0, 1);
            } else {
                base = firstLetterLowerCase(type.typeInfo().simpleName());
            }
        } else if (type.typeParameter() != null) {
            base = firstLetterLowerCase(type.typeParameter().simpleName());
        } else {
            base = "p";
        }
        if (!names.contains(base)) {
            names.add(base);
            return base;
        }
        int index = 1;
        while (true) {
            String name = base + index;
            if (!names.contains(name)) {
                names.add(name);
                return name;
            }
            index++;
        }
    }

    void register(String name) {
        this.names.add(name);
    }

    private static String firstLetterLowerCase(String s) {
        return Character.toLowerCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : "");
    }
}
