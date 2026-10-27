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
package io.codelaser.maddi.inspection.mixed

import io.codelaser.maddi.kotlin.api.KotlinFrontEnd
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds
import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * This module's tests run the Kotlin compiler FLAT — `maddi-kotlin-k2` is a `testImplementation`
 * dependency, on the test JVM's own classpath — and that is a choice, so it is stated here, once, rather
 * than inherited from a fallback. Until 2026-09-22 `KotlinFrontEnds.get()` loaded whatever front end the
 * classpath offered when nobody had installed one; that silent path is gone (NoSilentFallbackTest in
 * maddi-kotlin-api), and this is the explicit form it asks for. The realm itself is tested in
 * maddi-kotlin-realm and maddi-run-kotlin.
 */
class FlatFrontEndTestBootstrap : LauncherSessionListener {
    override fun launcherSessionOpened(session: LauncherSession) {
        if (!KotlinFrontEnds.isInstalled()) KotlinFrontEnds.install(KotlinFrontEnd.load())
    }
}
