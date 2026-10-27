#!/usr/bin/env python3
"""Real packaged server, file persistence, backup atomicity and HTTP security smoke."""
import base64
import copy
import http.cookiejar
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:8086'


def client():
    return urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def request(opener, path, method='GET', body=None, csrf=None, auth=None):
    headers = {}
    if body is not None:
        headers['Content-Type'] = 'application/json'
    if csrf:
        headers['X-CSRF-TOKEN'] = csrf
    if auth:
        headers['Authorization'] = 'Basic ' + base64.b64encode(auth.encode()).decode()
    req = urllib.request.Request(BASE + '/api' + path, data=json.dumps(body).encode() if body is not None else None, method=method, headers=headers)
    try:
        with opener.open(req, timeout=10) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.load(e)
        except (json.JSONDecodeError, ValueError):
            return e.code, {}


def start(folder, secure=False):
    config = dict(os.environ, PORT='8086', PRISM_BIND='127.0.0.1', PRISM_USERNAME='', PRISM_PASSWORD='', PRISM_DATABASE_URL=f'jdbc:h2:file:{folder}/prism;DB_CLOSE_ON_EXIT=FALSE')
    if secure:
        config.update(PRISM_BIND='0.0.0.0', PRISM_USERNAME='smoke-owner', PRISM_PASSWORD='smoke-only-password-123')
    log = open(Path(folder) / 'server.log', 'a')
    p = subprocess.Popen(['java', '-jar', str(ROOT / 'backend/target/prism-1.0.0.jar')], cwd=ROOT, env=config, stdout=log, stderr=log)
    for _ in range(200):
        if p.poll() is not None:
            raise RuntimeError('server failed: ' + (Path(folder) / 'server.log').read_text()[-3000:])
        try:
            if request(client(), '/health')[0] == 200:
                return p, log
        except OSError:
            pass
        time.sleep(.1)
    stop(p, log)
    raise RuntimeError('readiness timeout')


def stop(p, log):
    p.terminate()
    try:
        p.wait(timeout=10)
    except subprocess.TimeoutExpired:
        p.kill()
        p.wait(timeout=5)
    log.close()


def main():
    with tempfile.TemporaryDirectory(prefix='prism-smoke-') as folder:
        p, log = start(folder)
        try:
            c = client()
            csrf = request(c, '/session')[1]['csrf']
            payload = {'version': 0, 'title': '持久化验证', 'question': '重启后判断是否完整？', 'horizon': '一次测试', 'options': [{'id': 'a', 'name': '路径甲', 'description': '', 'scores': {'x': 8, 'y': 2}}, {'id': 'b', 'name': '路径乙', 'description': '', 'scores': {'x': 3, 'y': 9}}], 'criteria': [{'id': 'x', 'name': '持续性', 'weight': 70}, {'id': 'y', 'name': '连接', 'weight': 30}]}
            status, view = request(c, '/decisions', 'POST', payload, csrf)
            assert status == 200, view
            identity = view['decision']['id']
            path = '/decisions/' + identity
            status, view = request(c, path + '/commit', 'POST', {'version': 0, 'optionId': 'a', 'hypothesis': '稳定工作', 'signal': '连续三天', 'dueDate': '2099-12-01'}, csrf)
            assert status == 200, view
            status, view = request(c, path + '/review', 'POST', {'version': 1, 'outcome': 'MIXED', 'learning': '需要反复核实'}, csrf)
            assert status == 200, view
            saved = view
        finally:
            stop(p, log)
        p, log = start(folder)
        try:
            c = client()
            assert request(c, path)[1] == saved
            csrf = request(c, '/session')[1]['csrf']
            backup = request(c, '/backup')[1]
            before = len(request(c, '/decisions')[1])
            broken = copy.deepcopy(backup['decisions'][0])
            broken['title'] = ''
            assert request(c, '/restore', 'POST', {'formatVersion': 1, 'decisions': [backup['decisions'][0], broken]}, csrf)[0] == 422
            assert len(request(c, '/decisions')[1]) == before, 'restore must be atomic'
            status, restored = request(c, '/restore', 'POST', backup, csrf)
            assert status == 200 and restored[0]['decision']['experiment'] == saved['decision']['experiment']
            with ThreadPoolExecutor(2) as pool:
                statuses = list(pool.map(lambda _: request(c, path + '/archive', 'POST', {'version': 2}, csrf)[0], range(2)))
            assert sorted(statuses) == [200, 409], statuses
            assert request(c, '/decisions', 'POST', payload)[0] == 403
            for _ in range(29):
                assert request(c, '/decisions', 'POST', payload, csrf)[0] == 200
            durations = []
            for _ in range(30):
                tick = time.perf_counter()
                assert request(c, '/decisions')[0] == 200
                durations.append((time.perf_counter() - tick) * 1000)
            p95 = sorted(durations)[28]
            assert p95 < 300, p95
            print(f'PASS file restart equality; atomic restore; append restore; concurrent 200/409; real CSRF; list p95={p95:.1f}ms (31 decisions / 30 requests)')
        finally:
            stop(p, log)
        p, log = start(folder, secure=True)
        try:
            c = client()
            assert request(c, '/decisions')[0] == 401
            assert request(c, '/decisions', auth='smoke-owner:wrong')[0] == 401
            assert request(c, '/decisions', auth='smoke-owner:smoke-only-password-123')[0] == 200
            print('PASS packaged authenticated runtime: anonymous/wrong 401; owner 200')
        finally:
            stop(p, log)


if __name__ == '__main__':
    main()
