# 09 — Importar e rodar no Eclipse

Este guia cobre a importação do projeto no Eclipse, configuração do JDK 25, execução da aplicação,
execução dos testes e solução dos problemas mais comuns.

## 1. Requisitos

| Item | Versão / observação |
|---|---|
| Eclipse | IDE for Enterprise Java and Web Developers **2025-09 ou mais recente** (recomendado 2026-09) |
| JDK | **JDK 25** (não apenas JRE) |
| Maven | Não é necessário instalar; o projeto tem Maven Wrapper |
| Internet | Necessária para baixar dependências Maven e carregar o Tailwind via CDN |
| Porta | 8080 livre |

Verifique o Java no terminal:

```powershell
java -version
javac -version
```

Ambos devem apontar para **25**. Se `javac` não existir, você tem apenas um JRE; instale um JDK 25
(Temurin, Oracle, Microsoft etc.).

## 2. Configurar o JDK 25 no Eclipse

1. Abra **Window → Preferences** (Janela → Preferências).
2. Acesse **Java → Installed JREs**.
3. Clique em **Add...**.
4. Selecione **Standard VM** e clique em **Next**.
5. Em **JRE home**, selecione a pasta raiz do JDK 25.
   - Windows (exemplo): `C:\Program Files\Eclipse Adoptium\jdk-25...`
   - Linux (exemplo): `/usr/lib/jvm/temurin-25-jdk`
6. Clique em **Finish**.
7. Marque o JDK 25 na lista (pode ser o padrão).
8. Ainda em **Java → Installed JREs**, abra **Execution Environments**.
9. Selecione **JavaSE-25** e marque o JDK 25 instalado.
10. Clique em **Apply and Close**.

> O projeto está configurado para `JavaSE-25` no `.classpath` e no
> `.settings/org.eclipse.jdt.core.prefs`. Se esses arquivos não existirem no seu clone (eles são
> gerados e normalmente ignorados pelo Git), o Eclipse criará a configuração ao importar o Maven;
> basta garantir que o JDK 25 esteja selecionado.

## 3. Importar o projeto

### Método recomendado: Existing Maven Projects

1. No Eclipse, abra **File → Import...** (Arquivo → Importar...).
2. Expanda **Maven** e escolha **Existing Maven Projects**.
3. Clique em **Next**.
4. Em **Root Directory**, selecione a pasta que contém o `pom.xml`
   (por exemplo, `C:\Users\...\demo`).
5. O Eclipse deve listar o projeto `aquadist` ou `aquadist-spring` com o `pom.xml` marcado.
6. Clique em **Finish**.
7. Aguarde o download das dependências e a compilação do workspace.

> **Não use** `General → Existing Projects into Workspace` como primeira opção. Ele importa a pasta
> sem processar o Maven corretamente. Use **Existing Maven Projects**.

### Depois da importação

1. Clique com o botão direito no projeto.
2. Escolha **Maven → Update Project...** (ou pressione `Alt+F5`).
3. Marque o projeto e clique em **OK**.
4. Se houver erros de configuração, marque também **Force Update of Snapshots/Releases**.

O nome do projeto pode aparecer como **`aquadist-spring`** porque esse é o nome definido no
`.project`. Isso é normal.

## 4. Conferir o Java do projeto

1. Botão direito no projeto → **Properties** (Propriedades).
2. **Java Build Path → Libraries**.
3. Confira se existe **JRE System Library [JavaSE-25]**.
4. Se não existir:
   - clique em **Add Library...**;
   - escolha **JRE System Library**;
   - selecione **Execution Environment → JavaSE-25**;
   - clique em **Finish**.
5. Vá em **Java Compiler**.
6. Confira o **Compiler compliance level: 25**.
7. Clique em **Apply and Close**.
8. Faça **Project → Clean...** e selecione o projeto.

Se aparecer o erro:

```text
Java compiler level does not match the version of the installed Java project facet
```

verifique também **Properties → Project Facets → Java** e deixe em **25**. Em projetos Maven, o
`Maven → Update Project` normalmente corrige isso.

## 5. Rodar a aplicação

### Opção A — Maven (recomendada)

Essa opção usa a mesma configuração do terminal e resolve o classpath e o diretório de trabalho
corretamente.

1. Botão direito no projeto → **Run As → Maven build...**.
2. Em **Goals**, digite:
   ```text
   spring-boot:run
   ```
3. Clique em **Run**.
4. Acompanhe o console até aparecer:
   ```text
   Started AquaDistApplication in ... seconds
   ```
5. Abra `http://localhost:8080`.
6. Para parar, clique no botão vermelho **Terminate** no console.

> O `pom.xml` já passa `--enable-native-access=ALL-UNNAMED` ao processo forkado pelo
> `spring-boot:run`. Não é necessário adicionar argumentos de VM manualmente nessa opção.

