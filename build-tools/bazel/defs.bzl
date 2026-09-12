"""Module-level build macros for jackson-jq.

`jjq_java_mrjar` merges compiled package targets and release overlays into a single
jar (with optional OSGi bundle generation). `jjq_maven_artifact` tags the resulting jar
with Maven coordinates, derives external dependencies, and sets up Maven publication.

Modules are compiled one Java package at a time: each package directory holds a
BUILD.bazel with a `jjq_java_library`, and the module's own BUILD.bazel lists them in
`jjq_java_mrjar(packages = ...)`. Bazel then rejects any dependency cycle between
packages, which is the point -- see docs/package-cycles.md. `jjq_java_mrjar`
reassembles the per-package jars and release overlays into the single jar the module publishes.
"""

load("@contrib_rules_jvm//java:defs.bzl", "checkstyle_test", "java_test_suite")
load("@rules_java//java:defs.bzl", "java_import", "java_library")
load("@rules_jvm_external//:defs.bzl", "maven_export")
load("//:version.bzl", "VERSION")
load("//build-tools/bazel:external_deps.bzl", "external_deps")
load("//build-tools/bazel:java_defs.bzl", "JQ_PLUGINS", "javacopts")
load("//build-tools/bazel:jpms.bzl", "java_compile_jars", "merge_package_jars", "overlay_jars")
load("//build-tools/bazel:maven_artifact.bzl", "executable_maven_artifact", "maven_artifact")
load("//build-tools/bazel:osgi.bzl", "osgi_bundle")
load("//build-tools/bazel:publish.bzl", "publish_all", "publish_prebuilt_jar")

GROUP_ID = "net.thisptr.jackson.jq.v2"

def coordinates(artifact_id):
    """Maven coordinates for a module of this project."""
    return "%s:%s:%s" % (GROUP_ID, artifact_id, VERSION)

# Dependencies the Maven parent pom gives to every module.
INHERITED_DEPS = [
    "@maven//:com_google_auto_service_auto_service_annotations",
    "@maven//:com_google_errorprone_error_prone_annotations",
    "@maven//:org_jspecify_jspecify",
]

