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
package io.codelaser.maddi.inspection.kotlin

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
