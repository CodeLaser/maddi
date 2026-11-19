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

@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class, CompilerConfiguration.Internals::class,
    org.jetbrains.kotlin.analysis.api.KaPlatformInterface::class)

package io.codelaser.maddi.kotlin.k2

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.assignment.plugin.AssignmentComponentRegistrar
import org.jetbrains.kotlin.assignment.plugin.AssignmentConfigurationKeys
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ExtensionPointDescriptor
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverComponentRegistrar
import org.jetbrains.kotlin.samWithReceiver.SamWithReceiverConfigurationKeys
import org.slf4j.LoggerFactory

/**
 * <b>The compiler plugins a build applied, applied to the analysis too -- to the source sets it applied them to.</b>
 * Gradle's kotlin-dsl compiles build logic with two: sam-with-receiver, which makes an `Action<T>` lambda's parameter
 * its implicit receiver (`tasks.register("x") { group = "y" }`), and assignment, which reads `p = v` on a Gradle
 * `Property` as `p.assign(v)`. Without them K2 resolves neither, and every such call in build logic was a placeholder
 * (Exposed's 19, detekt's 3).
 *
 * <p>What a source set's kotlinc invocation said is its `-P plugin:<id>:<key>=<value>` options
 * (`SourceSet.kotlinPluginOptions`). The plugin implementations are the analyzer's own, at its Kotlin version: the
 * build's jars are its Kotlin version, which the Analysis API cannot load. A plugin id this does not know is logged and
 * left out -- its code keeps whatever placeholders it had.
 *
 * <p>⛔ PER MODULE, which is why this is a provider of its own rather than the standalone session's
 * `registerCompilerPluginServices`, which registers one configuration for every module. Measured on Exposed: with the
 * kotlin-dsl plugins session-wide, its Gradle PLUGIN module -- ordinary code, compiled without them, whose `Action`
 * lambdas say `it` -- resolved those lambdas with a receiver instead, and its placeholders went from 0 to 17.
 */
internal class CompilerPlugins : KotlinCompilerPluginsProvider {
    private val byModule = java.util.concurrent.ConcurrentHashMap<KaModule, Applied>()

    private class Applied(val storage: CompilerPluginRegistrar.ExtensionStorage, val ids: Set<String>)

    /** [module] was compiled with [options]: its extensions, built now. */
    fun register(module: KaModule, options: List<String>) {
        if (options.isEmpty()) return
        val byPlugin = parse(options)
        val storage = CompilerPluginRegistrar.ExtensionStorage()
        val ids = HashSet<String>()
        byPlugin.forEach { (id, values) ->
            val (registrar, configuration) = when (id) {
                SAM_WITH_RECEIVER -> SamWithReceiverComponentRegistrar() to CompilerConfiguration().apply {
                    put(SamWithReceiverConfigurationKeys.SAM_WITH_RECEIVER_ANNOTATION, values["annotation"].orEmpty())
                    put(SamWithReceiverConfigurationKeys.SAM_WITH_RECEIVER_PRESET, values["preset"].orEmpty())
                }
                ASSIGNMENT -> AssignmentComponentRegistrar() to CompilerConfiguration().apply {
                    put(AssignmentConfigurationKeys.ASSIGNMENT_ANNOTATION, values["annotation"].orEmpty())
                }
                else -> {
                    LOGGER.info("Compiler plugin {} is not applied to the analysis; its code may keep placeholders", id)
                    return@forEach
                }
            }
            with(registrar) { storage.registerExtensions(configuration) }
            ids += id
        }
        if (ids.isNotEmpty()) byModule[module] = Applied(storage, ids)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getRegisteredExtensions(module: KaModule, extensionType: ExtensionPointDescriptor<T>): List<T> =
        (byModule[module]?.storage?.registeredExtensions?.get(extensionType) as? List<T>) ?: emptyList()

    override fun isPluginOfTypeRegistered(module: KaModule, pluginType: KotlinCompilerPluginsProvider.CompilerPluginType): Boolean =
        when (pluginType) {
            KotlinCompilerPluginsProvider.CompilerPluginType.ASSIGNMENT -> byModule[module]?.ids?.contains(ASSIGNMENT) == true
        }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CompilerPlugins::class.java)

        const val SAM_WITH_RECEIVER = "org.jetbrains.kotlin.samWithReceiver"
        const val ASSIGNMENT = "org.jetbrains.kotlin.assignment"

        private val OPTION = Regex("plugin:([^:]+):([^=]+)=(.*)")

        /** `plugin:<id>:<key>=<value>` options, by plugin id and key. */
        private fun parse(options: List<String>): Map<String, Map<String, List<String>>> {
            val byPlugin = LinkedHashMap<String, MutableMap<String, MutableList<String>>>()
            options.distinct().forEach { option ->
                val m = OPTION.matchEntire(option.trim())
                if (m == null) LOGGER.warn("Not a compiler-plugin option, ignored: {}", option)
                else byPlugin.getOrPut(m.groupValues[1]) { LinkedHashMap() }.getOrPut(m.groupValues[2]) { ArrayList() } += m.groupValues[3]
            }
            return byPlugin
        }
    }
}
