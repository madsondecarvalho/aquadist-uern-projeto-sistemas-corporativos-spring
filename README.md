# AquaDist

Sistema de demonstração para gestão de clientes, estoque de garrafões, pedidos e entregas de água
mineral. Desenvolvido com **Spring Boot 4**, **SQLite** local, **Spring Security** com RBAC e telas
HTML usando **Tailwind CSS via CDN**.

O projeto cobre as regras de negócio RN01 a RN05 do enunciado da AquaDist, incluindo limite de
crédito corporativo, caução por vasilhame faltante, atribuição automática de entregador por zona e
capacidade, cancelamento antes do despacho e controle de validade dos garrafões.

**Repositório:** [madsondecarvalho/aquadist-uern-projeto-sistemas-corporativos-spring](https://github.com/madsondecarvalho/aquadist-uern-projeto-sistemas-corporativos-spring)

## Funcionalidades

- Autenticação HTTP Basic com três perfis: `ADMIN`, `CLIENTE` e `ENTREGADOR`.
- Cadastro de clientes residenciais e corporativos, com limite de crédito.
- Catálogo de garrafões (20 L, 10 L, 5 L) com preço e taxa de caução.
- Estoque central por produto e rastreamento individual de cada vasilhame por código e validade.
- Simulação e criação de pedidos com cálculo de caução e análise de crédito.
- Atribuição automática de entregador por zona e capacidade, com fallback e atribuição manual.
- Ciclo de entrega: `PENDENTE` → `ATRIBUIDO` → `EM_TRANSITO` → `ENTREGUE` (ou `CANCELADO`).
- Acompanhamento com atualização automática dos painéis de cliente e entregador a cada 30 segundos.
- SQLite em modo **WAL** com pool de conexões para leitores concorrentes.
- Conferência de vasilhames na entrega, com recusa de vencidos e recálculo de caução.
- Pedido assistido: o administrador cria pedidos para clientes com confirmação obrigatória do pagamento.
- Confirmação de pagamento por pedido, com registro no histórico e selos Pago/Pendente/Faturado.
- Registro de pagamentos e baixa de saldo devedor.
- Modais de pagamento e cancelamento com validação, histórico e confirmação explícita.
- Painéis web para cliente, entregador e administrador.
- Seed de demonstração, testes automatizados e pipeline de CI.

## Stack

| Camada | Tecnologia |
|---|---|
| Java | 25 |
| Framework | Spring Boot 4.2.0 |
| Web | Spring MVC |
| Persistência | Spring Data JPA / Hibernate 7.4 |
| Segurança | Spring Security 7 / BCrypt |
| Banco local | SQLite 3 |
| Banco de testes | H2 em memória |
| Front-end | HTML + JavaScript + Tailwind CDN |
| Build | Maven Wrapper |
| Testes | JUnit 6, AssertJ, Mockito, MockMvc |
| CI | GitHub Actions |

## Quick start

### Pré-requisitos

| Item | Detalhe |
|---|---|
| **JDK 25** | Obrigatório. Confira com `java -version` e `javac -version`; ambos devem mostrar 25. Se `javac` não existir, você tem apenas um JRE, não um JDK. |
| **Internet na primeira execução** | O Maven Wrapper baixa o Maven e as dependências do Spring. Depois elas ficam no cache local (`~/.m2`) e as próximas subidas não baixam tudo de novo. O Tailwind continua vindo da CDN a cada acesso às telas. |
| **Porta 8080 livre** | A aplicação sobe em `http://localhost:8080`. |

Não é necessário instalar Maven (o projeto inclui o Maven Wrapper), SQLite nem qualquer servidor
de banco: o banco é um arquivo local criado pela própria aplicação.

#### Instalar o JDK 25 no Linux

Confira o que já está instalado:

```bash
java -version
javac -version
```

Se os dois comandos não mostrarem **25**, instale um JDK (não apenas um JRE):

| Distribuição | Comando |
|---|---|
| Ubuntu / Debian | `sudo apt update && sudo apt install openjdk-25-jdk` |
| Fedora | `sudo dnf install java-25-openjdk-devel` |
| Outras distros | [Temurin 25](https://adoptium.net/temurin/releases/?version=25) ou SDKMAN (`sdk list java`) |

> Se `openjdk-25-jdk` não estiver disponível no repositório da sua versão da distro, use o Temurin
> ou o SDKMAN. O importante é que tanto `java` quanto `javac` apontem para a versão 25.

### Subir a aplicação

Na raiz do projeto (a pasta que contém o `pom.xml`):

#### Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

#### Linux / macOS

```bash
chmod +x mvnw          # só é necessário se aparecer "Permission denied"
./mvnw spring-boot:run
```

Na primeira execução, o Maven baixa as dependências; isso pode levar alguns minutos e é normal o
console parar em `Downloading from central...`. Aguarde a mensagem:

```text
Started AquaDistApplication
```

Na mesma subida, o seed registra:

```text
Seed concluido. Base em ./data/aquadist.db
```

Depois abra **http://localhost:8080** e entre com uma das contas de demonstração abaixo.

O que é criado automaticamente:

1. `data/aquadist.db` é criado, junto com os arquivos de WAL (`aquadist.db-wal` e `aquadist.db-shm`);
2. o Hibernate cria as tabelas;
3. o seed de demonstração insere usuários, clientes, entregadores, produtos e vasilhames.

Nas execuções seguintes, o banco e os dados são reaproveitados e o seed **não** roda de novo
(o log mostra `Base de dados ja possui dados. Seed ignorado.`). Para voltar ao estado inicial,
pare a aplicação e apague `data/aquadist.db*` — veja a seção **Banco de dados**, mais abaixo.

Para parar a aplicação: **Ctrl + C** no terminal onde ela está rodando.

Se aparecer `Port 8080 was already in use`, encerre o processo que está na porta antes de subir de novo:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

Alternativa no Linux/macOS: `lsof -ti:8080 | xargs kill`.

### Executável (JAR)

```powershell
.\mvnw.cmd -B package
java -jar target\aquadist-1.0.0.jar
```

```bash
./mvnw -B package
java -jar target/aquadist-1.0.0.jar
```

O `pom.xml` já configura o acesso nativo exigido pelo JDK 24+:

- `spring-boot:run` recebe `--enable-native-access=ALL-UNNAMED` via plugin Maven;
- o JAR executável leva `Enable-Native-Access: ALL-UNNAMED` no manifesto.

Isso evita o aviso do `sqlite-jdbc` ao carregar a biblioteca nativa (JNI). No Eclipse, use a
configuração compartilhada `AquaDist.launch`, descrita em [docs/09-eclipse.md](docs/09-eclipse.md).

### Contas de demonstração

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@aquadist.com` | `admin123` |
| Cliente corporativo | `mercado@aquadist.com` | `cliente123` |
| Cliente residencial | `maria@email.com` | `cliente123` |
| Entregador (Centro) | `joao@aquadist.com` | `entregador123` |
| Entregador (Sul) | `ana@aquadist.com` | `entregador123` |

A tela de login possui botões para preencher rapidamente cada conta.

## Rodar no Eclipse

O projeto local já está configurado para Java 25 (`.classpath` e `.settings`) e possui um guia
completo em **[docs/09-eclipse.md](docs/09-eclipse.md)**. Se esses arquivos não existirem no seu
clone, o Eclipse gera a configuração ao importar o Maven; apenas garanta que o JDK 25 esteja
selecionado. Resumo:

1. **Instale o JDK 25** e cadastre no Eclipse em
   **Window → Preferences → Java → Installed JREs → Add... → Standard VM**.
   Em **Execution Environments**, associe `JavaSE-25` ao JDK.
2. Importe como projeto Maven:
   **File → Import... → Maven → Existing Maven Projects**.
   Selecione a pasta que contém o `pom.xml` e clique em **Finish**.
3. Atualize as dependências:
   botão direito no projeto → **Maven → Update Project...** (`Alt+F5`).
4. Execute de uma destas formas:
   - **Java Application (mais direto):** abra
     `src/main/java/com/aquadist/AquaDistApplication.java`, clique com o botão direito no
     editor → **Run As → Java Application**.
   - **Configuração compartilhada (sem o aviso de native access):** botão direito no arquivo
     `AquaDist.launch` na raiz do projeto → **Run As → AquaDist**. Se essa opção não aparecer,
     use **Run → Run Configurations... → Java Application → AquaDist → Run**.
   - **Maven:** botão direito no projeto → **Run As → Maven build...** → em **Goals**, digite
     `spring-boot:run` → **Run**.
5. Confira no console a mensagem `Started AquaDistApplication in ... seconds` e abra
   **http://localhost:8080**.

> Para **Java Application**, use o **JDK 25** na aba **JRE** e mantenha o **Working directory**
> na raiz do projeto (aba **Arguments**), pois o SQLite usa o caminho relativo
> `./data/aquadist.db`. Se houver mais de um console aberto, selecione o do
> `AquaDistApplication` no ícone de console. O `AquaDist.launch` já configura o JDK 25, o
> diretório de trabalho e o argumento `--enable-native-access=ALL-UNNAMED`. Se você usar uma
> configuração **Java Application** própria e o aviso do SQLite aparecer, adicione esse mesmo
> argumento em **Arguments → VM arguments**.

Os testes também rodam no Eclipse: clique com o botão direito em `src/test/java` →
**Run As → JUnit Test**. Eles usam H2 em memória e não alteram o banco SQLite.

## Telas

| Página | Descrição |
|---|---|
| `/login.html` | Login com contas de demonstração e redirect por perfil |
| `/cliente.html` | Perfil, limite, vasilhames, catálogo, montagem/simulação de pedido e cancelamento |
| `/entregador.html` | Rota, capacidade, início de trânsito, conferência de vasilhames e conclusão |
| `/admin.html` | Pedidos, novo pedido assistido, confirmação de pagamento, clientes, entregadores, produtos, estoque e baixa de saldo |

As telas usam Tailwind CSS via `https://cdn.tailwindcss.com` e JavaScript puro. As credenciais
ficam em `sessionStorage` e são enviadas como `Authorization: Basic ...` nas chamadas `fetch`.

## Regras de negócio

| Regra | Implementação |
|---|---|
| **RN01** Limite de crédito corporativo | `Cliente.permiteCompra`, `PedidoService.verificarCredito` |
| **RN02** Caução por vasilhame faltante | `ItemPedido.recalcular`, `PedidoService` |
| **RN03** Atribuição por zona e capacidade | `AtribuicaoEntregadorService` + `PedidoCriadoEvent` |
| **RN04** Cancelamento antes do despacho | `StatusPedido.podeCancelar`, `PedidoService.cancelar` |
| **RN05** Validade do garrafão na troca | `Vasilhame`, `PedidoService.concluirEntrega` |

Detalhes, fluxos alternativos e rastreabilidade: [docs/02-regras-de-negocio.md](docs/02-regras-de-negocio.md).

## Testes

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

- **18 testes unitários** (JUnit 6, AssertJ, Mockito)
- **6 testes de integração** com Spring + H2 em memória
- **6 testes E2E** de HTTP com MockMvc + H2 em memória

A suíte não toca no arquivo SQLite. Veja [docs/07-testes.md](docs/07-testes.md).

Build completo:

```powershell
.\mvnw.cmd -B verify
```

## Banco de dados

### Desenvolvimento

SQLite em arquivo, com **WAL (Write-Ahead Logging)**:

```properties
spring.datasource.url=jdbc:sqlite:./data/aquadist.db?journal_mode=WAL&synchronous=NORMAL&busy_timeout=10000&foreign_keys=on
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.datasource.hikari.maximum-pool-size=5
```

### Concorrência com WAL

- **Leitores concorrentes**: consultas podem ler enquanto uma transação de escrita está em andamento.
- **Um escritor por vez**: o SQLite continua serializando escritas. O `busy_timeout=10000` faz a
  conexão aguardar até 10 segundos pelo lock em vez de falhar imediatamente com `SQLITE_BUSY`.
- **Pool de 5 conexões**: permite atender leituras e escritas curtas em paralelo.
- **`synchronous=NORMAL`**: combinação recomendada com WAL, equilibrando desempenho e durabilidade.
- **`foreign_keys=on`**: integridade referencial ativa em todas as conexões.
- **`@Version` em `Pedido`**: controle de concorrência otimista no domínio.
- O modo WAL gera os arquivos `aquadist.db-wal` e `aquadist.db-shm`, ambos ignorados pelo Git.

Ao iniciar, `SqliteWalVerifier` registra no log as pragmas efetivas e confirma o modo WAL.

Para resetar a base, pare a aplicação e apague o banco e os arquivos WAL:

```powershell
Remove-Item .\data\aquadist.db* -Force
```

```bash
rm -f data/aquadist.db data/aquadist.db-wal data/aquadist.db-shm
```

### Testes

H2 em memória, configurado em `src/test/resources/application-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:aquadist_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=create-drop
```

## Estrutura

```text
src/main/java/com/aquadist
├── config/        SecurityConfig, DataInitializer
├── controller/    API REST por perfil
├── dto/           Requests e responses (records)
├── event/         Evento e listener de atribuição
├── exception/     Exceções + ProblemDetail
├── model/         Entidades JPA e enums
├── repository/    Spring Data JPA
├── security/      UserDetailsService
└── service/       Regras de negócio e casos de uso

src/main/resources/static
├── login.html, cliente.html, entregador.html, admin.html
└── assets/        JS e CSS

src/test/java/com/aquadist
├── unit/          18 testes
├── integration/   6 testes (H2)
└── e2e/           6 testes (H2 + MockMvc)

docs/              Documentação completa
```

## Documentação

- [Índice](docs/README.md)
- [Visão geral](docs/01-visao-geral.md)
- [Regras de negócio](docs/02-regras-de-negocio.md)
- [Casos de uso](docs/03-casos-de-uso.md)
- [Arquitetura](docs/04-arquitetura.md)
- [Modelo de dados](docs/05-modelo-de-dados.md)
- [API REST](docs/06-api.md)
- [Testes](docs/07-testes.md)
- [Manual de uso](docs/08-manual-de-uso.md)
- [Importar e rodar no Eclipse](docs/09-eclipse.md)

## Integração contínua

O workflow em `.github/workflows/ci.yml` executa `./mvnw -B verify` em pushes e pull requests,
com Java 25, cache Maven e publicação dos relatórios de teste.

## Notas e limitações

- Projeto **local e educacional**: HTTP Basic + credenciais em `sessionStorage` não substituem
  uma autenticação de produção (JWT/OAuth2, HTTPS, refresh tokens).
- SQLite em WAL permite leitores concorrentes, mas mantém **um único escritor por vez**; para alta
  concorrência de escrita, a evolução recomendada é PostgreSQL.
- O Tailwind via CDN requer internet para carregar o estilo.
- Não há front-end SPA nem testes de navegador; a API e as páginas são testadas via MockMvc.
- Caução não é estornada automaticamente após devolução futura.
- O fechamento mensal do limite de crédito é conceitual; o saldo é baixado por pagamentos.

## Licença

Projeto de demonstração educacional. Sinta-se livre para adaptar para fins de estudo.
