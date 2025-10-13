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

package org.e2immu.analyzer.run.config.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JavaModules {

    public static Set<String> jmodsFromString(String jmodsString) {
        Set<String> jmods = new HashSet<>();
        Collections.addAll(jmods, "java.base");
        if (jmodsString != null && !jmodsString.isBlank()) {
            String[] split = jmodsString.split("[,;]\\s*");
            Collections.addAll(jmods, split);
            addClosure(jmods);
        }
        return jmods;
    }

    private static void addClosure(Set<String> jmods) {
        boolean change = true;
        while (change) {
            change = false;
            Set<String> copy = new HashSet<>(jmods);
            for (String jmod : copy) {
                for (String dep : jmodDependency(jmod)) {
                    if (jmods.add(dep)) change = true;
                }
            }
        }
    }

    public static Set<String> jmodDependencyClosure(String jmod) {
        Set<String> set = new HashSet<>(jmodDependency(jmod));
        addClosure(set);
        return set;
    }

    /*
    How to find the dependencies of jdk.* modules?

    jar xf /opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/jmods/jdk.unsupported.jmod classes/module-info.class
    javap classes/module-info.class
     */
    public static Set<String> jmodDependency(String jmod) {
        return switch (jmod) {
            case "java.base" -> Set.of();
            case "java.desktop" -> Set.of("java.xml", "java.datatransfer");
            case "java.management.rmi" -> Set.of("java.management", "java.rmi");
            case "java.se" -> Set.of("java.scripting", "java.sql.rowset", "java.xml.crypto", "java.desktop",
                    "java.compiler", "java.instrument", "java.management.rmi", "java.net.http", "java.prefs",
                    "java.security.jgss", "java.security.sasl");
            case "java.sql" -> Set.of("java.logging", "java.xml", "java.transaction.xa");
            case "java.sql.rowset" -> Set.of("java.sql", "java.naming");
            case "java.xml.crypto" -> Set.of("java.xml");
            case "java.compiler", "java.datatransfer", "java.instrument",
                 "java.logging", "java.management", "java.naming", "java.net.http", "java.prefs", "java.rmi",
                 "java.scripting", "java.security.jgss", "java.security.sasl", "java.smartcardio",
                 "java.transaction.xa", "java.xml",
                 "jdk.jfr",
                 "jdk.unsupported" -> Set.of("java.base");
            case "jdk.accessibility" -> Set.of("java.desktop");
            case "jdk.attach" -> Set.of("jdk.internal.jvmstat");
            default -> {
                if (jmod.startsWith("java.")) {
                    throw new UnsupportedOperationException("Implement: " + jmod + ". We should know the dependencies of all java.* modules");
                }
                yield Set.of("java.base");
            }
        };
    }
}