def jjq_java_mrjar(
        name,
        packages = [],
        resources = [],
        resource_strip_prefix = None,
        base = None,
        base_name = None,
        release_overlays = {},
        multi_release = True,
        osgi = False,
        osgi_exports = None,
        osgi_provide = None,
        osgi_require = None,
        artifact_id = None,
        testonly = False,
        **kwargs):
    """Assembles a multi-release jar from base packages and release overlays.

    Args:
      name: target name for the finished jar filegroup.
      packages: the module's compiled base `jjq_java_library` targets.
      resources: files for src/main/resources; none by default.
      resource_strip_prefix: jar-relative root of `resources`; defaults to the module's
        src/main/resources.
      base: optional explicit base jar target. If not provided, compiled from `packages`.
      base_name: name of the merged base jar target (defaults to `<name>-merged`).
      release_overlays: mapping of Java releases to lists of overlay jars.
      multi_release: whether this is a multi-release jar (default True).
      osgi: whether to generate an OSGi bundle manifest.
      osgi_exports: OSGi Export-Package instruction.
      osgi_provide: OSGi Provide-Capability instruction.
      osgi_require: OSGi Require-Capability instruction.
      artifact_id: Maven artifactId when calculating derived dependencies/manifests.
      testonly: whether this target is testonly.
      **kwargs: passed through to the base java_library.
    """
    if resource_strip_prefix == None and resources:
        resource_strip_prefix = native.package_name() + "/src/main/resources"

    mod_name = artifact_id or (name[:-6] if name.endswith("-mrjar") else name)
    module_coordinates = coordinates(mod_name)

    overlays = [
        overlay
        for release in sorted(release_overlays.keys())
        for overlay in release_overlays[release]
    ]
    content = packages + overlays
    derived_deps = []
    if content:
        external_deps(
            name = name + "-external-deps",
            coordinates = module_coordinates,
            # `exports`, not a name of its own: this is the one edge the IntelliJ Bazel aspect
            # follows from the module target back to the packages the merged jar was built from.
            # merge_package_jars hides them behind `packages`, which no IDE aspect reads, and the
            # IDE blanks every class out of an in-project jar whose source it can see -- so
            # without this edge nothing supplies them and cross-module references go unresolved.
            exports = content,
            # Prevent rules_jvm_external from treating the dependency-only JavaInfo
            # as content. MavenHintInfo carries the actual dependency metadata, and the
            # tag also stops has_maven_deps before it ever reads `exports` above.
            tags = ["no-maven"],
            testonly = testonly,
        )
        derived_deps = [":" + name + "-external-deps"]

    if base:
        base_jar = base
    else:
        merged_name = base_name if base_name else (name[:-6] + "-merged" if name.endswith("-mrjar") else name + "-merged")

        merge_inputs = list(packages)
        if resources:
            library_name = merged_name + "-lib"
            java_library(
                name = library_name,
                srcs = [],
                resource_strip_prefix = resource_strip_prefix,
                resources = resources,
                testonly = testonly,
                **kwargs
            )
            merge_inputs.append(":" + library_name)

        merge_package_jars(
            name = merged_name + "_rule",
            out = merged_name + ".jar",
            packages = merge_inputs,
            testonly = testonly,
        )
        base_jar = ":" + merged_name + ".jar"

    merge_package_jars(
        name = name + "-src_rule",
        out = name + "-src.jar",
        no_manifest = True,
        output_group = "_direct_source_jars",
        packages = packages,
        testonly = testonly,
    )

    if release_overlays:
        overlay_jars(
            name = name,
            base = base_jar,
            multi_release = multi_release,
            release_overlays = release_overlays,
        )
        final_jar = ":" + name + "_assembled"
    else:
        final_jar = base_jar

    if osgi:
        osgi_bundle(
            name = name + "_osgi",
            input_jar = final_jar,
            artifact_id = mod_name,
            deps = derived_deps,
            exports = osgi_exports,
            provide = osgi_provide,
            require = osgi_require,
            version = VERSION,
            multi_release = multi_release,
        )
        final_jar = ":" + name + "_osgi"

    # The module's JavaInfo: the finished jar, the source jar merged from the same packages,
    # and the external dependencies derived from them. `jjq_maven_artifact` reads all three off
    # this one target.
    #
    # The deps are exported, not merely depended on, because `external_deps` merges every
    # coordinate-carrying boundary it finds -- sibling jackson-jq modules included -- and the
    # build has always let a dependent see those transitively. They are also what gives the
    # generated pom Maven's `compile` scope rather than `runtime`.
    java_import(
        name = name,
        jars = [final_jar],
        srcjar = ":" + name + "-src.jar",
        testonly = testonly,
        deps = derived_deps,
        exports = derived_deps,
    )

