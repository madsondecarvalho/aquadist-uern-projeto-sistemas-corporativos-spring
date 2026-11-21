# AGENTS.md — Guia para agentes de código

Este arquivo descreve o contexto, as convenções e os comandos do projeto **AquaDist** para
qualquer agente de IA ou desenvolvedor que for alterar o repositório.

## 1. Visão geral

A AquaDist é uma empresa fictícia de distribuição de água mineral. O projeto é uma aplicação
Spring Boot para gestão de clientes, estoque de garrafões, pedidos, roteirização e entregas.

- **Repositório:** `madsondecarvalho/aquadist-uern-projeto-sistemas-corporativos-spring`
- **Branch principal:** `main`
- **Objetivo:** projeto acadêmico/demonstração, executado localmente.
- **Idioma:** interface e documentação em português; nomes de domínio em português
  (`Cliente`, `Pedido`, `Vasilhame`, `Entregador`); padrões técnicos em inglês quando fizer sentido.

## 2. Stack

| Item | Versão / escolha |
|---|---|
| Java | 25 |
| Spring Boot | 4.2.0 |
| Web | Spring MVC |
| Persistência | Spring Data JPA / Hibernate 7.4 |
| Segurança | Spring Security 7, HTTP Basic, BCrypt, RBAC |
| Validação | Jakarta Bean Validation / Hibernate Validator 9 |
| Banco local | SQLite 3 em modo WAL |
| Banco de testes | H2 em memória |
| Front-end | HTML + JavaScript puro + Tailwind CSS via CDN |
| Build | Maven Wrapper (`mvnw`, `mvnw.cmd`) |
| Testes | JUnit 6, AssertJ, Mockito, MockMvc |
| CI | GitHub Actions (`./mvnw -B verify`) |

## 3. Como executar

### Terminal

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Linux/macOS
./mvnw spring-boot:run
```

Acesse `http://localhost:8080`.

### Eclipse

Consulte [docs/09-eclipse.md](docs/09-eclipse.md). Resumo:

1. configurar JDK 25;
2. importar via **Maven → Existing Maven Projects**;
3. executar **Maven build...** com goal `spring-boot:run`;
4. se usar **Java Application**, configurar o working directory para a raiz do projeto.

### Contas demo

```text
ADMIN       admin@aquadist.com     / admin123
CLIENTE     mercado@aquadist.com   / cliente123   (corporativo)
CLIENTE     maria@email.com        / cliente123   (residencial)
ENTREGADOR  joao@aquadist.com      / entregador123
ENTREGADOR  ana@aquadist.com       / entregador123
```

## 4. Banco de dados

### Desenvolvimento

SQLite em `data/aquadist.db`, configurado em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlite:./data/aquadist.db?journal_mode=WAL&synchronous=NORMAL&busy_timeout=10000&foreign_keys=on
spring.datasource.hikari.maximum-pool-size=5
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

Características:

- **WAL** permite leitores concorrentes durante uma escrita.
- O SQLite continua permitindo **um único escritor por vez**.
- `busy_timeout=10000` aguarda até 10 segundos por um lock.
- O modo WAL cria `aquadist.db-wal` e `aquadist.db-shm`; eles são normais.
- `SqliteWalVerifier` loga as pragmas efetivas na inicialização.
- O seed é idempotente: `DataInitializer` só cria dados quando não há usuários.

### Reset

Com a aplicação parada:

```powershell
Remove-Item .\data\aquadist.db* -Force
.\mvnw.cmd spring-boot:run
```

```bash
rm -f data/aquadist.db data/aquadist.db-wal data/aquadist.db-shm
./mvnw spring-boot:run
```

### Testes

H2 em memória, configurado em `src/test/resources/application-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:aquadist_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=create-drop
```

Nenhum teste abre o arquivo SQLite de desenvolvimento.

## 5. Testes e validação

```powershell
.\mvnw.cmd test
.\mvnw.cmd -B verify
```

```bash
./mvnw test
./mvnw -B verify
```

A suíte atual tem **30 testes**:

- **18 unitários** em `src/test/java/com/aquadist/unit` — regras de cálculo e atribuição;
- **6 de integração** em `src/test/java/com/aquadist/integration` — Spring + H2;
- **6 E2E** em `src/test/java/com/aquadist/e2e` — MockMvc + H2, segurança/RBAC e fluxo completo.

Antes de commitar mudanças de comportamento:

1. execute `.\mvnw.cmd -B test`;
2. confirme que os 30 testes continuam passando;
3. verifique a sintaxe dos scripts das páginas HTML (quando alteradas);
4. atualize a documentação em `docs/` se a regra ou o comportamento mudar.

Os testes E2E são de backend/HTTP com MockMvc. Não há Selenium/Playwright na suíte.

## 6. Estrutura

```text
src/main/java/com/aquadist
├── config/        SecurityConfig, DataInitializer, SqliteWalVerifier
├── controller/    Auth, Cliente, Produto, Pedido, Entregador, Admin
├── dto/           Requests/responses em records
├── event/         PedidoCriadoEvent, PedidoCriadoListener
├── exception/     Exceções de domínio + ApiExceptionHandler (ProblemDetail)
├── model/         Entidades JPA e enums
├── repository/    Spring Data JPA
├── security/      UsuarioDetailsService
└── service/       Casos de uso e regras de negócio

src/main/resources/static
├── login.html
├── cliente.html
├── entregador.html
├── admin.html
└── assets/js/api.js, assets/css/app.css

docs/              Documentação funcional e técnica
```

## 7. Regras de negócio

Não altere estas regras sem atualizar testes e documentação.

