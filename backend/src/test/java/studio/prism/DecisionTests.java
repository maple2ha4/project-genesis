package studio.prism;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static studio.prism.Model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@Transactional
class DecisionTests {
    @Autowired DecisionService service;
    @Autowired MockMvc mvc;
    static Write input() { return new Write(0,"选择工作方式","哪一种适合未来三个月？","三个月",List.of(new Option("a","独立工作","",Map.of("x",9.0,"y",3.0)),new Option("b","协作工作","",Map.of("x",4.0,"y",9.0))),List.of(new Criterion("x","专注",70),new Criterion("y","连接",30))); }
    @Test void weightedRankingAndReversal() {
        var v=service.create(input());assertEquals("a",v.ranking().getFirst().optionId());assertEquals(7.2,v.ranking().getFirst().score(),0.00001);
        var d=v.decision();var n=service.update(d.id(),new Write(0,d.title(),d.question(),d.horizon(),d.options(),List.of(new Criterion("x","专注",10),new Criterion("y","连接",90))));
        assertEquals("b",n.ranking().getFirst().optionId());assertEquals(8.5,n.ranking().getFirst().score(),0.00001);
    }
    @Test void evidenceChangesRankingAndRemovalRestoresIt() {
        var d=service.create(input()).decision();
        var v=service.evidence(d.id(),new EvidenceWrite(0,"a","x","观察发现环境嘈杂","现场",-3,1));
        assertEquals(5.1,v.ranking().stream().filter(r->r.optionId().equals("a")).findFirst().orElseThrow().score(),0.00001);
        var restored=service.removeEvidence(d.id(),v.decision().evidence().getFirst().id(),1);assertEquals(7.2,restored.ranking().getFirst().score(),0.00001);
    }
    @Test void scoreClampsBeforeWeighting() {
        var d=service.create(input()).decision();var v=service.evidence(d.id(),new EvidenceWrite(0,"a","x","支持","",3,1));
        assertEquals(7.9,v.ranking().getFirst().score(),0.00001);
    }
    @Test void lifecyclePreservesSnapshotAndForks() {
        var d=service.create(input()).decision();
        var committed=service.commit(d.id(),new Commit(0,"a","专注更好","完成三项实验","2099-12-01")).decision();
        assertThrows(Problem.class,()->service.update(d.id(),new Write(1,d.title(),d.question(),d.horizon(),d.options(),d.criteria())));
        var reviewed=service.review(d.id(),new Review(1,"MIXED","连接比预期更重要")).decision();
        assertEquals(committed.experiment().snapshot(),reviewed.experiment().snapshot());
        assertThrows(Problem.class,()->service.review(d.id(),new Review(2,"SUPPORTED","再次复盘")));
        var fork=service.fork(d.id(),2).decision();assertNull(fork.experiment());assertTrue(fork.evidence().isEmpty());assertNotEquals(d.id(),fork.id());
    }
    @Test void staleWritesCannotOverwrite() {
        var d=service.create(input()).decision();service.archive(d.id(),0);
        var e=assertThrows(Problem.class,()->service.archive(d.id(),0));assertEquals(409,e.status);
        assertFalse(service.archive(d.id(),1).decision().archived());
    }
    @Test void permanentDeletionRequiresArchiveAndFreshVersion() {
        var d=service.create(input()).decision();assertThrows(Problem.class,()->service.delete(d.id(),0));
        service.archive(d.id(),0);assertEquals(409,assertThrows(Problem.class,()->service.delete(d.id(),0)).status);
        service.delete(d.id(),1);assertEquals(404,assertThrows(Problem.class,()->service.get(d.id())).status);
    }
    @Test void validatesWeightsScoresAndReferences() {
        var w=input();assertThrows(Problem.class,()->service.create(new Write(0,w.title(),w.question(),w.horizon(),w.options(),List.of(new Criterion("x","x",0),new Criterion("y","y",0)))));
        var d=service.create(w).decision();assertThrows(Problem.class,()->service.evidence(d.id(),new EvidenceWrite(0,"missing","x","观察","",1,.5)));
        assertThrows(Problem.class,()->service.evidence(d.id(),new EvidenceWrite(0,"a","x","观察","",1,Double.NaN)));
        assertThrows(Problem.class,()->service.commit(d.id(),new Commit(0,"a","假设","指标","2020-01-01")));
        assertThrows(Problem.class,()->service.review(d.id(),new Review(0,"MIXED","不能提前复盘")));
    }
    @Test void backupRestoresCompleteReviewedRecordWithoutOverwriting() {
        var d=service.create(input()).decision();service.commit(d.id(),new Commit(0,"a","假设","指标","2099-12-01"));service.review(d.id(),new Review(1,"REFUTED","新发现"));
        var backup=service.backup();var restored=service.restore(backup);assertEquals(1,restored.size());assertNotEquals(d.id(),restored.getFirst().decision().id());
        assertEquals(backup.decisions().getFirst().experiment(),restored.getFirst().decision().experiment());assertEquals(2,service.all().size());
    }
    @Test void invalidRestoreRollsBack() {
        var d=service.create(input()).decision();assertThrows(Problem.class,()->service.restore(new Backup(9,List.of(d))));assertEquals(1,service.all().size());
    }
    @Test void apiCsrfValidationMissingAndOversized() throws Exception {
        mvc.perform(get("/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.csrf").isNotEmpty());
        mvc.perform(post("/api/decisions").contentType("application/json").content("{} ")).andExpect(status().isForbidden());
        mvc.perform(post("/api/decisions").with(csrf()).contentType("application/json").content("{} ")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/unknown")).andExpect(status().isNotFound());
        mvc.perform(get("/api/decisions/missing")).andExpect(status().isNotFound());
        mvc.perform(post("/api/decisions").with(csrf()).contentType("application/json").content("x".repeat(524289))).andExpect(status().isPayloadTooLarge());
        mvc.perform(get("/api/health")).andExpect(header().string("X-Content-Type-Options","nosniff"));
    }
}
