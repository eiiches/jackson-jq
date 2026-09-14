"""Maven publication for artifacts maven_export cannot produce, and the credential wrapper."""

load("@rules_jvm_external//private/rules:javadoc.bzl", "javadoc")
load("@rules_jvm_external//private/rules:maven_publish.bzl", "maven_publish")
load("@rules_jvm_external//private/rules:pom_file.bzl", "pom_file")

# maven_publish substitutes these --define values straight into the publisher script it
# generates, so a credential passed that way is written to bazel-out and cached in the disk
# cache, outliving the release on disk. The environment variables of the same name are read
# at run time and leave nothing behind. rules_jvm_external only warns about this, and only
# for maven_password.
_CREDENTIAL_DEFINES = {
    "maven_password": "MAVEN_PASSWORD",
    "pgp_signing_key": "PGP_SIGNING_KEY",
    "pgp_signing_pwd": "PGP_SIGNING_PWD",
}

# Bazel writes every command's full client environment into the server log kept at
# $(bazel info output_base)/java.log*, as mode 0644 and with no redaction, so a credential
# that reaches `bazel run` is written to disk in the clear and stays there. Nothing in the
# build can scrub it after the fact; the credential simply must not be in Bazel's
# environment. Build the publisher first and execute the built script directly instead --
# it needs no Bazel server, so the secrets go only to the publisher process. Signing keys
# are not listed because signing happens outside Bazel entirely, in the Central bundle step.
_CREDENTIAL_VARIABLES = [
    "MAVEN_PASSWORD",
    "CENTRAL_TOKEN",
]

def publish_prebuilt_jar(
        name,
        coordinates,
        artifact,
        pom_library,
        sources,
        javadoc_library,
        excluded_packages = []):
    """Publishes a finished jar as-is, with a pom, sources and javadoc beside it.

    maven_export always republishes a jar it merges itself, and that merge drops every class
    the module's Maven dependencies also provide. For a library that is the point. For an
    uber jar it is the opposite of the point, so the jar goes to Maven untouched and only
    the metadata is derived from the build graph.

    Args:
      name: module target name.
      coordinates: Maven coordinates string.
      artifact: the finished jar to publish, as built.
      pom_library: target whose JavaInfo the pom's dependencies are read from.
      sources: sources jar to publish under the `sources` classifier.
      javadoc_library: target whose sources the javadoc jar is generated from.
      excluded_packages: packages to exclude from javadoc.
    """
    pom_file(
        name = name + "-pom",
        pom_template = "//build-tools/bazel:pom.tpl",
        target = pom_library,
    )
    javadoc(
        name = name + "-javadoc",
        deps = [javadoc_library],
        excluded_packages = excluded_packages,
    )
    maven_publish(
        name = name + ".publish-impl",
        artifact = artifact,
        classifier_artifacts = {
            sources: "sources",
            ":" + name + "-javadoc": "javadoc",
        },
        coordinates = coordinates,
        pom = ":" + name + "-pom",
    )
    publish_all(
        name = name + ".publish",
        publishers = [":" + name + ".publish-impl"],
    )

def _publish_all_impl(ctx):
    for define, variable in _CREDENTIAL_DEFINES.items():
        if ctx.var.get(define):
            fail("--define %s= writes the credential into the generated publisher script, " % define +
                 "which persists in bazel-out and the disk cache. Set the %s environment " % variable +
                 "variable instead.")

    executable = ctx.actions.declare_file(ctx.label.name)
    ascend = "".join(["/.." for _ in ctx.label.package.split("/") if ctx.label.package])

    # `bazel run` sets BUILD_WORKSPACE_DIRECTORY; a directly executed script does not.
    bazel_run_guard = [
        "if [[ -n ${BUILD_WORKSPACE_DIRECTORY:-} ]]; then",
        "  for _credential in %s; do" % " ".join(_CREDENTIAL_VARIABLES),
        "    if [[ -n ${!_credential:-} ]]; then",
        "      echo \"${_credential} is set under \\`bazel run\\`, which records the whole client\" >&2",
        "      echo \"environment -- credentials included -- in \\$(bazel info output_base)/java.log*.\" >&2",
        "      echo \"Build once and run the script directly instead:\" >&2",
        "      echo \"  bazel build //%s:%s && bazel-bin/%s\" >&2" % (ctx.label.package, ctx.label.name, executable.short_path),
        "      exit 1",
        "    fi",
        "  done",
        "fi",
    ]

    commands = [
        "#!/usr/bin/env bash",
        "set -euo pipefail",
        "export MAVEN_REPO=\"${MAVEN_REPO:-file://${HOME}/.m2/repository}\"",
    ] + bazel_run_guard + [
        # The publishers below are named by short_path and the uploader they exec is named
        # relative to the runfiles root, so both only resolve from inside a runfiles tree.
        # `bazel run` does not export RUNFILES_DIR for a plain executable rule, and the
        # release runs this script directly -- `bazel run` would hand Bazel the credentials
        # -- so locate the tree either beside this script or around it, and work from there.
        "if [[ -n ${RUNFILES_DIR:-} ]]; then",
        "  publisher_root=\"${RUNFILES_DIR}/_main\"",
        "elif [[ -d ${BASH_SOURCE[0]}.runfiles/_main ]]; then",
        "  publisher_root=\"${BASH_SOURCE[0]}.runfiles/_main\"",
        "else",
        "  publisher_root=\"$(dirname -- \"${BASH_SOURCE[0]}\")%s\"" % ascend,
        "fi",
        "cd -- \"${publisher_root}\"",
    ]
    runfiles = ctx.runfiles()
    for publisher in ctx.attr.publishers:
        info = publisher[DefaultInfo]
        commands.append("\"./%s\"" % info.files.to_list()[0].short_path)
        runfiles = runfiles.merge(info.default_runfiles)
    ctx.actions.write(executable, "\n".join(commands) + "\n", is_executable = True)
    return [DefaultInfo(executable = executable, runfiles = runfiles)]

publish_all = rule(
    implementation = _publish_all_impl,
    doc = """Runs the given Maven publishers in order behind one credential-handling wrapper.

Credentials are read from the environment (MAVEN_USER, MAVEN_PASSWORD) and stay in shell
variables: nothing is echoed, and no value is written to a file, so publishing leaves no
credential behind in bazel-out, the disk cache, or a log. Release artifacts are signed
outside Bazel, by the Central bundle step.
""",
    attrs = {
        "publishers": attr.label_list(cfg = "target"),
    },
    executable = True,
)
