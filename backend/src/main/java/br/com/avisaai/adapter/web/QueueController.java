package br.com.avisaai.adapter.web;

import br.com.avisaai.adapter.web.Dtos.NotificationOut;
import br.com.avisaai.adapter.web.Dtos.QueueItem;
import br.com.avisaai.adapter.web.Dtos.SkipIn;
import br.com.avisaai.application.DashboardService;
import br.com.avisaai.application.DashboardService.Summary;
import br.com.avisaai.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Fila de envio", description = "Mensagens prontas: o atendente abre o link do WhatsApp, envia e marca como enviada")
public class QueueController {

    private final NotificationService notifications;
    private final DashboardService dashboard;

    public QueueController(NotificationService notifications, DashboardService dashboard) {
        this.notifications = notifications;
        this.dashboard = dashboard;
    }

    @GetMapping("/queue")
    @Operation(summary = "Mensagens prontas para enviar agora, com links wa.me e do WhatsApp Desktop")
    public List<QueueItem> queue() {
        notifications.releaseDue();
        return notifications.queue().stream().map(QueueItem::of).toList();
    }

    @PostMapping("/queue/{id}/sent")
    @Operation(summary = "Marca a mensagem como enviada")
    public NotificationOut sent(@PathVariable Long id) {
        return NotificationOut.of(notifications.markSent(id));
    }

    @PostMapping("/queue/{id}/skip")
    @Operation(summary = "Dispensa a mensagem (com motivo)")
    public NotificationOut skip(@PathVariable Long id, @RequestBody(required = false) SkipIn in) {
        return NotificationOut.of(notifications.skip(id, in == null ? null : in.reason()));
    }

    @PostMapping("/messages/{id}/send-now")
    @Operation(summary = "Antecipa ou reenvia uma mensagem: ela vai para a fila agora")
    public NotificationOut sendNow(@PathVariable Long id) {
        return NotificationOut.of(notifications.sendNow(id));
    }

    public record DashboardOut(long readyNow, long sentToday, long scheduledToday, long confirmedNext7Days,
                               long awaitingConfirmation, long overduePayments, long openDeliveries, long noShows30Days,
                               double confirmationRate30Days, List<Dtos.ReminderOut> today) {
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Indicadores do painel")
    public DashboardOut summary() {
        notifications.releaseDue();
        Summary s = dashboard.summary();
        return new DashboardOut(s.readyNow(), s.sentToday(), s.scheduledToday(), s.confirmedNext7Days(), s.awaitingConfirmation(),
                s.overduePayments(), s.openDeliveries(), s.noShows30Days(), s.confirmationRate30Days(),
                s.today().stream().map(Dtos.ReminderOut::of).toList());
    }
}
