<template>
  <el-card class="panel-card">
    <div style="display:flex;flex-direction:column;gap:8px;">
      <span style="color:#6b7280;font-size:14px;">{{ title }}</span>
      <strong style="font-size:28px;">{{ value }}</strong>
      <span style="color:#94a3b8;font-size:12px;">{{ hint }}</span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ title: string; value: string | number; hint?: string }>();
</script>
