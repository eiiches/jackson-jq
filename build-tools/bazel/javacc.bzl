"""JavaCC parser generation.

Replaces javacc-maven-plugin. The generated sources are compiled as part of the
consuming java_library (as Maven does): the grammar references
net.thisptr.jackson.jq.v2.core.internal.ast.* and ...spi.Version, so the parser cannot
live in a library of its own.
"""

def javacc(name, grammar, package, generated_files, **kwargs):
    """Generates a JavaCC parser.

    Args:
      name: target name; the generated .java files are its outputs.
      grammar: the .jj grammar file.
      package: Java package the grammar declares, e.g. "com.example.parser".
      generated_files: the .java file names JavaCC emits for this grammar. Listing them
        explicitly keeps the action hermetic; a grammar change that adds a file is then
        a deliberate BUILD edit rather than a silently dropped source.
      **kwargs: passed through to the underlying genrule.

    The output path deliberately contains a "generated-sources" segment so that the
    -XepExcludedPaths:.*/generated-sources/.* flag inherited from the Maven build keeps
    exempting this code from Error Prone.
    """
    outdir = "generated-sources/javacc/" + package.replace(".", "/")
    native.genrule(
        name = name,
        srcs = [grammar],
        outs = ["%s/%s" % (outdir, f) for f in generated_files],
        cmd = "$(execpath //build-tools/bazel/tools:javacc)" +
              " -OUTPUT_DIRECTORY=$(RULEDIR)/" + outdir +
              " $(execpath %s)" % grammar,
        tools = ["//build-tools/bazel/tools:javacc"],
        **kwargs
    )
