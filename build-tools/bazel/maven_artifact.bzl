"""The module target a Maven artifact is published from.

The target named after a module has to be two things at once: the JavaInfo the rest of the
build depends on, and the carrier of the `maven_coordinates=` tag, because that tag is what
rules_jvm_external's has_maven_deps aspect reads off the rule to decide what a pom says. A
`java_import` can be both, but only by owning the jar itself -- it rejects an empty `jars`
-- and the jar already belongs to the target that built it. These rules forward that target
instead, and `executable_maven_artifact` additionally runs it.
"""

load("@rules_java//java:defs.bzl", "JavaInfo")

_LAUNCHER = """\
#!/usr/bin/env bash
set -euo pipefail
# `bazel run` does not export RUNFILES_DIR for a plain executable rule, so fall back to the
# tree beside this script -- the same order java_binary's own stub uses.
runfiles="${{RUNFILES_DIR:-}}"
if [[ -z $runfiles ]]; then
  self="$0"
  [[ $self == /* ]] || self="$PWD/$self"
  runfiles="$self.runfiles"
fi
exec {java} -jar "$runfiles/{jar}" "$@"
"""

# `exports`, not `deps`: has_maven_deps walks deps/exports/runtime_deps, and a dependency
# reached through `exports` is the only one that comes out of the pom with Maven's `compile`
# scope rather than `runtime` (format_dep in rules_jvm_external's maven_utils.bzl). It is a
# list because that aspect iterates the attribute directly, but exactly one target belongs
# in it: the artifact this module publishes.
_EXPORTS = {
    "exports": attr.label_list(
        doc = "The artifact this module publishes, as a single JavaInfo target.",
        mandatory = True,
        providers = [JavaInfo],
    ),
}

def _artifact(ctx):
    if len(ctx.attr.exports) != 1:
        fail("exports must name exactly one target, the artifact this module publishes; got %d" %
             len(ctx.attr.exports))
    return ctx.attr.exports[0]

def _artifact_jar(artifact):
    jars = artifact[DefaultInfo].files.to_list()
    if len(jars) != 1:
        fail("an executable artifact must be a single jar; %s builds %d files" %
             (artifact.label, len(jars)))
    return jars[0]

def _runfiles_path(workspace_name, short_path):
    """Turns a short_path into a path relative to the root of the runfiles tree.

    A short_path is relative to the main repository's directory inside that tree, so an
    external repository's starts with "../". Stripping that rather than letting it resolve
    keeps the launcher off ".." segments, which a tree built out of symlinks does not
    always traverse the way the text suggests.
    """
    if short_path.startswith("../"):
        return short_path[len("../"):]
    return workspace_name + "/" + short_path

def _maven_artifact_impl(ctx):
    artifact = _artifact(ctx)

    # Forwarded rather than merged, so the module target and the artifact describe the same
    # jar, source jar and dependency edges down to the provider instance.
    return [
        DefaultInfo(files = artifact[DefaultInfo].files),
        artifact[JavaInfo],
    ]

def _executable_maven_artifact_impl(ctx):
    artifact = _artifact(ctx)
    jar = _artifact_jar(artifact)
    java_runtime = ctx.toolchains["@bazel_tools//tools/jdk:runtime_toolchain_type"].java_runtime

    # An absolute path means a local JDK, which is not in the runfiles at all.
    java_path = java_runtime.java_executable_runfiles_path
    java = java_path if java_path.startswith("/") else "\"$runfiles/%s\"" % _runfiles_path(
        ctx.workspace_name,
        java_path,
    )

    launcher = ctx.actions.declare_file(ctx.label.name)
    ctx.actions.write(
        output = launcher,
        content = _LAUNCHER.format(
            java = java,
            jar = _runfiles_path(ctx.workspace_name, jar.short_path),
        ),
        is_executable = True,
    )

    return [
        DefaultInfo(
            executable = launcher,
            files = depset([jar, launcher]),
            runfiles = ctx.runfiles(files = [jar])
                .merge(ctx.runfiles(transitive_files = java_runtime.files)),
        ),
        artifact[JavaInfo],
    ]

maven_artifact = rule(
    implementation = _maven_artifact_impl,
    doc = "The module target for a module published from a library jar.",
    attrs = _EXPORTS,
)

executable_maven_artifact = rule(
    implementation = _executable_maven_artifact_impl,
    doc = """The module target for a module published as an executable uber jar.

Runs the published jar directly -- no stub and no classpath, because such an artifact is
already self-contained -- so `bazel run //<module>` exercises exactly what Maven Central
serves.
""",
    attrs = _EXPORTS,
    executable = True,
    toolchains = ["@bazel_tools//tools/jdk:runtime_toolchain_type"],
)
