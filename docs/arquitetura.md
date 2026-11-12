# Arquitetura do Avisa Aí (modelo C4)

O modelo C4 descreve o software em níveis de zoom: contexto → contêineres → componentes. Os diagramas abaixo são Mermaid (o GitHub desenha direto no navegador).

## 1. Contexto

```mermaid
flowchart LR
    att([Atendente<br/>no PC ou celular])
    cli([Cliente<br/>recebe no WhatsApp])
    sys[[Avisa Aí<br/>prepara os avisos na hora certa]]
    wa[(WhatsApp<br/>app / Web / Desktop)]
    att -- cadastra compromissos,<br/>marca respostas --> sys
    sys -- link wa.me com<br/>a mensagem pronta --> att
    att -- envia com 1 toque --> wa
    wa -- mensagem --> cli
    cli -- responde 1 / 2 --> wa
```

## 2. Contêineres

```mermaid
flowchart TB
    subgraph PC["PC da empresa (gratuito)"]
        api["API Spring Boot 3.5 / Java 17<br/>REST + OpenAPI · agendador a cada minuto"]
        db[("H2 em arquivo<br/>ou PostgreSQL")]
        static["App Angular 21 (PWA)<br/>servido pela própria API"]
    end
    browser["Navegador / app instalado<br/>PC e celular na mesma rede"]
    browser -- HTTPS/HTTP + Basic --> api
    browser -- carrega --> static
    api -- JPA --> db
```

## 3. Componentes (arquitetura hexagonal)

```mermaid
flowchart LR
    subgraph adapters["Adaptadores"]
        web["adapter.web<br/>Controllers REST · DTOs · erros RFC 9457"]
        wa["adapter.whatsapp<br/>ClickToChatLinks (wa.me / whatsapp://)"]
    end
    subgraph app["Aplicação (casos de uso)"]
        rs["ReminderService"]
        ns["NotificationService<br/>(fila 'enviar agora')"]
        ms["MessageService<br/>(textos e variáveis)"]
        ds["DashboardService"]
        ports{{"Portas: WhatsAppLinks · Repositories"}}
    end
    subgraph domain["Domínio (sem framework)"]
        pol["ReminderPolicy<br/>quando cada aviso fica pronto"]
        ent["Customer · Reminder · Notification<br/>Phone · DefaultTemplates"]
    end
    web --> rs & ns & ds
    rs --> pol
    ns --> ms
    ns --> ports
    wa -. implementa .-> ports
    app --> ent
```

Regras garantidas por teste (`ArchitectureTest`, ArchUnit): o domínio não depende de Spring nem das camadas externas; a aplicação não conhece os adaptadores; os adaptadores não se chamam entre si.

## Ciclo de uma mensagem

```mermaid
stateDiagram-v2
    [*] --> PENDING: compromisso criado (ReminderPolicy agenda)
    PENDING --> READY: chegou a hora (dentro de 8h–20h) e há consentimento
    PENDING --> SKIPPED: cliente sem consentimento
    PENDING --> CANCELED: compromisso alterado, confirmado ou encerrado
    READY --> SENT: atendente abriu o WhatsApp e tocou em "Enviei"
    READY --> SKIPPED: "Dispensar"
    SENT --> READY: "Reenviar"
```
