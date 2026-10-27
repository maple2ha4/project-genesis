<script setup lang="ts">
import { computed, ref } from "vue";
import { rank, type Decision, type Ranking } from "../model";
const props = defineProps<{
  decision: Decision;
  baseline: Ranking[];
  locked: boolean;
}>();
const emit = defineEmits<{ change: [] }>();
const ranking = computed(() => rank(props.decision));
const expanded = ref(false);
const leader = computed(() => ranking.value[0]);
const original = computed(() => props.baseline[0]);
const flipped = computed(
  () => leader.value?.optionId !== original.value?.optionId,
);
const name = (id: string) =>
  props.decision.options.find((o) => o.id === id)?.name;
const total = computed(() =>
  props.decision.criteria.reduce((s, c) => s + c.weight, 0),
);
function base(id: string) {
  return props.baseline.find((r) => r.optionId === id)?.score || 0;
}
function sensitivity(id: string) {
  const d = props.decision;
  const wins = new Set(
    [0, 25, 50, 75, 100].map(
      (weight) =>
        rank({
          ...d,
          criteria: d.criteria.map((c) => (c.id === id ? { ...c, weight } : c)),
        })[0]?.optionId,
    ),
  );
  return wins.size > 1 ? "能改变领先方案" : "采样内领先稳定";
}
</script>
<template>
  <div class="studio-grid">
    <section class="possibilities">
      <div class="section-label">
        <span>01 / 可能的路径</span
        ><span>{{ decision.options.length }} POSSIBILITIES</span>
      </div>
      <div class="ranking-note" :class="{ flipped }" role="status">
        <span class="status-dot" />{{
          total === 0
            ? "请保留至少一个非零权重"
            : flipped
              ? "视角改变，领先方案发生了翻转"
              : "此刻的倾向，只是探索的起点"
        }}<small v-if="flipped"
          >{{ name(original.optionId) }} → {{ name(leader.optionId) }}</small
        >
      </div>
      <article
        v-for="(o, index) in decision.options"
        :key="o.id"
        class="option-card"
        :class="{ leading: leader.optionId === o.id }"
      >
        <header>
          <span class="option-index">0{{ index + 1 }}</span
          ><span v-if="leader.optionId === o.id" class="leader-label"
            >当前倾向 ↗</span
          ><span v-else class="muted mono">另一种可能</span>
        </header>
        <h3>{{ o.name }}</h3>
        <p>{{ o.description || "尚未补充说明" }}</p>
        <div class="score-line">
          <div class="score-track">
            <div
              class="score-ghost"
              :style="{ width: base(o.id) * 10 + '%' }"
            />
            <div class="score-segments">
              <div
                v-for="(c, i) in decision.criteria"
                :key="c.id"
                :class="'tone-' + i"
                :style="{
                  width:
                    (ranking.find((r) => r.optionId === o.id)?.contributions[
                      c.id
                    ] || 0) *
                      10 +
                    '%',
                }"
              />
            </div>
          </div>
          <span class="score-value"
            >{{
              (ranking.find((r) => r.optionId === o.id)?.score || 0).toFixed(2)
            }}<small>/10</small></span
          >
        </div>
      </article>
      <div class="explanation">
        <span>如何理解这些光谱？</span>
        <p>
          每一段颜色对应一个视角。分数经过权重归一化；细线保留上次保存时的位置。它帮助你理解取舍，不替你做决定。
        </p>
      </div>
    </section>
    <aside class="lens-panel">
      <div class="section-label">
        <span>02 / 调整你的视角</span><span>↔</span>
      </div>
      <h3>什么对你更重要？</h3>
      <p class="muted">
        移动权重，看看选择如何折射。<br />所有视角都采用「越高越好」的评分。
      </p>
      <div v-for="(c, i) in decision.criteria" :key="c.id" class="lens">
        <label :for="'lens-' + c.id"
          ><span><i :class="'tone-' + i" />{{ c.name }}</span
          ><strong
            >{{ total ? Math.round((c.weight / total) * 100) : 0
            }}<small>%</small></strong
          ></label
        ><input
          :id="'lens-' + c.id"
          v-model.number="c.weight"
          :disabled="locked"
          type="range"
          min="0"
          max="100"
          step="1"
          @input="emit('change')"
        /><span class="lens-foot"
          >{{ sensitivity(c.id) }} <span>原始权重 {{ c.weight }}</span></span
        >
      </div>
      <div class="lens-tip">
        <span>观察，而不是追求高分</span>
        <p>
          若微调一个视角就使结论翻转，下一步也许应该是收集证据，而不是立即决定。敏感度按
          0 / 25 / 50 / 75 / 100 五个权重点采样。
        </p>
      </div>
    </aside>
  </div>
  <section class="score-editor">
    <button
      class="disclosure"
      :aria-expanded="expanded"
      @click="expanded = !expanded"
    >
      <span
        ><span class="mono">03 / </span>基础判断矩阵
        <small>为每条路径，给出你的初始判断</small></span
      ><span>{{ expanded ? "−" : "＋" }}</span>
    </button>
    <div v-if="expanded" class="table-scroll">
      <table>
        <caption class="sr-only">
          方案对各视角的基础评分，零到十分
        </caption>
        <thead>
          <tr>
            <th scope="col">可能的路径</th>
            <th v-for="c in decision.criteria" :key="c.id" scope="col">
              {{ c.name }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in decision.options" :key="o.id">
            <th scope="row">{{ o.name }}</th>
            <td v-for="c in decision.criteria" :key="c.id">
              <input
                v-model.number="o.scores[c.id]"
                :aria-label="o.name + ' / ' + c.name"
                type="number"
                min="0"
                max="10"
                step="0.5"
                :disabled="locked"
                @input="emit('change')"
              />
            </td>
          </tr>
        </tbody>
      </table>
      <p class="hint">
        基础分 + 证据影响 × 信心，限制在 0–10
        后再按权重加权。相同分数不代表方案相同。
      </p>
    </div>
  </section>
</template>
