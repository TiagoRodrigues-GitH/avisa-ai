package br.com.avisaai;

import br.com.avisaai.domain.NotificationKind;
import br.com.avisaai.domain.NotificationPlan;
import br.com.avisaai.domain.ReminderPolicy;
import br.com.avisaai.domain.ReminderType;
import br.com.avisaai.domain.Statuses.DeliveryStage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderPolicyTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private final ReminderPolicy policy = ReminderPolicy.brazilDefault();

    private static Instant at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 11, day, hour, minute).atZone(SP).toInstant();
    }

    private static LocalDateTime local(Instant i) {
        return LocalDateTime.ofInstant(i, SP);
    }

    @Test
    void appointmentAsksForConfirmation24hBeforeAndRemindsTwoHoursBefore() {
        List<NotificationPlan> plans = policy.plan(ReminderType.APPOINTMENT, at(20, 14, 0), at(12, 10, 0));
        assertThat(plans).extracting(NotificationPlan::kind)
                .containsExactly(NotificationKind.APPOINTMENT_CONFIRM, NotificationKind.APPOINTMENT_SOON);
        assertThat(local(plans.get(0).dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 19, 14, 0));
        assertThat(local(plans.get(1).dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 20, 12, 0));
    }

    @Test
    void earlyMorningAppointmentGetsTheConfirmationTheEveningBeforeNotAfterIt() {
        // Consulta às 8h30: "2 h antes" seria 6h30 (silêncio) -> 8h, ainda 30 min antes: ok.
        List<NotificationPlan> plans = policy.plan(ReminderType.APPOINTMENT, at(20, 8, 30), at(12, 10, 0));
        NotificationPlan soon = plans.stream().filter(p -> p.kind() == NotificationKind.APPOINTMENT_SOON).findFirst().orElseThrow();
        assertThat(local(soon.dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 20, 8, 0));
        // Consulta às 7h: a manhã seria tarde demais -> lembrete na noite anterior, dentro da janela.
        List<NotificationPlan> early = policy.plan(ReminderType.APPOINTMENT, at(20, 7, 0), at(12, 10, 0));
        NotificationPlan soonEarly = early.stream().filter(p -> p.kind() == NotificationKind.APPOINTMENT_SOON).findFirst().orElseThrow();
        assertThat(local(soonEarly.dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 19, 19, 30));
    }

    @Test
    void lastMinuteAppointmentAsksForConfirmationNow() {
        Instant now = at(12, 10, 0);
        List<NotificationPlan> plans = policy.plan(ReminderType.APPOINTMENT, at(12, 17, 0), now);
        assertThat(plans.get(0).kind()).isEqualTo(NotificationKind.APPOINTMENT_CONFIRM);
        assertThat(plans.get(0).dueAt()).isEqualTo(now);
    }

    @Test
    void paymentIsRemindedBeforeOnAndAfterTheDueDateAtBusinessHours() {
        List<NotificationPlan> plans = policy.plan(ReminderType.PAYMENT, at(20, 0, 0), at(12, 10, 0));
        assertThat(plans).extracting(NotificationPlan::kind).containsExactly(
                NotificationKind.PAYMENT_UPCOMING, NotificationKind.PAYMENT_DUE, NotificationKind.PAYMENT_OVERDUE);
        assertThat(local(plans.get(0).dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 17, 9, 0));
        assertThat(local(plans.get(2).dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 21, 10, 0));
    }

    @Test
    void pastStepsOfAPaymentAreNotScheduled() {
        List<NotificationPlan> plans = policy.plan(ReminderType.PAYMENT, at(13, 0, 0), at(12, 15, 0));
        assertThat(plans).extracting(NotificationPlan::kind)
                .containsExactly(NotificationKind.PAYMENT_DUE, NotificationKind.PAYMENT_OVERDUE);
    }

    @Test
    void nothingIsReadyDuringQuietHours() {
        assertThat(local(policy.inWindow(at(12, 23, 0)))).isEqualTo(LocalDateTime.of(2026, 11, 13, 8, 0));
        assertThat(local(policy.inWindow(at(12, 6, 0)))).isEqualTo(LocalDateTime.of(2026, 11, 12, 8, 0));
        assertThat(local(policy.inWindow(at(12, 15, 0)))).isEqualTo(LocalDateTime.of(2026, 11, 12, 15, 0));
    }

    @Test
    void pickupQuoteWaitlistFeedbackAndReturnHaveTheirOwnSequences() {
        Instant now = at(12, 10, 0);
        assertThat(policy.plan(ReminderType.PICKUP, now, now)).extracting(NotificationPlan::kind)
                .containsExactly(NotificationKind.PICKUP_READY, NotificationKind.PICKUP_REMINDER);
        assertThat(policy.plan(ReminderType.QUOTE, now, now)).extracting(NotificationPlan::kind)
                .containsExactly(NotificationKind.QUOTE_PENDING, NotificationKind.QUOTE_FOLLOWUP);
        assertThat(policy.plan(ReminderType.WAITLIST, at(13, 9, 0), now).get(0).dueAt()).isEqualTo(now);
        assertThat(local(policy.plan(ReminderType.FEEDBACK, now, now).get(0).dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 12, 12, 0));
        assertThat(local(policy.plan(ReminderType.RETURN_VISIT, at(30, 9, 0), now).get(0).dueAt())).isEqualTo(LocalDateTime.of(2026, 11, 23, 10, 0));
    }

    @Test
    void deliveriesAreDrivenByStagesNotByTheClock() {
        assertThat(policy.plan(ReminderType.DELIVERY, at(12, 18, 0), at(12, 10, 0))).isEmpty();
        assertThat(ReminderPolicy.deliveryKind(DeliveryStage.PREPARING)).isNull();
        assertThat(ReminderPolicy.deliveryKind(DeliveryStage.OUT_FOR_DELIVERY)).isEqualTo(NotificationKind.DELIVERY_OUT);
        assertThat(ReminderPolicy.deliveryKind(DeliveryStage.NEARBY)).isEqualTo(NotificationKind.DELIVERY_NEARBY);
        assertThat(ReminderPolicy.deliveryKind(DeliveryStage.DELIVERED)).isEqualTo(NotificationKind.DELIVERY_DONE);
    }
}
