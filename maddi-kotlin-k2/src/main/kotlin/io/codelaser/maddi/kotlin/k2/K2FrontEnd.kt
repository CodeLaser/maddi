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

import io.codelaser.maddi.cst.api.element.SourceSet
import io.codelaser.maddi.cst.api.runtime.Runtime
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager
import io.codelaser.maddi.inspection.resource.InfoByFqn
import io.codelaser.maddi.kotlin.api.KotlinFrontEnd
import io.codelaser.maddi.kotlin.api.KotlinProjectScanner
import io.codelaser.maddi.kotlin.api.KotlinReferenceIndex
import io.codelaser.maddi.kotlin.api.KotlinReferenceRecall
import io.codelaser.maddi.kotlin.api.KotlinSourceScan

/**
 * <b>The K2 front end, as a service.</b> The single class a host names — everything else in this module is
 * reached through the contracts in {@code maddi-kotlin-api}, so the compiler and the 62 MB of libraries it
 * bundles can live behind a classloader of their own (docs/design/kotlin-classloader-isolation.md).
 *
 * <p>⚠ Found by {@link java.util.ServiceLoader} rather than by name, and that is deliberate: the host passes
 * the realm's classloader to {@code KotlinFrontEnd.load(loader)}, and the same lookup then finds this class
 * inside the realm without the host ever holding a reference to a class the realm owns.
 */
class K2FrontEnd : KotlinFrontEnd {

    override fun sourceScan(runtime: Runtime, sourceSet: SourceSet, infoByFqn: InfoByFqn,
                            compiledTypesManager: CompiledTypesManager?): KotlinSourceScan =
        KotlinScan(runtime, sourceSet, infoByFqn, compiledTypesManager)

    override fun projectScan(runtime: Runtime, infoByFqn: InfoByFqn,
                             compiledTypesManager: CompiledTypesManager?): KotlinProjectScanner =
        KotlinProjectScan(runtime, infoByFqn, compiledTypesManager)

    override fun referenceIndex(): KotlinReferenceIndex = K2ReferenceIndex()

    override fun referenceRecall(samplesPerCell: Int): KotlinReferenceRecall =
        ReferenceRecall(samplesPerCell)
}
