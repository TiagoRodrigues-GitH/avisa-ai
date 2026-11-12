package br.com.avisaai.application;

import br.com.avisaai.application.Repositories.NotificationRepository;
import br.com.avisaai.domain.Notification;
import br.com.avisaai.domain.Phone;
import br.com.avisaai.domain.Statuses.NotificationStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * A fila "enviar agora": a cada minuto, avisos vencidos passam de PENDING para READY com o texto montado;
 * o atendente abre o link do WhatsApp, envia e marca como enviado.
 */
@Service
@Transactional
public class NotificationService {

    /** Mensagem pronta, com os links para enviar no celular (wa.me) ou no WhatsApp do computador. */
    public record Ready(Notification notification, String phonePretty, String chatLink, String desktopLink) {
    }

    private final NotificationRepository notifications;
    private final MessageService messages;
    private final WhatsAppLinks links;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, MessageService messages, WhatsAppLinks links, Clock clock) {
        this.notifications = notifications;
        this.messages = messages;
        this.links = links;
        this.clock = clock;
    }

    /** Libera os avisos cuja hora chegou. Roda a cada minuto e também logo após criar um compromisso. */
    @Scheduled(fixedDelayString = "${app.release-every-ms:60000}", initialDelay = 5000)
    public int releaseDue() {
        Instant now = clock.instant();
        int released = 0;
        for (Notification n : notifications.findByStatusAndDueAtLessThanEqualOrderByDueAtAsc(NotificationStatus.PENDING, now)) {
            if (n.getReminder().isClosed()) {
                n.setStatus(NotificationStatus.CANCELED);
                n.setReason("Compromisso encerrado");
            } else if (!n.getReminder().getCustomer().isConsent()) {
                n.setStatus(NotificationStatus.SKIPPED);
                n.setReason("Cliente sem consentimento para WhatsApp");
            } else {
                n.setText(messages.render(n));
                n.setStatus(NotificationStatus.READY);
                n.setReadyAt(now);
                released++;
            }
        }
        return released;
    }

    @Transactional(readOnly = true)
    public List<Ready> queue() {
        return notifications.findByStatusOrderByDueAtAsc(NotificationStatus.READY).stream().map(this::toReady).toList();
    }

    public Ready toReady(Notification n) {
        String phone = n.getReminder().getCustomer().getPhone();
        String text = n.getText() != null ? n.getText() : messages.render(n);
        return new Ready(n, Phone.pretty(phone), links.chatLink(phone, text), links.desktopLink(phone, text));
    }

    public Notification markSent(Long id) {
        Notification n = get(id);
        n.setStatus(NotificationStatus.SENT);
        n.setSentAt(clock.instant());
        return n;
    }

    public Notification skip(Long id, String reason) {
        Notification n = get(id);
        n.setStatus(NotificationStatus.SKIPPED);
        n.setReason(reason == null || reason.isBlank() ? "Dispensado pelo atendente" : reason);
        return n;
    }

    /** Coloca uma mensagem na fila agora (ex.: reenviar). */
    public Notification sendNow(Long id) {
        Notification n = get(id);
        n.setText(messages.render(n));
        n.setStatus(NotificationStatus.READY);
        n.setReadyAt(clock.instant());
        return n;
    }

    @Transactional(readOnly = true)
    public List<Notification> forReminder(Long reminderId) {
        return notifications.findByReminderIdOrderByDueAtAsc(reminderId);
    }

    private Notification get(Long id) {
        return notifications.findById(id).orElseThrow(() -> new NotFoundException("Mensagem", id));
    }
}
