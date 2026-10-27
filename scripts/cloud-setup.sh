#!/usr/bin/env bash
# Debian cloud machine bootstrap; all downloaded tools stay outside the checkout.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
tools=/workspace/.tools
mkdir -p "$tools"
if ! command -v javac >/dev/null && [[ ! -x "$tools/jdk/usr/lib/jvm/java-21-openjdk-amd64/bin/javac" ]]; then
 mkdir -p "$tools/apt/lists/partial" "$tools/apt/cache/archives/partial"
 cat > "$tools/apt/sources.list" <<'APT'
deb [signed-by=/usr/share/keyrings/debian-archive-keyring.gpg] https://deb.debian.org/debian trixie main
APT
 apt_options=(-o "Dir::Etc::sourcelist=$tools/apt/sources.list" -o Dir::Etc::sourceparts=- -o "Dir::State::lists=$tools/apt/lists" -o "Dir::Cache=$tools/apt/cache" -o Debug::NoLocking=1)
 apt-get "${apt_options[@]}" update
 (cd "$tools" && apt-get "${apt_options[@]}" download openjdk-21-jdk-headless)
 package=$(find "$tools" -maxdepth 1 -name 'openjdk-21-jdk-headless_*.deb' -print -quit)
 dpkg-deb -x "$package" "$tools/jdk"
 # This cloud image already contains the matching Java 21 JRE. Keep the extracted compiler files.
 cp -an /usr/lib/jvm/java-21-openjdk-amd64/. "$tools/jdk/usr/lib/jvm/java-21-openjdk-amd64/"
fi
if ! command -v mvn >/dev/null && [[ ! -x "$tools/apache-maven-3.9.11/bin/mvn" ]]; then
 python3 - <<'PY'
import urllib.request,hashlib,tarfile,pathlib
root=pathlib.Path('/workspace/.tools');target=root/'maven.tar.gz'
url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz'
expected='bcfe4fe305c962ace56ac7b5fc7a08b87d5abd8b7e89027ab251069faebee516b0ded8961445d6d91ec1985dfe30f8153268843c89aa392733d1a3ec956c9978'
urllib.request.urlretrieve(url,target)
if hashlib.sha512(target.read_bytes()).hexdigest()!=expected: raise SystemExit('Maven SHA-512 verification failed')
with tarfile.open(target) as archive: archive.extractall(root,filter='data')
PY
fi
mkdir -p "$tools/m2"
export PRISM_NPM_CACHE="$tools/npm"
"$root/scripts/build.sh"
