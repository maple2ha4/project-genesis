package studio.prism;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static studio.prism.Model.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DecisionService service;
    public ApiController(DecisionService service) { this.service=service; }
    @GetMapping("/health") public Map<String,String> health() { return Map.of("status","ok"); }
    @GetMapping("/session") public Map<String,String> session(HttpServletRequest request) {
        CsrfToken csrf=(CsrfToken)request.getAttribute(CsrfToken.class.getName());
        return Map.of("csrf",csrf.getToken(),"header",csrf.getHeaderName());
    }
    @GetMapping("/decisions") public List<View> list() { return service.all(); }
    @GetMapping("/decisions/{id}") public View get(@PathVariable String id) { return service.get(id); }
    @PostMapping("/decisions") public View create(@RequestBody Write write) { return service.create(write); }
    @PutMapping("/decisions/{id}") public View update(@PathVariable String id,@RequestBody Write write) { return service.update(id,write); }
    @DeleteMapping("/decisions/{id}") public Map<String,String> delete(@PathVariable String id,@RequestParam long version) { service.delete(id,version);return Map.of("status","deleted"); }
    @PostMapping("/decisions/{id}/evidence") public View evidence(@PathVariable String id,@RequestBody EvidenceWrite write) { return service.evidence(id,write); }
    @DeleteMapping("/decisions/{id}/evidence/{evidenceId}") public View remove(@PathVariable String id,@PathVariable String evidenceId,@RequestParam long version) { return service.removeEvidence(id,evidenceId,version); }
    @PostMapping("/decisions/{id}/commit") public View commit(@PathVariable String id,@RequestBody Commit write) { return service.commit(id,write); }
    @PostMapping("/decisions/{id}/review") public View review(@PathVariable String id,@RequestBody Review write) { return service.review(id,write); }
    @PostMapping("/decisions/{id}/archive") public View archive(@PathVariable String id,@RequestBody Revision write) { return service.archive(id,write.version()); }
    @PostMapping("/decisions/{id}/fork") public View fork(@PathVariable String id,@RequestBody Revision write) { return service.fork(id,write.version()); }
    @PostMapping("/restore") public List<View> restore(@RequestBody Backup backup) { return service.restore(backup); }
    @GetMapping("/backup") public Backup backup() { return service.backup(); }
}
