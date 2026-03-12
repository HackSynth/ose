<template>
  <el-card class="business-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">薄弱知识点推荐练习</span>
      </div>
    </template>
    
    <el-empty v-if="!recommendations?.length" description="当前没有推荐项" :image-size="80" />
    
    <div v-else class="recommend-list">
      <div v-for="item in recommendations" :key="item.knowledgePointId" class="recommend-item">
        <div class="recommend-main">
          <div class="recommend-title">{{ item.knowledgePointName }}</div>
          <div class="recommend-stats">
            掌握度 {{ item.masteryLevel }}% · 正确率 {{ item.accuracy }}% · 待处理错题 {{ item.pendingMistakes }}
          </div>
          <div class="recommend-reason">{{ item.reason }}</div>
        </div>
        <el-button type="primary" size="small" plain @click="$emit('practice', item)">
          去练习
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  recommendations: any[];
}>();

defineEmits(['practice']);
</script>

<style scoped>
.business-card {
  height: 100%;
}

.card-title {
  font-weight: 700;
  color: var(--text-primary);
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.recommend-item {
  padding: var(--space-4);
  background: var(--bg-app);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
  transition: border-color 0.2s;
}

.recommend-item:hover {
  border-color: var(--color-primary);
}

.recommend-main {
  flex: 1;
}

.recommend-title {
  font-weight: 700;
  color: var(--text-primary);
  font-size: 15px;
}

.recommend-stats {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 4px;
}

.recommend-reason {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 8px;
  line-height: 1.4;
}
</style>
