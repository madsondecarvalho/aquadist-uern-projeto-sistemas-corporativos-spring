# 08 — Manual de uso

> Para importar o projeto e executar no **Eclipse**, consulte o guia dedicado
> [09 — Eclipse](09-eclipse.md).

## 1. Pré-requisitos

- **JDK 25** — `java -version` e `javac -version` devem mostrar 25. Se `javac` não existir,
  você tem apenas um JRE.
- **Internet na primeira execução** — o Maven Wrapper baixa o Maven e as dependências do Spring.
  Depois elas ficam em cache; o Tailwind continua vindo da CDN e exige internet para carregar o
  estilo das telas.
- **Porta 8080 livre** — a aplicação sobe em `http://localhost:8080`.
- **Maven e SQLite não precisam ser instalados** — o projeto inclui o Maven Wrapper e o banco é um
  arquivo local criado pela aplicação.

## 2. Como iniciar

### Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
chmod +x mvnw          # só é necessário se aparecer "Permission denied"
./mvnw spring-boot:run
```

> No Linux, confira o JDK 25 com `java -version` e `javac -version`. Se precisar instalar:
> Ubuntu/Debian `sudo apt install openjdk-25-jdk`; Fedora `sudo dnf install java-25-openjdk-devel`;
> outras distros, use o Temurin 25. Veja o passo a passo no
> [README](../README.md#instalar-o-jdk-25-no-linux).

Na primeira execução, o download das dependências pode levar alguns minutos; é normal o console
parar em `Downloading from central...`. Aguarde a mensagem:

```text
Started AquaDistApplication
```

Na mesma subida, o seed registra:

```text
Seed concluido. Base em ./data/aquadist.db
```

Nas execuções seguintes, o seed é ignorado e o log mostra:

```text
Base de dados ja possui dados. Seed ignorado.
```

Abra:

```text
http://localhost:8080
```

Na primeira execução, o arquivo `data/aquadist.db` é criado e o seed de demonstração é inserido.
O SQLite é aberto em **modo WAL**; por isso também podem existir os arquivos `aquadist.db-wal` e
`aquadist.db-shm`.

## 3. Credenciais

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@aquadist.com` | `admin123` |
| Cliente corporativo | `mercado@aquadist.com` | `cliente123` |
| Cliente residencial | `maria@email.com` | `cliente123` |
| Entregador (Centro) | `joao@aquadist.com` | `entregador123` |
| Entregador (Sul) | `ana@aquadist.com` | `entregador123` |

A tela de login tem botões que preenchem as credenciais automaticamente.

## 4. Tela de login

1. Abra `http://localhost:8080`.
2. Informe e-mail e senha ou clique em uma conta demo.
3. Clique em **Entrar**.
4. O sistema chama `GET /api/auth/me` e redireciona:
   - `ADMIN` → `/admin.html`
   - `CLIENTE` → `/cliente.html`
   - `ENTREGADOR` → `/entregador.html`
5. **Sair** limpa as credenciais do `sessionStorage`.

![Fluxo de login descrito em texto] O login usa HTTP Basic; o navegador não exibe popup porque o
JavaScript envia o cabeçalho `Authorization` manualmente.

## 5. Área do cliente

### 5.1 Visão geral

Ao entrar, o cliente vê:

- nome, tipo, documento, e-mail e endereço;
- zona de entrega;
- limite mensal, saldo devedor e limite disponível (corporativo);
- aviso de pagamento na entrega (residencial);
- garrafões em posse, separados em válidos e vencidos;
- catálogo de produtos com preço, caução e estoque.

### 5.2 Montar um pedido

1. Em **Montar novo pedido**, selecione o produto.
2. Informe a quantidade de garrafões.
3. Informe quantos vasilhames serão devolvidos na troca.
4. Clique em **Adicionar**.
5. Repita para outros produtos, se desejar.
6. Escolha a forma de pagamento.
   - `Faturado` aparece apenas para clientes corporativos.
7. Clique em **Calcular valores**.
8. O sistema mostra:
   - total de produtos;
   - caução;
   - total;
   - limite suficiente;
   - avisos de estoque.
9. Clique em **Confirmar pedido**.
10. O pedido é criado como `PENDENTE` e pode ser atribuído automaticamente a um entregador.
11. O modal de detalhes mostra o status, o entregador, os itens e a caução.

### 5.3 Acompanhar e cancelar

- A seção **Meus pedidos** lista todos os pedidos e é atualizada automaticamente a cada 30 s.
- **Detalhes** abre o histórico, endereço, valores, vasilhames aceitos e recusados.
- **Cancelar** aparece somente para `PENDENTE` e `ATRIBUIDO` e abre um modal com resumo do pedido,
  campo de motivo e confirmação explícita.
- Após o entregador iniciar o trânsito, o botão desaparece e a API retorna `409` (RN04).

## 6. Área do entregador

### 6.1 Rota

A tela mostra:

- perfil, zona, veículo e capacidade;
- barra de ocupação (carga atual / capacidade);
- estoque em trânsito no veículo;
- pedidos atribuídos, com cliente, telefone, endereço, itens e status.

