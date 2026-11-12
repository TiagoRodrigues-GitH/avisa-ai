package br.com.avisaai.domain;

import java.time.Instant;

/** Uma mensagem que deve ficar pronta para envio em {@code dueAt}. */
public record NotificationPlan(NotificationKind kind, Instant dueAt) {
}
