package br.com.avisaai.config;

import br.com.avisaai.application.ReminderService;
import br.com.avisaai.application.ReminderService.ReminderCommand;
import br.com.avisaai.application.Repositories.CustomerRepository;
import br.com.avisaai.application.Repositories.SettingsRepository;
import br.com.avisaai.domain.Customer;
import br.com.avisaai.domain.ReminderType;
import br.com.avisaai.domain.Settings;
import br.com.avisaai.domain.Statuses.DeliveryStage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Dados de demonstração (clientes fictícios) para o primeiro uso e para apresentar o sistema. */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoData implements ApplicationRunner {

    private final CustomerRepository customers;
    private final SettingsRepository settings;
    private final ReminderService reminders;
    private final Clock clock;

    public DemoData(CustomerRepository customers, SettingsRepository settings, ReminderService reminders, Clock clock) {
        this.customers = customers;
        this.settings = settings;
        this.reminders = reminders;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) {
            return;
        }
        Settings s = new Settings();
        s.setBusinessName("Clínica Exemplo");
        s.setAddress("Rua das Flores, 100 — Centro");
        settings.save(s);

        Customer eduarda = customers.save(new Customer("Eduarda Martins", "(43) 99911-1111", true));
        Customer marcos = customers.save(new Customer("Marcos Esteves", "(43) 99922-2222", true));
        Customer elisa = customers.save(new Customer("Elisa Moreira", "(11) 98833-3333", true));
        Customer mateus = customers.save(new Customer("Mateus Elias", "(41) 97744-4444", false));

        Instant now = clock.instant().truncatedTo(ChronoUnit.HOURS);
        reminders.create(new ReminderCommand(eduarda.getId(), ReminderType.APPOINTMENT, "consulta com a Dra. Emília", now.plus(Duration.ofHours(20)), null, null));
        reminders.create(new ReminderCommand(marcos.getId(), ReminderType.PAYMENT, "a mensalidade de novembro", now.plus(Duration.ofDays(2)), new BigDecimal("180.00"), null));
        reminders.create(new ReminderCommand(elisa.getId(), ReminderType.PICKUP, "o resultado do seu exame", now.minus(Duration.ofHours(1)), null, null));
        reminders.create(new ReminderCommand(mateus.getId(), ReminderType.RETURN_VISIT, "retorno semestral", now.plus(Duration.ofDays(5)), null, null));
        var delivery = reminders.create(new ReminderCommand(eduarda.getId(), ReminderType.DELIVERY, "#1042", now.plus(Duration.ofHours(1)), new BigDecimal("89.90"), null));
        reminders.advanceDelivery(delivery.getId(), DeliveryStage.OUT_FOR_DELIVERY, 25);
        reminders.create(new ReminderCommand(marcos.getId(), ReminderType.QUOTE, "limpeza e clareamento", now, new BigDecimal("650.00"), null));
    }
}
