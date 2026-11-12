# Como apresentar o backend (portfólio, entrevista, cliente)

Um backend não tem tela; quem avalia precisa **ver o contrato, entender as decisões e confiar que funciona**. As formas abaixo são todas gratuitas e já estão no projeto.

| # | Forma | Por que funciona | No Avisa Aí |
|---|---|---|---|
| 1 | **OpenAPI + Swagger UI** | Contrato padrão de APIs REST; quem avalia chama os endpoints no navegador, sem instalar nada | `/swagger-ui.html` com descrições em português e login (*Authorize*) |
| 2 | **Diagramas C4** | Zoom do contexto até os componentes; padrão leve citado em guias de documentação de arquitetura | `docs/arquitetura.md` (Mermaid, renderiza no GitHub) |
| 3 | **ADRs (Architecture Decision Records)** | Mostram o *porquê* das escolhas, as alternativas e as consequências — é o que diferencia júnior de sênior | `docs/adr/0001…0004` |
| 4 | **Regras de arquitetura como teste (ArchUnit)** | Prova automática de que a arquitetura hexagonal é respeitada a cada build | `ArchitectureTest.java` |
| 5 | **Testes de regra de negócio com datas fixas** | Mostram o comportamento em casos-limite (madrugada, vencimento passado, horário em cima da hora) | `ReminderPolicyTest.java` |
| 6 | **Coleção de requisições (Bruno)** | Roteiro de demonstração reproduzível, versionado em texto no Git; Bruno é gratuito e de código aberto (alternativa ao Postman) | `backend/bruno/` |
| 7 | **Dados de demonstração** | Ao abrir, já há clientes, consultas, entrega e orçamento para mostrar o fluxo | `DemoData.java` |
| 8 | **Erros padronizados (RFC 9457 Problem Details)** | Mensagens de erro previsíveis e legíveis | `ApiErrors.java` |
| 9 | **Health check** | Mostra que o serviço está de pé (monitoração) | `/actuator/health` |
| 10 | **README com telas, pilha e "como rodar em 3 comandos"** | Primeira impressão em 30 segundos | `README.md` |

## Roteiro de 5 minutos

1. Abra o README (o problema, a solução gratuita, as telas).
2. Mostre o diagrama de componentes e o ADR 0001 (por que link `wa.me` e não API paga).
3. Abra o Swagger UI → `POST /api/reminders` com uma consulta para daqui a 5 horas → `GET /api/queue` mostra a mensagem pronta e o link.
4. Abra o link: o WhatsApp abre com o texto pronto.
5. Rode `mvn test` e mostre o `ArchitectureTest` e o `ReminderPolicyTest` passando.
