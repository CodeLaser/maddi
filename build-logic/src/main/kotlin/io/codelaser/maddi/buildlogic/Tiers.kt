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

package io.codelaser.maddi.buildlogic

/**
 * The tier (base, mod, dist) of every maddi module, read from tiers.txt in this build's resources: the one list
 * that tools/tiers/check_tiers.py, the maddi-tier-guard plugin and the conventions' publishing rule all use.
 */
object Tiers {
    val byModule: Map<String, String> by lazy {
        val stream = checkNotNull(Tiers::class.java.classLoader.getResourceAsStream("tiers.txt")) {
            "tiers.txt is missing from build-logic's resources"
        }
        stream.bufferedReader().readLines()
            .map { it.substringBefore('#').trim().split(Regex("\\s+")) }
            .filter { it.size == 2 && it[1] in setOf("base", "mod", "dist") }
            .associate { it[0] to it[1] }
    }

    fun of(module: String): String? = byModule[module]
}
