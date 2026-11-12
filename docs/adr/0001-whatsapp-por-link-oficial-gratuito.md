# ADR 0001 — WhatsApp pelo link oficial gratuito (Click to Chat)

- **Status:** aceita (12/11/2026)
- **Contexto:** o produto precisa enviar avisos só pelo WhatsApp e não pode ter custo. A WhatsApp Business Platform (Cloud API) cobra por mensagem de modelo desde 1º/07/2025 e, desde 1º/10/2026, também as mensagens de atendimento. Bibliotecas não oficiais (WhatsApp Web automatizado) são gratuitas, mas violam os Termos de Serviço e podem banir o número da empresa.
- **Decisão:** o sistema monta o link oficial `https://wa.me/<número>?text=<mensagem>` (e `whatsapp://send` para o WhatsApp Desktop). O atendente abre, confere e envia com um toque; depois marca "Enviei". As respostas do cliente (1 = confirma, 2 = remarcar) são registradas com um toque no app.
- **Alternativas:** Cloud API da Meta (automático, pago, exige conta Business verificada e modelos aprovados); automação não oficial (gratuita, arriscada, não aceita); SMS (pago).
- **Consequências:** custo zero e nenhum risco de bloqueio; o envio continua manual (1 toque por mensagem), aceitável para o volume de pequenos negócios. O envio passa por uma porta (`WhatsAppLinks`): ligar a Cloud API no futuro é escrever um adaptador novo, sem mexer em regras ou telas.
