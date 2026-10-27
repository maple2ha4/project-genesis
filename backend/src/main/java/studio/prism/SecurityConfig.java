package studio.prism;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
    private final boolean secured;
    private final String username,password;
    public SecurityConfig(@Value("${prism.username}") String username,@Value("${prism.password}") String password,@Value("${server.address}") String bind) {
        this.username=username;this.password=password;this.secured=!username.isBlank()&&!password.isBlank();
        if(!username.isBlank()!=!password.isBlank()) throw new IllegalStateException("PRISM_USERNAME and PRISM_PASSWORD must be set together");
        if(secured&&password.length()<12) throw new IllegalStateException("PRISM_PASSWORD must contain at least 12 characters");
        if(!secured&&!bind.equals("127.0.0.1")&&!bind.equals("localhost")) throw new IllegalStateException("Network binding requires PRISM_USERNAME and PRISM_PASSWORD");
    }
    @Bean UserDetailsService users() {
        return new InMemoryUserDetailsManager(User.withUsername(secured?username:"disabled-local-user").password("{bcrypt}"+new BCryptPasswordEncoder().encode(secured?password:java.util.UUID.randomUUID().toString())).roles("OWNER").build());
    }
    @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth->auth.requestMatchers("/api/health","/error").permitAll()
            .requestMatchers("/api/**").access((a,c)->new org.springframework.security.authorization.AuthorizationDecision(!secured||a.get().isAuthenticated()&&!a.get().getName().equals("anonymousUser")))
            .anyRequest().permitAll());
        http.httpBasic(basic->basic.authenticationEntryPoint((request,response,e)->{
            response.setStatus(401); response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"message\":\"请登录工作室\"}");
        }));
        http.csrf(csrf->csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        http.headers(headers->headers.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
            .referrerPolicy(Customizer.withDefaults()));
        return http.build();
    }
}
