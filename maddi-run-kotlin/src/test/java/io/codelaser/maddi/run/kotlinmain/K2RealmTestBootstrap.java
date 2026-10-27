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
