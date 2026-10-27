<script setup lang="ts">
import { computed } from "vue";
import { stage, type View } from "../model";
const props = defineProps<{
  views: View[];
  selected: string;
  busy: boolean;
  loading: boolean;
}>();
const filter = defineModel<string>("filter", { required: true });
const query = defineModel<string>("query", { required: true });
const emit = defineEmits<{
  guide: [];
  create: [];
  select: [view: View];
  export: [];
}>();
const shown = computed(() =>
  props.views.filter(
    (v) =>
      (filter.value === "archived"
        ? v.decision.archived
        : !v.decision.archived) &&
      (filter.value === "all" ||
        filter.value === "active" ||
        filter.value === "archived" ||
        stage(v.decision) === filter.value) &&
      (v.decision.title + " " + v.decision.question)
        .toLowerCase()
        .includes(query.value.toLowerCase()),
  ),
);
</script>
<template>
  <aside class="sidebar">
    <a class="brand" href="#" @click.prevent="emit('guide')"
      ><svg width="34" height="36" viewBox="0 0 34 36" aria-hidden="true">
        <path
          d="M17 2L32 31H2Z"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
        />
        <path
          d="M17 2V31M2 31L24 16M32 31L10 16"
          fill="none"
          stroke="currentColor"
          stroke-width=".8"
        /></svg
      ><span>折光 <b>PRISM</b><small>把选择，变成发现。</small></span></a
    >
    <div class="sidebar-rule" />
    <div class="workspace-label">
      <span class="status-dot" />个人决策工作室 <span>01</span>
    </div>
    <button class="new-button" @click="emit('create')">
      <span>＋</span> 打开一个新问题 <span>↗</span></button
    ><label class="search-label"
      ><span aria-hidden="true">⌕</span
      ><input
        v-model="query"
        aria-label="搜索决策"
        placeholder="寻找一个问题…"
      /><kbd>/</kbd></label
    ><label class="filter-label"
      >查看<select v-model="filter" aria-label="筛选决策">
        <option value="active">工作室中的全部</option>
        <option>探索中</option>
        <option>实践中</option>
        <option>已复盘</option>
        <option value="archived">已归档</option>
      </select></label
    >
    <nav class="decision-nav" aria-label="决策列表">
      <button
        v-for="v in shown"
        :key="v.decision.id"
        :class="{ selected: selected === v.decision.id }"
        @click="emit('select', v)"
      >
        <span class="nav-stage"
          ><i :class="{ warm: !!v.decision.experiment }" />{{
            stage(v.decision)
          }}</span
        ><span class="nav-title">{{ v.decision.title }}</span
        ><span class="nav-meta"
          >{{ v.decision.options.length }} 种可能 <span>↗</span></span
        >
      </button>
      <p v-if="!shown.length && !loading" class="nav-empty">
        {{ query ? "没有匹配的问题。" : "这里留给下一个值得探索的问题。" }}
      </p>
    </nav>
    <div class="sidebar-bottom">
      <div class="small-prism" aria-hidden="true">△</div>
      <p>答案会变。<br />理解选择的方式，会留下来。</p>
      <button class="text-button" @click="emit('guide')">
        关于这间工作室 ↗</button
      ><button class="text-button" :disabled="busy" @click="emit('export')">
        导出全部记录 ↓
      </button>
    </div>
  </aside>
</template>