Se preferir criar uma configuração reutilizável:

1. **Run → Run Configurations...**
2. **Maven Build → New**.
3. Nome: `AquaDist - spring-boot:run`.
4. **Base directory**: selecione o projeto importado.
5. **Goals**: `spring-boot:run`.
6. **JRE**: selecione o JDK 25.
7. **Run**.

### Opção B — Java Application

O projeto inclui a configuração compartilhada
[`AquaDist.launch`](../AquaDist.launch) na raiz. Ela já define:

- o argumento de VM `--enable-native-access=ALL-UNNAMED`;
- o diretório de trabalho na raiz do projeto (necessário para `./data/aquadist.db`);
- o JRE `JavaSE-25`;
- a classe principal `com.aquadist.AquaDistApplication`.

Formas de executar:

1. Abra:
   ```text
   src/main/java/com/aquadist/AquaDistApplication.java
   ```
2. **Jeito mais direto:** botão direito no arquivo → **Run As → Java Application**. Funciona, mas
   o Eclipse pode criar uma configuração local sem o argumento de VM; nesse caso o aviso de
   native access aparece (veja a seção de problemas comuns).
3. **Para rodar sem o aviso:** botão direito em `AquaDist.launch` → **Run As → AquaDist**; ou
   **Run → Run Configurations... → Java Application → AquaDist → Run**.

Se o Eclipse criar uma configuração local sem os argumentos (por exemplo, por já existir uma
configuração antiga), ajuste em **Run → Run Configurations... → Java Application → AquaDist**:

- **Arguments → VM arguments**: `--enable-native-access=ALL-UNNAMED`
- **Arguments → Working directory**: **Other → Workspace →** projeto importado.

> Se o projeto foi importado com outro nome (por exemplo, `aquadist` em vez de
> `aquadist-spring`), ajuste também **Project** na aba principal e o **Working directory**.

> **Por que o `.launch` é necessário?** O `pom.xml` configura o Maven, mas não altera a
> configuração **Java Application** do Eclipse. Sem o argumento, o JDK 24+ mostra um aviso ao
> carregar a biblioteca nativa do SQLite; versões futuras do Java poderão bloquear a chamada.

Se o console mostrar erro de caminho do SQLite, o diretório de trabalho provavelmente está errado.
A opção A (Maven) evita esse problema.

### Opção C — Spring Boot Dashboard

Se você tiver o **Spring Tools 4** instalado no Eclipse:

1. **Window → Show View → Other... → Spring → Spring Boot Dashboard**.
2. Localize o aplicativo `aquadist`.
3. Clique no botão de **Run** (▶).

> O dashboard pode criar uma configuração própria. Se aparecer o aviso de native access, use
> `AquaDist.launch` ou adicione `--enable-native-access=ALL-UNNAMED` nos argumentos de VM.

### Opção D — Terminal dentro do Eclipse

1. **Window → Show View → Terminal**.
2. Abra um terminal local na pasta do projeto.
3. Execute:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   ```bash
   ./mvnw spring-boot:run
   ```

## 6. Acessar o sistema

Abra no navegador:

```text
http://localhost:8080
```

