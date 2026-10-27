<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from "vue";
import { api, ApiError, credentials, session } from "./api";
import { clone, sample, stage, type Decision, type View } from "./model";
import Modal from "./components/Modal.vue";
import Sidebar from "./components/Sidebar.vue";
import DecisionEditor from "./components/DecisionEditor.vue";
import Studio from "./components/Studio.vue";
import EvidencePanel from "./components/EvidencePanel.vue";
import Journal from "./components/Journal.vue";
const views = ref<View[]>([]),
  selected = ref(""),
  draft = ref<Decision>(),
  busy = ref(false),
  loading = ref(true),
  error = ref(""),
  notice = ref(""),
  query = ref(""),
  filter = ref("active"),
  tab = ref("studio");
const modal = ref<
  "new" | "edit" | "commit" | "review" | "login" | "guide" | null
>(null);
const login = ref({ username: "", password: "" });
const practice = ref({
  optionId: "",
  hypothesis: "",
  signal: "",
  dueDate: new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
});
const reflection = ref({ outcome: "MIXED", learning: "" });
const current = computed(() =>
  views.value.find((v) => v.decision.id === selected.value),
);
const dirty = computed(
  () =>
    draft.value &&
    current.value &&
    JSON.stringify(draft.value) !== JSON.stringify(current.value.decision),
);
const locked = computed(
  () => !!draft.value?.experiment || !!draft.value?.archived,
);
let toastTimer: ReturnType<typeof setTimeout>;
function tell(s: string) {
  notice.value = s;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (notice.value = ""), 4500);
}
function accept(v: View) {
  const i = views.value.findIndex((x) => x.decision.id === v.decision.id);
  if (i >= 0) views.value.splice(i, 1);
  views.value.unshift(v);
  selected.value = v.decision.id;
  draft.value = clone(v.decision);
}
async function run(action: () => Promise<void>) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    await action();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "操作未完成，请重试";
    if (e instanceof ApiError && e.status === 401) modal.value = "login";
  } finally {
    busy.value = false;
  }
}
async function load() {
  loading.value = true;
  await run(async () => {
    await session();
    views.value = await api<View[]>("/decisions");
    const v =
      views.value.find((v) => v.decision.id === selected.value) ||
      views.value.find((v) => !v.decision.archived) ||
      views.value[0];
    if (v) {
      selected.value = v.decision.id;
      draft.value = clone(v.decision);
    } else {
      selected.value = "";
      draft.value = undefined;
    }
  });
  loading.value = false;
}
function discard() {
  return !dirty.value || window.confirm("有尚未保存的判断。放弃这些调整？");
}
function select(v: View) {
  if (!discard()) return;
  selected.value = v.decision.id;
  draft.value = clone(v.decision);
  tab.value = "studio";
  error.value = "";
}
function writeBody(d: Decision) {
  return {
    version: d.version,
    title: d.title,
    question: d.question,
    horizon: d.horizon,
    options: d.options,
    criteria: d.criteria,
  };
}
async function save() {
  if (!draft.value || !dirty.value) return;
  await run(async () => {
    accept(
      await api<View>(
        "/decisions/" + selected.value,
        "PUT",
        writeBody(draft.value!),
      ),
    );
    tell("判断已保存");
  });
}
async function create(body: unknown) {
  await run(async () => {
    accept(await api<View>("/decisions", "POST", body));
    modal.value = null;
    tab.value = "studio";
    filter.value = "active";
    query.value = "";
    tell("新的可能，已经打开");
  });
}
async function edit(body: unknown) {
  await run(async () => {
    accept(await api<View>("/decisions/" + selected.value, "PUT", body));
    modal.value = null;
    tell("决策信息已更新");
  });
}
function cleanRequired() {
  if (dirty.value) {
    error.value = "请先保存或重置视角调整，再继续此操作。";
    return false;
  }
  return true;
}
async function action(suffix: string, body: unknown, message: string) {
  if (!cleanRequired()) return;
  await run(async () => {
    accept(
      await api<View>("/decisions/" + selected.value + suffix, "POST", body),
    );
    modal.value = null;
    tell(message);
  });
}
function openCommit() {
  if (!cleanRequired()) return;
  practice.value = {
    optionId: current.value!.ranking[0].optionId,
    hypothesis: "",
    signal: "",
    dueDate: new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
  };
  modal.value = "commit";
}
async function evidence(body: unknown) {
  await action(
    "/evidence",
    { ...(body as object), version: draft.value!.version },
    "证据已记录，判断已重新计算",
  );
}
async function removeEvidence(id: string) {
  if (
    !cleanRequired() ||
    !window.confirm("移除这条证据？关联的评分影响也会撤销。")
  )
    return;
  await run(async () => {
    accept(
      await api<View>(
        `/decisions/${selected.value}/evidence/${id}?version=${draft.value!.version}`,
        "DELETE",
      ),
    );
    tell("证据影响已撤销");
  });
}
async function fork() {
  await action(
    "/fork",
    { version: draft.value!.version },
    "已创建新一轮；沿用基础判断，不自动沿用证据",
  );
  tab.value = "studio";
  filter.value = "active";
}
async function archive() {
  if (
    !window.confirm(
      draft.value!.archived
        ? "将这项决策放回工作室？"
        : "归档后仍可查看、导出和恢复。确认归档？",
    )
  )
    return;
  await action(
    "/archive",
    { version: draft.value!.version },
    draft.value!.archived ? "已恢复到工作室" : "已归档，记录会保留",
  );
}
async function deleteDecision() {
  const d=draft.value!;
  if(window.prompt('永久删除无法撤销。建议先导出备份。请输入完整标题以确认：\n'+d.title)!==d.title)return;
  await run(async()=>{
    await api('/decisions/'+d.id+'?version='+d.version,'DELETE');
    views.value=views.value.filter(v=>v.decision.id!==d.id);
    const next=views.value.find(v=>!v.decision.archived)||views.value[0];
    selected.value='';draft.value=undefined;tab.value='studio';
    if(next){accept(next);filter.value=next.decision.archived?'archived':'active'}
    tell('决策已永久删除');
  });
}
async function exportBackup() {
  await run(async () => {
    const backup = await api("/backup");
    const url = URL.createObjectURL(
      new Blob([JSON.stringify(backup, null, 2)], { type: "application/json" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download =
      "prism-backup-" + new Date().toISOString().slice(0, 10) + ".json";
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    tell("已导出全部决策；请妥善保存私人记录");
  });
}
async function importBackup(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (file.size > 524288) {
    error.value = "备份不能超过 512 KB，请使用数据库备份恢复大工作室";
    input.value = "";
    return;
  }
  if (!discard()) {
    input.value = "";
    return;
  }
  await run(async () => {
    const restored = await api<View[]>(
      "/restore",
      "POST",
      JSON.parse(await file.text()),
    );
    views.value.push(...restored);
    if (restored[0]) accept(restored[0]);
    filter.value = restored[0]?.decision.archived ? "archived" : "active";
    query.value = "";
    modal.value = null;
    tell(`已导入 ${restored.length} 项决策，未覆盖现有数据`);
  });
  input.value = "";
}
function openNew() {
  if (discard()) modal.value = "new";
}
function tabKeys(e: KeyboardEvent) {
  const ids = ["studio", "evidence", "journal"];
  let index = ids.indexOf(tab.value);
  if (e.key === "ArrowRight") index = (index + 1) % 3;
  else if (e.key === "ArrowLeft") index = (index + 2) % 3;
  else if (e.key === "Home") index = 0;
  else if (e.key === "End") index = 2;
  else return;
  e.preventDefault();
  tab.value = ids[index];
  document.getElementById("tab-" + tab.value)?.focus();
}
function keys(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === "s") {
    e.preventDefault();
    void save();
  }
}
function beforeUnload(e: BeforeUnloadEvent) {
  if (dirty.value) {
    e.preventDefault();
    e.returnValue = "";
  }
}
onMounted(() => {
  void load();
  window.addEventListener("keydown", keys);
  window.addEventListener("beforeunload", beforeUnload);
});
onBeforeUnmount(() => {
  window.removeEventListener("keydown", keys);
  window.removeEventListener("beforeunload", beforeUnload);
  clearTimeout(toastTimer);
});
</script>
<template>
  <div class="app-shell">
    <a class="skip-link" href="#workspace">跳到工作区</a>
    <Sidebar
      :views="views"
      :selected="selected"
      :busy="busy"
      :loading="loading"
      v-model:query="query"
      v-model:filter="filter"
      @guide="modal = 'guide'"
      @create="openNew"
      @select="select"
      @export="exportBackup"
    />
    <main id="workspace" class="workspace">
      <header class="topbar">
        <span>FIELDWORK FOR POSSIBLE FUTURES</span
        ><button
          class="text-button"
          :disabled="busy"
          @click="discard() && load()"
        >
          ↻ 重新加载
        </button>
      </header>
      <div v-if="error" class="error-banner" role="alert">
        <span>{{ error }}</span
        ><button aria-label="关闭错误提示" @click="error = ''">×</button>
      </div>
      <div v-if="loading" class="loading-state" role="status">
        <span class="loading-prism">△</span>
        <h2>正在打开工作室…</h2>
      </div>
      <section v-else-if="!draft" class="welcome">
        <div class="eyebrow">A PLACE TO THINK, THEN TRY.</div>
        <div class="welcome-prism" aria-hidden="true">
          <svg viewBox="0 0 560 260">
            <path
              d="M40 160H220L295 35L380 210H150Z"
              fill="none"
              stroke="currentColor"
              stroke-width="1"
            />
            <path
              d="M220 160L380 210M295 35L295 185"
              stroke="currentColor"
              stroke-width="1"
            />
            <path
              d="M340 123L520 72M350 144L520 145M362 170L520 218"
              stroke-width="3"
              fill="none"
              stroke="#cb6743"
            />
            <path d="M350 144L520 145" stroke="#728674" />
            <path d="M362 170L520 218" stroke="#b99b56" />
          </svg>
        </div>
        <h1>重要的选择，<br />值得被<span>看见。</span></h1>
        <p>
          把模糊的问题展开成可能。调整视角，让取舍显形。<br />再用一次真实的实践，听见选择的回声。
        </p>
        <div class="welcome-actions">
          <button class="primary" @click="openNew">打开我的第一个问题 ↗</button
          ><button class="secondary" :disabled="busy" @click="create(sample())">
            先走一遍街区实验 ↗
          </button>
        </div>
        <div class="welcome-steps">
          <span>01 <b>展开可能</b></span
          ><span>02 <b>看见取舍</b></span
          ><span>03 <b>实践与回声</b></span>
        </div>
      </section>
      <template v-else
        ><section class="decision-heading">
          <div class="eyebrow">
            <span
              >DECISION /
              {{
                String(
                  views.findIndex((v) => v.decision.id === selected) + 1,
                ).padStart(2, "0")
              }}</span
            ><span class="stage-pill">{{ stage(draft) }}</span>
          </div>
          <div class="title-row">
            <h1>{{ draft.title }}</h1>
            <button
              v-if="!locked"
              class="icon-button"
              aria-label="编辑决策信息"
              @click="cleanRequired() && (modal = 'edit')"
            >
              ↗
            </button>
          </div>
          <p class="question">{{ draft.question }}</p>
          <div class="heading-foot">
            <span class="horizon"
              >◷ {{ draft.horizon || "为这次探索，留一点时间" }}</span
            ><span class="saved-label"
              >{{ dirty ? "有未保存的调整" : "判断已保存在工作室" }}
              <i :class="{ unsaved: dirty }"
            /></span>
          </div>
        </section>
        <div class="tabs-row">
          <div
            class="tabs"
            role="tablist"
            aria-label="决策工作区"
            @keydown="tabKeys"
          >
            <button
              v-for="t in [
                { id: 'studio', name: '折光工作台' },
                {
                  id: 'evidence',
                  name: '现实线索',
                  count: draft.evidence.length,
                },
                { id: 'journal', name: '实践与回声' },
              ]"
              :id="'tab-' + t.id"
              role="tab"
              :aria-selected="tab === t.id"
              :tabindex="tab === t.id ? 0 : -1"
              :aria-controls="'panel-' + t.id"
              :class="{ active: tab === t.id }"
              @click="tab = t.id"
            >
              {{ t.name }}
              <small v-if="t.count !== undefined">{{ t.count }}</small>
            </button>
          </div>
          <div v-if="dirty" class="save-actions">
            <button
              class="text-button"
              :disabled="busy"
              @click="draft = clone(current!.decision)"
            >
              重置</button
            ><button
              class="primary compact"
              :disabled="busy || !draft.criteria.some((c) => c.weight > 0)"
              @click="save"
            >
              {{ busy ? "保存中…" : "保存判断" }}
            </button>
          </div>
          <span v-else class="edition">{{
            locked ? "已锁定 · 只读快照" : "LIVE EXPLORATION"
          }}</span>
        </div>
        <div
          :id="'panel-' + tab"
          class="tab-content"
          role="tabpanel"
          :aria-labelledby="'tab-' + tab"
        >
          <Studio
            v-if="tab === 'studio'"
            :key="selected"
            :decision="draft"
            :baseline="current!.ranking"
            :locked="locked"
            @change="() => {}"
          /><EvidencePanel
            v-if="tab === 'evidence'"
            :key="selected + '-' + draft.version"
            :decision="draft"
            :locked="locked"
            :busy="busy"
            @add="evidence"
            @remove="removeEvidence"
          /><Journal
            v-if="tab === 'journal'"
            :decision="draft"
            @commit="openCommit"
            @review="modal = 'review'"
            @fork="fork"
          />
        </div>
        <footer class="decision-footer">
          <div>
            <span class="footer-marker">↳</span>
            <p v-if="!draft.experiment">
              选择不需要完美，<b>但可以更清醒。</b
              ><small>准备好之后，把一种可能带到现实里。</small>
            </p>
            <p v-else>
              判断已经留下，<b>{{
                draft.experiment.reviewedAt
                  ? "认识正在生长。"
                  : "现在让现实发声。"
              }}</b
              ><small>{{
                draft.experiment.reviewedAt
                  ? "下一轮探索，可以从这里出发。"
                  : "约定复盘：" + draft.experiment.dueDate
              }}</small>
            </p>
          </div>
          <button
            v-if="!locked"
            class="primary"
            :disabled="busy"
            @click="openCommit"
          >
            设计一次实践 <span>↗</span></button
          ><button
            v-else-if="
              draft.experiment &&
              !draft.experiment.reviewedAt &&
              !draft.archived
            "
            class="primary"
            @click="
              tab = 'journal';
              modal = 'review';
            "
          >
            记录实践回声 ↗</button
          ><button v-else class="secondary" :disabled="busy" @click="fork">
            开启新一轮 ↗
          </button>
        </footer>
        <div class="utility-row">
          <span>属于你的判断 · 不生成标准答案</span>
          <div>
            <button class="text-button" :disabled="busy" @click="fork">
              复制为新一轮</button
            ><button class="text-button" :disabled="busy" @click="archive">
              {{ draft.archived ? "恢复到工作室" : "归档这项决策" }}
            </button><button v-if="draft.archived" class="text-button danger" :disabled="busy" @click="deleteDecision">永久删除</button>
          </div>
        </div></template
      >
    </main>
    <div v-if="notice" class="toast" role="status">✓ {{ notice }}</div>
    <Modal
      v-if="modal === 'new' || modal === 'edit'"
      :title="modal === 'new' ? '打开一个新问题' : '编辑这项决策'"
      @close="!busy && (modal = null)"
      ><DecisionEditor
        :decision="modal === 'edit' ? draft : undefined"
        :busy="busy"
        @save="modal === 'new' ? create($event) : edit($event)"
      />
      <p v-if="error" class="form-error" role="alert">{{ error }}</p></Modal
    >
    <Modal
      v-if="modal === 'commit'"
      title="把选择，变成一次实践"
      @close="!busy && (modal = null)"
      ><form
        class="form-stack"
        @submit.prevent="
          action(
            '/commit',
            { ...practice, version: draft!.version },
            '判断已锁定，实践开始',
          )
        "
      >
        <p class="hint">
          保存不可修改的判断快照。写下一个可观察的信号，未来才能诚实复盘。
        </p>
        <label
          >这次选择<select v-model="practice.optionId">
            <option v-for="o in draft!.options" :value="o.id">
              {{ o.name }}
            </option>
          </select></label
        ><label
          >我相信会发生什么<textarea
            v-model="practice.hypothesis"
            required
            maxlength="1000"
            rows="3"
            placeholder="如果尝试这个方案，那么…"
          /></label
        ><label
          >怎样知道它有价值<textarea
            v-model="practice.signal"
            required
            maxlength="1000"
            rows="2"
            placeholder="一个具体、可观察的成功信号"
          /></label
        ><label
          >何时回来复盘（UTC 日期）<input
            v-model="practice.dueDate"
            required
            type="date"
            :min="new Date().toISOString().slice(0, 10)" /></label
        ><button class="primary" :disabled="busy">锁定判断，开始实践 ↗</button>
      </form>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p></Modal
    >
    <Modal
      v-if="modal === 'review'"
      title="听见现实的回声"
      @close="!busy && (modal = null)"
      ><form
        class="form-stack"
        @submit.prevent="
          action(
            '/review',
            { ...reflection, version: draft!.version },
            '复盘已保存，认识比输赢更重要',
          )
        "
      >
        <p class="hint">
          原假设：{{ draft!.experiment?.hypothesis }}<br />成功信号：{{
            draft!.experiment?.signal
          }}
        </p>
        <label
          >现实如何回应<select v-model="reflection.outcome">
            <option value="SUPPORTED">支持假设</option>
            <option value="MIXED">结果混合</option>
            <option value="REFUTED">推翻假设</option>
          </select></label
        ><label
          >发生了什么？你会如何调整下一次判断？<textarea
            v-model="reflection.learning"
            required
            maxlength="3000"
            rows="6"
            placeholder="同时记录意外和反例，不必让结果看起来漂亮。"
          /></label
        ><button class="primary" :disabled="busy">保存这次认识 ↗</button>
      </form>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p></Modal
    >
    <Modal v-if="modal === 'login'" title="进入你的工作室" @close="modal = null"
      ><form
        class="form-stack"
        @submit.prevent="
          credentials(login.username, login.password);
          modal = null;
          load();
        "
      >
        <p class="hint">
          这是一个单人私有工作室。使用部署时配置的账号进入；登录凭据只保留在当前页面内存中。
        </p>
        <label
          >账号<input
            v-model="login.username"
            required
            autocomplete="username" /></label
        ><label
          >密码<input
            v-model="login.password"
            type="password"
            required
            autocomplete="current-password" /></label
        ><button class="primary">进入工作室 ↗</button>
      </form></Modal
    >
    <Modal
      v-if="modal === 'guide'"
      title="一间留给可能性的工作室"
      @close="modal = null"
      ><div class="guide">
        <p class="intro-copy">
          折光，不替你选择。<br />它让你看见自己为什么这样选择。
        </p>
        <h3>01 / 展开可能</h3>
        <p>
          围绕一个真实问题，写下 2–5 个方案。用 2–6
          个「越高越好」的视角给出初始判断。
        </p>
        <h3>02 / 看见取舍</h3>
        <p>
          调整权重，观察排名如何变化。加入证据，用影响程度与信心修正基础分。敏感度仅为五点采样，不是统计结论。
        </p>
        <h3>03 / 实践与回声</h3>
        <p>
          锁定当时的判断，写下假设与成功信号。实践后记录结果；复盘不会重写历史。新一轮沿用基础分与视角，证据需重新核实。
        </p>
        <button class="secondary" :disabled="busy" @click="exportBackup">
          导出全部记录 ↓</button
        ><label class="secondary import-button"
          >从 JSON 备份导入<input
            type="file"
            accept="application/json,.json"
            :disabled="busy"
            @change="importBackup"
        /></label>
        <p class="hint">
          每次最多 50 项、512
          KB；导入生成新记录，不覆盖现有数据。完整数据库恢复见运维文档。
        </p>
        <div class="lens-tip">
          <p>
            分数是你判断的可视化，不是客观概率或专业建议。本产品为单人私有部署；请定期导出记录，并备份数据库。示例内容为虚构场景。
          </p>
        </div>
      </div></Modal
    >
  </div>
</template>
