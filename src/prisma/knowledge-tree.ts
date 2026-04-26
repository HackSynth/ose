/**
 * Shared 软件设计师 knowledge-point tree.
 *
 * Used by both `seed.ts` and `seed-51cto.ts`, plus the classifier
 * (`scripts/classify-51cto-questions.ts`) — single source of truth.
 *
 * IDs are stable so classifications keyed by ID stay valid across reseeds.
 */

export type KnowledgeTreeNode = {
  id: string;
  name: string;
  children: Array<{ id: string; name: string }>;
};

export const KNOWLEDGE_TREE: KnowledgeTreeNode[] = [
  {
    id: 'kp-1',
    name: '计算机组成与体系结构',
    children: [
      { id: 'kp-1-1', name: '数据表示' },
      { id: 'kp-1-2', name: '计算机结构' },
      { id: 'kp-1-3', name: '指令系统' },
      { id: 'kp-1-4', name: '存储系统' },
      { id: 'kp-1-5', name: '输入输出' },
      { id: 'kp-1-6', name: '总线' },
    ],
  },
  {
    id: 'kp-2',
    name: '操作系统',
    children: [
      { id: 'kp-2-1', name: '进程管理' },
      { id: 'kp-2-2', name: '存储管理' },
      { id: 'kp-2-3', name: '文件管理' },
      { id: 'kp-2-4', name: '设备管理' },
      { id: 'kp-2-5', name: '作业管理' },
    ],
  },
  {
    id: 'kp-3',
    name: '数据结构与算法',
    children: [
      { id: 'kp-3-1', name: '线性表' },
      { id: 'kp-3-2', name: '栈和队列' },
      { id: 'kp-3-3', name: '树与二叉树' },
      { id: 'kp-3-4', name: '图' },
      { id: 'kp-3-5', name: '查找' },
      { id: 'kp-3-6', name: '排序' },
      { id: 'kp-3-7', name: '算法设计策略' },
    ],
  },
  {
    id: 'kp-4',
    name: '软件工程',
    children: [
      { id: 'kp-4-1', name: '软件过程模型' },
      { id: 'kp-4-2', name: '需求分析' },
      { id: 'kp-4-3', name: '软件设计' },
      { id: 'kp-4-4', name: '软件测试' },
      { id: 'kp-4-5', name: '软件维护' },
      { id: 'kp-4-6', name: '项目管理' },
    ],
  },
  {
    id: 'kp-5',
    name: '数据库系统',
    children: [
      { id: 'kp-5-1', name: '关系数据库' },
      { id: 'kp-5-2', name: 'SQL语言' },
      { id: 'kp-5-3', name: '数据库设计' },
      { id: 'kp-5-4', name: '规范化理论' },
      { id: 'kp-5-5', name: '事务与并发控制' },
    ],
  },
  {
    id: 'kp-6',
    name: '计算机网络',
    children: [
      { id: 'kp-6-1', name: 'OSI模型' },
      { id: 'kp-6-2', name: 'TCP/IP协议' },
      { id: 'kp-6-3', name: '网络设备' },
      { id: 'kp-6-4', name: '网络安全' },
      { id: 'kp-6-5', name: 'Internet应用' },
    ],
  },
  {
    id: 'kp-7',
    name: '程序设计语言',
    children: [
      { id: 'kp-7-1', name: '编译与解释' },
      { id: 'kp-7-2', name: '文法与语言' },
      { id: 'kp-7-3', name: '程序设计范型' },
    ],
  },
  {
    id: 'kp-8',
    name: '面向对象技术',
    children: [
      { id: 'kp-8-1', name: '面向对象基础' },
      { id: 'kp-8-2', name: 'UML' },
      { id: 'kp-8-3', name: '设计模式' },
    ],
  },
  {
    id: 'kp-9',
    name: '知识产权与标准化',
    children: [
      { id: 'kp-9-1', name: '知识产权基础' },
      { id: 'kp-9-2', name: '软件保护' },
      { id: 'kp-9-3', name: '标准化' },
    ],
  },
  {
    id: 'kp-10',
    name: '数据流图与系统分析',
    children: [
      { id: 'kp-10-1', name: '数据流图绘制' },
      { id: 'kp-10-2', name: '系统分析方法' },
    ],
  },
];

/** Flat list of leaf knowledge-point IDs (used as enum in classifier). */
export function leafKnowledgePoints(): Array<{ id: string; name: string; parent: string }> {
  return KNOWLEDGE_TREE.flatMap((parent) =>
    parent.children.map((child) => ({
      id: child.id,
      name: child.name,
      parent: parent.name,
    }))
  );
}

/** All node IDs (parents + leaves) — used for upserting the full tree. */
export function allKnowledgePoints(): Array<{
  id: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  description: string;
}> {
  const out: Array<{
    id: string;
    name: string;
    parentId: string | null;
    sortOrder: number;
    description: string;
  }> = [];
  KNOWLEDGE_TREE.forEach((parent, parentIdx) => {
    out.push({
      id: parent.id,
      name: parent.name,
      parentId: null,
      sortOrder: parentIdx + 1,
      description: `${parent.name}相关考点`,
    });
    parent.children.forEach((child, childIdx) => {
      out.push({
        id: child.id,
        name: child.name,
        parentId: parent.id,
        sortOrder: childIdx + 1,
        description: `${parent.name} · ${child.name}`,
      });
    });
  });
  return out;
}

export const FALLBACK_KP = {
  id: 'kp-uncategorized',
  name: '未分类',
  description: '尚未分类的题目',
  sortOrder: 999,
};
