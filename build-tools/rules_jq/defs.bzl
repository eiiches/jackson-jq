"""Build rules for platform-specific jq binaries."""

load("@bazel_skylib//rules:copy_file.bzl", "copy_file")

_ALL_VERSIONS = [
    "1.5",
    "1.6",
    "1.7",
    "1.7.1",
    "1.8.0",
    "1.8.1",
    "1.8.2",
]

_ARM64_VERSIONS = _ALL_VERSIONS[2:]

_PLATFORMS = {
    "linux_amd64": ["@platforms//os:linux", "@platforms//cpu:x86_64"],
    "linux_arm64": ["@platforms//os:linux", "@platforms//cpu:arm64"],
    "macos_amd64": ["@platforms//os:osx", "@platforms//cpu:x86_64"],
    "macos_arm64": ["@platforms//os:osx", "@platforms//cpu:arm64"],
}

def _jq_directory_impl(ctx):
    output = ctx.actions.declare_directory(ctx.label.name)
    args = ctx.actions.args()
    args.add(output.path)
    args.add_all(ctx.files.srcs)
    ctx.actions.run_shell(
        arguments = [args],
        command = """set -eu
output="$1"
shift
mkdir -p "$output"
for src in "$@"; do
  name="${src##*/}"
  cp "$src" "$output/$name"
  chmod +x "$output/$name"
done
""",
        inputs = ctx.files.srcs,
        outputs = [output],
    )
    return [DefaultInfo(files = depset([output]))]

_jq_directory = rule(
    implementation = _jq_directory_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = True),
    },
)

def _config(platform):
    return ":" + platform

def _repository(version, platform):
    return "@jq_%s_%s//file" % (version.replace(".", "_"), platform)

def _supported_platforms(version):
    if version in _ARM64_VERSIONS:
        return _PLATFORMS.keys()
    return ["linux_amd64", "macos_amd64"]

def _selected_source(version):
    choices = {
        _config(platform): _repository(version, platform)
        for platform in _supported_platforms(version)
    }

    # Bazel resolves configurable attributes before compatibility. The fallback
    # keeps unsupported configurations analyzable; target_compatible_with then
    # prevents the foreign binary from being built or consumed.
    choices["//conditions:default"] = _repository(version, "linux_amd64")
    return select(choices)

def _compatibility(version):
    choices = {
        _config(platform): []
        for platform in _supported_platforms(version)
    }
    choices["//conditions:default"] = ["@platforms//:incompatible"]
    return select(choices)

def _supported_platform_compatibility():
    choices = {
        _config(platform): []
        for platform in _PLATFORMS
    }
    choices["//conditions:default"] = ["@platforms//:incompatible"]
    return select(choices)

def jq_binaries(name):
    """Declares native jq executables and their aggregate filegroup.

    Args:
      name: Name of the aggregate filegroup.
    """
    for platform, constraints in _PLATFORMS.items():
        native.config_setting(
            name = platform,
            constraint_values = constraints,
        )

    for version in _ALL_VERSIONS:
        copy_file(
            name = "copy-jq-" + version,
            src = _selected_source(version),
            out = "jq-" + version,
            is_executable = True,
            target_compatible_with = _compatibility(version),
            testonly = True,
        )

    x86_sources = [":copy-jq-" + version for version in _ALL_VERSIONS]
    arm64_sources = [":copy-jq-" + version for version in _ARM64_VERSIONS]
    _jq_directory(
        name = name,
        srcs = select({
            _config("linux_amd64"): x86_sources,
            _config("linux_arm64"): arm64_sources,
            _config("macos_amd64"): x86_sources,
            _config("macos_arm64"): arm64_sources,
            "//conditions:default": [],
        }),
        target_compatible_with = _supported_platform_compatibility(),
        testonly = True,
    )

def jq_binaries_rootpath():
    """Returns a Bazel rootpath expression for the jq binaries directory."""
    return "$(rootpath @jq_binaries//:all)"