def jjq_maven_artifact(
        name,
        artifact,
        artifact_id = None,
        doc_excluded_packages = [],
        pom_template = "//build-tools/bazel:pom.tpl",
        publish_prebuilt = False,
        executable = False):
    """Prepares a Maven artifact with coordinates and publication targets.

    Args:
      name: target name; the module's JavaInfo target, and what other modules depend on.
      artifact: the target providing the artifact's JavaInfo -- its jar, its source jar, and
        the dependency edges the pom is derived from.
      artifact_id: Maven artifactId when it differs from `name`.
      doc_excluded_packages: packages to exclude from generated javadocs.
      pom_template: template for pom.xml.
      publish_prebuilt: whether to publish the artifact's jar as built, without running
        maven_export's repacking.
      executable: whether the artifact is an executable uber jar. The module target then runs
        it, so `bazel run //<module>` is the published CLI.
    """
    if executable and not publish_prebuilt:
        fail("executable = True publishes an uber jar as built; maven_export cannot repack " +
             "one, so set publish_prebuilt = True.")

    module_coordinates = coordinates(artifact_id or name)
    tags = ["maven_coordinates=" + module_coordinates]

    if executable:
        executable_maven_artifact(
            name = name,
            exports = [artifact],
            tags = tags,
        )
    else:
        maven_artifact(
            name = name,
            exports = [artifact],
            tags = tags,
        )

    if publish_prebuilt:
        # java_import republishes its srcjar as this output group, so the sources
        # classifier comes off the artifact rather than being passed in beside it.
        native.filegroup(
            name = name + "-sources",
            srcs = [artifact],
            output_group = "_source_jars",
        )
        publish_prebuilt_jar(
            name = name,
            artifact = artifact,
            coordinates = module_coordinates,
            excluded_packages = doc_excluded_packages,
            javadoc_library = artifact,
            pom_library = ":" + name,
            sources = ":" + name + "-sources",
        )
    else:
        # Everything a module publishes -- artifact, sources, javadoc, pom -- is derived
        # from the module target's JavaInfo. Merging it reproduces the finished jar entry
        # for entry, including the OSGi manifest and the multi-release overlays.
        #
        # module-info.class is allowlisted because the merge drops every entry whose
        # name a Maven dependency also uses, comparing names rather than contents. A
        # multi-release module keeps its descriptor at META-INF/versions/N, a path no
        # dependency collides on, but a module that is not multi-release keeps it at the
        # jar root -- and would silently lose it to a dependency's own descriptor.
        maven_export(
            name = name + "_mvn",
            allowed_duplicate_names = ["module-info.class"],
            doc_excluded_packages = doc_excluded_packages,
            lib_name = name,
            maven_coordinates = module_coordinates,
            pom_template = pom_template,
        )
        publish_all(
            name = name + ".publish",
            publishers = [":" + name + "_mvn.publish"],
        )

# A source directory is an implementation detail of its Maven module: it is visible to that
# module and nothing else. Code outside depends on the module's own target instead, so the
# published artifact and the build graph tell the same story about what a module offers. A
# package that genuinely has to cross the module boundary gets an explicit alias at the
# module root, as //jackson-jq-json-provider:contract-test does.
#
# `visibility` and `release` are mandatory rather than defaulted so that every BUILD.bazel
# states the boundary it sits on and the bytecode level it targets, and changing either is
# a visible edit rather than a silent omission.
def jjq_java_library(
        name,
        visibility,
        srcs,
        release,
        deps = [],
        checkstyle_srcs = None,
        **kwargs):
    """The checkstyle-checked java_library for a single Java source directory.

    Lives in the directory it compiles, so `srcs` is just the .java files next to it and a
    dependency on another package is a dependency on another Bazel target -- which is what
    makes a package cycle a build error rather than a latent tangle. See
    docs/package-cycles.md.

    Args:
      name: target name; use the directory name, so `//path/to/dir` resolves to it.
      visibility: required, and expected to name the enclosing module -- see above.
      srcs: Java sources, normally the .java files beside this BUILD.bazel. A recursive
        source set is supported for intentionally unsplit modules.
      release: required --release level for this source directory.
      deps: what this directory depends on, beyond the inherited annotations.
      checkstyle_srcs: handwritten Java sources to check; defaults to `srcs`.
      **kwargs: passed through to java_library.
    """
    java_library(
        name = name,
        srcs = srcs,
        javacopts = javacopts(release),
        plugins = JQ_PLUGINS,
        visibility = visibility,
        deps = deps + INHERITED_DEPS,
        **kwargs
    )

    if checkstyle_srcs == None:
        checkstyle_srcs = [src for src in srcs if not src.endswith("module-info.java")]

    if checkstyle_srcs:
        checkstyle_test(
            name = name + "-checkstyle",
            srcs = checkstyle_srcs,
            config = "//build-tools/checkstyle:checkstyle-config",
        )

