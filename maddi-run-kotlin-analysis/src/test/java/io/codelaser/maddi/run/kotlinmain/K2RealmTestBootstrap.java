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

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.kotlin.realm.K2Realm;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.io.UncheckedIOException;

/**
 * <b>Installs the K2 realm once, for every test in this module.</b> The Kotlin compiler is not on this JVM's
 * classpath — it is in {@code lib-k2} terms, a path handed over as {@code -Dmaddi.k2.classpath} by this
 * module's build — so a test that drives {@code MixedInspector} or {@code KotlinInspector} directly has no
 * front end until a host installs one. The shipped CLI does that in {@code Main}; this is the same act, for
 * tests.
 *
 * <p>⭐ Which means the corpus runs in this module exercise the realm, rather than merely coexisting with a
 * flat compiler on the classpath. That is what makes them evidence for the isolation and not just for the
 * parser.
 */
public class K2RealmTestBootstrap implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        try {
            K2Realm.installIfAbsent();
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(
                    "the test JVM could not build the K2 realm; see maddi-run-kotlin/build.gradle.kts, which"
                    + " passes -D" + K2Realm.CLASSPATH_PROPERTY + " from the k2Runtime configuration", e);
        }
    }
}
