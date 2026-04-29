import path from 'path';

import type { AIImageStyle } from '@/lib/ai/image-types';

export type WrongNoteImageStyleAnchor = {
  style: AIImageStyle;
  label: string;
  publicPath: string;
  filePath: string;
  version: string;
  promptInstruction: string;
};

const anchorVersion = '2026-04-29-v1';

const anchors: Record<
  AIImageStyle,
  Omit<WrongNoteImageStyleAnchor, 'style' | 'filePath' | 'version'>
> = {
  clean_education_card: {
    label: '清爽复盘卡',
    publicPath: '/ai-image-anchors/clean-education-card.png',
    promptInstruction:
      '沿用参考图的清爽教育卡片风格：浅色背景、精确圆角卡片、左侧过程图区、右侧四个信息框、留白克制、标题醒目、文字层级清晰。',
  },
  flowchart: {
    label: '流程图风格',
    publicPath: '/ai-image-anchors/flowchart.png',
    promptInstruction:
      '沿用参考图的流程图风格：左侧用节点、菱形判断、箭头和流程顺序表达解题动作，右侧四个框保持强边框与清晰分区。',
  },
  hand_drawn: {
    label: '手绘草图风格',
    publicPath: '/ai-image-anchors/hand-drawn.png',
    promptInstruction:
      '沿用参考图的手绘草图风格：纸张质感、略不规则手绘线框、板书式箭头、重点用少量彩色标注，整体像课堂手写讲义。',
  },
};

export function getWrongNoteImageStyleAnchor(style: AIImageStyle): WrongNoteImageStyleAnchor {
  const anchor = anchors[style] ?? anchors.clean_education_card;
  const relativePath = anchor.publicPath.replace(/^\//, '');
  return {
    ...anchor,
    style,
    version: anchorVersion,
    filePath: path.join(process.cwd(), 'public', relativePath),
  };
}
