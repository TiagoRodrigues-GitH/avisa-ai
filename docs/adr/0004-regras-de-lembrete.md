# ADR 0004 — Regras de quando cada aviso é enviado

- **Status:** aceita (12/11/2026)
- **Contexto:** faltas em consultas custam caro a clínicas; lembretes por mensagem de texto aumentam o comparecimento em relação a nenhum lembrete (revisão Cochrane, Gurol-Urganci et al., 2013, *Mobile phone messaging reminders for attendance at healthcare appointments*). Pedir confirmação ativa (responder 1/2) permite reaproveitar o horário de quem não vem (lista de espera).
- **Decisão:** a sequência de cada tipo está em `ReminderPolicy` (classe pura, com testes de datas fixas):
  consulta 24 h antes (confirmação) + 2 h antes; pagamento D-3, D0 9 h e D+1 10 h; retirada na hora + 2 dias; retorno 7 dias antes; vaga e orçamento na hora (+3 dias para orçamento); pesquisa 2 h depois; entrega por etapa.
  Janela de envio 8 h–20 h (configurável): fora dela o aviso vai para a manhã, ou para a noite anterior quando a manhã seria tarde demais. Cliente sem consentimento nunca recebe. Confirmou → não pede de novo; remarcar/encerrar → avisos pendentes cancelados; etapa nova de entrega → aviso velho cancelado.
- **Consequências:** comportamento previsível e auditável (a tela de cada compromisso mostra a linha do tempo das mensagens). Mudar uma regra é mudar uma classe e seus testes.
