package br.com.avisaai.application;

import br.com.avisaai.application.Repositories.NotificationRepository;
import br.com.avisaai.application.Repositories.ReminderRepository;
import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.Reminder;
import br.com.avisaai.domain.ReminderPolicy;
import br.com.avisaai.domain.ReminderType;
import br.com.avisaai.domain.Statuses.NotificationStatus;
import br.com.avisaai.domain.Statuses.ReminderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Números do painel: fila, enviados hoje, confirmações, faltas e pagamentos em aberto. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    public record Summary(long readyNow, long sentToday, long scheduledToday, long confirmedNext7Days,
                          long awaitingConfirmation, long overduePayments, long openDeliveries, long noShows30Days,
                          double confirmationRate30Days, List<Reminder> today) {
    }

    private final ReminderRepository reminders;
    private final NotificationRepository notifications;
    private final ReminderPolicy policy;
    private final Clock clock;

    public DashboardService(ReminderRepository reminders, NotificationRepository notifications, ReminderPolicy policy, Clock clock) {
        this.reminders = reminders;
        this.notifications = notifications;
        this.policy = policy;
        this.clock = clock;
    }

    public Summary summary() {
        Instant now = clock.instant();
        LocalDate todayLocal = now.atZone(policy.zone()).toLocalDate();
        Instant startToday = todayLocal.atStartOfDay(policy.zone()).toInstant();
        Instant endToday = todayLocal.plusDays(1).atStartOfDay(policy.zone()).toInstant();

        List<Reminder> today = reminders.findByEventAtBetweenOrderByEventAtAsc(startToday, endToday);
        List<Reminder> next7 = reminders.findByTypeAndEventAtBetweenOrderByEventAtAsc(ReminderType.APPOINTMENT, now, now.plus(Duration.ofDays(7)));
        List<Reminder> last30 = reminders.findByTypeAndEventAtBetweenOrderByEventAtAsc(ReminderType.APPOINTMENT, now.minus(Duration.ofDays(30)), now);
        List<Reminder> payments = reminders.findByTypeAndEventAtBetweenOrderByEventAtAsc(ReminderType.PAYMENT, now.minus(Duration.ofDays(365)), now);

        long asked = notifications.countByKindAndStatusAndSentAtAfter(NotificationKind.APPOINTMENT_CONFIRM, NotificationStatus.SENT, now.minus(Duration.ofDays(30)));
        long confirmed = last30.stream().filter(r -> r.getStatus() == ReminderStatus.CONFIRMED || r.getStatus() == ReminderStatus.DONE).count();

        return new Summary(
                notifications.findByStatusOrderByDueAtAsc(NotificationStatus.READY).size(),
                notifications.countByStatusAndSentAtAfter(NotificationStatus.SENT, startToday),
                today.size(),
                next7.stream().filter(r -> r.getStatus() == ReminderStatus.CONFIRMED).count(),
                next7.stream().filter(r -> r.getStatus() == ReminderStatus.SCHEDULED).count(),
                payments.stream().filter(r -> !r.isClosed()).count(),
                reminders.openDeliveries().size(),
                last30.stream().filter(r -> r.getStatus() == ReminderStatus.NO_SHOW).count(),
                asked == 0 ? 0 : Math.min(1.0, (double) confirmed / asked),
                today);
    }
}
