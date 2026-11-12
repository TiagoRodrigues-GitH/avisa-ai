package br.com.avisaai.adapter.web;

import br.com.avisaai.adapter.web.Dtos.DeliveryIn;
import br.com.avisaai.adapter.web.Dtos.NotificationOut;
import br.com.avisaai.adapter.web.Dtos.ReminderIn;
import br.com.avisaai.adapter.web.Dtos.ReminderOut;
import br.com.avisaai.adapter.web.Dtos.StatusIn;
import br.com.avisaai.application.NotificationService;
import br.com.avisaai.application.ReminderService;
import br.com.avisaai.application.ReminderService.ReminderCommand;
import br.com.avisaai.application.Repositories.ReminderRepository;
import br.com.avisaai.domain.ReminderType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@Tag(name = "Compromissos", description = "Consultas, pagamentos, retiradas, entregas, retornos, vagas, orçamentos e pesquisas")
public class ReminderController {

    private final ReminderService service;
    private final ReminderRepository reminders;
    private final NotificationService notifications;

    public ReminderController(ReminderService service, ReminderRepository reminders, NotificationService notifications) {
        this.service = service;
        this.reminders = reminders;
        this.notifications = notifications;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Compromissos num período (padrão: próximos 30 dias), opcionalmente de um tipo")
    public List<ReminderOut> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                  @RequestParam(required = false) ReminderType type) {
        Instant start = from != null ? from : Instant.now().minusSeconds(86_400);
        Instant end = to != null ? to : start.plusSeconds(31L * 86_400);
        var found = type == null
                ? reminders.findByEventAtBetweenOrderByEventAtAsc(start, end)
                : reminders.findByTypeAndEventAtBetweenOrderByEventAtAsc(type, start, end);
        return found.stream().map(ReminderOut::of).toList();
    }

    @GetMapping("/deliveries")
    @Transactional(readOnly = true)
    @Operation(summary = "Entregas em aberto (quadro de entregas)")
    public List<ReminderOut> deliveries() {
        return reminders.openDeliveries().stream().map(ReminderOut::of).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ReminderOut get(@PathVariable Long id) {
        return ReminderOut.of(service.get(id));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Mensagens agendadas e enviadas deste compromisso")
    public List<NotificationOut> messages(@PathVariable Long id) {
        return notifications.forReminder(id).stream().map(NotificationOut::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria compromisso e agenda os avisos conforme o tipo")
    public ReminderOut create(@Valid @RequestBody ReminderIn in) {
        return ReminderOut.of(service.create(command(in)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera compromisso; avisos pendentes são refeitos")
    public ReminderOut update(@PathVariable Long id, @Valid @RequestBody ReminderIn in) {
        return ReminderOut.of(service.update(id, command(in)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Registra a resposta do cliente (confirmou / remarcar) ou o desfecho (pago, retirado, atendido, faltou, cancelado)")
    public ReminderOut status(@PathVariable Long id, @Valid @RequestBody StatusIn in) {
        return ReminderOut.of(service.changeStatus(id, in.status()));
    }

    @PatchMapping("/{id}/delivery")
    @Operation(summary = "Avança a entrega (saiu, a caminho, entregue) e prepara o aviso no WhatsApp")
    public ReminderOut delivery(@PathVariable Long id, @Valid @RequestBody DeliveryIn in) {
        return ReminderOut.of(service.advanceDelivery(id, in.stage(), in.etaMinutes()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private static ReminderCommand command(ReminderIn in) {
        return new ReminderCommand(in.customerId(), in.type(), in.title(), in.eventAt(), in.amount(), in.notes());
    }
}
