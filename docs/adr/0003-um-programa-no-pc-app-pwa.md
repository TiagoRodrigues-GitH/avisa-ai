# ADR 0003 — Um programa no PC; app Angular instalável (PWA) servido pela API

- **Status:** aceita (12/11/2026)
- **Contexto:** precisa funcionar no celular e no computador, sem lojas de aplicativo e sem nuvem paga.
- **Decisão:** monólito Spring Boot que também entrega o app Angular compilado. O app é PWA: no celular, "Adicionar à tela inicial"; no PC, "Instalar app" no navegador. O celular acessa o PC pela rede Wi-Fi (`http://IP-DO-PC:8080`). Banco H2 em arquivo (zero instalação); PostgreSQL opcional pela variável `DATABASE_URL`.
- **Alternativas:** app nativo Android/iOS (taxas de loja, dois códigos); hospedagem em nuvem gratuita (Render/Neon funcionam, mas o servidor "dorme" e os dados de clientes saem do PC).
- **Consequências:** custo zero e dados no PC da empresa (LGPD mais simples). Para acesso fora da rede local, o mesmo pacote pode ir para um plano gratuito de nuvem ou para um túnel gratuito, sem mudar o código.
