package br.com.avisaai.application;

import br.com.avisaai.domain.Customer;
import br.com.avisaai.domain.MessageTemplate;
import br.com.avisaai.domain.Notification;
import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.Reminder;
import br.com.avisaai.domain.ReminderType;
import br.com.avisaai.domain.Settings;
import br.com.avisaai.domain.Statuses.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Portas de persistência. São interfaces do Spring Data: o adaptador (implementação) é gerado pelo framework.
 * Decisão registrada em docs/adr/0002-portas-de-persistencia-com-spring-data.md.
 */
public final class Repositories {

    private Repositories() {
    }

    public interface CustomerRepository extends JpaRepository<Customer, Long> {
        List<Customer> findByNameContainingIgnoreCaseOrPhoneContainingOrderByNameAsc(String name, String phone);

        List<Customer> findAllByOrderByNameAsc();
    }

    public interface ReminderRepository extends JpaRepository<Reminder, Long> {
        List<Reminder> findByEventAtBetweenOrderByEventAtAsc(Instant from, Instant to);

        List<Reminder> findByTypeAndEventAtBetweenOrderByEventAtAsc(ReminderType type, Instant from, Instant to);

        List<Reminder> findByCustomerIdOrderByEventAtDesc(Long customerId);

        @Query("select r from Reminder r where r.type = br.com.avisaai.domain.ReminderType.DELIVERY "
                + "and r.status not in (br.com.avisaai.domain.Statuses.ReminderStatus.DONE, br.com.avisaai.domain.Statuses.ReminderStatus.CANCELED) "
                + "order by r.eventAt asc")
        List<Reminder> openDeliveries();
    }

    public interface NotificationRepository extends JpaRepository<Notification, Long> {
        List<Notification> findByStatusAndDueAtLessThanEqualOrderByDueAtAsc(NotificationStatus status, Instant now);

        List<Notification> findByStatusOrderByDueAtAsc(NotificationStatus status);

        List<Notification> findByReminderIdOrderByDueAtAsc(Long reminderId);

        List<Notification> findByReminderIdAndStatusIn(Long reminderId, Collection<NotificationStatus> statuses);

        List<Notification> findByStatusAndDueAtBetweenOrderByDueAtAsc(NotificationStatus status, Instant from, Instant to);

        long countByStatusAndSentAtAfter(NotificationStatus status, Instant since);

        long countByKindAndStatusAndSentAtAfter(NotificationKind kind, NotificationStatus status, Instant since);
    }

    public interface TemplateRepository extends JpaRepository<MessageTemplate, NotificationKind> {
    }

    public interface SettingsRepository extends JpaRepository<Settings, Long> {
    }
}
