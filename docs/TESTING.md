# 测试与验证

验证日期：2026-10-27。所有数字来自实际运行，不把构建成功等同于业务成功。机器可读摘要在 `verification.json`。

## 执行方式
根目录：
```bash
./scripts/build.sh
python3 scripts/smoke.py
cd frontend && npm run e2e
npm audit
```
云环境 npm 缓存权限可用 `--cache /workspace/.tools/npm`，包装脚本已处理 Maven 代理和缓存。浏览器使用系统 Chromium，或 PRISM_CHROMIUM 指向 Playwright 安装的 Chromium。

## 最终结果
- Java：13 项，0失败、0错误、0跳过。真实 Spring 测试上下文、Flyway 和 JDBC，而非替身数据库访问。
- Vue：5 项全部通过。归一化数值、证据与限幅、全零安全处理、滑块真实翻转、锁定输入。
- Chromium：5 条全部通过。全决策旅程、空白创建编辑、无障碍/键盘/故障恢复、私有登录与写入、永久删除。
- 真实文件库测试：停止并重启可执行包，逐字段比较已复盘记录；无效批次导入回滚；合法备份新增 ID；两个相同版本请求得到 200/409；无 CSRF 拒绝；账号模式真实401/200。
- 本机列表性能：31项记录，30次顺序读取，p95 22.7ms（门槛300ms）。受本机并行构建与负载影响，不代表最大容量或公网延迟。
- 前端资产预算：压缩体积见 verification.json；JS<80KB、CSS<12KB（gzip）。
- axe：三个工作区在 Chromium 中执行 WCAG2 A/AA 标签规则，无违规；不能代替全面辅助技术与用户测试。
- 响应式：1280px桌面、390px手机实图；手机 scrollWidth 不超过视口，搜索、归档入口与核心操作可用。
- npm audit：本次 0 已知漏洞。首次版本存在漏洞，修复后重新构建测试；没有关闭校验。
- 开发模式：实际启动 Maven + Vite，验证源码入口与同源 API；开发/生产共享根目录 data 的配置已实测。
- Docker：完整源码多阶段构建，镜像内运行 Java/Vue 测试；Compose 非 root UID10001、只读根/命名卷、健康、真实认证写入、重启、停机文件备份恢复均验证。

## 证据位置
- Java：`backend/target/surefire-reports`（生成输出，不入源码）。
- 浏览器：`frontend/playwright-report`，失败时 `frontend/test-results` 有 trace/截图。
- 实图：`docs/screenshots/studio-desktop.png`、`studio-mobile.png`。
- 机器摘要：`docs/verification.json`。
- 当前机器运行日志：`/tmp/prism-build-final.log`、`prism-e2e-final.log`、`prism-smoke-final.log`、`prism-docker-final.log`，不假定跨机器保留。

## 诊断过的失败
认证 BCrypt 前缀缺失、示例初始权重不符合翻转预期、测试选择器未包含箭头文案、早期辅助文字对比度不足、干净容器代理 DNS/CA 信任缺失、停机恢复 chown 需要显式临时能力。均修复后重跑对应检查。不会把失败运行计入最终通过数。

## 未验证
远程 GitHub Actions 执行结果（工作流随源码交付，结果尚未验证）、第二台机器从头部署、真实用户长期反馈、完整容器/Java依赖 CVE 全库扫描、公网 HTTPS 与真实生产负载。没有付费、没有公网部署、没有自动运行的后续后台研发。
