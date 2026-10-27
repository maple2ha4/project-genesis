<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from "vue";
defineProps<{ title: string }>();
const emit = defineEmits<{ close: [] }>();
const panel = ref<HTMLElement>();
const previous = document.activeElement as HTMLElement | null;
function keys(e: KeyboardEvent) {
  if (e.key === "Escape") emit("close");
  if (e.key === "Tab") {
    const els = panel.value?.querySelectorAll<HTMLElement>(
      'button:not(:disabled),input,textarea,select,[tabindex="0"]',
    );
    if (!els?.length) return;
    const first = els[0],
      last = els[els.length - 1];
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  }
}
onMounted(() => {
  panel.value?.querySelector<HTMLElement>("input,textarea,button")?.focus();
  document.addEventListener("keydown", keys);
  document.body.style.overflow = "hidden";
});
onBeforeUnmount(() => {
  document.removeEventListener("keydown", keys);
  document.body.style.overflow = "";
  previous?.focus();
});
</script>
<template>
  <Teleport to="body"
    ><div class="modal-shade" @click.self="emit('close')">
      <section
        ref="panel"
        class="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <header>
          <h2 id="modal-title">{{ title }}</h2>
          <button
            class="icon-button"
            aria-label="关闭对话框"
            @click="emit('close')"
          >
            ×
          </button>
        </header>
        <slot />
      </section></div
  ></Teleport>
</template>
