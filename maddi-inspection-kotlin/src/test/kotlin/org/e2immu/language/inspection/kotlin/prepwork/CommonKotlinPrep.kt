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

package org.e2immu.language.inspection.kotlin.prepwork

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer
import org.e2immu.language.cst.api.info.MethodInfo
import org.e2immu.language.cst.api.info.TypeInfo
import org.e2immu.language.cst.api.runtime.Runtime
import org.e2immu.language.cst.impl.runtime.RuntimeImpl
import org.e2immu.language.inspection.resource.SourceSetImpl
import org.e2immu.language.kotlin.k2.KotlinScan
import java.net.URI

/**
 * Tier-1 cross-language validation: the Kotlin analogue of `modification-prepwork`'s `CommonTest`. Each
 * subclass ports a Java prep-analyzer test — the SAME behaviour expressed in Kotlin source — and asserts the
 * SAME [org.e2immu.analyzer.modification.prepwork.variable.VariableData] result. The Java test's assertion
 * strings (e.g. `"D:0, A:[0, 1]"`) are the oracle: if the Kotlin front-end produces a faithful CST (same
 * statement indices, same reads/assignments), the analyzer yields the identical string.
 *
 * A ported method usually becomes `class X { fun method(...) { … } }` (a Java `static` method needs no
 * receiver, but the instance form is fine — the assignment-string assertions ignore the extra `this`).
 */
abstract class CommonKotlinPrep {
    protected val runtime: Runtime = RuntimeImpl()
    private val sourceSet = SourceSetImpl.Builder().setName("source").setUri(URI.create("file:/")).build()

    /** Parse Kotlin [source]; return the first (primary) type. */
    protected fun parse(fileName: String, source: String): TypeInfo =
        KotlinScan(runtime, sourceSet).parse(fileName, source).first()

    /** Run the prep analyzer on a single [method] (mirrors the Java tests' `analyzer.doMethod(method)`). */
    protected fun doMethod(method: MethodInfo) = PrepAnalyzer(runtime).doMethod(method)
}
