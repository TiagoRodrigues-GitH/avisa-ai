package br.com.avisaai.domain;

import br.com.avisaai.domain.Statuses.DeliveryStage;
import br.com.avisaai.domain.Statuses.ReminderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Um compromisso a lembrar: consulta, vencimento, retirada, entrega, retorno, vaga, orçamento ou pesquisa. */
@Entity
@Table(name = "reminders", indexes = @Index(name = "idx_reminders_event", columnList = "eventAt"))
@Getter
@Setter
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderType type;

    /** O que aparece na mensagem: "consulta com a Dra. Ana", "mensalidade de março", "pedido #1234". */
    @Column(nullable = false, length = 160)
    private String title;

    /** Data e hora do compromisso / vencimento / atendimento. */
    @Column(nullable = false)
    private Instant eventAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReminderStatus status = ReminderStatus.SCHEDULED;

    /** Só para entregas. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeliveryStage deliveryStage;

    /** Minutos estimados até a entrega (aviso "a caminho"). */
    private Integer etaMinutes;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Pagamento quitado, material retirado, atendimento feito, entrega concluída ou cancelado. */
    public boolean isClosed() {
        return status == ReminderStatus.DONE || status == ReminderStatus.CANCELED || status == ReminderStatus.NO_SHOW;
    }
}
