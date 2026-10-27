import { sample } from "../src/model";
import AxeBuilder from "@axe-core/playwright";
import { test, expect } from "@playwright/test";
test("完整决策旅程、真实 API、刷新恢复、归档和响应式", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /重要的选择/ })).toBeVisible();
  await page.getByRole("button", { name: "先走一遍街区实验 ↗" }).click();
  await expect(
    page.getByRole("heading", { name: "让空置的一楼，重新与街区发生关系" }),
  ).toBeVisible();
  await expect(page.locator(".toast")).toHaveCount(0);
  await page.locator("h1").click();
  await page.screenshot({
    path: "../docs/screenshots/studio-desktop.png",
    fullPage: true,
  });
  await page.locator("#lens-sustainable").fill("100");
  await page.locator("#lens-connection").fill("0");
  await page.locator("#lens-ease").fill("0");
  await expect(
    page.getByRole("status").filter({ hasText: "领先方案发生了翻转" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "保存判断", exact: true }).click();
  await expect(page.getByText("判断已保存在工作室")).toBeVisible();
  await page.reload();
  await expect(page.locator("#lens-sustainable")).toHaveValue("100");
  await page.getByRole("tab", { name: /现实线索/ }).click();
  await page
    .getByLabel("观察或事实")
    .fill("访谈发现附近居民有稳定的旧物修补需求");
  await page.getByLabel("来源").fill("现场访谈");
  await page.getByRole("button", { name: /记录证据，重新折光/ }).click();
  await expect(
    page.getByText("访谈发现附近居民有稳定的旧物修补需求"),
  ).toBeVisible();
  await page.getByRole("tab", { name: "实践与回声" }).click();
  await page
    .getByRole("button", { name: /设计一次实践/ })
    .first()
    .click();
  await page
    .getByLabel("我相信会发生什么")
    .fill("每周的修补开放日能让居民形成稳定的协作关系");
  await page.getByLabel("怎样知道它有价值").fill("四周内至少十位居民再次到访");
  await page.getByRole("button", { name: "锁定判断，开始实践 ↗" }).click();
  await expect(page.getByText("已经选择：修补实验室")).toBeVisible();
  await page.getByRole("tab", { name: "折光工作台" }).click();
  await expect(page.locator("#lens-sustainable")).toBeDisabled();
  await page.getByRole("tab", { name: "实践与回声" }).click();
  await page.getByRole("button", { name: /^记录复盘/ }).click();
  await page.getByLabel("现实如何回应").selectOption("MIXED");
  await page
    .getByLabel("发生了什么？你会如何调整下一次判断？")
    .fill(
      "回访人数达标，但志愿者的准备时间过长。下一轮要重新考虑持续运转的成本。",
    );
  await page.getByRole("button", { name: "保存这次认识 ↗" }).click();
  await expect(page.getByRole("heading", { name: "结果混合" })).toBeVisible();
  await page.getByText("查看锁定时的判断快照").click();
  await expect(
    page.getByText("快照不可修改；复盘不会重写当时的判断。"),
  ).toBeVisible();
  await page.reload();
  await page.getByRole("tab", { name: "实践与回声" }).click();
  await expect(
    page.getByText(
      "回访人数达标，但志愿者的准备时间过长。下一轮要重新考虑持续运转的成本。",
    ),
  ).toBeVisible();
  const downloadEvent = page.waitForEvent("download");
  await page.getByRole("button", { name: "导出全部记录 ↓" }).click();
  const download = await downloadEvent;
  expect(download.suggestedFilename()).toMatch(/prism-backup/);
  await page.getByRole("button", { name: "关于这间工作室 ↗" }).click();
  await page
    .locator("input[type=file]")
    .setInputFiles((await download.path())!);
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(
    page.getByRole("status").filter({ hasText: "已导入" }),
  ).toBeVisible();
  page.on("dialog", (d) => d.accept());
  await page.getByRole("button", { name: "归档这项决策", exact: true }).click();
  await expect(page.locator(".stage-pill")).toHaveText("已归档");
  await page.getByRole("button", { name: "恢复到工作室", exact: true }).click();
  await expect(page.locator(".stage-pill")).toHaveText("已复盘");
  await page.getByRole("button", { name: "带着认识，开启新一轮 ↗" }).click();
  await expect(page.getByRole("heading", { name: /新一轮/ })).toBeVisible();
  await expect(page.locator("#lens-sustainable")).toBeEnabled();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator(".toast")).toHaveCount(0);
  await page.locator("h1").click();
  await page.screenshot({
    path: "../docs/screenshots/studio-mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.getByRole("tab", { name: /现实线索/ }).click();
  await expect(page.getByRole("heading", { name: "还没有证据" })).toBeVisible();
  expect(errors).toEqual([]);
});
test("从空白创建，表单校验和信息编辑", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: /打开一个新问题/ }).click();
  await page.getByLabel("决策标题").fill("下一季的创作实验");
  await page
    .getByLabel("你真正想解决的问题")
    .fill("怎样在有限时间里保持创作？");
  await page.getByLabel("方案 1 名称").fill("早晨写作");
  await page.getByLabel("方案 2 名称").fill("周末集中");
  await page.getByRole("button", { name: /创建这项决策/ }).click();
  await expect(
    page.getByRole("heading", { name: "下一季的创作实验" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "编辑决策信息" }).click();
  await page.getByLabel("决策标题").fill("下一季的长期创作");
  await page.getByRole("button", { name: /保存决策信息/ }).click();
  await expect(
    page.getByRole("heading", { name: "下一季的长期创作" }),
  ).toBeVisible();
  await page.getByRole("button", { name: /基础判断矩阵/ }).click();
  await page.getByLabel("早晨写作 / 长期价值").fill("9");
  await page.getByRole("button", { name: "保存判断", exact: true }).click();
  await page.reload();
  await page.getByRole("button", { name: /基础判断矩阵/ }).click();
  await expect(page.getByLabel("早晨写作 / 长期价值")).toHaveValue("9");
});

