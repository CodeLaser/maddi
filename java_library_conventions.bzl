"""
Java library conventions - equivalent to buildSrc java-library-conventions
"""

load("@rules_java//java:defs.bzl", "java_library", "java_test")
load("//:platform.bzl", "COMMON_JAVA_DEPS", "COMMON_JAVA_TEST_DEPS")

def codelaser_java_library(
        name,
        srcs = None,
        deps = None,
        exports = None,
        runtime_deps = None,
        resources = None,
        visibility = None,
        testonly = False,
        **kwargs):
    """
    Creates a Java library with common io.codelaser conventions.
    
    This macro automatically includes common dependencies like slf4j-api
    and sets up standard configurations equivalent to java-library-conventions.gradle.kts
    """
    
    # Merge common deps with provided deps
    all_deps = COMMON_JAVA_DEPS + (deps or [])
    
    java_library(
        name = name,
        srcs = srcs or [],
        deps = all_deps,
        exports = exports,
        runtime_deps = runtime_deps,
        resources = resources,
        visibility = visibility,
        testonly = testonly,
        **kwargs
    )

def codelaser_java_test(
        name,
        srcs = None,
        deps = None,
        runtime_deps = None,
        data = None,
        resources = None,
        size = "medium",
        **kwargs):
    """
    Creates a Java test with common io.codelaser test conventions.
    
    Automatically includes JUnit Jupiter and other common test dependencies.
    Uses JUnit Platform for test execution (equivalent to useJUnitPlatform()).
    """
    
    # Merge common test deps with provided deps
    all_deps = COMMON_JAVA_DEPS + COMMON_JAVA_TEST_DEPS + (deps or [])
    all_runtime_deps = (runtime_deps or [])
    
    java_test(
        name = name,
        srcs = srcs or [],
        deps = all_deps,
        runtime_deps = all_runtime_deps,
        data = data,
        resources = resources,
        size = size,
        use_testrunner = True,
        **kwargs
    )
