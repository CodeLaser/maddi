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
