import { describe, it, expect } from "vitest";
import { rank, sample } from "../src/model";
import { mount } from "@vue/test-utils";
import Studio from "../src/components/Studio.vue";
import type { Decision } from "../src/model";
function decision(): Decision {
  return {
    ...sample(),
    id: "test",
    createdAt: "2026-01-01",
    updatedAt: "2026-01-01",
    archived: false,
    evidence: [],
    experiment: null,
  };
}
describe("透明折光规则", () => {
  it("归一化权重并计算不同方案", () => {
    const d = decision();
    const r = rank(d);
    expect(r[0].optionId).toBe("library");
    expect(r[0].score).toBeCloseTo(7.8);
    d.criteria[1].weight = 100;
    d.criteria[0].weight = 10;
    d.criteria[2].weight = 5;
    expect(rank(d)[0].optionId).toBe("repair");
  });
  it("证据以信心折减，先限幅再加权", () => {
    const d = decision();
    d.evidence = [
      {
        id: "e",
        optionId: "library",
        criterionId: "connection",
        text: "支持",
        source: "",
        impact: 3,
        confidence: 1,
      },
    ];
    expect(rank(d).find((r) => r.optionId === "library")!.score).toBeCloseTo(
      8.6,
    );
  });
  it("零权重不产生 NaN", () => {
    const d = decision();
    d.criteria.forEach((c) => (c.weight = 0));
    expect(rank(d).every((r) => r.score === 0)).toBe(true);
  });
  it("滑块调整真实改变排序和翻转提示", async () => {
    const d = decision();
    const wrapper = mount(Studio, {
      props: { decision: d, baseline: rank(d), locked: false },
    });
    await wrapper.get("#lens-sustainable").setValue(100);
    await wrapper.get("#lens-connection").setValue(0);
    await wrapper.get("#lens-ease").setValue(0);
    expect(wrapper.text()).toContain("领先方案发生了翻转");
    expect(wrapper.find(".option-card.leading").text()).toContain("修补实验室");
  });
  it("锁定的判断不能继续输入", async () => {
    const d = decision();
    const w = mount(Studio, {
      props: { decision: d, baseline: rank(d), locked: true },
    });
    expect(
      w
        .findAll("input[type=range]")
        .every((x) => x.attributes("disabled") !== undefined),
    ).toBe(true);
    await w.get(".disclosure").trigger("click");
    expect(
      w
        .findAll("input[type=number]")
        .every((x) => x.attributes("disabled") !== undefined),
    ).toBe(true);
  });
});
