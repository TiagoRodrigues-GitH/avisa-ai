package br.com.avisaai.adapter.web;

import br.com.avisaai.adapter.web.Dtos.SettingsIn;
import br.com.avisaai.adapter.web.Dtos.SettingsOut;
import br.com.avisaai.adapter.web.Dtos.TemplateIn;
import br.com.avisaai.adapter.web.Dtos.TemplateOut;
import br.com.avisaai.application.MessageService;
import br.com.avisaai.application.Repositories.SettingsRepository;
import br.com.avisaai.application.Repositories.TemplateRepository;
import br.com.avisaai.domain.Customer;
import br.com.avisaai.domain.DefaultTemplates;
import br.com.avisaai.domain.MessageTemplate;
import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.Phone;
import br.com.avisaai.domain.Reminder;
import br.com.avisaai.domain.Settings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Mensagens e empresa", description = "Textos dos avisos (com pré-visualização) e dados da empresa")
@Transactional
public class SettingsController {

    private final TemplateRepository templates;
    private final SettingsRepository settings;
    private final MessageService messages;

    public SettingsController(TemplateRepository templates, SettingsRepository settings, MessageService messages) {
        this.templates = templates;
        this.settings = settings;
        this.messages = messages;
    }

    @GetMapping("/templates")
    @Operation(summary = "Todos os textos de aviso, com exemplo preenchido")
    public List<TemplateOut> templates() {
        Reminder sample = sample();
        return Arrays.stream(NotificationKind.values()).map(kind -> {
            String text = messages.templateFor(kind);
            return new TemplateOut(kind, kind.label(), kind.type(), kind.type().label(), text,
                    templates.existsById(kind), messages.render(text, sampleOf(sample, kind)));
        }).toList();
    }

    @PutMapping("/templates/{kind}")
    @Operation(summary = "Personaliza o texto de um aviso. Variáveis: {nome} {empresa} {titulo} {data} {hora} {valor} {endereco} {minutos}")
    public TemplateOut saveTemplate(@PathVariable NotificationKind kind, @Valid @RequestBody TemplateIn in) {
        templates.save(new MessageTemplate(kind, in.text().trim()));
        return templates().stream().filter(t -> t.kind() == kind).findFirst().orElseThrow();
    }

    @DeleteMapping("/templates/{kind}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Volta ao texto padrão")
    public void resetTemplate(@PathVariable NotificationKind kind) {
        templates.deleteById(kind);
    }

    @GetMapping("/settings")
    public SettingsOut settings() {
        Settings s = messages.currentSettings();
        return new SettingsOut(s.getBusinessName(), s.getAddress(), s.getBusinessPhone());
    }

    @PutMapping("/settings")
    public SettingsOut saveSettings(@Valid @RequestBody SettingsIn in) {
        Settings s = settings.findById(Settings.SINGLETON).orElseGet(Settings::new);
        s.setBusinessName(in.businessName().trim());
        s.setAddress(in.address() == null ? "" : in.address().trim());
        s.setBusinessPhone(in.businessPhone() == null || in.businessPhone().isBlank() ? null : Phone.normalize(in.businessPhone()));
        settings.save(s);
        return settings();
    }

    /** Dados fictícios só para a pré-visualização dos textos. */
    private static Reminder sample() {
        Reminder r = new Reminder();
        r.setCustomer(new Customer("Maria Souza", "43999990000", true));
        r.setEventAt(Instant.now().plus(Duration.ofDays(1)).truncatedTo(java.time.temporal.ChronoUnit.HOURS));
        r.setAmount(new BigDecimal("150.00"));
        r.setEtaMinutes(15);
        return r;
    }

    private static Reminder sampleOf(Reminder base, NotificationKind kind) {
        base.setType(kind.type());
        base.setTitle(switch (kind.type()) {
            case APPOINTMENT -> "consulta com a Dra. Ana";
            case PAYMENT -> "a mensalidade de novembro";
            case PICKUP -> "o seu pedido #1042";
            case DELIVERY -> "#1042";
            case RETURN_VISIT -> "revisão semestral";
            case WAITLIST -> "horário extra";
            case QUOTE -> "conserto do notebook";
            case FEEDBACK -> "atendimento";
        });
        return base;
    }

    static {
        // Garante que todo aviso tem texto padrão (falha cedo se alguém criar um tipo novo sem texto).
        for (NotificationKind k : NotificationKind.values()) {
            if (DefaultTemplates.text(k) == null) {
                throw new IllegalStateException("Sem texto padrão para " + k);
            }
        }
    }
}
