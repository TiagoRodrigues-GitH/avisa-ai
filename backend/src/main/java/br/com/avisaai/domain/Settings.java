package br.com.avisaai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dados da empresa usados nas mensagens (linha única, id = 1). */
@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
public class Settings {

    public static final long SINGLETON = 1L;

    @Id
    private Long id = SINGLETON;

    @Column(nullable = false, length = 80)
    private String businessName = "Sua Empresa";

    @Column(length = 200)
    private String address = "";

    /** Telefone da empresa (opcional, só para exibir). */
    @Column(length = 15)
    private String businessPhone;
}
