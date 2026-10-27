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

package io.codelaser.maddi.analysis.api;

/**
 * What the nullability facts are for: the language a translation prints, whose type system decides how "no
 * verdict" must be read. Each target maps onto a policy of the nullability pass (maddi-mod).
 */
public enum NullabilityTarget {
    /** Kotlin source has no platform type: what the analysis does not call nullable is non-null. */
    KOTLIN,
    /** C# with nullable reference types: the facts as for Kotlin; the C# printer adds the stored verdicts. */
    CSHARP,
}
