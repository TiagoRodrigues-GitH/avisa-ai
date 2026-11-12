package br.com.avisaai.config;

import br.com.avisaai.domain.ReminderPolicy;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

@Configuration
@EnableScheduling
// As portas de persistência ficam aninhadas em application.Repositories (um arquivo, fácil de ler).
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "br.com.avisaai.application", considerNestedRepositories = true)
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ReminderPolicy reminderPolicy(@Value("${app.zone:America/Sao_Paulo}") String zone,
                                         @Value("${app.send-from:08:00}") String from,
                                         @Value("${app.send-until:20:00}") String until) {
        return new ReminderPolicy(ZoneId.of(zone), LocalTime.parse(from), LocalTime.parse(until));
    }

    /** Documentação OpenAPI exibida no Swagger UI (/swagger-ui.html). */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Avisa Aí — API de lembretes por WhatsApp")
                        .version("0.1.0")
                        .description("Consultas, pagamentos, retiradas, entregas, retornos, vagas, orçamentos e pesquisas. "
                                + "As mensagens saem pelo link oficial e gratuito do WhatsApp (wa.me), enviadas pelo atendente.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("basic",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basic"));
    }
}
