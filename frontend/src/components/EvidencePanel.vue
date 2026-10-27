<script setup lang="ts">
import { ref } from "vue";
import type { Decision } from "../model";
const props = defineProps<{
  decision: Decision;
  locked: boolean;
  busy: boolean;
}>();
const emit = defineEmits<{ add: [body: unknown]; remove: [id: string] }>();
const form = ref({
  optionId: props.decision.options[0].id,
  criterionId: props.decision.criteria[0].id,
  text: "",
  source: "",
  impact: 1,
  confidence: 0.7,
});
</script>
<template>
  <div class="evidence-grid">
    <section>
      <div class="section-label">证据不是装饰，它应该改变判断</div>
      <h2 class="section-heading">让现实进入选择。</h2>
      <p class="muted">
        一段观察，一次访谈，一个反例。记录它支持或削弱哪一个视角，以及你有多确信。
      </p>
      <div v-if="!decision.evidence.length" class="empty-inline">
        <span>↳</span>
        <h3>还没有证据</h3>
        <p>先写下判断也没关系。<br />当现实给出线索，让它在这里留下痕迹。</p>
      </div>
      <article v-for="e in decision.evidence" :key="e.id" class="evidence-card">
        <div class="section-label">
          <span
            >{{ decision.options.find((o) => o.id === e.optionId)?.name }} /
            {{
              decision.criteria.find((c) => c.id === e.criterionId)?.name
            }}</span
          ><strong :class="e.impact >= 0 ? 'positive' : 'negative'"
            >{{ e.impact >= 0 ? "+" : ""
            }}{{ (e.impact * e.confidence).toFixed(2) }}</strong
          >
        </div>
        <p>{{ e.text }}</p>
        <small
          >{{ e.source || "未填写来源" }} · 影响 {{ e.impact }} × 信心
          {{ Math.round(e.confidence * 100) }}%</small
        ><button
          v-if="!locked"
          class="text-button danger"
          :disabled="busy"
          @click="emit('remove', e.id)"
        >
          移除证据
        </button>
      </article>
    </section>
    <form
      v-if="!locked"
      class="form-stack inset-panel"
      @submit.prevent="emit('add', { ...form })"
    >
      <h3>新增一条现实线索</h3>
      <label
        >关联方案<select v-model="form.optionId">
          <option v-for="o in decision.options" :value="o.id">
            {{ o.name }}
          </option>
        </select></label
      ><label
        >影响的视角<select v-model="form.criterionId">
          <option v-for="c in decision.criteria" :value="c.id">
            {{ c.name }}
          </option>
        </select></label
      ><label
        >观察或事实<textarea
          v-model="form.text"
          required
          maxlength="1000"
          rows="3"
          placeholder="发生了什么？尽量写事实，而非结论。"
        /></label
      ><label
        >来源 <span class="optional">选填</span
        ><input
          v-model="form.source"
          maxlength="500"
          placeholder="例如：10 月 12 日的街坊访谈" /></label
      ><label
        >对基础分的影响
        <strong>{{ form.impact > 0 ? "+" : "" }}{{ form.impact }}</strong
        ><input
          v-model.number="form.impact"
          aria-label="证据影响"
          type="range"
          min="-3"
          max="3"
          step="0.5"
        /><span class="range-label"
          ><span>削弱 −3</span><span>支持 +3</span></span
        ></label
      ><label
        >对这条线索的信心
        <strong>{{ Math.round(form.confidence * 100) }}%</strong
        ><input
          v-model.number="form.confidence"
          aria-label="证据信心"
          type="range"
          min="0"
          max="1"
          step="0.1" /></label
      ><button class="primary" :disabled="busy">
        {{ busy ? "正在记录…" : "记录证据，重新折光" }} <span>↗</span>
      </button>
    </form>
  </div>
</template>
