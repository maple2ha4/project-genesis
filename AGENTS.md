# PROJECT GENESIS / 折光 PRISM

本仓库是一个真实可部署的单人决策实验工作室，前端 Vue 3 + TypeScript，后端 Java 21 + Spring Boot，数据库嵌入式 H2，Flyway 迁移。用户授权自主完成产品研发，并在后续明确要求提交并推送到现有仓库的 main。此授权不包含公网部署、改变仓库可见性、付费或操作真实生产数据。

## 恢复工作
1. 先读本文件，再读 `docs/PRODUCT_VISION.md`、`docs/PROGRESS.md`、`docs/QUALITY_GATES.md`。
2. 检查 `git status --short` 和实际代码，不覆盖用户工作。
3. 按进度中最近验证的命令确认当前功能，再实施最重要的未完成项；不要重新选择主题。
4. 里程碑更新进度、测试证据及批评记录。未运行的检查必须标未验证。

## 约束
- 核心体验是权重敏感性探索 → 证据修正 → 不可变判断快照 → 实践 → 复盘。不得退化成通用任务工具或聊天界面。
- 所有评分必须透明可解释，不得伪装为 AI、客观概率或专业建议。
- 修改后运行相关测试；不能绕过签名、TLS 或断言。
- 保持前后端评分规则一致。后端是权威来源，全部写操作带版本号，冲突返回 409。
- 不在源码、日志、文档中保留真实凭据。生产对外绑定必须配置用户名与 12 字符以上密码，外部访问必须 HTTPS。
- 文档用中文，代码标识符用英文。模块分工：Rules（纯规则）、DecisionService（事务与生命周期）、DecisionStore（乐观锁存储）、ApiController（HTTP）、Vue 组件（展示与编辑）。
- 现有云任务已隔离，直接使用当前 checkout，不创建 Git worktree。

## 命令（仓库根目录）
- 云环境依赖：`./scripts/cloud-setup.sh`（Debian trixie + 现有 Java 21 JRE）；普通本机安装 JDK 21、Maven 3.9、Node 24。
- 全构建与 Java/Vue 测试：`./scripts/build.sh`
- 开发：`./scripts/dev.sh`（后端 8080、Vite 5173；API 代理同源）
- 生产包本机运行：`./scripts/run.sh`（8080，默认 loopback）
- 浏览器 E2E：`cd frontend && npm run e2e`（独立 8085，内存库，不触碰用户库）
- 容器：填好 `.env` 后 `docker compose up --build -d`，详情见部署文档。

测试使用虚构场景。勿把端到端内存库当成持久化验证。浏览器、重启持久化、Docker 和故障恢复结果分别记录。