### 6.2 Iniciar trânsito

1. Em um pedido `ATRIBUIDO`, clique em **Iniciar trânsito**.
2. O sistema verifica o estoque central.
3. Os vasilhames são carregados no veículo (`EM_TRANSITO`).
4. O pedido muda para `EM_TRANSITO`.
5. A barra de capacidade e o estoque do veículo são atualizados.

### 6.3 Concluir entrega

1. Clique em **Concluir entrega** no pedido em trânsito.
2. O sistema carrega os vasilhames do cliente relacionados ao pedido.
3. O modal limita a seleção à quantidade comprada por produto e já marca apenas os válidos
   necessários.
4. Marque os vasilhames efetivamente recolhidos.
   - Os válidos vêm marcados por padrão, respeitando o limite do pedido.
   - Os vencidos aparecem com aviso vermelho e vêm desmarcados.
5. Se tentar selecionar mais vasilhames do que a quantidade do pedido, a seleção é bloqueada e o
   aviso aparece no topo do modal.
6. Confira a estimativa de caução e total.
7. Informe a forma de pagamento recebida.
8. Adicione uma observação, se necessário.
9. Clique em **Confirmar entrega**.
10. O sistema:
    - aceita os válidos e devolve à central;
    - descarta os vencidos;
    - entrega os garrafões carregados ao cliente;
    - recalcula caução e total;
    - muda o pedido para `ENTREGUE`;
    - se `FATURADO`, lança o valor no saldo devedor.
11. Um resumo mostra total final, caução, aceitos e recusados.
12. Erros da API são exibidos **dentro do modal**, no topo, e não atrás dele.

## 7. Área administrativa

### 7.1 Visão geral

Cartões com:

- pedidos em aberto;
- entregues;
- clientes;
- entregadores;
- estoque central válido.

Abas:

- **Pedidos**
- **Clientes**
- **Entregadores**
- **Estoque e produtos**

### 7.2 Pedidos

- **+ Novo pedido** abre o modal de pedido assistido:
  - seleção do cliente e exibição de saldo/limite/vasilhames válidos;
  - inclusão de itens, quantidade e vasilhames devolvidos;
  - forma de pagamento imediata (PIX, dinheiro, crédito ou débito);
  - confirmação obrigatória do recebimento com o valor total destacado.
- Ao criar, o pedido pode ser atribuído automaticamente e já nasce **pago**, com registro no
  histórico financeiro do cliente.
- A tabela mostra a situação do pagamento: **Pago**, **Pendente** ou **Faturado**.
- Pedidos pendentes de pagamento exibem **Confirmar pagamento**, que abre um modal de confirmação
  com cliente, valor, forma e status.
- Pedidos `PENDENTES` também exibem um seletor de entregador e o botão **Atribuir**.
- A atribuição manual valida capacidade e atividade.
- A tabela mostra valores, caução, cliente, zona, pagamento e entregador.

### 7.3 Clientes

- Tabela com tipo, zona, limite e saldo devedor.
- **Registrar pagamento** é exclusivo para **saldo devedor** e fica desabilitado quando o saldo é
  zero. Para receber o pagamento de um pedido, use **Confirmar pagamento** na aba **Pedidos**.
- O modal de saldo devedor contém:
  - identificação do cliente e tipo;
  - saldo devedor, limite e limite disponível;
  - campo de valor com atalho **Usar saldo total**;
  - descrição opcional;
  - histórico dos últimos pagamentos do cliente.
- O valor é validado no modal (maior que zero e menor/igual ao saldo) e o saldo é atualizado após
  a confirmação.
- Formulário **Cadastrar novo cliente**:
  - nome, e-mail, senha;
  - tipo;
  - CPF/CNPJ e telefone;
  - endereço completo e zona;
  - limite de crédito (obrigatório > 0 para corporativo).

### 7.4 Entregadores

- Tabela com zona, veículo, capacidade, carga e disponibilidade.
- Formulário de cadastro com zona, capacidade e veículo.

### 7.5 Estoque e produtos

- Tabela por produto:
  - na central;
  - em trânsito;
  - com clientes;
  - vencidos;
  - descartados.
- **Novo produto**: código, nome, volume, preço e caução.
- **Repor estoque central**: produto, quantidade e validade.
  - O formulário começa **vazio** (sem quantidade ou validade pré-preenchidas).
  - Cada unidade recebe um código único.
  - A validade não pode estar no passado.
  - Após o sucesso, os campos são limpos automaticamente para o próximo lote.

## 8. Roteiro de demonstração das regras

### RN02 — Caução

1. Entre como `maria@email.com`.
2. Adicione **4 garrafões de 20 L** com **2 vasilhames devolvidos**.
3. Clique em **Calcular valores**.
4. Observe:
   - produtos: R$ 48,00;
   - caução: R$ 50,00;
   - total: R$ 98,00.
5. Confirme o pedido.

### RN03 — Atribuição automática