test("键盘导航、无障碍、移动端归档入口与错误恢复", async ({ page }) => {
  const session = await (await page.request.get("/api/session")).json();
  await page.request.post("/api/decisions", {
    data: { ...sample(), title: "下一季的可访问探索" },
    headers: { "X-CSRF-TOKEN": session.csrf },
  });
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /下一季/ })).toBeVisible();
  await page.getByRole("tab", { name: "折光工作台" }).focus();
  await page.keyboard.press("ArrowRight");
  await expect(page.getByRole("tab", { name: /现实线索/ })).toHaveAttribute(
    "aria-selected",
    "true",
  );
  for (const name of [/现实线索/, "折光工作台", "实践与回声"]) {
    await page.getByRole("tab", { name }).click();
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    expect(
      results.violations.map((v) => ({
        id: v.id,
        impact: v.impact,
        nodes: v.nodes.map((n) => n.target),
      })),
    ).toEqual([]);
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByLabel("筛选决策")).toBeVisible();
  await page.getByRole("button", { name: /打开一个新问题/ }).click();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.route("**/api/decisions", (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/json",
      body: JSON.stringify({ message: "模拟网络故障" }),
    }),
  );
  await page.getByRole("button", { name: /重新加载/ }).click();
  await expect(page.getByRole("alert")).toContainText("模拟网络故障");
  await page.unroute("**/api/decisions");
  await page.getByRole("button", { name: /重新加载/ }).click();
  await expect(page.getByRole("heading", { name: /下一季/ })).toBeVisible();
});

test('私有工作室真实登录与受保护写入',async({page})=>{
 await page.goto('http://127.0.0.1:8087/');await expect(page.getByRole('dialog')).toBeVisible();
 await page.getByLabel('账号').fill('browser-owner');await page.getByLabel('密码').fill('incorrect');await page.getByRole('button',{name:'进入工作室 ↗'}).click();await expect(page.getByRole('dialog')).toBeVisible();
 await page.getByLabel('密码').fill('browser-test-only-123');await page.getByRole('button',{name:'进入工作室 ↗'}).click();await expect(page.getByRole('dialog')).toHaveCount(0);await expect(page.getByRole('heading',{name:/重要的选择/})).toBeVisible();
 await page.getByRole('button',{name:'先走一遍街区实验 ↗'}).click();await expect(page.getByRole('heading',{name:'让空置的一楼，重新与街区发生关系'})).toBeVisible();
})

test('归档记录可确认永久删除并从数据库移除',async({page})=>{
 const token=await (await page.request.get('/api/session')).json();const response=await page.request.post('/api/decisions',{data:{...sample(),title:'只用于删除流程的测试记录'},headers:{'X-CSRF-TOKEN':token.csrf}});const view=await response.json();
 await page.goto('/');await expect(page.getByRole('heading',{name:'只用于删除流程的测试记录'})).toBeVisible();
 page.on('dialog',async dialog=>{if(dialog.type()==='prompt')await dialog.accept('只用于删除流程的测试记录');else await dialog.accept()});
 await page.getByRole('button',{name:'归档这项决策',exact:true}).click();await page.getByRole('button',{name:'永久删除',exact:true}).click();
 await expect(page.getByRole('heading',{name:'只用于删除流程的测试记录'})).toHaveCount(0);expect((await page.request.get('/api/decisions/'+view.decision.id)).status()).toBe(404);
});
