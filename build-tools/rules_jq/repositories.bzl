"""Repository rules for downloading jq release binaries."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

_JQ_BINARIES = [
    ("1.5", "linux_amd64", "jq-linux64", "c6b3a7d7d3e7b70c6f51b706a3b90bd01833846c54d32ca32f0027f00226ff6d"),
    ("1.5", "macos_amd64", "jq-osx-amd64", "386e92c982a56fe4851468d7a931dfca29560cee306a0e66c6a1bd4065d3dac5"),
    ("1.6", "linux_amd64", "jq-linux64", "af986793a515d500ab2d35f8d2aecd656e764504b789b66d7e1a0b727a124c44"),
    ("1.6", "macos_amd64", "jq-osx-amd64", "5c0a0a3ea600f302ee458b30317425dd9632d1ad8882259fcaf4e9b868b2b1ef"),
    ("1.7", "linux_amd64", "jq-linux-amd64", "2f312b9587b1c1eddf3a53f9a0b7d276b9b7b94576c85bda22808ca950569716"),
    ("1.7", "linux_arm64", "jq-linux-arm64", "ab57ee39075db4a23f899d396ecef3c6e58f6aada35bfee472468210bd126940"),
    ("1.7", "macos_amd64", "jq-macos-amd64", "4b4568fb5c6cbb8f1a8f640ff601c6d2bbecf9fa8f2cf796c7482e7054a01c41"),
    ("1.7", "macos_arm64", "jq-macos-arm64", "b8d313fd4f3bd8a0b338b9ddd1c006fccf9312dc609a20d2d6e4bad964aa99f9"),
    ("1.7.1", "linux_amd64", "jq-linux-amd64", "5942c9b0934e510ee61eb3e30273f1b3fe2590df93933a93d7c58b81d19c8ff5"),
    ("1.7.1", "linux_arm64", "jq-linux-arm64", "4dd2d8a0661df0b22f1bb9a1f9830f06b6f3b8f7d91211a1ef5d7c4f06a8b4a5"),
    ("1.7.1", "macos_amd64", "jq-macos-amd64", "4155822bbf5ea90f5c79cf254665975eb4274d426d0709770c21774de5407443"),
    ("1.7.1", "macos_arm64", "jq-macos-arm64", "0bbe619e663e0de2c550be2fe0d240d076799d6f8a652b70fa04aea8a8362e8a"),
    ("1.8.0", "linux_amd64", "jq-linux-amd64", "8926c33326111bcd67a47a970b5a5db933ef9194ad925994934c639c76a0605c"),
    ("1.8.0", "linux_arm64", "jq-linux-arm64", "1084e6bf5060a463daf77193888d326c83e56bcfbc18a52e6eaa99dbe82a8b54"),
    ("1.8.0", "macos_amd64", "jq-macos-amd64", "a594f3740bf570f0dbc43ff102a9034c17719d1bb5b40f0192751234d67f172a"),
    ("1.8.0", "macos_arm64", "jq-macos-arm64", "aaf1efbb376d6e3eaf61f63807c32c1df519f5857dfc4f581826fa2df4b715ae"),
    ("1.8.1", "linux_amd64", "jq-linux-amd64", "020468de7539ce70ef1bceaf7cde2e8c4f2ca6c3afb84642aabc5c97d9fc2a0d"),
    ("1.8.1", "linux_arm64", "jq-linux-arm64", "6bc62f25981328edd3cfcfe6fe51b073f2d7e7710d7ef7fcdac28d4e384fc3d4"),
    ("1.8.1", "macos_amd64", "jq-macos-amd64", "e80dbe0d2a2597e3c11c404f03337b981d74b4a8504b70586c354b7697a7c27f"),
    ("1.8.1", "macos_arm64", "jq-macos-arm64", "a9fe3ea2f86dfc72f6728417521ec9067b343277152b114f4e98d8cb0e263603"),
    ("1.8.2", "linux_amd64", "jq-linux-amd64", "b1c22172dd303f3be49e935aa56aa48a8b7a46e0bc838b4997d3bb451495870f"),
    ("1.8.2", "linux_arm64", "jq-linux-arm64", "8b85c817833814ddca00a144c33705546355afccf0cf39b188f3cdb48b852309"),
    ("1.8.2", "macos_amd64", "jq-macos-amd64", "e94b266e3c26690550006abe63152b782280f4e14374accdf04cbde844f00bc0"),
    ("1.8.2", "macos_arm64", "jq-macos-arm64", "2d75340ba57a4b4b4c8708a21c2dc8e958a48aaa8bba13b27f77f6e4c0eca07e"),
]

def _jq_binaries_hub_repository_impl(repository_ctx):
    repository_ctx.symlink(repository_ctx.attr.build_file, "BUILD.bazel")
    repository_ctx.symlink(repository_ctx.attr.defs_file, "defs.bzl")

_jq_binaries_hub_repository = repository_rule(
    implementation = _jq_binaries_hub_repository_impl,
    attrs = {
        "build_file": attr.label(mandatory = True),
        "defs_file": attr.label(mandatory = True),
    },
)

def _repository_name(version, platform):
    return "jq_%s_%s" % (version.replace(".", "_"), platform)

def jq_repositories():
    """Creates lazy download repositories and their public hub repository."""
    for version, platform, asset, sha256 in _JQ_BINARIES:
        http_file(
            name = _repository_name(version, platform),
            downloaded_file_path = "jq",
            executable = True,
            sha256 = sha256,
            urls = ["https://github.com/jqlang/jq/releases/download/jq-%s/%s" % (version, asset)],
        )

    _jq_binaries_hub_repository(
        name = "jq_binaries",
        build_file = Label("//:jq.BUILD.bazel"),
        defs_file = Label("//:defs.bzl"),
    )
