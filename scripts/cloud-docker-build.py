#!/usr/bin/env python3
"""Build in this cloud's proxy network without disabling TLS verification."""
import os
from pathlib import Path
import socket
import subprocess
import tempfile
from urllib.parse import urlsplit
from xml.sax.saxutils import escape

root = Path(__file__).resolve().parents[1]
config = dict(os.environ)
config.setdefault('DOCKER_CONFIG', '/workspace/.tools/docker')
Path(config['DOCKER_CONFIG']).mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='prism-build-') as temp:
    folder = Path(temp)
    args = ['docker', 'build', '--network', 'host']
    hosts = set()
    for name in ['HTTP_PROXY', 'HTTPS_PROXY', 'NO_PROXY', 'http_proxy', 'https_proxy', 'no_proxy']:
        if os.environ.get(name):
            args += ['--build-arg', name]
            if name.lower() != 'no_proxy':
                p = urlsplit(os.environ[name])
                if p.username or p.password:
                    raise SystemExit('Proxy credentials cannot be copied into build configuration')
                hosts.add(p.hostname)
    for host in hosts:
        args += ['--add-host', f'{host}:{socket.gethostbyname(host)}']
    proxy = os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy')
    if proxy:
        p = urlsplit(proxy)
        settings = folder / 'maven.xml'
        settings.write_text(f'<settings><proxies><proxy><id>cloud</id><active>true</active><protocol>http</protocol><host>{escape(p.hostname)}</host><port>{p.port or 80}</port><nonProxyHosts>localhost|127.0.0.1</nonProxyHosts></proxy></proxies></settings>')
        args += ['--secret', f'id=maven_settings,src={settings}']
        certs = list(Path('/usr/local/share/ca-certificates').glob('*.crt'))
        if certs:
            ca = folder / 'ca.pem'
            ca.write_bytes(b''.join(cert.read_bytes() + b'\n' for cert in certs))
            args += ['--secret', f'id=build_ca,src={ca}']
    args += ['-t', 'prism:local', '.']
    result = subprocess.run(args, cwd=root, env=config)
    raise SystemExit(result.returncode)
