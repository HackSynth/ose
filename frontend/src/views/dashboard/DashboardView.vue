<template>
  <div class="page-container" data-testid="dashboard-page">
    <div class="page-header">
      <div>
        <h2 style="margin:0;">备考总览</h2>
        <p style="margin:6px 0 0;color:#64748b;">距离考试还有 {{ overview?.daysUntilExam ?? '-' }} 天，先做今天最重要的任务。</p>
      </div>
      <el-tag size="large" type="danger" data-testid="dashboard-exam-date-tag">目标考试日：{{ overview?.examDate || '-' }}</el-tag>
    </div>

    <div class="card-grid" data-testid="dashboard-summary-cards">
      <StatCard
        v-for="item in overview?.summaryCards || []"
        :key="item.label"
        :data-testid="`dashboard-card-${item.label}`"
        :title="item.label"
        :value="item.value"
        :hint="item.hint"
      />
      <StatCard
        data-testid="dashboard-card-due-reviews"
        title="待复习错题"
        :value="overview?.dueReviewCount || 0"
        hint="到期错题复习提醒"
      />
    </div>

    <div class="split-layout">
      <el-card class="panel-card" data-testid="dashboard-today-tasks">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span>今日学习任务</span>
            <el-tag data-testid="dashboard-today-task-count">{{ overview?.todayTasks?.length || 0 }} 项</el-tag>
          </div>
        </template>
        <el-empty v-if="!overview?.todayTasks?.length" description="今天没有任务，去生成计划吧" />
        <el-timeline v-else>
          <el-timeline-item v-for="task in overview?.todayTasks" :key="task.id" :timestamp="task.scheduledDate">
            <strong>{{ task.title }}</strong>
            <div style="color:#64748b;margin-top:6px;">{{ task.description }}</div>
            <div style="margin-top:8px;display:flex;gap:8px;">
              <el-tag size="small">{{ task.phase }}</el-tag>
              <el-tag size="small" type="success">{{ task.estimatedMinutes }} 分钟</el-tag>
              <el-tag size="small" :type="task.status === 'DONE' ? 'success' : 'info'">{{ task.status }}</el-tag>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="panel-card" data-testid="dashboard-completion">
        <template #header><span>完成情况</span></template>
        <div style="display:flex;flex-direction:column;gap:16px;">
          <div>
            <div style="display:flex;justify-content:space-between;margin-bottom:8px;">
              <span>本周</span>
              <span data-testid="dashboard-week-completion">{{ overview?.weekCompletion?.done || 0 }}/{{ overview?.weekCompletion?.total || 0 }}</span>
            </div>
            <el-progress :percentage="percentage(overview?.weekCompletion?.done, overview?.weekCompletion?.total)" />
          </div>
          <div>
            <div style="display:flex;justify-content:space-between;margin-bottom:8px;">
              <span>本月</span>
              <span data-testid="dashboard-month-completion">{{ overview?.monthCompletion?.done || 0 }}/{{ overview?.monthCompletion?.total || 0 }}</span>
            </div>
            <el-progress status="success" :percentage="percentage(overview?.monthCompletion?.done, overview?.monthCompletion?.total)" />
          </div>
        </div>
      </el-card>
    </div>

    <div class="split-layout">
      <el-card class="panel-card" data-testid="dashboard-review-reminders">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span>错题复习提醒</span>
            <el-button link type="primary" @click="router.push('/mistakes')">查看错题本</el-button>
          </div>
        </template>
        <el-empty v-if="!overview?.reviewReminders?.length" description="当前没有到期错题" />
        <div v-else style="display:flex;flex-direction:column;gap:12px;">
          <div v-for="item in overview?.reviewReminders" :key="item.id" style="padding:12px;border:1px solid #e5e7eb;border-radius:12px;">
            <div style="display:flex;justify-content:space-between;gap:12px;align-items:center;">
              <div>
                <strong>{{ item.questionTitle }}</strong>
                <div style="color:#64748b;font-size:13px;margin-top:4px;">{{ item.knowledgePointName }} · {{ item.reasonType }}</div>
              </div>
              <el-tag type="warning">{{ item.nextReviewAt }}</el-tag>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="panel-card" data-testid="dashboard-practice-recommendations">
        <template #header><span>薄弱知识点推荐练习</span></template>
        <el-empty v-if="!overview?.practiceRecommendations?.length" description="当前没有推荐项" />
        <div v-else style="display:flex;flex-direction:column;gap:12px;">
          <div v-for="item in overview?.practiceRecommendations" :key="item.knowledgePointId" style="padding:12px;border:1px solid #e5e7eb;border-radius:12px;display:flex;justify-content:space-between;gap:12px;align-items:center;">
            <div>
              <strong>{{ item.knowledgePointName }}</strong>
              <div style="color:#64748b;font-size:13px;margin-top:4px;">掌握度 {{ item.masteryLevel }}% · 正确率 {{ item.accuracy }}% · 待处理错题 {{ item.pendingMistakes }}</div>
              <div style="color:#334155;font-size:13px;margin-top:6px;">{{ item.reason }}</div>
            </div>
            <el-button type="primary" @click="goToPractice(item)">去练习</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div class="split-layout">
      <el-card class="panel-card" data-testid="dashboard-knowledge-overview">
        <template #header><span>知识域掌握度</span></template>
        <div style="display:flex;flex-direction:column;gap:12px;">
          <div v-for="item in overview?.knowledgeOverview || []" :key="item.name">
            <div style="display:flex;justify-content:space-between;margin-bottom:6px;">
              <span>{{ item.name }}</span>
              <span>{{ item.masteryLevel }}%</span>
            </div>
            <el-progress :percentage="item.masteryLevel" />
          </div>
        </div>
      </el-card>

      <el-card class="panel-card" data-testid="dashboard-recent-records">
        <template #header><span>最近记录</span></template>
        <el-tabs>
          <el-tab-pane label="错题">
            <el-empty v-if="!overview?.recentMistakes?.length" description="暂无错题" />
            <el-timeline v-else>
              <el-timeline-item v-for="item in overview?.recentMistakes" :key="item.id" :timestamp="item.extra">
                <strong>{{ item.title }}</strong>
                <div style="color:#64748b;">{{ item.subtitle }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
          <el-tab-pane label="模拟考">
            <el-empty v-if="!overview?.recentExams?.length" description="暂无模拟记录" />
            <el-timeline v-else>
              <el-timeline-item v-for="item in overview?.recentExams" :key="item.id" :timestamp="item.extra">
                <strong>{{ item.title }}</strong>
                <div style="color:#64748b;">{{ item.subtitle }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
          <el-tab-pane label="笔记">
            <el-empty v-if="!overview?.recentNotes?.length" description="暂无笔记" />
            <el-timeline v-else>
              <el-timeline-item v-for="item in overview?.recentNotes" :key="item.id" :timestamp="item.extra">
                <strong>{{ item.title }}</strong>
                <div style="color:#64748b;">{{ item.subtitle }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '@/api';
import StatCard from '@/components/StatCard.vue';

const router = useRouter();
const overview = ref<any>(null);

const load = async () => {
  overview.value = await api.dashboard();
};

const percentage = (done = 0, total = 0) => (total ? Math.round((done / total) * 100) : 0);

const goToPractice = (item: any) => {
  router.push({
    path: '/practice',
    query: {
      sessionType: 'KNOWLEDGE',
      questionType: 'MORNING_SINGLE',
      knowledgePointId: String(item.knowledgePointId),
    },
  });
};

onMounted(load);
</script>
