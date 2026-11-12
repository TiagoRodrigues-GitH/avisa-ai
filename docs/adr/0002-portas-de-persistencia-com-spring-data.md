# ADR 0002 — Hexagonal pragmática: JPA no domínio e portas Spring Data

- **Status:** aceita (12/11/2026)
- **Contexto:** a arquitetura hexagonal "pura" pede entidades de domínio sem anotações e um mapeamento para entidades de persistência separadas. Para um produto pequeno isso dobra o código sem ganho prático.
- **Decisão:** as entidades do domínio usam apenas anotações `jakarta.persistence` (padrão, não Spring) e Lombok. As portas de persistência são interfaces Spring Data em `application.Repositories`; o framework gera os adaptadores. As regras que importam (`ReminderPolicy`, `Phone`, `DefaultTemplates`) são classes puras, testadas sem banco.
- **Consequências:** menos código e o mesmo isolamento onde ele vale; o `ArchitectureTest` (ArchUnit) impede que o domínio dependa de Spring ou das camadas externas e que a aplicação dependa dos adaptadores. Se um dia o domínio precisar sair do JPA, a troca fica localizada nas entidades.
