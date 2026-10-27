<script setup lang="ts">
import { ref } from "vue";
import { sample, clone, type Decision } from "../model";
const props = defineProps<{ decision?: Decision; busy: boolean }>();
const emit = defineEmits<{ save: [body: unknown] }>();
const value = ref<
  Pick<
    Decision,
    "version" | "title" | "question" | "horizon" | "options" | "criteria"
  >
>(
  props.decision
    ? clone(props.decision)
    : {
        ...sample(),
        title: "",
        question: "",
        horizon: "",
        options: [
          {
            id: "a",
            name: "",
            description: "",
            scores: { c1: 5, c2: 5, c3: 5 },
          },
          {
            id: "b",
            name: "",
            description: "",
            scores: { c1: 5, c2: 5, c3: 5 },
          },
        ],
        criteria: [
          { id: "c1", name: "长期价值", weight: 40 },
          { id: "c2", name: "可行程度", weight: 35 },
          { id: "c3", name: "投入成本低", weight: 25 },
        ],
      },
);
function addOption() {
  value.value.options.push({
    id: crypto.randomUUID(),
    name: "",
    description: "",
    scores: Object.fromEntries(value.value.criteria.map((c) => [c.id, 5])),
  });
}
function addCriterion() {
  const id = crypto.randomUUID();
  value.value.criteria.push({ id, name: "", weight: 20 });
  value.value.options.forEach((o) => (o.scores[id] = 5));
}
function removeCriterion(id: string) {
  value.value.criteria = value.value.criteria.filter((c) => c.id !== id);
  value.value.options.forEach((o) => delete o.scores[id]);
}
</script>
<template>
  <form
    class="form-stack"
    @submit.prevent="
      emit('save', {
        version: value.version,
        title: value.title,
        question: value.question,
        horizon: value.horizon,
        options: value.options,
        criteria: value.criteria,
      })
    "
  >
    <label
      >决策标题<input
        v-model="value.title"
        required
        maxlength="100"
        placeholder="什么选择，值得认真想一想？"
    /></label>
    <label
      >你真正想解决的问题<textarea
        v-model="value.question"
        required
        maxlength="2000"
        rows="3"
        placeholder="写下处境、限制和你在意的改变。"
      />
    </label>
    <label
      >时间范围 <span class="optional">选填</span
      ><input
        v-model="value.horizon"
        maxlength="100"
        placeholder="例如：用四周做一次小规模尝试"
    /></label>
    <fieldset>
      <legend>可能的路径 <span>2–5 个</span></legend>
      <div v-for="(o, i) in value.options" :key="o.id" class="editor-item">
        <div class="inline">
          <span class="mono">0{{ i + 1 }}</span
          ><input
            v-model="o.name"
            :aria-label="`方案 ${i + 1} 名称`"
            required
            maxlength="80"
            placeholder="给这个方案一个名字"
          /><button
            v-if="value.options.length > 2"
            type="button"
            class="text-button"
            @click="value.options.splice(i, 1)"
            :aria-label="`移除方案 ${i + 1}`"
          >
            移除
          </button>
        </div>
        <input
          v-model="o.description"
          :aria-label="`方案 ${i + 1} 说明`"
          maxlength="500"
          placeholder="一句话描述它（选填）"
        />
      </div>
      <button
        v-if="value.options.length < 5"
        type="button"
        class="text-button"
        @click="addOption"
      >
        ＋ 增加一种可能
      </button>
    </fieldset>
    <fieldset>
      <legend>看待选择的视角 <span>2–6 个</span></legend>
      <p class="hint">
        统一使用「越高越好」的标准，例如把成本写为「投入成本低」。
      </p>
      <div v-for="(c, i) in value.criteria" :key="c.id" class="inline">
        <input
          v-model="c.name"
          :aria-label="`视角 ${i + 1} 名称`"
          required
          maxlength="40"
        /><button
          v-if="value.criteria.length > 2"
          type="button"
          class="text-button"
          :aria-label="`移除视角 ${i + 1}`"
          @click="removeCriterion(c.id)"
        >
          移除
        </button>
      </div>
      <button
        v-if="value.criteria.length < 6"
        type="button"
        class="text-button"
        @click="addCriterion"
      >
        ＋ 增加一个视角
      </button>
    </fieldset>
    <p class="hint">
      创建后可在工作台调整权重与评分。它们是你的判断，不是客观概率。
    </p>
    <button class="primary" :disabled="busy">
      {{ busy ? "正在保存…" : decision ? "保存决策信息" : "创建这项决策" }}
      <span>↗</span>
    </button>
  </form>
</template>
