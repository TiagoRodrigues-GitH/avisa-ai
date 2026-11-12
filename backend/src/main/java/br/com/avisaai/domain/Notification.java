package br.com.avisaai.domain;

import br.com.avisaai.domain.Statuses.NotificationStatus;
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

import java.time.Instant;

/** Uma mensagem de WhatsApp preparada pelo sistema. O texto é montado quando ela fica pronta (READY). */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_due", columnList = "status,dueAt"))
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reminder_id")
    private Reminder reminder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationKind kind;

    @Column(nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(length = 1000)
    private String text;

    /** Motivo de SKIPPED/CANCELED, para o painel. */
    @Column(length = 120)
    private String reason;

    private Instant readyAt;

    private Instant sentAt;

    public Notification(Reminder reminder, NotificationKind kind, Instant dueAt) {
        this.reminder = reminder;
        this.kind = kind;
        this.dueAt = dueAt;
    }
}
