<template>
  <el-drawer
    v-model="visible"
    direction="ltr"
    size="240px"
    :with-header="false"
    class="mobile-menu-drawer"
  >
    <slot />
  </el-drawer>
</template>

<script setup lang="ts">
const visible = defineModel<boolean>({ required: true });
</script>
