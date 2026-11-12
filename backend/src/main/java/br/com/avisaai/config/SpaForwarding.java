package br.com.avisaai.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * O backend também entrega o app Angular (um só programa no PC: http://IP-DO-PC:8080 no celular).
 * Rotas do app (/agenda, /entregas/...) voltam para o index.html; /api, /swagger-ui e arquivos com extensão não.
 */
@Controller
public class SpaForwarding {

    @GetMapping({"/login", "/agenda", "/agenda/{id}", "/entregas", "/clientes", "/mensagens"})
    public String app() {
        return "forward:/index.html";
    }
}
