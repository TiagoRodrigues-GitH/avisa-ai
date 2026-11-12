package br.com.avisaai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Texto personalizado pela empresa para um tipo de aviso; sem registro, vale o padrão. */
@Entity
@Table(name = "message_templates")
@Getter
@Setter
@NoArgsConstructor
public class MessageTemplate {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private NotificationKind kind;

    @Column(nullable = false, length = 1000)
    private String text;

    public MessageTemplate(NotificationKind kind, String text) {
        this.kind = kind;
        this.text = text;
    }
}