1. O pedido da Maria (zona SUL) é atribuído a **Ana Souza** (zona SUL).
2. O status vai para `ATRIBUIDO`.
3. O admin vê o entregador na tabela de pedidos.
4. Para testar o fallback, cadastre um pedido com quantidade maior que a capacidade da Ana ou
   deixe a Ana sem capacidade; o sistema tenta outra zona.

### RN04 — Cancelamento

1. Ainda como Maria, o pedido `ATRIBUIDO` exibe **Cancelar**.
2. Entre como Ana, inicie o trânsito.
3. Volte como Maria: o botão **Cancelar** não aparece mais.
4. Se forçar a API:

```bash
curl -u maria@email.com:cliente123 -X POST \
  -H "Content-Type: application/json" -d '{}' \
  http://localhost:8080/api/pedidos/1/cancelar
```

Resultado: `409 Conflict` com mensagem da RN04.

### RN05 — Vasilhame vencido

1. Crie um pedido com 4 garrafões de 20 L e **0 devoluções declaradas**
   (ou 2, para comparar).
2. Ana inicia o trânsito.
3. Abra **Concluir entrega**.
4. Marque:
   - um vasilhame válido, por exemplo `G20-0061`;
   - um vasilhame vencido, por exemplo `G20-0069`.
5. Observe a estimativa: apenas o válido conta na troca.
6. Confirme.
7. O resumo mostra 1 aceito e 1 recusado.
8. A caução considera todos os garrafões sem troca válida.
9. O vasilhame vencido fica `DESCARTADO` e o válido retorna à central.

### RN01 — Limite corporativo

1. Entre como `mercado@aquadist.com`.
2. O limite é R$ 2.000,00 e o saldo devedor é R$ 300,00.
3. Monte um pedido de **50 garrafões de 20 L** sem devoluções.
4. Total: 50 × R$ 12,00 + 50 × R$ 25,00 = **R$ 1.850,00**.
5. Comprometido: R$ 300,00 + R$ 1.850,00 = R$ 2.150,00 > R$ 2.000,00.
6. A simulação mostra limite insuficiente e o botão **Confirmar pedido** fica desabilitado.
7. Registre um pagamento no admin para liberar limite e tente novamente.

### UC02 — Fluxo completo

1. Cliente cria o pedido.
2. Entregador inicia o trânsito.
3. Entregador conclui a entrega; para formas não `FATURADO`, o pagamento é confirmado e registrado.
4. Cliente consulta o pedido como `ENTREGUE`.
5. Admin visualiza o pedido entregue, o pagamento e o estoque atualizado; se faturado, o saldo devedor.

### Pedido assistido com pagamento

1. Entre como administrador e abra a aba **Pedidos**.
2. Clique em **+ Novo pedido**.
3. Selecione o cliente; o modal mostra saldo, limite e vasilhames válidos.
4. Adicione os produtos, quantidades e vasilhames devolvidos.
5. Escolha PIX, dinheiro, cartão de crédito ou débito.
6. Marque **“Confirmo que recebi R$ ...”**.
7. Clique em **Criar pedido e confirmar pagamento**.
8. O pedido aparece na tabela com o selo **Pago**, pode ser atribuído automaticamente e o
   pagamento fica no histórico do cliente.
9. Para um pedido existente ainda não pago, use **Confirmar pagamento** na coluna de ações.
10. O botão **Registrar pagamento** da aba Clientes é apenas para saldo devedor; não é necessário
    para receber um pedido.

## 9. Resetar a demonstração

Com a aplicação **parada**:

```powershell
# Windows
Remove-Item .\data\aquadist.db* -Force
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
rm -f data/aquadist.db data/aquadist.db-wal data/aquadist.db-shm
./mvnw spring-boot:run
```

Isso remove o banco e os arquivos WAL (`-wal` e `-shm`) e recria o esquema e o seed original.

## 10. Problemas comuns

| Problema | Causa / solução |
|---|---|
| `Port 8080 was already in use` | Encerre o processo na porta (veja o README) ou use `-Dspring-boot.run.arguments=--server.port=8081` |
| Página sem estilo | Sem internet para o CDN do Tailwind; a funcionalidade continua |
| `SQLITE_BUSY` | O projeto usa WAL e `busy_timeout` de 10 s. Evite múltiplas instâncias e transações longas; se persistir, pare a aplicação e recrie o banco |
| Login retorna 401 | Confira e-mail/senha; senhas diferenciam maiúsculas/minúsculas |
| Pedido fica `PENDENTE` | Nenhum entregador ativo com capacidade; use a atribuição manual |
| Estoque insuficiente | Reponha no admin em **Estoque e produtos** |
| Dados estranhos de testes antigos | Apague `data/aquadist.db*` e reinicie |
| Arquivos `aquadist.db-wal`/`-shm` | Normais no modo WAL; não apague com a aplicação em execução |
| Tailwind não carrega | O projeto usa CDN; em ambiente offline, as telas ficam sem estilo |

## 11. Parar a aplicação

No terminal do Maven, pressione `Ctrl+C`.

Se estiver rodando em background no Windows:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force
}
```
