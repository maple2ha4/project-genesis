package studio.prism;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import static studio.prism.Model.*;

@Repository
public class DecisionStore {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public DecisionStore(JdbcTemplate db, ObjectMapper json) { this.db=db; this.json=json; }
    private String encode(Decision d) {
        try { return json.writeValueAsString(d); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private Decision decode(String s) {
        try { return json.readValue(s,Decision.class); } catch (JsonProcessingException e) { throw new IllegalStateException("Stored document unreadable",e); }
    }
    public List<Decision> all() { return db.query("SELECT document FROM decisions ORDER BY updated_at DESC, id",(rs,n)->decode(rs.getString(1))); }
    public Decision get(String id) {
        var list=db.query("SELECT document FROM decisions WHERE id=?",(rs,n)->decode(rs.getString(1)),id);
        if(list.isEmpty()) throw new Problem(404,"这项决策不存在");
        return list.getFirst();
    }
    public void insert(Decision d) { db.update("INSERT INTO decisions VALUES(?,?,?,?)",d.id(),d.version(),encode(d),d.updatedAt()); }
    public void delete(Decision d) {
        if(db.update("DELETE FROM decisions WHERE id=? AND revision=?",d.id(),d.version())!=1)
            throw new Problem(409,"记录已被其他窗口修改，请重新加载");
    }
    public void update(Decision d, long expected) {
        if(db.update("UPDATE decisions SET revision=?,document=?,updated_at=? WHERE id=? AND revision=?",d.version(),encode(d),d.updatedAt(),d.id(),expected)!=1)
            throw new Problem(409,"另一个窗口已经修改这项决策，请重新加载后再操作");
    }
}
