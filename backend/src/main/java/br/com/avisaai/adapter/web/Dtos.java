package br.com.avisaai.adapter.web;

import br.com.avisaai.application.NotificationService.Ready;
import br.com.avisaai.domain.Customer;
import br.com.avisaai.domain.Notification;
import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.Phone;
import br.com.avisaai.domain.Reminder;
import br.com.avisaai.domain.ReminderType;
import br.com.avisaai.domain.Statuses.DeliveryStage;
import br.com.avisaai.domain.Statuses.NotificationStatus;
import br.com.avisaai.domain.Statuses.ReminderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** Formatos JSON da API (o domínio nunca é exposto diretamente). */
public final class Dtos {

    private Dtos() {
    }

    public record CustomerIn(@NotBlank @Size(max = 120) String name,
                             @NotBlank @Size(max = 30) String phone,
                             boolean consent,
                             @Size(max = 500) String notes) {
    }

    public record CustomerOut(Long id, String name, String phone, String phonePretty, boolean consent, String notes) {
        static CustomerOut of(Customer c) {
            return new CustomerOut(c.getId(), c.getName(), c.getPhone(), Phone.pretty(c.getPhone()), c.isConsent(), c.getNotes());
        }
    }

    public record ReminderIn(@NotNull Long customerId,
                             @NotNull ReminderType type,
                             @NotBlank @Size(max = 160) String title,
                             @NotNull Instant eventAt,
                             @PositiveOrZero BigDecimal amount,
                             @Size(max = 500) String notes) {
    }

    public record ReminderOut(Long id, CustomerOut customer, ReminderType type, String typeLabel, String title,
                              Instant eventAt, BigDecimal amount, ReminderStatus status, DeliveryStage deliveryStage,
                              Integer etaMinutes, String notes) {
        static ReminderOut of(Reminder r) {
            return new ReminderOut(r.getId(), CustomerOut.of(r.getCustomer()), r.getType(), r.getType().label(), r.getTitle(),
                    r.getEventAt(), r.getAmount(), r.getStatus(), r.getDeliveryStage(), r.getEtaMinutes(), r.getNotes());
        }
    }

    public record StatusIn(@NotNull ReminderStatus status) {
    }

    public record DeliveryIn(@NotNull DeliveryStage stage, @Min(1) @Max(600) Integer etaMinutes) {
    }

    public record NotificationOut(Long id, Long reminderId, NotificationKind kind, String kindLabel, Instant dueAt,
                                  NotificationStatus status, String text, String reason, Instant sentAt) {
        static NotificationOut of(Notification n) {
            return new NotificationOut(n.getId(), n.getReminder().getId(), n.getKind(), n.getKind().label(), n.getDueAt(),
                    n.getStatus(), n.getText(), n.getReason(), n.getSentAt());
        }
    }

    /** Item da fila "enviar agora". */
    public record QueueItem(Long id, NotificationKind kind, String kindLabel, Instant dueAt, String text,
                            String chatLink, String desktopLink, ReminderOut reminder) {
        static QueueItem of(Ready r) {
            Notification n = r.notification();
            return new QueueItem(n.getId(), n.getKind(), n.getKind().label(), n.getDueAt(), n.getText(),
                    r.chatLink(), r.desktopLink(), ReminderOut.of(n.getReminder()));
        }
    }

    public record SkipIn(@Size(max = 120) String reason) {
    }

    public record TemplateOut(NotificationKind kind, String label, ReminderType type, String typeLabel, String text,
                              boolean custom, String preview) {
    }

    public record TemplateIn(@NotBlank @Size(max = 1000) String text) {
    }

    public record SettingsIn(@NotBlank @Size(max = 80) String businessName,
                             @Size(max = 200) String address,
                             @Size(max = 30) String businessPhone) {
    }

    public record SettingsOut(String businessName, String address, String businessPhone) {
    }
}
