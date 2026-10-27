package studio.prism;

import java.util.*;
import static studio.prism.Model.*;

public final class Rules {
    private Rules() {}
    public static void require(boolean condition, String message) { if (!condition) throw new Problem(422, message); }
    public static String text(String value, int min, int max, String name) {
        require(value != null && value.strip().length() >= min && value.length() <= max, name + "长度应为 " + min + "–" + max + " 字");
        return value.strip();
    }
    public static void number(double n, double min, double max, String name) {
        require(Double.isFinite(n) && n >= min && n <= max, name + "超出范围");
    }
    public static void validate(Write w) {
        text(w.title(), 1, 100, "标题"); text(w.question(), 1, 2000, "问题"); text(w.horizon(), 0, 100, "时间范围");
        require(w.options() != null && w.options().size() >= 2 && w.options().size() <= 5, "需要 2–5 个方案");
        require(w.criteria() != null && w.criteria().size() >= 2 && w.criteria().size() <= 6, "需要 2–6 个视角");
        Set<String> keys = new HashSet<>();
        for (Criterion c : w.criteria()) {
            require(c != null, "视角不能为空"); text(c.id(),1,50,"视角标识"); text(c.name(),1,40,"视角名称"); number(c.weight(),0,100,"权重");
            require(keys.add(c.id()),"视角标识重复");
        }
        require(w.criteria().stream().mapToDouble(Criterion::weight).sum() > 0,"至少一个视角权重大于零");
        Set<String> options = new HashSet<>();
        for (Option o : w.options()) {
            require(o != null,"方案不能为空"); text(o.id(),1,50,"方案标识"); text(o.name(),1,80,"方案名称"); text(o.description(),0,500,"方案说明");
            require(options.add(o.id()),"方案标识重复");
            require(o.scores()!=null && o.scores().keySet().equals(keys),"每个方案必须对全部视角评分");
            for (Double score : o.scores().values()) { require(score!=null,"评分不能为空"); number(score,0,10,"评分"); }
        }
    }
    public static List<Ranking> rank(Decision d) {
        double total = d.criteria().stream().mapToDouble(Criterion::weight).sum();
        List<Ranking> ranking = new ArrayList<>();
        for (Option o : d.options()) {
            Map<String,Double> contributions = new LinkedHashMap<>();
            for (Criterion c : d.criteria()) {
                double shift = d.evidence().stream().filter(e -> e.optionId().equals(o.id()) && e.criterionId().equals(c.id()))
                    .mapToDouble(e -> e.impact()*e.confidence()).sum();
                double value = Math.clamp(o.scores().get(c.id())+shift,0,10)*c.weight()/total;
                contributions.put(c.id(),value);
            }
            ranking.add(new Ranking(o.id(),contributions.values().stream().mapToDouble(Double::doubleValue).sum(),contributions));
        }
        ranking.sort(Comparator.comparingDouble(Ranking::score).reversed().thenComparing(Ranking::optionId));
        return ranking;
    }
}
