"""Standalone tests for individual golden jq test-case files."""

load("@rules_java//java:defs.bzl", "java_test")

_RESOURCE_PREFIX = "test-cases/"
_YAML_SUFFIX = ".yaml"

def _target_suffix(test_case):
    resource = test_case[len(_RESOURCE_PREFIX):-len(_YAML_SUFFIX)]
    return resource.replace("/", "-").replace("_", "-").replace("@", "")

def test_case_suite(
        name,
        test_cases,
        test_class,
        test_library,
        visibility,
        data = [],
        env = {},
        jvm_flags = []):
    """Creates one standalone java_test per YAML test-case resource.

    Args:
      name: Name of the aggregate test suite and prefix for individual tests.
      test_cases: YAML resource files to test.
      test_class: Java main class that executes one test-case resource.
      test_library: Java library containing the test class.
      visibility: Visibility of the generated tests and aggregate suite.
      data: Runtime data required by the test.
      env: Environment variables for every generated test.
      jvm_flags: JVM flags for every generated test.
    """
    tests = []
    seen = {}
    for test_case in sorted(test_cases):
        suffix = _target_suffix(test_case)
        if suffix in seen:
            fail("test target name collision between %s and %s" % (seen[suffix], test_case))
        seen[suffix] = test_case

        test_name = name + "-" + suffix
        tests.append(":" + test_name)
        java_test(
            name = test_name,
            args = [test_case[len(_RESOURCE_PREFIX):]],
            data = data,
            env = env,
            main_class = test_class,
            jvm_flags = jvm_flags,
            resource_strip_prefix = native.package_name() + "/" + _RESOURCE_PREFIX,
            resources = [test_case],
            runtime_deps = [
                test_library,
                "@maven//:org_assertj_assertj_core",
                "@maven//:org_junit_jupiter_junit_jupiter_api",
            ],
            size = "small",
            use_testrunner = False,
            visibility = visibility,
        )

    native.test_suite(
        name = name,
        tags = ["manual"],
        tests = tests,
        visibility = visibility,
    )
