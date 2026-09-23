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
 * bundles can live behind a classloader of their own (docs/kotlin-classloader-isolation.md).
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
