export interface Criterion {
  id: string;
  name: string;
  weight: number;
}
export interface Option {
  id: string;
  name: string;
  description: string;
  scores: Record<string, number>;
}
export interface Evidence {
  id: string;
  optionId: string;
  criterionId: string;
  text: string;
  source: string;
  impact: number;
  confidence: number;
}
export interface Ranking {
  optionId: string;
  score: number;
  contributions: Record<string, number>;
}
export interface Experiment {
  optionId: string;
  hypothesis: string;
  signal: string;
  dueDate: string;
  snapshot: Ranking[];
  lenses: Criterion[];
  committedAt: string;
  outcome: "SUPPORTED" | "MIXED" | "REFUTED" | null;
  learning: string | null;
  reviewedAt: string | null;
}
export interface Decision {
  id: string;
  version: number;
  title: string;
  question: string;
  horizon: string;
  options: Option[];
  criteria: Criterion[];
  evidence: Evidence[];
  experiment: Experiment | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface View {
  decision: Decision;
  ranking: Ranking[];
}
export function rank(
  d: Pick<Decision, "options" | "criteria" | "evidence">,
): Ranking[] {
  const total = d.criteria.reduce((sum, c) => sum + c.weight, 0);
  return d.options
    .map((o) => {
      const contributions = Object.fromEntries(
        d.criteria.map((c) => {
          const shift = d.evidence
            .filter((e) => e.optionId === o.id && e.criterionId === c.id)
            .reduce((sum, e) => sum + e.impact * e.confidence, 0);
          return [
            c.id,
            total > 0
              ? (Math.max(0, Math.min(10, o.scores[c.id] + shift)) * c.weight) /
                total
              : 0,
          ];
        }),
      );
      return {
        optionId: o.id,
        score: Object.values(contributions).reduce((a, b) => a + b, 0),
        contributions,
      };
    })
    .sort((a, b) => b.score - a.score || a.optionId.localeCompare(b.optionId));
}
export const stage = (d: Decision) =>
  d.archived
    ? "已归档"
    : d.experiment?.reviewedAt
      ? "已复盘"
      : d.experiment
        ? "实践中"
        : "探索中";
export const outcomeName = {
  SUPPORTED: "支持假设",
  MIXED: "结果混合",
  REFUTED: "推翻假设",
};
export const clone = <T>(v: T): T => JSON.parse(JSON.stringify(v));
export function sample() {
  return {
    version: 0,
    title: "让空置的一楼，重新与街区发生关系",
    question:
      "老街的一楼空了很久。我们想在有限预算里，让这里成为一个人们愿意停留、也愿意再来的地方。该先尝试哪一种可能？",
    horizon: "用 4 周，试一次小而真实的改变",
    criteria: [
      { id: "connection", name: "人与人的连接", weight: 40 },
      { id: "sustainable", name: "持续运转的能力", weight: 20 },
      { id: "ease", name: "启动的轻盈程度", weight: 40 },
    ],
    options: [
      {
        id: "library",
        name: "共享书房",
        description: "一张长桌，交换书与故事。",
        scores: { connection: 8, sustainable: 5, ease: 9 },
      },
      {
        id: "repair",
        name: "修补实验室",
        description: "修好一件旧物，认识一个邻居。",
        scores: { connection: 9, sustainable: 8, ease: 4 },
      },
      {
        id: "kitchen",
        name: "周末共食",
        description: "从一顿饭开始，重新认识街区。",
        scores: { connection: 9, sustainable: 6, ease: 6 },
      },
    ],
  };
}
