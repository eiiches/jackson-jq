"""JavaCC parser generation.

Replaces javacc-maven-plugin. The generated sources are compiled as part of the
consuming java_library (as Maven does): the grammar references
net.thisptr.jackson.jq.v2.core.internal.ast.* and ...spi.Version, so the parser cannot
live in a library of its own.
"""

def javacc(name, grammar, package, generated_files, patches = [], **kwargs):
    """Generates a JavaCC parser.

    Args:
      name: target name; the generated .java files are its outputs.
      grammar: the .jj grammar file.
      package: Java package the grammar declares, e.g. "com.example.parser".
      generated_files: the .java file names JavaCC emits for this grammar. Listing them
        explicitly keeps the action hermetic; a grammar change that adds a file is then
        a deliberate BUILD edit rather than a silently dropped source.
      patches: unified diffs applied, in order, to the generated sources. They exist for
        edits JavaCC's own templates cannot express -- the grammar controls the parser,
        not the support classes emitted beside it. Each diff is taken against the output
        of the JavaCC version pinned in MODULE.bazel and must be regenerated when that
        version changes: `patch` exits non-zero when a hunk no longer applies, so a
        version bump fails the build rather than silently dropping the edit. `patch`
        itself is a host tool (POSIX; present on both the ubuntu and macos CI runners).
      **kwargs: passed through to the underlying genrule.

    The output path deliberately contains a "generated-sources" segment so that the
    -XepExcludedPaths:.*/generated-sources/.* flag inherited from the Maven build keeps
    exempting this code from Error Prone.
    """
    outdir = "generated-sources/javacc/" + package.replace(".", "/")
    cmd = "$(execpath //build-tools/bazel/tools:javacc)" + \
          " -OUTPUT_DIRECTORY=$(RULEDIR)/" + outdir + \
          " $(execpath %s)" % grammar
    for patch in patches:
        # -F 0 is what makes a stale diff fail: patch's default fuzz of 2 would otherwise
        # let a hunk land on drifted context, and the hunks here are mostly insertions,
        # whose every line is context. </dev/null turns any prompt into an error rather
        # than a hang. -i has to be absolute: patch honours -d before it opens the diff,
        # so a path relative to the exec root would resolve inside the output directory.
        cmd += " && patch -s --no-backup-if-mismatch -F 0 -p1" + \
               " -d $(RULEDIR)/" + outdir + \
               " -i $$PWD/$(execpath %s)" % patch + \
               " </dev/null"
    native.genrule(
        name = name,
        srcs = [grammar] + patches,
        outs = ["%s/%s" % (outdir, f) for f in generated_files],
        cmd = cmd,
        tools = ["//build-tools/bazel/tools:javacc"],
        **kwargs
    )
