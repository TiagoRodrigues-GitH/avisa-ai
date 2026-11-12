package br.com.avisaai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Acesso por usuário e senha (HTTP Basic sobre HTTPS em produção). Um único usuário, definido por variáveis
 * de ambiente APP_USERNAME e APP_PASSWORD_HASH (BCrypt). Sem senha configurada, uma senha aleatória é gerada
 * e mostrada no log ao iniciar — nunca há senha padrão fixa.
 */
@Configuration
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService users(@Value("${app.username:admin}") String username,
                                    @Value("${app.password-hash:}") String hash,
                                    PasswordEncoder encoder) {
        String passwordHash = hash;
        if (passwordHash == null || passwordHash.isBlank()) {
            byte[] bytes = new byte[9];
            new SecureRandom().nextBytes(bytes);
            String generated = HexFormat.of().formatHex(bytes);
            passwordHash = encoder.encode(generated);
            LOG.warn("APP_PASSWORD_HASH não definido. Senha temporária deste início para o usuário '{}': {}", username, generated);
        }
        return new InMemoryUserDetailsManager(User.withUsername(username).password(passwordHash).roles("STAFF").build());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // API sem cookies de sessão: CSRF não se aplica
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Só a API exige login; o app Angular (arquivos estáticos), a saúde e a documentação são públicos.
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // 401 sem o cabeçalho WWW-Authenticate: o navegador não abre a janela nativa de senha;
                // quem pede login é a tela do aplicativo.
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (request, response, ex) -> response.sendError(401, "Login necessário")));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins:http://localhost:4200}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
