# 构建与部署

## 本机部署
JDK 21、Maven 3.9.11、Node 24；精确前端版本锁于 package-lock，Java 版本由 Boot 3.5.16 BOM 管理。运行根目录 `./scripts/build.sh`，产物 `backend/target/prism-1.0.0.jar` 同时包含 Vue 静态资源。
`./scripts/run.sh` 从根目录启动并使用 `data/prism` 文件库；不要在两个进程中同时打开同一文件库。数据库目录、备份目录和 `.env` 限制为所有者可读。

## 配置
| 变量 | 默认/约束 | 含义 |
|---|---|---|
| PORT | 8080 | HTTP 端口 |
| PRISM_BIND | 127.0.0.1 | 非 loopback 必须配置工作室身份 |
| PRISM_USERNAME | 空 | 单一所有者账号 |
| PRISM_PASSWORD | 空；启用时至少 12 字符 | 推荐随机 20 字符以上，非硬编码 |
| PRISM_DATABASE_URL | jdbc:h2:file:./data/prism;DB_CLOSE_ON_EXIT=FALSE | 文件路径决定于进程工作目录；容器是 /app/data/prism |
| PRISM_DB_PASSWORD | 本机默认空 | H2 连接口令，不代表文件加密；有数据后不得随意修改 |
| PRISM_PORT | 8080 | Compose 宿主 loopback 映射 |
| PRISM_CHROMIUM | /usr/bin/chromium | 测试浏览器路径，不影响产品运行 |

无需 Redis、外部数据库、GitHub token 或 AI 服务。默认 loopback 免认证只用于可信本机。不要将数据库内存模式用于真实部署。

## Docker
多阶段镜像：Node 构建并测 Vue → Maven 编译并测 Java → Java 21 JRE 非 root 运行。Compose 使用 UID 10001、只读根文件系统、写数据卷、临时 tmpfs、全部能力丢弃、no-new-privileges 与 768MB 上限。健康检查访问真实健康路由；业务就绪还需认证查询或创建/读取记录。

普通网络：填 `.env` 后 `docker compose up --build -d`。无须额外代理设置。Compose 镜像名默认 `prism:local`，可以用 PRISM_IMAGE 指定已构建的标签以便升级回滚。

当前云网络：容器桥接网络不能解析/访问平台代理，且干净镜像不含平台信任 CA。`python3 scripts/cloud-docker-build.py` 使用 host 构建网络与仅对代理名的解析映射；通过 BuildKit 临时 secret 挂载 Maven 代理设置和本机已信任的公共 CA。npm 使用额外 CA，Java 构建信任库导入 CA，始终保留证书验证。配置无原始凭据，临时文件自动删除，运行镜像不包含代理设置。先用该脚本构建，再 `docker compose up -d --no-build`。不得改成 strict-ssl=false、curl -k 或跳过签名。

本次验证的源构建含 Java/Vue 测试；Compose 真实认证写入、UID10001、健康状态、命名卷重启与停机恢复均验证。未在另一台机器部署，未执行公网 HTTPS/域名配置，也未执行容器镜像 CVE 全库扫描；不把这些描述为已通过。

## 对外部署前
只监听受保护的本机端口，通过已有 HTTPS 反向代理接入。反向代理设置 512KB 请求限制、连接/请求超时、请求速率限制；限制访问来源，禁用代理访问日志中的 Authorization。不要直接把 Basic 认证暴露在明文公网 HTTP。对外发布仍需用户明确授权，本次不包含该动作。

## 迁移、升级与回滚
Flyway 启动执行迁移并验证历史校验。升级前停机备份数据库与旧镜像标签，保留密钥配置。构建新镜像 → 验证测试 → 使用新标签启动 → 验证健康及认证业务。若失败，停机恢复对应数据库与旧镜像后启动；仅回退二进制不保证兼容新 schema。不要对生产执行 Flyway clean 或删除卷。
