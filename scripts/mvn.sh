#!/usr/bin/env bash
set -euo pipefail
if [[ -z "${JAVA_HOME:-}" && -x /workspace/.tools/jdk/usr/lib/jvm/java-21-openjdk-amd64/bin/javac ]]; then
 export JAVA_HOME=/workspace/.tools/jdk/usr/lib/jvm/java-21-openjdk-amd64
fi
if command -v mvn >/dev/null; then maven=mvn
elif [[ -x /workspace/.tools/apache-maven-3.9.11/bin/mvn ]]; then maven=/workspace/.tools/apache-maven-3.9.11/bin/mvn
else echo '需要 Maven 3.9+；云环境请运行 scripts/cloud-setup.sh' >&2; exit 1; fi
args=()
if [[ -n "${HTTPS_PROXY:-}" ]]; then
 settings=$(mktemp)
 trap 'rm -f "$settings"' EXIT
 python3 - "$settings" <<'PY'
import os,sys,urllib.parse,xml.sax.saxutils
p=urllib.parse.urlsplit(os.environ['HTTPS_PROXY'])
if p.username or p.password: raise SystemExit('不支持将代理凭据写入 Maven 配置，请使用平台无凭据代理')
host=xml.sax.saxutils.escape(p.hostname or '')
open(sys.argv[1],'w').write(f'<settings><proxies><proxy><id>runtime-proxy</id><active>true</active><protocol>http</protocol><host>{host}</host><port>{p.port or 80}</port><nonProxyHosts>localhost|127.0.0.1</nonProxyHosts></proxy></proxies></settings>')
PY
 args+=(-s "$settings")
fi
if [[ -d /workspace/.tools/m2 ]]; then args+=(-Dmaven.repo.local=/workspace/.tools/m2); fi
"$maven" "${args[@]}" -B "$@"
