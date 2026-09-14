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
package io.codelaser.maddi.kotlin.k2

import com.intellij.util.pico.DefaultPicoContainer
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.references.utils.KotlinKDocResolutionStrategyProviderService

/**
 * Makes KDoc names resolvable in a standalone session. The Analysis API's FIR implementation of this service
 * (registered by `analysis-api-fir.xml`) subscribes to an IntelliJ Registry key through
 * `RegistryValue.addListener(RegistryValueListener, Disposable)`, which the IntelliJ core bundled in kotlin-compiler
 * does not have: the first KDoc resolution died with a NoSuchMethodError. All the service answers is whether to use
 * the experimental KDoc resolution, which that Registry key leaves off.
 *
 * To be called on the built session: the builder registers the XML services in `build()`, after its configuration
 * lambda, so a registration made in the lambda is replaced.
 */
internal fun StandaloneAnalysisAPISession.registerKDocResolution() {
    // the mock project keys a service by its interface's name. Its container is reached reflectively: Kotlin cannot
    // type-check against MockProject here (a supertype, ComponentManagerEx, is not on the compile class path), and
    // the Project interface has no accessor for it.
    val container = project.javaClass.getMethod("getPicoContainer").invoke(project) as DefaultPicoContainer
    val key = KotlinKDocResolutionStrategyProviderService::class.java.name
    container.unregisterComponent(key)
    container.registerComponentInstance(key, object : KotlinKDocResolutionStrategyProviderService {
        override fun shouldUseExperimentalStrategy(): Boolean = false
        override fun dispose() {}
    })
}
