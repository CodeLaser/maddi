
# Platform dependencies - equivalent to platform/build.gradle.kts constraints
# Note: In Bzlmod, versions are managed in MODULE.bazel
# This file now just defines logical groupings for convenience

# Common dependencies for all java libraries (equivalent to java-library-conventions.gradle.kts)
COMMON_JAVA_DEPS = [
    # Logging (implementation scope)
    "@maven//:org_slf4j_slf4j_api",
]

COMMON_JAVA_TEST_DEPS = [
    # Test runtime
    "@maven//:ch_qos_logback_logback_classic",
    
    # Test implementation
    "@maven//:org_junit_jupiter_junit_jupiter_api",
    "@maven//:org_jetbrains_annotations",
    
    # Test runtime
    "@maven//:org_junit_jupiter_junit_jupiter_engine",
    "@maven//:org_junit_platform_junit_platform_launcher",
    "@maven//:org_junit_platform_junit_platform_console",
]
