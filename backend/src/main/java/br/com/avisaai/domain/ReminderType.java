package br.com.avisaai.domain;

/** O que está sendo lembrado. Cada tipo tem a sua sequência de avisos em {@link ReminderPolicy}. */
public enum ReminderType {
    /** Consulta, sessão, horário marcado: pede confirmação e lembra pouco antes (anti-falta). */
    APPOINTMENT("Consulta / horário"),
    /** Boleto, mensalidade, parcela: antes, no dia e depois do vencimento. */
    PAYMENT("Pagamento"),
    /** Pedido, exame, documento ou material pronto para retirar. */
    PICKUP("Retirada"),
    /** Entrega: saiu, a caminho, entregue (avisos por etapa). */
    DELIVERY("Entrega"),
    /** Retorno ou revisão periódica (consulta de retorno, revisão do carro, vacina, troca de óleo). */
    RETURN_VISIT("Retorno / revisão"),
    /** Lista de espera: abriu um horário. */
    WAITLIST("Vaga liberada"),
    /** Orçamento aguardando aprovação. */
    QUOTE("Orçamento"),
    /** Pesquisa de satisfação depois do atendimento (nota de 0 a 10). */
    FEEDBACK("Pesquisa de satisfação");

    private final String label;

    ReminderType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
