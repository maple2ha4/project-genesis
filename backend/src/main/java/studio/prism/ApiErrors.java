package studio.prism;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(Problem.class) public ResponseEntity<?> problem(Problem e) { return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage())); }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) public ResponseEntity<?> malformed(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message","请求格式无效，请检查输入"));
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<?> notFound() { return ResponseEntity.status(404).body(Map.of("message","请求的资源不存在")); }
    @ExceptionHandler(Exception.class) public ResponseEntity<?> unexpected(Exception e) {
        String id=java.util.UUID.randomUUID().toString();log.error("Request failed: {}",id,e);
        return ResponseEntity.internalServerError().body(Map.of("message","暂时无法完成操作，请稍后重试。诊断编号："+id));
    }
}
