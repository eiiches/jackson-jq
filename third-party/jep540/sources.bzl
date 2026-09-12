"""Fetches the unpublished JEP 540 sources from OpenJDK."""

_COMMIT = "39f3ef1b97917110e5237ad2bbe4f6bcec1bd5e5"
_SOURCE_ROOT = "src/jdk.incubator.json/share/classes/"
_RAW_ROOT = "https://raw.githubusercontent.com/openjdk/jdk/%s/%s" % (_COMMIT, _SOURCE_ROOT)

# Keep per-file hashes rather than downloading the complete 115 MB JDK source archive for this
# 164 KB incubator module.
_SOURCES = {
    "module-info.java": "2d7415020f5cd0dba7ad0517d69d8dce327344cbd6c619ad94442d157737e770",
    "jdk/incubator/json/Json.java": "60cd0c74049668cd1eaaeebdef2cf86bedfbffcfae33f005fe1ca43628461bd8",
    "jdk/incubator/json/JsonArray.java": "4a3e986c180ce56124cf75dfeee7b45fb9becf879989225ea773a72e6c3f1735",
    "jdk/incubator/json/JsonBoolean.java": "555dc9c3f9f3d551c4cba46c25e81485b6aab57fc867d7953b530882befdedf6",
    "jdk/incubator/json/JsonNull.java": "933e113afcf452bc13731235e9acdeb6b46e7a50283c31de264de92345239f35",
    "jdk/incubator/json/JsonNumber.java": "017528eb30fd7fa936b261de5cc5e635ea2fb065dc6f10f3b04b17aec5599ce0",
    "jdk/incubator/json/JsonObject.java": "21b7b63d1e048b76244579567138304cf3f7a5d56d314466511540bf1d5e34b5",
    "jdk/incubator/json/JsonParseException.java": "c42a52e51859146791606994d060974f47de2adeca1666ad61058e4d59a7befb",
    "jdk/incubator/json/JsonString.java": "b01ba74c0a2644972acab942f67d5b5fc90989cd0dca099a60c6039ab3944b0f",
    "jdk/incubator/json/JsonValue.java": "78fe6ef37e8440cf349a785364cc4d5b52969b5578105acb66043e38de548adc",
    "jdk/incubator/json/JsonValueException.java": "29d3aa90127d7e1f8f080d8b492e690e7805e27a3d6414a9f3f85bf1bc20174b",
    "jdk/incubator/json/impl/JsonArrayImpl.java": "f8a06dd080b32f4a450f03fd3421f1b69396cdd2fb68aaa70ec5340c41eee0be",
    "jdk/incubator/json/impl/JsonBooleanImpl.java": "cb27022f9ce3f5cc5fc1860fbe2ea23ec2a6eb1ba3059e45272cf0070699b6f3",
    "jdk/incubator/json/impl/JsonGenerator.java": "3bc40db9e279128056732e24e6b695401e9f5163e06b234073e2637feb8cf9bb",
    "jdk/incubator/json/impl/JsonNullImpl.java": "d8c8bc8aaa43092d3987a6bb0182bdebb50c259cb3b25c15b0fd5d149a2e12b2",
    "jdk/incubator/json/impl/JsonNumberImpl.java": "27c4b57f5b9f1c6b5c903ce8960316bb15d83b0ffb6925a38e9e9eff320740d7",
    "jdk/incubator/json/impl/JsonObjectImpl.java": "4a7bf4a8f659727b016e1f4d29a5979a070ab717bd65d53e6c7b9ae71434e473",
    "jdk/incubator/json/impl/JsonParser.java": "a7c0e5a0e8088348a940846589841940ac409cf14f7a74eb73141fa4573eb79d",
    "jdk/incubator/json/impl/JsonStringImpl.java": "fa3e4c887b8eeea25219f66ddd4874df1be11a0b60677b35bd9fb4e6669193d7",
    "jdk/incubator/json/impl/JsonValueSupport.java": "ee12de881602272e25c91b490e9e7522785e9464e049a2b74b59544a0bcb63cd",
    "jdk/incubator/json/impl/Utils.java": "f66270ae45df381a584fdf4db25a9dec45aa5ee265f4b015b146d759a260de4f",
    "jdk/incubator/json/package-info.java": "3d1d96a78302f832356e0ae24939ec5d15764a4e7c929880d2949e241743dd19",
}

def _jep540_sources_repository_impl(ctx):
    for path, sha256 in _SOURCES.items():
        ctx.download(
            output = path,
            sha256 = sha256,
            url = _RAW_ROOT + path,
        )

    # @ParticipatesInPreview comes from jdk.internal.javac, which java.base exports to nobody, so
    # the descriptor cannot compile outside a full JDK build as written. Strip the annotation and
    # its import; the module declaration and the copyright header are left untouched, and the
    # sha256 above still pins the bytes this patch is applied to.
    #
    # This modifies a GPLv2 file. Nothing derived from it is distributed -- the jar is compile-only
    # and no published artifact embeds it -- so GPLv2 section 2's conditions on modified copies are
    # not engaged. Distributing it would first require a notice of this change and its date.
    module_info = ctx.read("module-info.java")
    module_info = module_info.replace("import jdk.internal.javac.ParticipatesInPreview;\n\n", "")
    module_info = module_info.replace("@ParticipatesInPreview\n", "")
    ctx.file("module-info.java", module_info)

    ctx.file("BUILD.bazel", """
load("@rules_java//java:defs.bzl", "java_library")

filegroup(
    name = "sources",
    srcs = glob([
        "module-info.java",
        "jdk/**/*.java",
    ]),
    visibility = ["//visibility:public"],
)

java_library(
    name = "jdk_incubator_json",
    srcs = [":sources"],
    javacopts = [
        "--enable-preview",
        "--release",
        "26",
    ],
    # These two tags are a pair, read by two different aspects, and neither works alone.
    #
    # The coordinates are fictional and are NEVER emitted: JEP 540 ships as a JDK module and has no
    # Maven artifact. They exist only because jackson-jq's own external_deps aspect records a
    # dependency boundary by looking for this tag, and that is what carries this jar into a
    # dependent module's JavaInfo -- which is how javadoc gets it on its classpath.
    #
    # maven:compile-only is what keeps the GPLv2 code out of every published Apache-2.0 artifact.
    # rules_jvm_external checks stop tags before reading coordinates, so this jar is routed to
    # dep_infos: subtracted from the artifact jar and its source jar, and contributing an empty
    # as_maven_dep, so no pom ever names it. Delete this tag and the fictional coordinates above
    # become a real <dependency> that resolves from nowhere.
    tags = [
        "maven_coordinates=jdk.incubator:json:26",
        "maven:compile-only",
    ],
    visibility = ["//visibility:public"],
)
""")

jep540_sources_repository = repository_rule(
    implementation = _jep540_sources_repository_impl,
)
