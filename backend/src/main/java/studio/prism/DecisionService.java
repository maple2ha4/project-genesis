package studio.prism;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static studio.prism.Model.*;
import static studio.prism.Rules.*;

@Service
public class DecisionService {
    private final DecisionStore store;
    public DecisionService(DecisionStore store) { this.store=store; }
    public List<View> all() { return store.all().stream().map(this::view).toList(); }
    public View get(String id) { return view(store.get(id)); }
    private View view(Decision d) { return new View(d,rank(d)); }
    private Decision current(String id,long version) {
        Decision d=store.get(id);
        if(d.version()!=version) throw new Problem(409,"另一个窗口已经修改这项决策，请重新加载后再操作");
        return d;
    }
    private void editable(Decision d) { require(!d.archived() && d.experiment()==null,"已锁定或归档的决策不能修改，请复制为新决策继续探索"); }
    private View save(Decision old, List<Option> options, List<Criterion> criteria,List<Evidence> evidence,Experiment experiment,boolean archived) {
        Decision d=new Decision(old.id(),old.version()+1,old.title(),old.question(),old.horizon(),options,criteria,evidence,experiment,archived,old.createdAt(),now());
        store.update(d,old.version()); return view(d);
    }
    @Transactional
    public View create(Write w) {
        validate(w); require(store.all().size()<500,"工作室最多容纳 500 项决策，请先备份，再归档并删除不再需要的记录");
        String time=now(); Decision d=new Decision(UUID.randomUUID().toString(),0,w.title().strip(),w.question().strip(),w.horizon().strip(),w.options(),w.criteria(),List.of(),null,false,time,time);
        store.insert(d); return view(d);
    }
    @Transactional
    public View update(String id,Write w) {
        validate(w); Decision old=current(id,w.version()); editable(old);
        Set<String> os=new HashSet<>(),cs=new HashSet<>();w.options().forEach(o->os.add(o.id()));w.criteria().forEach(c->cs.add(c.id()));
        require(old.evidence().stream().allMatch(e->os.contains(e.optionId())&&cs.contains(e.criterionId())),"先移除关联证据，再移除方案或视角");
        Decision d=new Decision(id,old.version()+1,w.title().strip(),w.question().strip(),w.horizon().strip(),w.options(),w.criteria(),old.evidence(),null,false,old.createdAt(),now());
        store.update(d,old.version()); return view(d);
    }
    @Transactional
    public View evidence(String id,EvidenceWrite w) {
        Decision d=current(id,w.version());editable(d); require(d.evidence().size()<100,"每项决策最多 100 条证据");
        require(d.options().stream().anyMatch(o->o.id().equals(w.optionId())),"方案不存在");
        require(d.criteria().stream().anyMatch(c->c.id().equals(w.criterionId())),"视角不存在");
        text(w.text(),1,1000,"证据");text(w.source(),0,500,"来源");number(w.impact(),-3,3,"影响");number(w.confidence(),0,1,"信心");
        List<Evidence> es=new ArrayList<>(d.evidence());es.add(new Evidence(UUID.randomUUID().toString(),w.optionId(),w.criterionId(),w.text().strip(),w.source().strip(),w.impact(),w.confidence()));
        return save(d,d.options(),d.criteria(),es,null,false);
    }
    @Transactional
    public View removeEvidence(String id,String evidenceId,long version) {
        Decision d=current(id,version);editable(d);require(d.evidence().stream().anyMatch(e->e.id().equals(evidenceId)),"证据不存在");
        return save(d,d.options(),d.criteria(),d.evidence().stream().filter(e->!e.id().equals(evidenceId)).toList(),null,false);
    }
    @Transactional
    public View commit(String id,Commit w) {
        Decision d=current(id,w.version());editable(d);
        require(d.options().stream().anyMatch(o->o.id().equals(w.optionId())),"方案不存在");text(w.hypothesis(),1,1000,"假设");text(w.signal(),1,1000,"成功信号");
        try { LocalDate date=LocalDate.parse(w.dueDate()); require(!date.isBefore(LocalDate.now(java.time.ZoneOffset.UTC)),"复盘日期不能早于今天（UTC）"); } catch (DateTimeParseException|NullPointerException e) { throw new Problem(422,"请填写有效的复盘日期"); }
        Experiment e=new Experiment(w.optionId(),w.hypothesis().strip(),w.signal().strip(),w.dueDate(),rank(d),d.criteria(),now(),null,null,null);
        return save(d,d.options(),d.criteria(),d.evidence(),e,false);
    }
    @Transactional
    public View review(String id,Review w) {
        Decision d=current(id,w.version());require(!d.archived()&&d.experiment()!=null&&d.experiment().reviewedAt()==null,"只有正在实践的决策可以复盘");
        require(Set.of("SUPPORTED","MIXED","REFUTED").contains(w.outcome()==null?"":w.outcome()),"请选择复盘结论");text(w.learning(),1,3000,"复盘记录");
        Experiment e=d.experiment();Experiment next=new Experiment(e.optionId(),e.hypothesis(),e.signal(),e.dueDate(),e.snapshot(),e.lenses(),e.committedAt(),w.outcome(),w.learning().strip(),now());
        return save(d,d.options(),d.criteria(),d.evidence(),next,false);
    }
    @Transactional
    public View archive(String id,long version) { Decision d=current(id,version);return save(d,d.options(),d.criteria(),d.evidence(),d.experiment(),!d.archived()); }
    @Transactional
    public View fork(String id,long version) {
        Decision d=current(id,version);
        return create(new Write(0,(d.title().length()>90?d.title().substring(0,90):d.title())+" · 新一轮",d.question(),d.horizon(),d.options(),d.criteria()));
    }
    @Transactional
    public List<View> restore(Backup backup) {
        require(backup.formatVersion()==1 && backup.decisions()!=null && !backup.decisions().isEmpty() && backup.decisions().size()<=50,"仅支持格式 1，每次导入 1–50 项决策");
        require(store.all().size()+backup.decisions().size()<=500,"导入后不能超过 500 项决策");
        List<View> restored=new ArrayList<>();
        for(Decision source:backup.decisions()) {
            require(source!=null,"备份记录不能为空");
            validate(new Write(0,source.title(),source.question(),source.horizon(),source.options(),source.criteria()));
            require(source.evidence()!=null && source.evidence().size()<=100,"证据列表无效");
            Set<String> evidenceIds=new HashSet<>();
            for(Evidence e:source.evidence()) {
                require(e!=null,"证据不能为空");text(e.id(),1,50,"证据标识");require(evidenceIds.add(e.id()),"证据标识重复");
                require(source.options().stream().anyMatch(o->o.id().equals(e.optionId()))&&source.criteria().stream().anyMatch(c->c.id().equals(e.criterionId())),"证据引用无效");
                text(e.text(),1,1000,"证据");text(e.source(),0,500,"来源");number(e.impact(),-3,3,"影响");number(e.confidence(),0,1,"信心");
            }
            try { java.time.Instant.parse(source.createdAt()); java.time.Instant.parse(source.updatedAt()); }
            catch(Exception ex) { throw new Problem(422,"备份时间无效"); }
            Experiment e=source.experiment();
            if(e!=null) {
                require(source.options().stream().anyMatch(o->o.id().equals(e.optionId())),"实践引用无效");
                text(e.hypothesis(),1,1000,"假设");text(e.signal(),1,1000,"成功信号");
                try { LocalDate.parse(e.dueDate());java.time.Instant.parse(e.committedAt());if(e.reviewedAt()!=null)java.time.Instant.parse(e.reviewedAt()); }
                catch(Exception ex) { throw new Problem(422,"实践时间无效"); }
                require(e.lenses()!=null&&e.lenses().equals(source.criteria()),"快照视角不一致");
                List<Ranking> expected=rank(source);
                require(e.snapshot()!=null&&e.snapshot().size()==expected.size(),"快照无效");
                for(int i=0;i<expected.size();i++) {
                    Ranking a=e.snapshot().get(i),b=expected.get(i);
                    require(a!=null&&b.optionId().equals(a.optionId())&&Double.isFinite(a.score())&&Math.abs(a.score()-b.score())<0.000001&&b.contributions().equals(a.contributions()),"快照评分不一致");
                }
                if(e.reviewedAt()!=null) {require(Set.of("SUPPORTED","MIXED","REFUTED").contains(e.outcome()==null?"":e.outcome()),"复盘结论无效");text(e.learning(),1,3000,"复盘记录");}
                else require(e.outcome()==null&&e.learning()==null,"未复盘记录不能包含结论");
            }
            Decision d=new Decision(UUID.randomUUID().toString(),0,source.title(),source.question(),source.horizon(),source.options(),source.criteria(),source.evidence(),source.experiment(),source.archived(),source.createdAt(),source.updatedAt());
            store.insert(d);restored.add(view(d));
        }
        return restored;
    }
    @Transactional
    public void delete(String id,long version) {
        Decision d=current(id,version);require(d.archived(),"请先归档再永久删除");store.delete(d);
    }
    public Backup backup() { return new Backup(1,store.all()); }
}
