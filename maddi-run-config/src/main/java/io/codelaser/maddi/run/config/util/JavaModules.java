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

package io.codelaser.maddi.run.config.util;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaModules {

    /**
     * The JDK modules a parse needs when the caller states none. <b>Not {@code java.base}:</b> a non-modular
     * corpus sees the whole platform, so anything reaching {@code java.xml}, {@code java.sql} or
     * {@code java.desktop} resolves against nothing when only {@code java.base} is offered.
     * <p>
     * ⛔ It lives here because it was previously stated once per plugin, and only once: the Maven plugin declared
     * {@code @Parameter(defaultValue = "java.se")} while the Gradle plugin's extension field defaulted to null,
     * i.e. to {@code java.base} alone. Measured on fernflower: 1 JDK class path part instead of 20, and 17
     * "Unknown module java.compiler, add to classpath?" warnings that a caller has no way to connect to a
     * missing default.
     */
    public static final String DEFAULT_JMODS = "java.se";

    /**
     * One JDK module as a class path part. The three callers that used to build this inline -- both build plugins
     * and {@code CompileListToInputConfiguration} -- wrote the same six setters, and the {@code jmod:} URI scheme
     * is what {@code JavaInspectorImpl} dispatches on, so a divergence here is not cosmetic.
     */
    public static SourceSet jmodSourceSet(String jmod) {
        return new SourceSetImpl.Builder().setName(jmod)
                .setUri(URI.create("jmod:" + jmod))
                .setLibrary(true)
                .setExternalLibrary(true)
                .setPartOfJdk(true)
                .setModule(true)
                .build();
    }

    /**
     * The JDK class path parts a build plugin contributes, from its {@code jmods} option: the closure of what was
     * asked for, or of {@link #DEFAULT_JMODS} when nothing was.
     * <p>
     * Sorted, because {@link #jmodsFromString} returns a {@link HashSet} and its iteration order is not stable
     * across runs -- which would shuffle the serialized configuration and make two runs at one revision differ.
     */
    public static List<SourceSet> javaModuleSourceSets(String jmodsString) {
        String effective = jmodsString == null || jmodsString.isBlank() ? DEFAULT_JMODS : jmodsString;
        return jmodsFromString(effective).stream()
                .filter(jmod -> !jmod.isBlank())
                .sorted()
                .map(JavaModules::jmodSourceSet)
                .toList();
    }

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
