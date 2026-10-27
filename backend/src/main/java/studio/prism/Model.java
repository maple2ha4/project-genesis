package studio.prism;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Model {
    private Model() {}
    public record Criterion(String id, String name, double weight) {}
    public record Option(String id, String name, String description, Map<String, Double> scores) {}
    public record Evidence(String id, String optionId, String criterionId, String text, String source, double impact, double confidence) {}
    public record Ranking(String optionId, double score, Map<String, Double> contributions) {}
    public record Experiment(String optionId, String hypothesis, String signal, String dueDate,
                             List<Ranking> snapshot, List<Criterion> lenses, String committedAt,
                             String outcome, String learning, String reviewedAt) {}
    public record Decision(String id, long version, String title, String question, String horizon,
                           List<Option> options, List<Criterion> criteria, List<Evidence> evidence,
                           Experiment experiment, boolean archived, String createdAt, String updatedAt) {}
    public record Write(long version, String title, String question, String horizon, List<Option> options, List<Criterion> criteria) {}
    public record Commit(long version, String optionId, String hypothesis, String signal, String dueDate) {}
    public record Review(long version, String outcome, String learning) {}
    public record Revision(long version) {}
    public record EvidenceWrite(long version, String optionId, String criterionId, String text, String source, double impact, double confidence) {}
    public record View(Decision decision, List<Ranking> ranking) {}
    public record Backup(int formatVersion, List<Decision> decisions) {}
    public static String now() { return Instant.now().toString(); }
}
