"""Bzlmod extension for the jq binary repository."""

load(":repositories.bzl", "jq_repositories")

def _jq_impl(_module_ctx):
    jq_repositories()

jq = module_extension(implementation = _jq_impl)
