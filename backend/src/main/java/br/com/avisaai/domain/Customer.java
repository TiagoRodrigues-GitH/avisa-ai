package br.com.avisaai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Cliente que recebe os avisos. Só recebe mensagens com consentimento registrado (LGPD, art. 7º, I). */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    /** Só dígitos, com DDI (ver {@link Phone#normalize}). */
    @Column(nullable = false, length = 15)
    private String phone;

    /** O cliente aceitou receber avisos pelo WhatsApp. */
    @Column(nullable = false)
    private boolean consent;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Customer(String name, String phone, boolean consent) {
        this.name = name;
        this.phone = Phone.normalize(phone);
        this.consent = consent;
    }

    /** Primeiro nome, para mensagens mais pessoais. */
    public String firstName() {
        String trimmed = name == null ? "" : name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}
