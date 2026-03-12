import { expect, test } from '@playwright/test';
import fs from 'node:fs/promises';
import {
  authed,
  fillElInput,
  fillElNumber,
  loginByApi,
  prepareStableData,
  selectElOption,
  selectMultipleElOptions,
  waitForMessage,
} from './helpers';

test('核心备考链路可端到端跑通', async ({ page, request }) => {
  const token = await prepareStableData(request);
  await loginByApi(page, request);

  await test.step('设置考试日期并保存', async () => {
    await page.goto('/settings');
    await expect(page.getByTestId('settings-page')).toBeVisible();
    await fillElInput('settings-exam-date', page, '2026-06-30');
    await fillElNumber('settings-weekly-hours', page, '14');
    await page.getByTestId('settings-save-button').click();
    await waitForMessage(page, '设置已保存');
    await expect(page.getByTestId('settings-summary-exam-date')).toContainText('2026-06-30');
    await expect(page.getByTestId('settings-summary-weekly-hours')).toContainText('14 小时');
  });

  await test.step('自动生成学习计划', async () => {
    await page.goto('/plans');
    await expect(page.getByTestId('plan-page')).toBeVisible();
    await page.getByTestId('plan-generate-button').click();
    await waitForMessage(page, '计划已重新生成');
    await expect(page.getByTestId('plan-task-table')).toBeVisible();
    await expect(page.getByTestId('plan-task-count')).not.toContainText('0 项');
  });

  await test.step('浏览知识点', async () => {
    await page.goto('/knowledge');
    await expect(page.getByTestId('knowledge-page')).toBeVisible();
    await expect(page.getByTestId('knowledge-node-ARCH')).toContainText('软件架构设计');
    await expect(page.getByTestId('knowledge-node-REQ.USECASE')).toContainText('用例建模');
  });

  await test.step('完成一组上午题并自动判分', async () => {
    await page.goto('/practice');
    await expect(page.getByTestId('practice-page')).toBeVisible();
    await selectElOption(page.getByTestId('practice-session-type'), '知识点专项', page);
    await selectElOption(page.getByTestId('practice-question-type'), '上午题 (单选)', page);
    await selectElOption(page.getByTestId('practice-knowledge-point'), 'DB.TXN - 事务与并发控制', page);
    await fillElNumber('practice-count', page, '1');
    await page.getByTestId('practice-create-button').click();

    await expect(page.getByTestId('practice-question-title-0')).toContainText('事务一致性概念');
    await page.getByTestId('practice-answer-group-0').getByRole('radio', { name: /原子性/ }).click();
    await page.getByTestId('practice-submit-button').click();

    await waitForMessage(page, '练习已提交');
    await expect(page.getByTestId('practice-result-0')).toContainText('WRONG');
    await expect(page.getByTestId('practice-auto-score-0')).toContainText('0');
  });

  await test.step('完成一组下午题并提交自评', async () => {
    await page.goto('/practice');
    await selectElOption(page.getByTestId('practice-session-type'), '知识点专项', page);
    await selectElOption(page.getByTestId('practice-question-type'), '下午题 (案例)', page);
    await selectElOption(page.getByTestId('practice-knowledge-point'), 'REQ.USECASE - 用例建模', page);
    await fillElNumber('practice-count', page, '1');
    await page.getByTestId('practice-create-button').click();

    await expect(page.getByTestId('practice-question-title-0')).toContainText('在线考试系统需求分析');
    await fillElInput('practice-answer-text-0', page, '先识别考生、管理员、阅卷人员，再梳理主成功场景与异常流。');
    await fillElNumber('practice-score-input-0', page, '12');
    await page.getByTestId('practice-submit-button').click();

    await waitForMessage(page, '练习已提交');
    await expect(page.getByTestId('practice-subjective-score-0')).toContainText('12');
  });

  await test.step('验证错题自动入库', async () => {
    await page.goto('/mistakes');
    await expect(page.getByTestId('mistakes-page')).toBeVisible();
    await expect(page.getByTestId('mistakes-table')).toContainText('事务一致性概念');
  });

  await test.step('创建并提交模拟考试', async () => {
    const examName = `E2E 模考 ${Date.now()}`;
    await page.goto('/exams');
    await expect(page.getByTestId('exams-page')).toBeVisible();
    await fillElInput('exam-name', page, examName);
    await selectElOption(page.getByTestId('exam-type'), '上午卷', page);
    await fillElNumber('exam-duration', page, '90');
    await fillElInput('exam-description', page, 'E2E 自动化创建的上午卷。');
    await selectMultipleElOptions(page.getByTestId('exam-question-ids'), ['面向对象特性辨析', '事务一致性概念'], page);
    await page.getByTestId('exam-create-button').click();
    await waitForMessage(page, '模拟卷已创建');

    const examRow = page.locator('.el-table__row').filter({ hasText: examName }).first();
    await expect(examRow).toBeVisible();
    await examRow.getByRole('button', { name: '开始' }).click();

    await expect(page.getByTestId('exam-attempt-card')).toBeVisible();
    const answerRows = await page.locator('[data-testid^="exam-answer-card-"]').all();
    for (const row of answerRows) {
      const title = await row.locator('strong').textContent();
      if (title?.includes('面向对象特性辨析')) {
        await row.getByText('A. 隐藏对象内部实现细节，仅暴露稳定接口').click();
      } else if (title?.includes('事务一致性概念')) {
        await row.getByText('B. 一致性').click();
      }
    }
    await fillElInput('exam-self-review', page, '本次模考主要验证自动化链路。');
    await page.getByTestId('exam-submit-button').click();

    await waitForMessage(page, '已交卷');
    await expect(page.getByTestId('exam-attempt-status')).toContainText('SUBMITTED');
    await expect(page.getByTestId('exam-attempt-card')).toContainText('自动得分');
  });

  await test.step('验证统计页数据展示', async () => {
    await page.goto('/analytics');
    await expect(page.getByTestId('analytics-page')).toBeVisible();
    await expect(page.getByTestId('analytics-cards')).toContainText('计划完成率');
    await expect(page.getByTestId('analytics-knowledge-chart-card')).toBeVisible();
    await expect(page.getByTestId('analytics-exam-chart-card')).toBeVisible();
  });

  await test.step('执行整包导出', async () => {
    await page.goto('/settings');
    const downloadPromise = page.waitForEvent('download');
    await page.getByTestId('settings-export-button').click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe('ose-export.json');
    const downloadPath = await download.path();
    expect(downloadPath).toBeTruthy();
    const payload = await fs.readFile(downloadPath!, 'utf-8');
    expect(payload).toContain('knowledgeTree');
    expect(payload).toContain('questions');
  });

  await test.step('API 侧确认计划和统计仍可读取', async () => {
    const analyticsResponse = await authed(request, token, 'GET', '/api/analytics/summary');
    expect(analyticsResponse.data.cards.length).toBeGreaterThan(0);
  });
});
