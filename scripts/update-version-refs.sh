#!/bin/bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
	echo "Usage: $0 {prepare-release|prepare-next-development-iteration} version" 1>&2
	exit 1
fi

scriptpath="$(readlink -f "$0")"
scriptdir="$(dirname "$scriptpath")"
cd "$scriptdir/.."

mode="$1"
versions_maven_plugin=org.codehaus.mojo:versions-maven-plugin:2.18.0

update_project_version_refs() {
	local target_version="$1"

	mvn "${versions_maven_plugin}:set" -DnewVersion="$target_version" -DgenerateBackupPoms=false

	for pom in smoke-tests/{java8,jpms,graalvm,osgi}/pom.xml; do
		mvn -f "$pom" "${versions_maven_plugin}:set-property" -Dproperty=jackson-jq.version -DnewVersion="$target_version" -DautoLinkItems=false -DgenerateBackupPoms=false
	done
}

update_scm_tag() {
	local tag="$1"

	mvn "${versions_maven_plugin}:set-scm-tag" -DnewTag="$tag" -DgenerateBackupPoms=false
}

update_readme_version_refs() {
	local release_version="$1"

	sed -i "s;<version>[0-9A-Za-z.-]*</version>;<version>$release_version</version>;" README.md
	sed -i "s;https://search.maven.org/artifact/net.thisptr.jackson.jq.v2/jackson-jq/[0-9A-Za-z.-]*/;https://search.maven.org/artifact/net.thisptr.jackson.jq.v2/jackson-jq/$release_version/;" README.md
	sed -i "s;jackson-jq-cli-[0-9A-Za-z.-]*.jar;jackson-jq-cli-$release_version.jar;" README.md
	sed -i "s;https://repo1.maven.org/maven2/net/thisptr/jackson/jq/v2/jackson-jq-cli/[0-9A-Za-z.-]*/;https://repo1.maven.org/maven2/net/thisptr/jackson/jq/v2/jackson-jq-cli/$release_version/;" README.md
	sed -i "s;*You are currently viewing the .* branch. Some of the features may not be released yet.*;;" README.md
}

case "$mode" in
prepare-release)
	release_version="$2"
	update_project_version_refs "$release_version"
	update_scm_tag "$release_version"
	update_readme_version_refs "$release_version"
	;;
prepare-next-development-iteration)
	next_development_version="$2"
	update_project_version_refs "$next_development_version"
	update_scm_tag HEAD
	;;
*)
	echo "Unknown mode: $mode" 1>&2
	echo "Usage: $0 {prepare-release|prepare-next-development-iteration} version" 1>&2
	exit 1
	;;
esac
