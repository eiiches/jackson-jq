"""OSGi manifest generation without rebuilding the multi-release jar."""

def _osgi_bundle_impl(ctx):
    args = ctx.actions.args()
    args.add("--input", ctx.file.input_jar)
    args.add("--output", ctx.outputs.out)
    args.add_all(ctx.files.deps, before_each = "--classpath")
    for key in sorted(ctx.attr.properties.keys()):
        args.add("--property", "%s=%s" % (key, ctx.attr.properties[key]))
    ctx.actions.run(
        executable = ctx.executable._bnd_manifest,
        arguments = [args],
        inputs = [ctx.file.input_jar] + ctx.files.deps,
        outputs = [ctx.outputs.out],
        mnemonic = "OsgiBundle",
    )
    return [DefaultInfo(files = depset([ctx.outputs.out]))]

_osgi_bundle = rule(
    implementation = _osgi_bundle_impl,
    attrs = {
        "_bnd_manifest": attr.label(
            default = "//build-tools/bazel/tools:bnd_manifest",
            cfg = "exec",
            executable = True,
        ),
        "deps": attr.label_list(),
        "input_jar": attr.label(allow_single_file = True, mandatory = True),
        "out": attr.output(mandatory = True),
        "properties": attr.string_dict(),
    },
)

def osgi_bundle(name, input_jar, artifact_id, deps, version, multi_release, exports = None, provide = None, require = None):
    """Generates an OSGi bundle jar using bnd.

    Args:
      name: target name.
      input_jar: input jar to convert to OSGi bundle.
      artifact_id: Maven artifactId for bundle headers.
      deps: dependencies required for classpath scanning.
      version: version string for Bundle-Version header.
      multi_release: whether the bundle is multi-release.
      exports: Export-Package instruction string.
      provide: Provide-Capability instruction string.
      require: Require-Capability instruction string.
    """
    properties = {
        "Bundle-Description": "jq for Jackson JSON Processor",
        "Bundle-License": "http://www.apache.org/licenses/LICENSE-2.0.txt",
        "Bundle-ManifestVersion": "2",
        "Bundle-Name": "net.thisptr.jackson.jq.v2:%s" % artifact_id,
        "Bundle-SymbolicName": "net.thisptr.jackson.jq.v2.%s" % artifact_id,
        "Bundle-Version": version.replace("-", "."),
        "Import-Package": "org.jspecify.annotations;resolution:=optional,*",
        # Bazel stamps its own provenance onto the jars it builds. Those headers name
        # internal build targets and have no business in a published artifact.
        "-removeheaders": "Target-Label,Originally-Created-By",
    }
    if multi_release:
        properties["Multi-Release"] = "true"
    if exports != None:
        properties["Export-Package"] = exports
    if provide != None:
        properties["Provide-Capability"] = provide
    if require != None:
        properties["Require-Capability"] = require

    _osgi_bundle(
        name = name,
        input_jar = input_jar,
        deps = deps,
        out = name + ".jar",
        properties = properties,
    )
