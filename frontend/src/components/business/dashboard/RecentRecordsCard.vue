<template>
  <el-card class="business-card recent-records-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">最近记录</span>
      </div>
    </template>
    <el-tabs class="records-tabs">
      <el-tab-pane label="错题">
        <el-empty v-if="!recentMistakes?.length" description="暂无错题" :image-size="60" />
        <el-timeline v-else class="record-timeline">
          <el-timeline-item v-for="item in recentMistakes" :key="item.id" :timestamp="item.extra">
            <div class="record-item">
              <div class="record-title">{{ item.title }}</div>
              <div class="record-subtitle">{{ item.subtitle }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
      <el-tab-pane label="模拟考">
        <el-empty v-if="!recentExams?.length" description="暂无模拟记录" :image-size="60" />
        <el-timeline v-else class="record-timeline">
          <el-timeline-item v-for="item in recentExams" :key="item.id" :timestamp="item.extra">
            <div class="record-item">
              <div class="record-title">{{ item.title }}</div>
              <div class="record-subtitle">{{ item.subtitle }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
      <el-tab-pane label="笔记">
        <el-empty v-if="!recentNotes?.length" description="暂无笔记" :image-size="60" />
        <el-timeline v-else class="record-timeline">
          <el-timeline-item v-for="item in recentNotes" :key="item.id" :timestamp="item.extra">
            <div class="record-item">
              <div class="record-title">{{ item.title }}</div>
              <div class="record-subtitle">{{ item.subtitle }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  recentMistakes: any[];
  recentExams: any[];
  recentNotes: any[];
}>();
</script>

<style scoped>
.business-card {
  height: 100%;
}

.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

.records-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.record-timeline {
  padding-left: var(--space-2);
  margin-top: var(--space-4);
}

.record-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.record-title {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
}

.record-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
