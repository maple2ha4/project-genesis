<script setup lang="ts">
import { outcomeName, type Decision } from "../model";
defineProps<{ decision: Decision }>();
defineEmits<{ commit: []; review: []; fork: [] }>();
const date = (s: string) => new Date(s).toLocaleDateString("zh-CN");
</script>
<template>
  <section class="journal">
    <div class="section-label">从判断走向现实 / FIELD NOTES</div>
    <h2 class="section-heading">给选择一个回声。</h2>
    <p class="muted">
      不只记住选了什么，也保留当时为什么这样选。<br />实践结束后，让结果修正你的判断方式。
    </p>
    <ol class="timeline">
      <li>
        <span class="timeline-dot" /><small
          >01 / {{ date(decision.createdAt) }}</small
        >
        <h3>打开一个问题</h3>
        <p>{{ decision.question }}</p>
      </li>
      <li :class="{ pending: !decision.experiment }">
        <span class="timeline-dot" /><small
          >02 /
          {{
            decision.experiment
              ? date(decision.experiment.committedAt)
              : "等待行动"
          }}</small
        >
        <h3>
          {{
            decision.experiment
              ? "已经选择：" +
                decision.options.find(
                  (o) => o.id === decision.experiment?.optionId,
                )?.name
              : "把一种可能，带到现实里"
          }}
        </h3>
        <template v-if="decision.experiment"
          ><dl>
            <dt>当时的假设</dt>
            <dd>{{ decision.experiment.hypothesis }}</dd>
            <dt>可观察的成功信号</dt>
            <dd>{{ decision.experiment.signal }}</dd>
            <dt>约定复盘</dt>
            <dd>{{ decision.experiment.dueDate }}（UTC 日期）</dd>
          </dl>
          <details>
            <summary>查看锁定时的判断快照</summary>
            <div class="snapshot">
              <p v-for="r in decision.experiment.snapshot" :key="r.optionId">
                <span>{{
                  decision.options.find((o) => o.id === r.optionId)?.name
                }}</span
                ><strong>{{ r.score.toFixed(2) }}</strong>
              </p>
              <div class="snapshot-lenses">
                <span v-for="c in decision.experiment.lenses"
                  >{{ c.name }} · {{ c.weight }}</span
                >
              </div>
              <small>快照不可修改；复盘不会重写当时的判断。</small>
            </div>
          </details></template
        ><template v-else
          ><p>锁定会保存当前分数与权重。之后这轮探索变为只读。</p>
          <button
            v-if="!decision.archived"
            class="primary"
            @click="$emit('commit')"
          >
            设计一次实践 <span>↗</span>
          </button></template
        >
      </li>
      <li :class="{ pending: !decision.experiment?.reviewedAt }">
        <span class="timeline-dot" /><small
          >03 /
          {{
            decision.experiment?.reviewedAt
              ? date(decision.experiment.reviewedAt)
              : "等待回声"
          }}</small
        >
        <h3>
          {{
            decision.experiment?.outcome
              ? outcomeName[decision.experiment.outcome]
              : "让结果回来，修正下一次选择"
          }}
        </h3>
        <p v-if="decision.experiment?.learning" class="preserve-lines">
          {{ decision.experiment.learning }}
        </p>
        <p v-else>
          无需证明自己正确。一次推翻假设的实践，也是一份有价值的答案。
        </p>
        <button
          v-if="
            decision.experiment &&
            !decision.experiment.reviewedAt &&
            !decision.archived
          "
          class="primary"
          @click="$emit('review')"
        >
          记录复盘 <span>↗</span></button
        ><button
          v-if="decision.experiment?.reviewedAt"
          class="secondary"
          @click="$emit('fork')"
        >
          带着认识，开启新一轮 ↗
        </button>
      </li>
    </ol>
  </section>
</template>
