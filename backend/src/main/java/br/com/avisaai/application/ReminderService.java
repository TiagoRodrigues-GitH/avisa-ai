package br.com.avisaai.application;

import br.com.avisaai.application.Repositories.CustomerRepository;
import br.com.avisaai.application.Repositories.NotificationRepository;
import br.com.avisaai.application.Repositories.ReminderRepository;
import br.com.avisaai.domain.Customer;
import br.com.avisaai.domain.Notification;
import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.NotificationPlan;
import br.com.avisaai.domain.Reminder;
import br.com.avisaai.domain.ReminderPolicy;
import br.com.avisaai.domain.ReminderType;
import br.com.avisaai.domain.Statuses.DeliveryStage;
import br.com.avisaai.domain.Statuses.NotificationStatus;
import br.com.avisaai.domain.Statuses.ReminderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/** Casos de uso dos compromissos: criar, alterar, confirmar, concluir e avançar entregas. */
@Service
@Transactional
public class ReminderService {

    /** Dados de entrada vindos de qualquer adaptador (web, importação…). */
    public record ReminderCommand(Long customerId, ReminderType type, String title, Instant eventAt,
                                  BigDecimal amount, String notes) {
    }

    private final ReminderRepository reminders;
    private final CustomerRepository customers;
    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    private final ReminderPolicy policy;
    private final Clock clock;

    public ReminderService(ReminderRepository reminders, CustomerRepository customers, NotificationRepository notifications,
                           NotificationService notificationService, ReminderPolicy policy, Clock clock) {
        this.reminders = reminders;
        this.customers = customers;
        this.notifications = notifications;
        this.notificationService = notificationService;
        this.policy = policy;
        this.clock = clock;
    }

    public Reminder create(ReminderCommand cmd) {
        Reminder r = new Reminder();
        apply(r, cmd);
        if (r.getType() == ReminderType.DELIVERY) {
            r.setDeliveryStage(DeliveryStage.PREPARING);
        }
        reminders.save(r);
        schedule(r);
        notificationService.releaseDue();
        return r;
    }

    public Reminder update(Long id, ReminderCommand cmd) {
        Reminder r = get(id);
        apply(r, cmd);
        cancelOpen(r, "Compromisso alterado");
        schedule(r);
        notificationService.releaseDue();
        return r;
    }

    /** Confirmou, pediu para remarcar, cancelou, concluiu (pagou/retirou/atendido) ou faltou. */
    public Reminder changeStatus(Long id, ReminderStatus status) {
        Reminder r = get(id);
        r.setStatus(status);
        if (r.isClosed()) {
            cancelOpen(r, "Compromisso encerrado");
        } else if (status == ReminderStatus.CONFIRMED) {
            // Já confirmou: não faz sentido pedir confirmação de novo.
            cancelKinds(r, EnumSet.of(NotificationKind.APPOINTMENT_CONFIRM), "Já confirmado");
        } else if (status == ReminderStatus.RESCHEDULE_REQUESTED) {
            cancelOpen(r, "Cliente pediu para remarcar");
        }
        return r;
    }

    /** Avança a entrega para a etapa informada e prepara o aviso correspondente na hora. */
    public Reminder advanceDelivery(Long id, DeliveryStage stage, Integer etaMinutes) {
        Reminder r = get(id);
        if (r.getType() != ReminderType.DELIVERY) {
            throw new IllegalArgumentException("Este compromisso não é uma entrega");
        }
        r.setDeliveryStage(stage);
        if (etaMinutes != null) {
            r.setEtaMinutes(etaMinutes);
        }
        if (stage == DeliveryStage.DELIVERED) {
            r.setStatus(ReminderStatus.DONE);
        }
        NotificationKind kind = ReminderPolicy.deliveryKind(stage);
        if (kind != null) {
            // Aviso de etapa anterior ainda não enviado ficou velho: só vale o da etapa atual.
            cancelKinds(r, EnumSet.of(NotificationKind.DELIVERY_OUT, NotificationKind.DELIVERY_NEARBY, NotificationKind.DELIVERY_DONE),
                    "Substituído pela etapa seguinte");
            notifications.save(new Notification(r, kind, clock.instant()));
            notificationService.releaseDue();
        }
        return r;
    }

    public void delete(Long id) {
        Reminder r = get(id);
        notifications.deleteAll(notifications.findByReminderIdOrderByDueAtAsc(id));
        reminders.delete(r);
    }

    @Transactional(readOnly = true)
    public Reminder get(Long id) {
        return reminders.findById(id).orElseThrow(() -> new NotFoundException("Compromisso", id));
    }

    private void apply(Reminder r, ReminderCommand cmd) {
        Customer c = customers.findById(cmd.customerId()).orElseThrow(() -> new NotFoundException("Cliente", cmd.customerId()));
        r.setCustomer(c);
        r.setType(cmd.type());
        r.setTitle(cmd.title().trim());
        r.setEventAt(cmd.eventAt());
        r.setAmount(cmd.amount());
        r.setNotes(cmd.notes());
    }

    private void schedule(Reminder r) {
        for (NotificationPlan plan : policy.plan(r.getType(), r.getEventAt(), clock.instant())) {
            notifications.save(new Notification(r, plan.kind(), plan.dueAt()));
        }
    }

    private void cancelOpen(Reminder r, String reason) {
        cancelKinds(r, EnumSet.allOf(NotificationKind.class), reason);
    }

    private void cancelKinds(Reminder r, EnumSet<NotificationKind> kinds, String reason) {
        if (r.getId() == null) {
            return;
        }
        List<Notification> open = notifications.findByReminderIdAndStatusIn(r.getId(),
                EnumSet.of(NotificationStatus.PENDING, NotificationStatus.READY));
        for (Notification n : open) {
            if (kinds.contains(n.getKind())) {
                n.setStatus(NotificationStatus.CANCELED);
                n.setReason(reason);
            }
        }
    }
}
