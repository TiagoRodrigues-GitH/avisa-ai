package br.com.avisaai.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de quando cada aviso fica pronto, sem dependência de framework (testável com datas fixas).
 *
 * <ul>
 *   <li>Consulta: pedido de confirmação 24 h antes e lembrete 2 h antes. Lembretes com pedido de confirmação
 *       reduzem faltas em consultas (revisão Cochrane de Gurol-Urganci et al., 2013).</li>
 *   <li>Pagamento: 3 dias antes, no dia e no dia seguinte (às 9 h).</li>
 *   <li>Retirada: quando fica pronto e de novo 2 dias depois.</li>
 *   <li>Retorno/revisão: 7 dias antes da data prevista.</li>
 *   <li>Vaga liberada e orçamento: na hora; orçamento volta a ser lembrado em 3 dias.</li>
 *   <li>Pesquisa de satisfação: 2 h depois do atendimento.</li>
 *   <li>Entrega: por etapa (ver {@link #deliveryKind}).</li>
 * </ul>
 * Horário de silêncio: nada fica pronto antes de {@code quietEnd} nem depois de {@code quietStart};
 * o aviso passa para a próxima manhã — a menos que isso o empurre para depois do compromisso.
 */
public final class ReminderPolicy {

    private final ZoneId zone;
    private final LocalTime quietEnd;
    private final LocalTime quietStart;

    public ReminderPolicy(ZoneId zone, LocalTime quietEnd, LocalTime quietStart) {
        if (!quietEnd.isBefore(quietStart)) {
            throw new IllegalArgumentException("Janela de envio inválida");
        }
        this.zone = zone;
        this.quietEnd = quietEnd;
        this.quietStart = quietStart;
    }

    /** Padrão: São Paulo, mensagens entre 8 h e 20 h. */
    public static ReminderPolicy brazilDefault() {
        return new ReminderPolicy(ZoneId.of("America/Sao_Paulo"), LocalTime.of(8, 0), LocalTime.of(20, 0));
    }

    public ZoneId zone() {
        return zone;
    }

    /** Avisos agendados para um compromisso (entregas usam {@link #deliveryKind}). */
    public List<NotificationPlan> plan(ReminderType type, Instant eventAt, Instant now) {
        List<NotificationPlan> plans = new ArrayList<>();
        switch (type) {
            case APPOINTMENT -> {
                if (eventAt.isAfter(now.plus(Duration.ofHours(3)))) {
                    // Confirmação: 24 h antes; se o horário foi marcado em cima da hora, pede já.
                    plans.add(beforeEvent(NotificationKind.APPOINTMENT_CONFIRM, eventAt.minus(Duration.ofHours(24)), eventAt, now));
                }
                Instant soon = eventAt.minus(Duration.ofHours(2));
                if (soon.isAfter(now)) {
                    plans.add(beforeEvent(NotificationKind.APPOINTMENT_SOON, soon, eventAt, now));
                }
            }
            case PAYMENT -> {
                addIfFuture(plans, NotificationKind.PAYMENT_UPCOMING, atLocal(eventAt, -3, 9), now);
                addIfFuture(plans, NotificationKind.PAYMENT_DUE, atLocal(eventAt, 0, 9), now);
                addIfFuture(plans, NotificationKind.PAYMENT_OVERDUE, atLocal(eventAt, 1, 10), now);
            }
            case PICKUP -> {
                plans.add(new NotificationPlan(NotificationKind.PICKUP_READY, inWindow(max(eventAt, now))));
                plans.add(new NotificationPlan(NotificationKind.PICKUP_REMINDER, atLocal(max(eventAt, now), 2, 10)));
            }
            case RETURN_VISIT -> plans.add(new NotificationPlan(NotificationKind.RETURN_DUE,
                    inWindow(max(atLocal(eventAt, -7, 10), now))));
            case WAITLIST -> plans.add(new NotificationPlan(NotificationKind.WAITLIST_SLOT, inWindow(now)));
            case QUOTE -> {
                plans.add(new NotificationPlan(NotificationKind.QUOTE_PENDING, inWindow(now)));
                plans.add(new NotificationPlan(NotificationKind.QUOTE_FOLLOWUP, atLocal(now, 3, 10)));
            }
            case FEEDBACK -> plans.add(new NotificationPlan(NotificationKind.FEEDBACK_REQUEST,
                    inWindow(max(eventAt.plus(Duration.ofHours(2)), now))));
            case DELIVERY -> {
                // Avisos de entrega são disparados pelas etapas, não pelo relógio.
            }
        }
        return plans;
    }

    /** Aviso gerado quando a entrega muda de etapa (nenhum para "preparando"). */
    public static NotificationKind deliveryKind(Statuses.DeliveryStage stage) {
        return switch (stage) {
            case PREPARING -> null;
            case OUT_FOR_DELIVERY -> NotificationKind.DELIVERY_OUT;
            case NEARBY -> NotificationKind.DELIVERY_NEARBY;
            case DELIVERED -> NotificationKind.DELIVERY_DONE;
        };
    }

    /** Move um instante para dentro da janela de envio (8 h–20 h por padrão). */
    public Instant inWindow(Instant at) {
        ZonedDateTime local = at.atZone(zone);
        LocalTime time = local.toLocalTime();
        if (time.isBefore(quietEnd)) {
            return local.with(quietEnd).toInstant();
        }
        if (!time.isBefore(quietStart)) {
            return local.plusDays(1).with(quietEnd).toInstant();
        }
        return at;
    }

    private NotificationPlan beforeEvent(NotificationKind kind, Instant ideal, Instant eventAt, Instant now) {
        Instant due = max(ideal, now);
        Instant shifted = inWindow(due);
        if (shifted.isAfter(eventAt.minus(Duration.ofMinutes(30)))) {
            // A manhã seguinte seria tarde demais: antecipa para a noite anterior dentro da janela.
            ZonedDateTime local = due.atZone(zone);
            ZonedDateTime evening = (local.toLocalTime().isBefore(quietEnd) ? local.minusDays(1) : local)
                    .with(quietStart.minusMinutes(30));
            shifted = evening.toInstant().isBefore(now) ? due : evening.toInstant();
        }
        return new NotificationPlan(kind, shifted);
    }

    private void addIfFuture(List<NotificationPlan> plans, NotificationKind kind, Instant at, Instant now) {
        if (!at.isBefore(now.minus(Duration.ofMinutes(5)))) {
            plans.add(new NotificationPlan(kind, inWindow(max(at, now))));
        }
    }

    /** Dia do evento + {@code days}, no horário local {@code hour}:00. */
    private Instant atLocal(Instant eventAt, int days, int hour) {
        return eventAt.atZone(zone).toLocalDate().plusDays(days).atTime(hour, 0).atZone(zone).toInstant();
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
