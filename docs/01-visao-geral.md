# 01 — Visão geral

## 1. Contexto

A **AquaDist** é uma empresa fictícia que distribui água mineral para residências e estabelecimentos
comerciais. A operação atual depende de planilhas, anotações em papel e comunicação informal entre
vendedores, motoristas e financeiro.

Esse modelo gera problemas recorrentes:

- atraso na confirmação e no despacho de pedidos;
- rotas mal distribuídas e entregadores sobrecarregados;
- falhas de cobrança, especialmente de caução e valores faturados;
- falta de controle sobre garrafões vazios e vencidos;
- dificuldade para acompanhar o status da entrega;
- perda de histórico e de rastreabilidade.

## 2. Objetivo da solução

Substituir o processo manual por uma solução corporativa baseada no ecossistema Spring, com:

1. cadastro de clientes residenciais e corporativos;
2. catálogo de garrafões e controle de estoque por unidade;
3. processamento de pedidos com cálculo de caução e validação de crédito;
4. roteirização e atribuição automática de entregadores;
5. acompanhamento do ciclo de vida da entrega;
6. controle de validade dos vasilhames na troca;
7. telas web simples para cliente, entregador e administrador;
8. testes automatizados e pipeline de integração contínua.

## 3. Escopo entregue

### Incluído

- Autenticação HTTP Basic com RBAC (`ADMIN`, `CLIENTE`, `ENTREGADOR`).
- Cadastro e consulta de clientes, entregadores e produtos.
- Estoque central e estoque em trânsito por entregador.
- Rastreamento individual de garrafões por código e validade.
- Criação, simulação, consulta, cancelamento e conclusão de pedidos.
- Atribuição automática por zona e capacidade; atribuição manual pelo administrador.
- Registro de pagamentos e controle de saldo devedor.
- Seed de demonstração com produtos, clientes, entregadores e vasilhames.
- Telas HTML com Tailwind CSS via CDN.
- Documentação, testes e workflow de CI.

### Fora do escopo (possíveis evoluções)

- aplicativo móvel nativo;
- autenticação JWT/OAuth2 e refresh tokens;
- integração com gateway de pagamento, emissão de boleto ou nota fiscal;
- geolocalização real, mapa e otimização de rotas com algoritmo de roteamento;
- notificações por e-mail, SMS ou push;
- auditoria completa de todas as alterações;
- fechamento mensal automático do limite de crédito;
- devolução/estorno de caução após a entrega;
- testes de interface em navegador (Selenium/Playwright).

## 4. Personas e permissões

| Persona | Perfil | Principais ações |
|---|---|---|
| Cliente residencial | `ROLE_CLIENTE` | Consultar perfil, simular e criar pedidos, acompanhar e cancelar pedidos antes do despacho |
| Cliente corporativo | `ROLE_CLIENTE` | Mesmas ações, com limite de crédito mensal e possibilidade de pagamento faturado |
| Entregador | `ROLE_ENTREGADOR` | Consultar rota, iniciar trânsito, conferir vasilhames, concluir entrega e consultar estoque do veículo |
| Administrador | `ROLE_ADMIN` | Visualizar todos os pedidos, atribuir entregadores, cadastrar clientes/entregadores/produtos, repor estoque e registrar pagamentos |

## 5. Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.2.0 |
| Web | Spring MVC / Tomcat |
| Persistência | Spring Data JPA / Hibernate ORM 7.4 |
| Segurança | Spring Security 7 / BCrypt / HTTP Basic |
| Validação | Jakarta Bean Validation / Hibernate Validator 9 |
| Banco local | SQLite 3 (`sqlite-jdbc` + `SQLiteDialect`, modo WAL) |
| Banco de testes | H2 em memória |
| Telas | HTML, JavaScript puro e Tailwind CSS via CDN |
| Build | Maven Wrapper |
| Testes | JUnit 6, AssertJ, Mockito, Spring Test, MockMvc |
| CI | GitHub Actions |

## 6. Diferenciais da implementação

- **Dinheiro com `BigDecimal`**, não `double`, evitando erros de arredondamento.
- **DTOs** separados das entidades JPA; nenhuma entidade é serializada diretamente.
- **Modelo de estado explícito** para o pedido, com regras de transição centralizadas.
- **Rastreamento individual de vasilhames**, permitindo aplicar a RN05 de forma concreta.
- **SQLite em modo WAL** com pool de conexões: leitores concorrentes durante escritas, mantendo
  um único escritor por vez.
- **Atribuição por evento** (`PedidoCriadoEvent`), desacoplando a criação da roteirização.
- **Testes com H2 em memória**, sem tocar no arquivo SQLite de desenvolvimento.
- **Telas funcionais** para os três perfis, com autenticação Basic e chamadas `fetch`.
- **Documentação rastreável**: cada regra de negócio aponta para classes, endpoints e testes.

## 7. Como navegar na documentação

1. Leia [02 — Regras de negócio](02-regras-de-negocio.md) para entender o comportamento esperado.
2. Leia [03 — Casos de uso](03-casos-de-uso.md) para acompanhar os fluxos.
3. Consulte [04 — Arquitetura](04-arquitetura.md) e [05 — Modelo de dados](05-modelo-de-dados.md)
   antes de alterar o domínio.
4. Use [06 — API](06-api.md) para integrar ou testar via `curl`.
5. Siga [08 — Manual de uso](08-manual-de-uso.md) para demonstrar o sistema.
