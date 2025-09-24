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
        javacopts = [
            "--enable-preview",
            #  "-source", "24",
        ],
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
        test_packages = None,
        **kwargs):
    """
    Creates a Java test with common io.codelaser test conventions.
    """

    all_deps = COMMON_JAVA_DEPS + COMMON_JAVA_TEST_DEPS + (deps or [])
    all_runtime_deps = (runtime_deps or [])
    select_packages = ["--select-package=" + pkg for pkg in test_packages]
    all_args = [ "--reports-dir=bazel-testlogs/" + name ]  + select_packages + kwargs.pop("args", [])

    java_test(
        name = name,
        srcs = srcs or [],
        deps = all_deps,
        runtime_deps = all_runtime_deps,
        data = data,
        resources = resources,
        size = size,
        test_class = None,
        # Force JUnit 5 platform runner
        use_testrunner = False,  # Don't use Bazel's default JUnit 4 runner
        main_class = "org.junit.platform.console.ConsoleLauncher",
        args = all_args,
        javacopts = [
            "--enable-preview",
            #  "-source", "24",
        ],
        **kwargs
    )
