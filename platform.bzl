
# Platform dependencies - equivalent to platform/build.gradle.kts constraints
# Note: In Bzlmod, versions are managed in MODULE.bazel
# This file now just defines logical groupings for convenience

# Common dependencies for all java libraries (equivalent to java-library-conventions.gradle.kts)
COMMON_JAVA_DEPS = [
    # Logging (implementation scope)
    "@maven//:org_slf4j_slf4j_api",

    # Nullability annotations — java-library-conventions.gradle.kts puts these on
    # every module's implementation classpath.
    "@maven//:org_jetbrains_annotations",
]

# The openjdk-based front-end reaches into javac internals; these exports are needed
# both to compile it (javacopts) and to run it (jvm_flags). Mirrors the per-module
# lists in maddi-{java,inspection,run}-openjdk/build.gradle.kts.
OPENJDK_JAVAC_EXPORTS = [
    "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
]

COMMON_JAVA_TEST_DEPS = [
    # Test runtime
    "@maven//:ch_qos_logback_logback_classic",
    
    # Test implementation (org.jetbrains:annotations comes in via COMMON_JAVA_DEPS)
    "@maven//:org_junit_jupiter_junit_jupiter_api",

    # Test runtime
    "@maven//:org_junit_jupiter_junit_jupiter_engine",
    "@maven//:org_junit_platform_junit_platform_launcher",
    "@maven//:org_junit_platform_junit_platform_console",
]
