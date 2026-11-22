#!/usr/bin/env bash
# Sync the versions documented in the readmes with the values declared by the build.
set -euo pipefail

root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
pom="$root/pom.xml"
gatling_pom="$root/testing/gatling/pom.xml"
workflow="$root/.github/workflows/build-deploy.yml"
readmes=(
  "$root/Readme.adoc"
  "$root/testing/jmeter/Readme.md"
  "$root/testing/gatling/Readme.md"
)

prop() { sed -n "s:.*<$2>\([^<]*\)</$2>.*:\1:p" "$1" | head -1; }

# Java comes from the maven build; Gradle compiles with the same toolchain.
java_version=$(prop "$pom" java.version)

# The Spring Boot version is the parent version, not a property.
parent_version=$(awk '/<parent>/{p=1} p && /<version>/{print; exit}' "$pom" |
  sed 's:.*<version>\([^<]*\)</version>.*:\1:')
boot_minor=$(cut -d. -f1-2 <<<"$parent_version")

# Gradle is pinned by the CI setup step; the repo has no wrapper.
gradle_version=$(sed -n "s#.*gradle-version: *'\([^']*\)'.*#\1#p" "$workflow" | head -1)
gradle_minor=$(cut -d. -f1-2 <<<"$gradle_version")

# Gatling is pinned by its test module pom.
gatling_version=$(prop "$gatling_pom" gatling-app.version)
gatling_minor=$(cut -d. -f1-2 <<<"$gatling_version")

[[ -n $java_version && -n $parent_version && -n $gradle_version && -n $gatling_version ]] || {
  echo "cannot read versions from $pom, $workflow or $gatling_pom" >&2
  exit 1
}

# Each pair is "literal anchor in the doc" -> "value from the build". The anchor
# keeps the replacement away from unrelated numbers; the version right after it
# is fully replaced.
sync() {
  local file=$1 anchor=$2 value=$3
  perl -0777 -pi -e "s{\Q$anchor\E[0-9]+(?:\.[0-9]+)*}{$anchor$value}g" "$file"
}

for file in "${readmes[@]}"; do
  before=$(cksum <"$file")
  # Badges and dependency tables show the supported major/minor.
  sync "$file" 'OpenJDK-' "$java_version"
  sync "$file" 'GraalVM-' "$java_version"
  sync "$file" 'Gradle-' "$gradle_minor"
  sync "$file" 'Spring%20Boot-' "$boot_minor"
  sync "$file" 'Gatling-' "$gatling_minor"
  sync "$file" 'JDK ~' "$java_version"
  sync "$file" 'Gatling ~' "$gatling_minor"
  # Links point at the exact version in use.
  sync "$file" 'graalvm.org/jdk' "$java_version"
  sync "$file" '&version=' "$java_version"
  sync "$file" 'docs.gradle.org/' "$gradle_version"
  if [[ $before == "$(cksum <"$file")" ]]; then
    echo "${file#"$root"/}: already up to date"
  else
    echo "${file#"$root"/}: updated"
  fi
done

echo "pom: Java $java_version, Spring Boot $parent_version (docs show $boot_minor); CI: Gradle $gradle_version (docs show $gradle_minor); Gatling $gatling_version (docs show $gatling_minor)"
