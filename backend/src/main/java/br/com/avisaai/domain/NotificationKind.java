package br.com.avisaai.domain;

/** Cada mensagem que o sistema prepara. O texto padrão fica em {@link DefaultTemplates}. */
public enum NotificationKind {
    APPOINTMENT_CONFIRM(ReminderType.APPOINTMENT, "Pedido de confirmação (24 h antes)"),
    APPOINTMENT_SOON(ReminderType.APPOINTMENT, "Lembrete do dia (2 h antes)"),
    PAYMENT_UPCOMING(ReminderType.PAYMENT, "Vence em 3 dias"),
    PAYMENT_DUE(ReminderType.PAYMENT, "Vence hoje"),
    PAYMENT_OVERDUE(ReminderType.PAYMENT, "Em atraso (dia seguinte)"),
    PICKUP_READY(ReminderType.PICKUP, "Pronto para retirada"),
    PICKUP_REMINDER(ReminderType.PICKUP, "Ainda aguardando retirada (2 dias)"),
    DELIVERY_OUT(ReminderType.DELIVERY, "Saiu para entrega"),
    DELIVERY_NEARBY(ReminderType.DELIVERY, "Entregador a caminho"),
    DELIVERY_DONE(ReminderType.DELIVERY, "Entregue"),
    RETURN_DUE(ReminderType.RETURN_VISIT, "Hora do retorno (7 dias antes)"),
    WAITLIST_SLOT(ReminderType.WAITLIST, "Horário disponível"),
    QUOTE_PENDING(ReminderType.QUOTE, "Orçamento pronto"),
    QUOTE_FOLLOWUP(ReminderType.QUOTE, "Acompanhamento do orçamento (3 dias)"),
    FEEDBACK_REQUEST(ReminderType.FEEDBACK, "Pesquisa de satisfação (2 h depois)");

    private final ReminderType type;
    private final String label;

    NotificationKind(ReminderType type, String label) {
        this.type = type;
        this.label = label;
    }

    public ReminderType type() {
        return type;
    }

    public String label() {
        return label;
    }
}
