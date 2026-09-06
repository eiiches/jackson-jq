"""Shared javac configuration, transcribed from the Maven parent pom.

Kept deliberately close to pom.xml so the two can be diffed by eye during the
migration. Three groups of pom flags are intentionally absent because rules_java
already supplies them:

  * -XDcompilePolicy=simple and --should-stop=ifError=FLOW are in DEFAULT_JAVACOPTS.
  * The ten -J--add-exports/--add-opens into jdk.compiler are in BASE_JDK9_JVM_OPTS;
    they only existed to let Maven's forked javac load Error Prone.
  * -Xplugin:ErrorProne itself: JavaBuilder runs Error Prone natively and consumes
    the -Xep* flags directly (JavacOptions.isBazelSpecificFlag).
"""

LINT_OPTS = [
    "-Xlint:all",
    # -Werror:<categories> here is JavaBuilder's own option, not javac's: JavaBuilder
    # parses and applies it itself (JavacOptions.WErrorOptionNormalizer / WerrorCustomOption),
    # so it works on the toolchain's javac, which rejects the flag outright before JDK 26.
    # javac grew a -Werror:<categories> of its own in JDK 26, where negated categories do not
    # exclude properly (https://bugs.openjdk.org/browse/JDK-8380971); that bug does not apply
    # to JavaBuilder's implementation, which handles `all,-<category>` correctly. Enumerating
    # the categories positively is therefore a choice, not a workaround.
    "-Werror:exports,opens,overrides,overloads,fallthrough,finally,divzero,empty,static,try,cast,synchronization,lossy-conversions,missing-explicit-ctor,dep-ann,removal,restricted",
    "-XDaddTypeAnnotationsToSymbol=true",
]

# Error Prone checks. Every name here is present in Error Prone 2.50.0 (java_tools
# v21.0, pulled in by the rules_java pin in MODULE.bazel). JavaBuilder always passes
# -XepIgnoreUnknownCheckNames, so a name that does not exist is silently ignored rather
# than rejected; keep this list aligned with the pinned Error Prone version.
#
# -Xep:ReturnMissingNullable:ERROR also overrides rules_java DEFAULT_JAVACOPTS, which
# turns that check OFF. Target javacopts are appended last and win.
ERROR_PRONE_OPTS = [
    "-Xep:StringCaseLocaleUsage:ERROR",
    "-Xep:Var:ERROR",
    "-Xep:UnnecessaryFinal:ERROR",
    "-Xep:DefaultCharset:ERROR",
    "-Xep:DeprecatedVariable:ERROR",
    "-Xep:EmptyCatch:ERROR",
    "-Xep:FallThrough:ERROR",
    "-Xep:ImmutableEnumChecker:ERROR",
    "-Xep:IncorrectMainMethod:ERROR",
    "-Xep:InconsistentHashCode:ERROR",
    "-Xep:MissingOverride:ERROR",
    "-Xep:WildcardImport:ERROR",
    "-Xep:RemoveUnusedImports:ERROR",
    "-Xep:Finally:ERROR",
    "-Xep:BadInstanceof:ERROR",
    "-Xep:BadImport:ERROR",
    "-Xep:RedundantNullCheck:ERROR",
    "-Xep:ExposedPrivateType:ERROR",
    "-Xep:CanonicalDuration:ERROR",
    "-Xep:JdkObsolete:ERROR",
    "-Xep:UnnecessaryParentheses:ERROR",
    "-Xep:OperatorPrecedence:ERROR",
    "-Xep:FloatCast:ERROR",
    "-Xep:ReturnAtTheEndOfVoidFunction:ERROR",
    "-Xep:EffectivelyPrivate:ERROR",
    "-Xep:EqualsGetClass:ERROR",
    "-Xep:LoopOverCharArray:ERROR",
    "-Xep:MissingSummary:ERROR",
    "-Xep:NullAway:ERROR",
    "-Xep:VoidMissingNullable:ERROR",
    "-Xep:ReturnMissingNullable:ERROR",
    "-Xep:ParameterMissingNullable:ERROR",
    "-Xep:FieldMissingNullable:ERROR",
    "-Xep:EqualsMissingNullable:ERROR",
    "-Xep:NullableConstructor:ERROR",
    "-Xep:NullablePrimitive:ERROR",
    "-Xep:NullablePrimitiveArray:ERROR",
    "-Xep:NullableOnContainingClass:ERROR",
]

NULLAWAY_OPTS = [
    "-XepOpt:NullAway:AnnotatedPackages=net.thisptr.jackson.jq",
    "-XepOpt:NullAway:TreatGeneratedAsUnannotated=true",
]

# Generated parser sources are exempt, as in the Maven build.
ERROR_PRONE_EXCLUDED_PATHS = [
    "-XepExcludedPaths:.*/generated-sources/.*",
]

JQ_JAVACOPTS = LINT_OPTS + ERROR_PRONE_OPTS + NULLAWAY_OPTS + ERROR_PRONE_EXCLUDED_PATHS

JQ_PLUGINS = [
    "//build-tools/bazel:nullaway",
    "//build-tools/bazel:auto_service",
]

def javacopts(release = 8, extra = []):
    """javacopts for a jackson-jq target.

    `--release` is passed explicitly, exactly as <release> in the poms, and is
    appended last: JavaBuilder's ReleaseOptionNormalizer drops the java_toolchain's
    -source/-target when --release is present, so no custom toolchain is needed and
    per-target release levels just work.
    """
    return JQ_JAVACOPTS + extra + ["--release", str(release)]
