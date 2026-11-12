package br.com.avisaai.application;

import br.com.avisaai.application.Repositories.SettingsRepository;
import br.com.avisaai.application.Repositories.TemplateRepository;
import br.com.avisaai.domain.DefaultTemplates;
import br.com.avisaai.domain.MessageTemplate;
import br.com.avisaai.domain.Notification;
import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.Reminder;
import br.com.avisaai.domain.ReminderPolicy;
import br.com.avisaai.domain.Settings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Monta o texto de cada aviso a partir do modelo (personalizado ou padrão) e dos dados do compromisso. */
@Service
public class MessageService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, dd/MM", PT_BR);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH'h'mm", PT_BR);

    private final TemplateRepository templates;
    private final SettingsRepository settings;
    private final ReminderPolicy policy;

    public MessageService(TemplateRepository templates, SettingsRepository settings, ReminderPolicy policy) {
        this.templates = templates;
        this.settings = settings;
        this.policy = policy;
    }

    public String templateFor(NotificationKind kind) {
        return templates.findById(kind).map(MessageTemplate::getText).orElse(DefaultTemplates.text(kind));
    }

    public Settings currentSettings() {
        return settings.findById(Settings.SINGLETON).orElseGet(Settings::new);
    }

    public String render(Notification n) {
        return render(templateFor(n.getKind()), n.getReminder());
    }

    /** Texto final para um compromisso (também usado na pré-visualização da tela de mensagens). */
    public String render(String template, Reminder r) {
        Settings s = currentSettings();
        ZonedDateTime when = r.getEventAt().atZone(policy.zone());
        Map<String, String> values = new HashMap<>();
        values.put("nome", r.getCustomer().firstName());
        values.put("empresa", s.getBusinessName());
        values.put("endereco", s.getAddress());
        values.put("titulo", r.getTitle());
        values.put("data", DATE.format(when));
        values.put("hora", TIME.format(when).replace("h00", "h"));
        values.put("valor", money(r.getAmount()));
        values.put("minutos", r.getEtaMinutes() == null ? "15" : String.valueOf(r.getEtaMinutes()));
        return DefaultTemplates.render(template, values);
    }

    static String money(BigDecimal amount) {
        return amount == null ? "" : NumberFormat.getCurrencyInstance(PT_BR).format(amount).replace(' ', ' ');
    }
}