| Regra | Resumo | Implementação principal |
|---|---|---|
| **RN01** | Limite de crédito mensal para cliente corporativo; considera saldo devedor e pedidos em aberto | `Cliente.permiteCompra`, `PedidoService.verificarCredito` |
| **RN02** | Caução por garrafão sem vasilhame devolvido | `ItemPedido.recalcular`, `PedidoService` |
| **RN03** | Priorizar entregador da mesma zona com capacidade; fallback para outra zona | `AtribuicaoEntregadorService`, `PedidoCriadoEvent` |
| **RN04** | Só cancelar antes de `EM_TRANSITO` | `StatusPedido.podeCancelar`, `PedidoService.cancelar` |
| **RN05** | Vasilhame vencido não entra na troca simples; pode ser recusado/descartado e gerar caução | `Vasilhame.estaVencido`, `PedidoService.concluirEntrega` |

Regras complementares importantes:

- cliente precisa de endereço completo com zona para pedir;
- estoque central deve atender o pedido na criação e no despacho;
- `FATURADO` é exclusivo de cliente corporativo;
- não repetir produto no mesmo pedido;
- não devolver mais vasilhames do que a quantidade do item;
- pagamento não pode exceder o saldo devedor;
- O cliente só acessa os próprios pedidos; o entregador só acessa pedidos atribuídos a ele.

## 8. Máquina de estados

```text
PENDENTE -> ATRIBUIDO -> EM_TRANSITO -> ENTREGUE
    |            |
    +------------+------> CANCELADO
```

- Criação: `PENDENTE`.
- Atribuição automática/manual: `ATRIBUIDO`.
- Despacho carrega vasilhames centrais e muda para `EM_TRANSITO`.
- Conclusão atualiza estoque, caucão, pagamento e muda para `ENTREGUE`.
- Cancelamento só é permitido em `PENDENTE` ou `ATRIBUIDO`.

## 9. Convenções de código

- **Dinheiro:** sempre `BigDecimal`, escala 2, `RoundingMode.HALF_UP`. Nunca `double`.
- **DTOs:** records em `dto`, com validação Jakarta; não serializar entidades JPA.
- **Transações:** regras de negócio nos services; controllers apenas orquestram.
- **Mapeamento:** feito dentro da transação, pois `open-in-view=false`.
- **Erros:** exceções de domínio + `ApiExceptionHandler` com `ProblemDetail`.
- **Segurança:** `@PreAuthorize` nos controllers; verificação de propriedade nos services.
- **Sem Lombok:** getters/setters explícitos.
- **Nomes de domínio em português**, mantendo consistência com o código existente.
- **Logs:** usar SLF4J; não logar senhas ou dados sensíveis.
- **SQLite:** manter WAL; não reduzir o pool para 1 sem justificar.
- **Comentários:** apenas quando agregarem contexto; evitar comentários óbvios.

## 10. Convenções de interface

- Tailwind CSS via CDN; sem build de front-end.
- JavaScript puro centralizado em `assets/js/api.js`.
- Toda chamada de API usa `Aqua.API` (Basic Auth + tratamento de erro).
- **Não usar `alert`, `prompt` ou `confirm` nativos.** Use modais, toasts e mensagens inline.
- Formulários devem:
  - começar vazios quando o fluxo exigir;
  - validar campos no cliente e no servidor;
  - limpar os campos após sucesso;
  - desabilitar o botão durante o envio (`comBotao`).
- Modais devem ter resumo, validação, confirmação explícita e botão de fechar.
- Manter o layout responsivo e os estados de carregamento/erro.
- Atualização dos painéis de cliente e entregador a cada 30 segundos via polling.

## 11. Segurança

- HTTP Basic, stateless, RBAC (`ROLE_ADMIN`, `ROLE_CLIENTE`, `ROLE_ENTREGADOR`).
- CSRF desabilitado por ser API stateless de demonstração.
- Credenciais no `sessionStorage` apenas para demonstração local.
- Em produção, usar HTTPS, JWT/OAuth2, expiração de token e proteção CSRF adequada.

## 12. Documentação

Sempre consulte/atualize:

- [README.md](README.md)
- [docs/README.md](docs/README.md)
- [docs/02-regras-de-negocio.md](docs/02-regras-de-negocio.md)
- [docs/04-arquitetura.md](docs/04-arquitetura.md)
- [docs/06-api.md](docs/06-api.md)
- [docs/07-testes.md](docs/07-testes.md)
- [docs/08-manual-de-uso.md](docs/08-manual-de-uso.md)
- [docs/09-eclipse.md](docs/09-eclipse.md)

## 13. Git

- Branch principal: `main`.
- Não versionar:
  - `data/aquadist.db`, `data/*.db-wal`, `data/*.db-shm`;
  - `target/`;
  - arquivos de IDE (`.project`, `.classpath`, `.settings`);
  - credenciais, segredos ou bancos locais.
- Antes de commitar: `git status` e revisar o diff.
- Mensagem de commit descritiva, em português ou inglês.
- Fluxo:

```powershell
git add .
git commit -m "Descricao objetiva da mudanca"
git push origin main
```

## 14. Definição de pronto

Uma alteração é considerada concluída quando:

- [ ] compila com Java 25;
- [ ] os 30 testes passam;
- [ ] a documentação afetada foi atualizada;
- [ ] a interface não usa `alert`/`prompt`/`confirm` nativos;
- [ ] o banco SQLite continua em WAL;
- [ ] não há segredos ou arquivos gerados no commit;
- [ ] o app inicia em `http://localhost:8080`;
- [ ] as contas demo continuam funcionando.