Contas de demonstração:

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@aquadist.com` | `admin123` |
| Cliente corporativo | `mercado@aquadist.com` | `cliente123` |
| Cliente residencial | `maria@email.com` | `cliente123` |
| Entregador (Sul) | `ana@aquadist.com` | `entregador123` |
| Entregador (Centro) | `joao@aquadist.com` | `entregador123` |

O banco `data/aquadist.db` é criado automaticamente na primeira execução e já vem com o seed de
demonstração. O SQLite usa modo **WAL**, então também podem aparecer `aquadist.db-wal` e
`aquadist.db-shm`; eles são normais e não devem ser apagados com a aplicação rodando.

## 7. Rodar os testes no Eclipse

### Pelo JUnit

1. No Package Explorer, expanda `src/test/java`.
2. Clique com o botão direito em `com.aquadist` ou em uma classe de teste.
3. Escolha **Run As → JUnit Test**.
4. Os resultados aparecem na view **JUnit**.

Os testes usam `@ActiveProfiles("test")` e um banco **H2 em memória**. O arquivo SQLite de
desenvolvimento não é alterado.

### Pelo Maven

1. Botão direito no projeto → **Run As → Maven build...**.
2. Goals:
   ```text
   test
   ```
3. Run.

Ou, no terminal:

```powershell
.\mvnw.cmd test
```

### Build completo

Para compilar, testar e empacotar:

```text
clean verify
```

O JAR será gerado em `target/aquadist-1.0.0.jar`.

## 8. Problemas comuns

### `UnsupportedClassVersionError` ou `class file version 70`

O projeto foi compilado com um Java mais novo do que o Java que está executando. Faça:

1. Confirme que o JDK 25 é o **Installed JRE** e o **Execution Environment JavaSE-25**.
2. **Project → Clean...**.
3. **Maven → Update Project...**.
4. Apague a pasta `target` se o problema persistir.
5. Rode novamente.

O projeto agora está configurado para Java 25 (`.classpath` e `.settings`).

### `Java compiler level does not match...`

Ajuste **Java Compiler** para 25 e, se existir, **Project Facets → Java** para 25. Depois faça
**Maven → Update Project** e **Project → Clean**.

### `No compiler is provided in this environment`

O Eclipse/Maven está usando um **JRE**, não um **JDK**. Instale o JDK 25 e selecione-o em
**Installed JREs** e na JRE da execução Maven.

### `Port 8080 was already in use`

Já existe outra instância rodando. Pare o processo ou use outra porta:

- Altere `server.port=8081` em `src/main/resources/application.properties`; ou
- Crie a configuração Maven com os goals:
  ```text
  spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
  ```

### Aviso `A restricted method in java.lang.System has been called`

No JDK 24+ esse aviso aparece quando o driver `sqlite-jdbc` carrega a biblioteca nativa via JNI:

```text
WARNING: java.lang.System::load has been called by org.sqlite.SQLiteJDBCLoader...
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
```

O aviso é benigno, mas indica que versões futuras bloquearão a chamada sem acesso nativo. O
projeto já o evita em cada modo de execução:

- **Maven (`spring-boot:run`)**: o `pom.xml` passa o argumento ao processo forkado.
- **JAR executável**: o manifesto contém `Enable-Native-Access: ALL-UNNAMED`; gere o JAR de novo
  com `.\mvnw.cmd -B package` depois de atualizar o projeto.
- **Eclipse (Java Application)**: use `AquaDist.launch`. Se preferir uma configuração própria,
  adicione `--enable-native-access=ALL-UNNAMED` em **Arguments → VM arguments**.
- **Testes**: usam H2 em memória, sem SQLite, portanto não precisam do argumento.

Alternativa global no Eclipse: **Window → Preferences → Java → Installed JREs →** selecione o
JDK 25 → **Edit... → Default VM arguments** e informe `--enable-native-access=ALL-UNNAMED`.
Isso vale para todos os aplicativos Java executados com esse JRE.

### Erro de caminho do SQLite / banco não encontrado

O caminho `./data/aquadist.db` é relativo ao diretório de trabalho. Ao rodar como **Java
Application**, ajuste **Run Configurations → Arguments → Working directory** para a raiz do
projeto. A opção Maven `spring-boot:run` usa a raiz do projeto por padrão.

Também confirme que a pasta `data` existe. Ela contém um `.gitkeep` justamente para ser versionada.

### `SQLITE_BUSY` / banco travado

O projeto usa **WAL** e `busy_timeout=10000`, permitindo leitores concorrentes e espera de até 10 s
por um lock de escrita. Se o erro persistir:

1. Encerre outras instâncias da aplicação.
2. Evite transações longas ou debug parado em um breakpoint dentro de uma transação.
3. Pare tudo e apague `data/aquadist.db*` para recriar o banco.

O SQLite mantém apenas um escritor por vez; para alta concorrência de escrita, use PostgreSQL.

### Dependências não resolvem

1. **Maven → Update Project...** com **Force Update**.
2. Verifique a internet, proxy e o `settings.xml` do Maven.
3. Se necessário, rode no terminal:
   ```powershell
   .\mvnw.cmd -U clean verify
   ```

### Tailwind sem estilo

As telas usam o Tailwind via CDN (`https://cdn.tailwindcss.com`). Sem internet, o HTML e o
JavaScript continuam funcionando, mas o layout fica sem estilo.

### O projeto não aparece na lista de importação

Confira se você selecionou a pasta que contém o `pom.xml`, não uma pasta acima. Se o projeto já
estiver no workspace, remova-o (sem apagar do disco) e importe novamente.

## 9. Checklist rápido

- [ ] JDK 25 instalado e configurado no Eclipse.
- [ ] Projeto importado via **Maven → Existing Maven Projects**.
- [ ] **Maven → Update Project** executado.
- [ ] **JRE System Library [JavaSE-25]** e **Compiler compliance 25**.
- [ ] Aplicação rodando via **Maven build** com goal `spring-boot:run`, ou via `AquaDist.launch`.
- [ ] Console sem o aviso de `native access` do SQLite (JDK 24+).
- [ ] `http://localhost:8080` abrindo a tela de login.
- [ ] Testes verdes em **Run As → JUnit Test**.
- [ ] Banco `data/aquadist.db` criado na primeira execução.
- [ ] Se existirem, `aquadist.db-wal` e `aquadist.db-shm` são arquivos normais do modo WAL.
