"""Derives a module's external dependencies from its compiled content targets."""

load("@rules_java//java:defs.bzl", "JavaInfo")
load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_jvm_external//:providers.bzl", "MavenHintInfo", "MavenInfo")
load("@rules_jvm_external//private/rules:has_maven_deps.bzl", "has_maven_deps")

_ExternalDepsInfo = provider(
    doc = "External Maven-coordinate boundaries found in a Java target graph.",
    fields = {"deps": "External dependency records."},
)

_MAVEN_COORDINATES_PREFIX = "maven_coordinates="
_DEPENDENCY_ATTRS = [
    "deps",
    "exports",
    "library",
    "runtime_deps",
]

def _coordinates(target, ctx):
    if JavaInfo not in target:
        return None
    for tag in getattr(ctx.rule.attr, "tags", []):
        if tag.startswith(_MAVEN_COORDINATES_PREFIX):
            return tag[len(_MAVEN_COORDINATES_PREFIX):]
    return None

def _external_deps_aspect_impl(target, ctx):
    coordinates = _coordinates(target, ctx)
    if coordinates:
        return [_ExternalDepsInfo(deps = [struct(
            coordinates = coordinates,
            java_info = target[JavaInfo],
            maven_info = target[MavenInfo],
        )])]

    deps = []
    for attr_name in _DEPENDENCY_ATTRS:
        value = getattr(ctx.rule.attr, attr_name, None)
        if not value:
            continue
        targets = value if type(value) == "list" else [value]
        for dep in targets:
            if _ExternalDepsInfo in dep:
                deps.extend(dep[_ExternalDepsInfo].deps)
    return [_ExternalDepsInfo(deps = deps)]

_external_deps_aspect = aspect(
    implementation = _external_deps_aspect_impl,
    attr_aspects = _DEPENDENCY_ATTRS,
    requires = [has_maven_deps],
)

def _external_deps_impl(ctx):
    deps_by_coordinates = {}
    for content in ctx.attr.content:
        for dep in content[_ExternalDepsInfo].deps:
            if dep.coordinates != ctx.attr.coordinates:
                deps_by_coordinates[dep.coordinates] = dep

    deps = deps_by_coordinates.values()
    java_info = java_common.merge([dep.java_info for dep in deps])
    return [
        DefaultInfo(files = java_info.transitive_runtime_jars),
        java_info,
        MavenHintInfo(maven_infos = depset([dep.maven_info for dep in deps])),
    ]

external_deps = rule(
    implementation = _external_deps_impl,
    attrs = {
        "content": attr.label_list(
            aspects = [_external_deps_aspect],
            providers = [JavaInfo],
        ),
        "coordinates": attr.string(mandatory = True),
    },
)
