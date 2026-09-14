"""Small JPMS helpers for jackson-jq's modular and multi-release jars."""

load("@rules_java//java:defs.bzl", "JavaInfo")

def _java_compile_jars_impl(ctx):
    java_info = ctx.attr.library[JavaInfo]
    compile_jars = [
        output.compile_jar or output.class_jar
        for output in java_info.java_outputs
    ]
    return [
        DefaultInfo(files = depset(compile_jars)),
        java_info,
    ]

java_compile_jars = rule(
    implementation = _java_compile_jars_impl,
    attrs = {
        "library": attr.label(mandatory = True, providers = [JavaInfo]),
    },
)

def _merge_package_jars_impl(ctx):
    out = ctx.outputs.out
    args = ctx.actions.args()
    args.add("--output", out)
    if ctx.attr.no_manifest:
        args.add("--no-manifest")

    inputs = []
    output_group = ctx.attr.output_group
    for pkg in ctx.attr.packages:
        if output_group == "_direct_source_jars":
            if JavaInfo in pkg and pkg[JavaInfo].source_jars:
                for f in pkg[JavaInfo].source_jars:
                    inputs.append(f)
                    args.add("--input", f)
            elif OutputGroupInfo in pkg and hasattr(pkg[OutputGroupInfo], "_direct_source_jars"):
                for f in getattr(pkg[OutputGroupInfo], "_direct_source_jars").to_list():
                    inputs.append(f)
                    args.add("--input", f)
        elif output_group:
            if OutputGroupInfo in pkg and hasattr(pkg[OutputGroupInfo], output_group):
                for f in getattr(pkg[OutputGroupInfo], output_group).to_list():
                    inputs.append(f)
                    args.add("--input", f)
        else:
            for f in pkg.files.to_list():
                inputs.append(f)
                args.add("--input", f)

    ctx.actions.run(
        executable = ctx.executable._multi_release_jar,
        arguments = [args],
        inputs = inputs,
        outputs = [out],
        mnemonic = "MergePackageJars",
    )
    return [DefaultInfo(files = depset([out]))]

merge_package_jars = rule(
    implementation = _merge_package_jars_impl,
    attrs = {
        "no_manifest": attr.bool(default = False),
        "out": attr.output(mandatory = True),
        "output_group": attr.string(default = ""),
        "packages": attr.label_list(mandatory = True),
        "_multi_release_jar": attr.label(
            default = "//build-tools/bazel/tools:multi_release_jar",
            cfg = "exec",
            executable = True,
        ),
    },
)

def overlay_jars(
        name,
        base,
        release_overlays,
        multi_release = True):
    """Combines Java release overlay jars with a base Java jar.

    Args:
      name: target name.
      base: base Java jar without module descriptor.
      release_overlays: mapping of Java releases to lists of overlay jars.
      multi_release: whether to assemble as a multi-release jar.
    """
    overlay_labels = [
        overlay
        for release in sorted(release_overlays.keys())
        for overlay in release_overlays[release]
    ]
    if multi_release:
        overlay_args = "".join([
            " --overlay %s=$(location %s)" % (version, overlay)
            for version in sorted(release_overlays.keys())
            for overlay in release_overlays[version]
        ])
        native.genrule(
            name = name + "_assembled",
            srcs = [base] + overlay_labels,
            outs = [name + ".jar"],
            cmd = "$(location //build-tools/bazel/tools:multi_release_jar)" +
                  " --base $(location %s)" % base +
                  " --multi-release" +
                  " --output $@" + overlay_args,
            tools = ["//build-tools/bazel/tools:multi_release_jar"],
        )
        return

    overlay_args = "".join([
        " --input $(location %s)" % overlay
        for overlay in overlay_labels
    ])
    native.genrule(
        name = name + "_assembled",
        srcs = [base] + overlay_labels,
        outs = [name + ".jar"],
        cmd = "$(location //build-tools/bazel/tools:multi_release_jar)" +
              " --base $(location %s)" % base +
              " --output $@" + overlay_args,
        tools = ["//build-tools/bazel/tools:multi_release_jar"],
    )
