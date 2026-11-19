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

@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class, CompilerConfiguration.Internals::class)

package io.codelaser.maddi.kotlin.k2

import org.jetbrains.kotlin.assignment.plugin.AssignmentComponentRegistrar
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.assignment.plugin.AssignmentConfigurationKeys
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverComponentRegistrar
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverConfigurationKeys
import org.slf4j.LoggerFactory

/**
 * <b>The compiler plugins a build applied, applied to the analysis too.</b> Gradle's kotlin-dsl compiles build logic
 * with two: sam-with-receiver, which makes an `Action<T>` lambda's parameter its implicit receiver (`tasks.register("x")
 * { group = "y" }`), and assignment, which reads `p = v` on a Gradle `Property` as `p.assign(v)`. Without them K2
 * resolves neither, and every such call in build logic was a placeholder (Exposed's 19, detekt's 3).
 *
 * <p>What a source set's kotlinc invocation said is its `-P plugin:<id>:<key>=<value>` options
 * (`SourceSet.kotlinPluginOptions`). The plugin implementations are the analyzer's own, at its Kotlin version: the
 * build's jars are its Kotlin version, which the Analysis API cannot load. A plugin id this does not know is logged and
 * left out -- its code keeps whatever placeholders it had.
 *
 * <p>⚠ For the whole session: the standalone Analysis API registers plugins per session, not per module. Both plugins
 * act only on declarations annotated with the configured annotations (`org.gradle.api.HasImplicitReceiver`,
 * `org.gradle.api.SupportsKotlinAssignmentOverloading`), which only Gradle's API carries, so a source set compiled
 * without them is unaffected unless it calls the Gradle API in the plugins' way -- which kotlinc would have refused.
 */
internal object CompilerPlugins {
    private val LOGGER = LoggerFactory.getLogger(CompilerPlugins::class.java)

    const val SAM_WITH_RECEIVER = "org.jetbrains.kotlin.samWithReceiver"
    const val ASSIGNMENT = "org.jetbrains.kotlin.assignment"

    private val OPTION = Regex("plugin:([^:]+):([^=]+)=(.*)")

    /** The configuration registering the plugins [options] configure; null when they configure none this knows. */
    fun configuration(options: Collection<String>): CompilerConfiguration? {
        val byPlugin = LinkedHashMap<String, MutableMap<String, MutableList<String>>>()
        options.distinct().forEach { option ->
            val m = OPTION.matchEntire(option.trim())
            if (m == null) LOGGER.warn("Not a compiler-plugin option, ignored: {}", option)
            else byPlugin.getOrPut(m.groupValues[1]) { LinkedHashMap() }.getOrPut(m.groupValues[2]) { ArrayList() } += m.groupValues[3]
        }
        val configuration = CompilerConfiguration()
        var registered = false
        byPlugin.forEach { (id, values) ->
            when (id) {
                SAM_WITH_RECEIVER -> {
                    configuration.put(SamWithReceiverConfigurationKeys.SAM_WITH_RECEIVER_ANNOTATION, values["annotation"].orEmpty())
                    configuration.put(SamWithReceiverConfigurationKeys.SAM_WITH_RECEIVER_PRESET, values["preset"].orEmpty())
                    configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, SamWithReceiverComponentRegistrar())
                    registered = true
                }
                ASSIGNMENT -> {
                    configuration.put(AssignmentConfigurationKeys.ASSIGNMENT_ANNOTATION, values["annotation"].orEmpty())
                    configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, AssignmentComponentRegistrar())
                    registered = true
                }
                else -> LOGGER.info("Compiler plugin {} is not applied to the analysis; its code may keep placeholders", id)
            }
        }
        if (!registered) return null
        // where the registrars put their extensions, as kotlinc's CLI provides it; the standalone provider requires it
        configuration.extensionsStorage = CompilerPluginRegistrar.ExtensionStorage()
        return configuration
    }
}
