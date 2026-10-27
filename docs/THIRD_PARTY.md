# 第三方组件

直接依赖来自官方 Maven Central / npm 注册表，未引入品牌素材、远程字体或第三方图片。图形由源码 SVG 与 CSS 绘制，示例街区场景为虚构。

- Vue / Vite / Vitest / Vue Test Utils / vue-tsc / jsdom：MIT。
- TypeScript / Spring Boot / Spring Framework / Spring Security / Flyway Community / Maven / JUnit / Playwright：Apache-2.0；以具体发行包附带许可证为准。
- H2：MPL-2.0 或 EPL-1.0 双许可证。
- axe-core / @axe-core/playwright：MPL-2.0，开发测试用途，不进入运行时前端包。
- OpenJDK / Eclipse Temurin：GPL-2.0 with Classpath Exception；Debian 签名仓库提供云编译组件。早期 Oracle 下载地址 404，未使用或绕过校验。
- 容器基础镜像的系统组件分别遵循上游许可证。发行方应保留对应通知和许可证；未将产品源码自动公开或授权给外部服务。

前端锁文件与 Boot BOM 记录依赖版本。npm audit 零已知漏洞只代表该次注册表审计结果，不是全部供应链或镜像安全认证。未为用户自主决定源码开源许可证，也未推送或公开源码。
