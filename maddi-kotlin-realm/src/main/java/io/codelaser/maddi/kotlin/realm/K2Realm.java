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

package io.codelaser.maddi.kotlin.realm;

import io.codelaser.maddi.kotlin.api.KotlinFrontEnd;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <b>A classloader of its own for the Kotlin front end.</b> {@code kotlin-compiler-2.4.0.jar} is 61.9 MB and
 * carries 8,235 non-Kotlin classes under their original package names — {@code org.antlr.v4.runtime},
 * {@code com.google.common}, {@code com.sun.jna}, … — ProGuard-minimised to the members Kotlin itself calls.
 * On a flat classpath the first jar with a class wins, so those copies shadow the real libraries: measured on
 * a consumer's classpath, 174 of ANTLR's 215 classes, 787 of guava's 1,962 and 115 of JNA's 124. Checkstyle
 * then died on a {@code CharStreams} with one method left. Full diagnosis in
 * {@code docs/kotlin-classloader-isolation.md}.
 *
 * <p>Here those jars go in a realm instead of on the classpath, and the only things that cross are the
 * contracts in {@code maddi-kotlin-api} and the CST they speak in.
 *
 * <h2>⛔ What must be SHARED, and why getting it wrong is silent</h2>
 * The host and the realm must agree on the CST types, because {@link io.codelaser.maddi.cst.api.info.TypeInfo}
 * instances cross in both directions and the mixed inspector's core invariant is that a cross-language
 * reference resolves to a <b>single</b> {@code TypeInfo}. Two copies of those classes would not throw
 * anywhere obvious — the parse would simply stop agreeing with itself. So {@link #SHARED} is imported from
 * the host, and {@code TestTypeInfoIdentity} asserts the identity that depends on it.
 *
 * <p>⚠ {@code kotlin.*} is shared too, and it is easy to miss: a Kotlin lambda in the contract
 * ({@code KotlinSession.declare} takes one) is a {@code kotlin.jvm.functions.Function1} at run time. Loaded
 * twice, that is a {@code ClassCastException} at the boundary.
 */
public final class K2Realm {

    /**
     * Packages the realm takes from the HOST rather than from its own jars: the contracts, the CST they speak
     * in, the registry types handed across, the Kotlin runtime those signatures are written in, and the
     * logging API (so the realm's logs come out of the host's appenders, not a second binding).
     */
    public static final List<String> SHARED = List.of(
            "io.codelaser.maddi.kotlin.api",
            "io.codelaser.maddi.cst.api",
            "io.codelaser.maddi.inspection.api",
            "io.codelaser.maddi.inspection.resource",
            "kotlin",
            "org.slf4j");

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private K2Realm() {
    }

    /**
     * A front end loaded from [jars] inside a realm of its own.
     *
     * @param jars the K2 runtime: {@code maddi-kotlin-k2} and everything it needs. A consumer resolves these
     *             from a configuration that is deliberately NOT part of its own runtime classpath.
     */
    public static KotlinFrontEnd create(List<Path> jars) throws IOException {
        if (jars.isEmpty()) {
            throw new IllegalArgumentException("the K2 realm needs the front end's jars; none were given");
        }
        ClassLoader host = K2Realm.class.getClassLoader();
        ClassWorld world = new ClassWorld();
        ClassRealm realm;
        try {
            // a fresh id per realm: a host may build more than one (tests do), and ClassWorld refuses a repeat
            realm = world.newRealm("maddi-k2-" + COUNTER.incrementAndGet(), host);
        } catch (DuplicateRealmException impossible) {
            throw new IllegalStateException(impossible);
        }
        // imports win over the realm's own jars, which is what keeps ONE copy of the CST in the JVM
        for (String pkg : SHARED) {
            realm.importFrom(host, pkg);
        }
        for (Path jar : jars) {
            if (!Files.exists(jar)) {
                throw new IOException("the K2 realm was given a path that does not exist: " + jar);
            }
            realm.addURL(toUrl(jar));
        }
        // ⭐ found INSIDE the realm, by the service lookup the contract already uses: the host never names a
        // class the realm owns, so no class of the compiler's is ever resolved against the host's loader
        return KotlinFrontEnd.load(realm);
    }

    /**
     * The system property a host sets to say where the realm's jars are: a path-separated list of jars, as a
     * resolved Gradle configuration prints itself. Tests and embedders use this.
     */
    public static final String CLASSPATH_PROPERTY = "maddi.k2.classpath";

    /** The directory of jars a distribution ships them in, beside {@code lib/} but never on the CLASSPATH. */
    public static final String HOME_PROPERTY = "maddi.k2.home";

    /** The distribution's directory name for those jars, looked for next to this module's own jar. */
    public static final String DISTRIBUTION_DIRECTORY = "lib-k2";

    /**
     * Build the realm and make it the front end for this JVM ({@code KotlinFrontEnds.install}). Call it once,
     * early. The jars are found, in order: {@code -Dmaddi.k2.classpath}, {@code -Dmaddi.k2.home}, then a
     * {@code lib-k2} directory beside the jar this class was loaded from (how the CLI distribution ships).
     *
     * <p>⛔ Throws when it finds nothing, rather than falling back to the ordinary classpath. A silent
     * fallback would reintroduce the leak exactly where nobody would look for it — and on a JVM where the
     * compiler is genuinely absent it would fail later, deeper, and less legibly.
     */
    public static void install() throws IOException {
        io.codelaser.maddi.kotlin.api.KotlinFrontEnds.install(create(discover()));
    }

    /** {@link #install}, unless a front end is already installed. Safe to call on every entry point. */
    public static void installIfAbsent() throws IOException {
        if (!io.codelaser.maddi.kotlin.api.KotlinFrontEnds.isInstalled()) install();
    }

    /** Where this JVM's K2 jars are; see {@link #install}. */
    public static List<Path> discover() throws IOException {
        String classpath = System.getProperty(CLASSPATH_PROPERTY);
        if (classpath != null && !classpath.isBlank()) return split(classpath);
        String home = System.getProperty(HOME_PROPERTY);
        if (home != null && !home.isBlank()) return jarsIn(Path.of(home));
        Path beside = besideOwnJar();
        if (beside != null && Files.isDirectory(beside)) return jarsIn(beside);
        throw new IOException("cannot find the Kotlin front end's jars. Set -D" + CLASSPATH_PROPERTY
                              + " (a path-separated jar list) or -D" + HOME_PROPERTY + " (a directory of jars),"
                              + " or ship them in a '" + DISTRIBUTION_DIRECTORY + "' directory beside "
                              + K2Realm.class.getSimpleName() + "'s own jar.");
    }

    private static List<Path> jarsIn(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) throw new IOException("not a directory of K2 jars: " + directory);
        try (var entries = Files.list(directory)) {
            List<Path> jars = entries.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList();
            if (jars.isEmpty()) throw new IOException("no jars in " + directory);
            return jars;
        }
    }

    private static Path besideOwnJar() {
        try {
            var source = K2Realm.class.getProtectionDomain().getCodeSource();
            if (source == null) return null;
            Path own = Path.of(source.getLocation().toURI());
            Path parent = own.getParent();
            return parent == null ? null : parent.resolveSibling(DISTRIBUTION_DIRECTORY);
        } catch (Exception notAJarOnDisk) {
            return null;
        }
    }

    private static List<Path> split(String classpath) {
        List<Path> jars = new ArrayList<>();
        for (String entry : classpath.split(java.io.File.pathSeparator)) {
            if (!entry.isBlank()) jars.add(Path.of(entry));
        }
        return jars;
    }

    /** As {@link #create}, reading the jar list from a path-separated string (a resolved Gradle configuration). */
    public static KotlinFrontEnd create(String classpath) throws IOException {
        return create(split(classpath));
    }

    private static java.net.URL toUrl(Path jar) throws MalformedURLException {
        return jar.toUri().toURL();
    }
}
