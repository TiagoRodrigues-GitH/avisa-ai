package br.com.avisaai.domain;

/** Enumerações de estado agrupadas para leitura rápida do modelo. */
public final class Statuses {

    private Statuses() {
    }

    /** Situação do compromisso. */
    public enum ReminderStatus {
        SCHEDULED, CONFIRMED, RESCHEDULE_REQUESTED, CANCELED, DONE, NO_SHOW
    }

    /** Etapas de uma entrega; cada avanço gera um aviso. */
    public enum DeliveryStage {
        PREPARING, OUT_FOR_DELIVERY, NEARBY, DELIVERED
    }

    /**
     * Ciclo de uma mensagem: PENDING (agendada) → READY (na fila "enviar agora", com link do WhatsApp)
     * → SENT (o atendente enviou). SKIPPED: sem consentimento ou fora de hora; CANCELED: compromisso mudou.
     */
    public enum NotificationStatus {
        PENDING, READY, SENT, SKIPPED, CANCELED
    }
}
