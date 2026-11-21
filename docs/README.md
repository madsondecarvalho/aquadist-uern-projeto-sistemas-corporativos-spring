# Documentação — AquaDist

Bem-vindo à documentação do sistema **AquaDist**, uma aplicação de demonstração para gestão de
clientes, estoque de garrafões, pedidos e entregas de água mineral.

## Índice

| Documento | Conteúdo |
|---|---|
| [01 — Visão geral](01-visao-geral.md) | Problema, solução, escopo, personas e tecnologias |
| [02 — Regras de negócio](02-regras-de-negocio.md) | RN01 a RN05, regras complementares e rastreabilidade no código |
| [03 — Casos de uso](03-casos-de-uso.md) | Catálogo UC01–UC16: atores, fluxos, exceções, endpoints, classes, testes, diagramas e rastreabilidade |
| [04 — Arquitetura](04-arquitetura.md) | Camadas, pacotes, segurança, eventos, banco e decisões de projeto |
| [05 — Modelo de dados](05-modelo-de-dados.md) | Entidades, relacionamentos, enums, ciclo do vasilhame e seed |
| [06 — API REST](06-api.md) | Endpoints, autenticação, erros e exemplos `curl` |
| [07 — Testes](07-testes.md) | Unitários, integração, E2E, H2 em memória e CI |
| [08 — Manual de uso](08-manual-de-uso.md) | Passo a passo das telas e roteiro de demonstração |
| [09 — Eclipse](09-eclipse.md) | Importação, JDK 25, execução, testes e troubleshooting no Eclipse |

## Resumo técnico

- **Java 25** e **Spring Boot 4.2.0**
- **Spring MVC**, **Spring Data JPA**, **Spring Security 7** e **Bean Validation**
- **SQLite** como banco local (`./data/aquadist.db`), em modo **WAL** com pool de 5 conexões
- **H2 em memória** nos testes de integração e E2E
- **Tailwind CSS via CDN** nas telas HTML estáticas
- **30 testes automatizados**: 18 unitários, 6 de integração e 6 E2E
- CI com GitHub Actions executando `./mvnw -B verify`

## Acesso rápido

```text
ADMIN       admin@aquadist.com     / admin123
CLIENTE     mercado@aquadist.com   / cliente123   (corporativo)
CLIENTE     maria@email.com        / cliente123   (residencial)
ENTREGADOR  joao@aquadist.com      / entregador123
ENTREGADOR  ana@aquadist.com       / entregador123
```

Abra `http://localhost:8080` após iniciar a aplicação.

> Esta documentação descreve a implementação real do repositório. Sempre que houver divergência
> entre um diagrama e o código, considere o código como fonte da verdade.
