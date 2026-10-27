# 运维

## 日常
`docker compose ps` 观察健康，`docker compose logs --tail=100 prism` 查看日志。应用不记录请求内容、密码、Authorization。500 返回诊断编号，按日志匹配；不要把整份私人决策备份上传到故障报告。
GET /api/health 为公开最小探活；私有 API 的认证读取与一次可逆测试流程才验证业务。数据库异常时健康路由仍可能是 200，监控必须结合业务探针与磁盘容量。

## 备份
小工作室用 UI 导出 JSON，再通过品牌/关于对话框导入，新增独立 ID、不覆盖现有。导入最多 50 项且 512KB；更大工作室使用文件备份。证据、快照和复盘随备份保留。JSON 以及 H2 文件都含私人信息，未加密；按用户的文件加密策略安全存放，定期做离线恢复演练。

完整停机备份（已在当前 Compose 测试卷执行）：
```bash
mkdir -p backups && chmod 700 backups
docker compose stop prism
container=$(docker compose ps -a -q prism)
docker cp "$container":/app/data/prism.mv.db ./backups/prism.mv.db
chmod 600 ./backups/prism.mv.db
docker compose start prism
```
复制历史文件时换带日期的新文件名，勿覆盖唯一备份。停止进程后再复制，不能把热复制的 H2 文件当一致备份。备份还应安全保存数据库密码；不要将其写入备份文件名。

恢复（在空环境或已经保存当前数据的前提下）：
```bash
docker compose stop prism
container=$(docker compose ps -a -q prism)
docker cp ./backups/prism.mv.db "$container":/app/data/prism.mv.db
docker compose run --rm --user 0 --cap-add CHOWN --cap-add FOWNER --entrypoint sh prism -c 'chown 10001:10001 /app/data/prism.mv.db && chmod 600 /app/data/prism.mv.db'
docker compose start prism
```
恢复后校验健康、所有者登录、代表性记录与快照。这里的临时 root 命令临时添加 CHOWN/FOWNER（服务正常运行仍丢弃全部能力），仅修复数据卷文件所有权，运行服务仍是非 root。恢复不要在运行中的数据库上覆盖文件。

## 故障排查
- 401：检查部署的账号密码，页面刷新后可能需要重新登录；不用 GitHub token。
- 403：CSRF 会话过期，重新加载获取 token；不得关闭 CSRF。
- 409：另一个窗口先写入，保留当前输入，重新加载后手动合并；不自动覆盖。
- 422：字段超长、全零权重、无效引用、锁定后修改或错误日期；按提示修正。
- H2 文件锁：同一数据文件已有另一个 Java 实例。停止自己启动的重复进程；不要删除锁或数据解决。
- 端口占用：PORT 换一个，Vite 开发代理也相应修改配置；测试专用 8085/8086/8087。
- 容器数据权限：卷路径应属 UID 10001；普通容器重建不删除命名卷。
- Maven DNS：JVM 不自动读取 HTTP_PROXY，使用 scripts/mvn.sh 的运行时代理配置。不要绕过 TLS。
- 构建缺少 javac：安装完整 JDK，不只是 JRE；云机器用 cloud-setup。

没有跨实例同步、邮件提醒、自动备份调度或远端监控服务；运营者需要安排备份任务与磁盘监控。不得声称会话结束后测试进程会一直运行。

## 归档与删除
归档后可以恢复；只有已归档记录才能永久删除。UI 必须输入完整标题确认，后端校验归档状态与最新版本。删除不可撤销，请先备份；删除不是安全擦除物理磁盘，历史备份也需单独管理。达到 500 项上限时，可在备份后删除不再需要的记录以释放容量。