def jjq_java_module_info(
        module_name,
        module_packages,
        release,
        visibility,
        name = "module-info",
        module_info_src = "module-info.java",
        deps = [],
        **kwargs):
    """Compiles module-info.java against unmerged module packages.

    Constructs:
    - `--patch-module <module_name>=<pkg1_compile_jar>:<pkg2_compile_jar>:...`
    - `--module-path <dep1_compile_jar>:<dep2_compile_jar>:...`

    Args:
      module_name: JPMS module name declared in module-info.java.
      module_packages: unmerged package targets patched into this module.
      release: required --release level for compiling module-info.java.
      visibility: visibility of the target.
      name: target name (defaults to "module-info").
      module_info_src: path to the module descriptor (defaults to "module-info.java").
      deps: external dependencies on the module path.
      **kwargs: passed through to java_library.
    """
    all_deps = deps + INHERITED_DEPS

    pkg_targets = []
    for index, pkg in enumerate(module_packages):
        target = "%s-pkg-%s" % (name, index)
        java_compile_jars(
            name = target,
            library = pkg,
        )
        pkg_targets.append(":" + target)

    dep_targets = []
    for index, dep in enumerate(all_deps):
        target = "%s-dep-%s" % (name, index)
        java_compile_jars(
            name = target,
            library = dep,
        )
        dep_targets.append(":" + target)

    patch_module_path = ":".join(["$(location %s)" % t for t in pkg_targets])
    module_path = ":".join(["$(location %s)" % t for t in dep_targets])

    javac_opts = [
        "--patch-module",
        "%s=%s" % (module_name, patch_module_path),
    ]
    if dep_targets:
        javac_opts.extend(["--module-path", module_path])

    java_library(
        name = name,
        srcs = [module_info_src],
        javacopts = javacopts(release, javac_opts),
        plugins = JQ_PLUGINS,
        visibility = visibility,
        deps = dep_targets + pkg_targets + all_deps + module_packages,
        **kwargs
    )

def jjq_java_test_suite(
        visibility,
        srcs,
        release,
        name = "tests",
        deps = [],
        resources = [],
        resource_strip_prefix = None,
        data = [],
        env = {},
        jvm_flags = [],
        size = None,
        test_suffixes_excludes = []):
    """The checkstyle-checked JUnit 5 tests for a single Java source directory.

    Helpers shared with a sibling test package are reachable as that package's
    `:tests-test-lib`, which java_test_suite generates from its non-test sources.

    Test sources are held to the same checkstyle config as main sources, so `srcs` here
    means the same thing to checkstyle as `jjq_java_library`'s does.

    `visibility` and `release` are mandatory for the same reason as in `jjq_java_library`.

    Args:
      visibility: visibility of the test suite.
      srcs: java test sources.
      release: required --release level for compilation.
      name: test suite name.
      deps: dependencies for compiling and running tests. AssertJ and JUnit 5
        dependencies (API, params, launcher, engine) are included automatically.
      resources: test resource files. A directory under src/test/java leaves these empty
        and depends on the module's resources library instead, because test resources stay
        at the module root.
      resource_strip_prefix: jar-relative root of `resources`; defaults to the module's
        src/test/resources.
      data: runtime data dependencies.
      env: environment variables for tests.
      jvm_flags: JVM flags for each generated test.
      size: test size (small, medium, large, enormous).
      test_suffixes_excludes: patterns excluded from runner execution.
    """
    if resource_strip_prefix == None and resources:
        resource_strip_prefix = native.package_name() + "/src/test/resources"

    java_test_suite(
        name = name,
        srcs = srcs,
        runner = "junit5",
        javacopts = javacopts(release),
        plugins = JQ_PLUGINS,
        resources = resources,
        resource_strip_prefix = resource_strip_prefix,
        data = data,
        env = env,
        jvm_flags = jvm_flags,
        size = size,
        test_suffixes_excludes = test_suffixes_excludes,
        visibility = visibility,
        deps = deps + INHERITED_DEPS + [
            "@maven//:org_assertj_assertj_core",
            "@maven//:org_junit_jupiter_junit_jupiter_api",
            "@maven//:org_junit_jupiter_junit_jupiter_params",
            "@maven//:org_junit_platform_junit_platform_launcher",
            "@maven//:org_junit_platform_junit_platform_reporting",
            "@maven//:org_slf4j_slf4j_api",
        ],
        runtime_deps = [
            "@maven//:org_junit_jupiter_junit_jupiter_engine",
            "@maven//:org_slf4j_slf4j_simple",
        ],
    )

    checkstyle_test(
        name = name + "-checkstyle",
        srcs = srcs,
        config = "//build-tools/checkstyle:checkstyle-config",
    )
