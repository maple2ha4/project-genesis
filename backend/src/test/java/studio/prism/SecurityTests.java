package studio.prism;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:security","prism.username=owner","prism.password=test-only-password-123"})
@AutoConfigureMockMvc
class SecurityTests {
 @Autowired MockMvc mvc;
 @Test void ownerCredentialsRequired() throws Exception {
  mvc.perform(get("/api/decisions")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/decisions").with(httpBasic("owner","wrong"))).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/decisions").with(httpBasic("owner","test-only-password-123"))).andExpect(status().isOk());
  mvc.perform(get("/api/health")).andExpect(status().isOk());
 }
 @Test void authenticationDoesNotBypassCsrf() throws Exception {
  mvc.perform(post("/api/decisions").with(httpBasic("owner","test-only-password-123")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
 @Test void unsafeNetworkBindingFailsClosed() {
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->new SecurityConfig("","","0.0.0.0"));
 }
}
