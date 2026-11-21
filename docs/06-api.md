# 06 — API REST

Base URL: `http://localhost:8080`

Autenticação: **HTTP Basic** em todas as rotas `/api/**`.

```http
Authorization: Basic base64(email:senha)
```

As páginas HTML são públicas; os dados só são retornados com credenciais válidas.

## 1. Formato

- Requisições e respostas: `application/json`.
- Datas: ISO-8601 (`2026-11-21T14:30:00`).
- Datas sem hora: `yyyy-MM-dd`.
- Valores monetários: número decimal (`98.00`).
- Erros: `ProblemDetail` (RFC 9457).

## 2. Perfis e permissões

| Perfil | Pode acessar |
|---|---|
| Público | `/`, `/login.html`, `/cliente.html`, `/entregador.html`, `/admin.html`, `/assets/**` |
| Autenticado | `/api/auth/me`, `/api/produtos`, `/api/pedidos/{id}` (com verificação de propriedade) |
| `CLIENTE` | `/api/clientes/me`, `/api/clientes/me/vasilhames`, `/api/pedidos/**` do próprio cliente |
| `ENTREGADOR` | `/api/entregador/**` |
| `ADMIN` | `/api/admin/**` |

## 3. Endpoints

### 3.1 Autenticação

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| `GET` | `/api/auth/me` | autenticado | Retorna nome, e-mail e perfil |

### 3.2 Catálogo

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| `GET` | `/api/produtos` | autenticado | Lista produtos com estoque central e vencidos |
| `POST` | `/api/admin/produtos` | `ADMIN` | Cria produto |
| `POST` | `/api/admin/vasilhames/lote` | `ADMIN` | Adiciona lote de vasilhames ao estoque central |
| `GET` | `/api/admin/estoque` | `ADMIN` | Estoque consolidado por produto |

### 3.3 Clientes

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| `GET` | `/api/clientes/me` | `CLIENTE` | Perfil e limite |
| `GET` | `/api/clientes/me/vasilhames` | `CLIENTE` | Vasilhames em posse, válidos e vencidos |
| `GET` | `/api/admin/clientes` | `ADMIN` | Lista clientes |
| `GET` | `/api/admin/clientes/{id}` | `ADMIN` | Detalha cliente |
| `GET` | `/api/admin/clientes/{id}/vasilhames` | `ADMIN` | Vasilhames válidos/vencidos do cliente |
| `POST` | `/api/admin/clientes` | `ADMIN` | Cadastra cliente |
| `POST` | `/api/admin/clientes/{id}/pagamentos` | `ADMIN` | Registra baixa de saldo devedor |
| `GET` | `/api/admin/clientes/{id}/pagamentos` | `ADMIN` | Histórico de pagamentos (saldo e pedidos) |

### 3.4 Entregadores

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| `GET` | `/api/admin/entregadores` | `ADMIN` | Lista entregadores com carga |
| `POST` | `/api/admin/entregadores` | `ADMIN` | Cadastra entregador |
| `GET` | `/api/entregador/me` | `ENTREGADOR` | Perfil e capacidade |
| `GET` | `/api/entregador/pedidos` | `ENTREGADOR` | Rota ativa (`ATRIBUIDO` + `EM_TRANSITO`) |
| `GET` | `/api/entregador/pedidos/{id}` | `ENTREGADOR` | Detalhe de pedido atribuído |
| `GET` | `/api/entregador/pedidos/{id}/vasilhames-cliente` | `ENTREGADOR` | Vasilhames do cliente para conferência |
| `GET` | `/api/entregador/estoque` | `ENTREGADOR` | Vasilhames em trânsito no veículo |
| `POST` | `/api/entregador/pedidos/{id}/iniciar-transito` | `ENTREGADOR` | Carrega estoque e muda para `EM_TRANSITO` |
| `POST` | `/api/entregador/pedidos/{id}/concluir` | `ENTREGADOR` | Conclui entrega e movimenta vasilhames |

### 3.5 Pedidos

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| `POST` | `/api/pedidos` | `CLIENTE` | Cria pedido |
| `POST` | `/api/pedidos/simulacao` | `CLIENTE` | Calcula valores e limite sem salvar |
| `GET` | `/api/pedidos/me` | `CLIENTE` | Pedidos do cliente |
| `GET` | `/api/pedidos/{id}` | autenticado | Detalhe com verificação de propriedade |
| `POST` | `/api/pedidos/{id}/cancelar` | `CLIENTE` | Cancela antes do despacho (RN04) |
| `GET` | `/api/admin/pedidos` | `ADMIN` | Todos os pedidos; filtro `?status=` |
| `POST` | `/api/admin/pedidos` | `ADMIN` | Cria pedido em nome do cliente com pagamento confirmado |
| `POST` | `/api/admin/pedidos/{id}/confirmar-pagamento` | `ADMIN` | Confirma pagamento de pedido existente |
| `POST` | `/api/admin/pedidos/{id}/atribuir` | `ADMIN` | Atribuição manual |

## 4. Exemplos de uso

### 4.1 Ver o próprio perfil

```bash
curl -u maria@email.com:cliente123 http://localhost:8080/api/auth/me
```

```json
{
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "perfil": "CLIENTE"
}
```

### 4.2 Listar produtos

```bash
curl -u maria@email.com:cliente123 http://localhost:8080/api/produtos
```

### 4.3 Consultar vasilhames em posse

```bash
curl -u maria@email.com:cliente123 http://localhost:8080/api/clientes/me/vasilhames
```

Exemplo parcial:

```json
{
  "clienteId": 2,
  "clienteNome": "Maria Silva",
  "totalValidos": 11,
  "totalVencidos": 2,
  "resumo": [
    { "produtoId": 1, "produtoNome": "Garrafao 20L", "validos": 8, "vencidos": 2 },
    { "produtoId": 2, "produtoNome": "Garrafao 10L", "validos": 3, "vencidos": 0 }
  ],
  "vasilhames": [
    {
      "codigo": "G20-0069",
      "produtoNome": "Garrafao 20L",
      "dataValidade": "2026-11-16",
      "status": "COM_CLIENTE",
      "vencido": true
    }
  ]
}
```

### 4.4 Simular pedido

```bash
curl -u maria@email.com:cliente123 \
  -H "Content-Type: application/json" \
  -d '{
    "itens": [
      { "produtoId": 1, "quantidade": 4, "vasilhamesDevolvidos": 2 }
    ]
  }' \
  http://localhost:8080/api/pedidos/simulacao
```

Resposta (trecho):

```json
{
  "valorProdutos": 48.00,
  "valorCaucao": 50.00,
  "valorTotal": 98.00,
  "limiteSuficiente": true,
  "itens": [
    {
      "produtoId": 1,
      "produtoNome": "Garrafao 20L",
      "quantidade": 4,
      "vasilhamesDevolvidos": 2,
      "vasilhamesDisponiveisTroca": 8,
      "quantidadeCaucao": 2,
      "valorCaucao": 50.00,
      "subtotal": 48.00
    }
  ],
  "avisos": []
}
```

### 4.5 Criar pedido

```bash
curl -u maria@email.com:cliente123 \
  -H "Content-Type: application/json" \
  -d '{
    "itens": [
      { "produtoId": 1, "quantidade": 4, "vasilhamesDevolvidos": 2 }
    ],
    "formaPagamento": "PIX",
    "observacao": "Entregar até 18h"
  }' \
  http://localhost:8080/api/pedidos
```

Resposta `201 Created` (trecho):

```json
{
  "id": 1,
  "status": "ATRIBUIDO",
  "clienteNome": "Maria Silva",
  "entregadorId": 2,
  "entregadorNome": "Ana Souza",
  "formaPagamento": "PIX",
  "totalGarrafoes": 4,
  "valorProdutos": 48.00,
  "valorCaucao": 50.00,
  "valorTotal": 98.00
}
```

> O pedido é criado como `PENDENTE` e o evento de atribuição pode alterá-lo para `ATRIBUIDO`
> antes da resposta. Se não houver entregador com capacidade, o status permanece `PENDENTE`.

### 4.6 Consultar rota do entregador

```bash
curl -u ana@aquadist.com:entregador123 \
  http://localhost:8080/api/entregador/pedidos
```

### 4.7 Iniciar trânsito

```bash
curl -u ana@aquadist.com:entregador123 \
  -X POST \
  http://localhost:8080/api/entregador/pedidos/1/iniciar-transito
```

O status muda para `EM_TRANSITO` e os vasilhames saem de `NA_CENTRAL` para `EM_TRANSITO`.

### 4.8 Consultar vasilhames do cliente

```bash
curl -u ana@aquadist.com:entregador123 \
  http://localhost:8080/api/entregador/pedidos/1/vasilhames-cliente
```

### 4.9 Concluir entrega

```bash
curl -u ana@aquadist.com:entregador123 \
  -H "Content-Type: application/json" \
  -d '{
    "codigosVasilhamesDevolvidos": ["G20-0061", "G20-0062"],
    "formaPagamentoRecebida": "PIX",
    "observacao": "Recebido pela portaria"
  }' \
  http://localhost:8080/api/entregador/pedidos/1/concluir
```

Resposta (trecho):

```json
{
  "status": "ENTREGUE",
  "pagamentoConfirmado": true,
  "pagoEm": "2026-11-21T15:30:00",
  "valorCaucao": 50.00,
  "valorTotal": 98.00,
  "itens": [
    {
      "quantidade": 4,
      "vasilhamesValidosRecebidos": 2,
      "vasilhamesRecusadosVencidos": 0,
      "quantidadeCaucao": 2
    }
  ]
}
```

Ao concluir com forma diferente de `FATURADO`, o sistema confirma o pagamento do pedido e cria um registro em `Pagamento` (caso ainda não tenha sido pago). Para `FATURADO`, o valor é lançado no saldo devedor e o pedido permanece sem pagamento confirmado. Pedido já pago não pode ser alterado para `FATURADO` na entrega.

#### Exemplo com vasilhame vencido

```bash
curl -u ana@aquadist.com:entregador123 \
  -H "Content-Type: application/json" \
  -d '{
    "codigosVasilhamesDevolvidos": ["G20-0061", "G20-0069"],
    "formaPagamentoRecebida": "PIX"
  }' \
  http://localhost:8080/api/entregador/pedidos/1/concluir
```

Resultado esperado para 4 garrafões:

- 1 vasilhame válido aceito;
- 1 vasilhame vencido descartado;
- caução recalculada para 3 faltantes (R$ 75,00);
- total final R$ 123,00.

### 4.10 Cancelar pedido

```bash
curl -u maria@email.com:cliente123 \
  -H "Content-Type: application/json" \
  -d '{"motivo":"Mudei de endereço"}' \
  http://localhost:8080/api/pedidos/1/cancelar
```

- `PENDENTE` ou `ATRIBUIDO` → `200 OK` com `status = CANCELADO`.
- `EM_TRANSITO` em diante → `409 Conflict`.

### 4.11 Administração

#### Atribuir entregador

```bash
curl -u admin@aquadist.com:admin123 \
  -H "Content-Type: application/json" \
  -d '{"entregadorId": 1}' \
  http://localhost:8080/api/admin/pedidos/1/atribuir
```

#### Cadastrar cliente

```bash
curl -u admin@aquadist.com:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Café Central",
    "email": "cafe@central.com",
    "senha": "cafe123",
    "tipo": "CORPORATIVO",
    "documento": "98.765.432/0001-10",
    "telefone": "(11) 97777-0000",
    "endereco": {
      "logradouro": "Rua Augusta",
      "numero": "1000",
      "complemento": "Loja 4",
      "bairro": "Consolação",
      "cidade": "São Paulo",
      "uf": "SP",
      "cep": "01304-001",
      "zona": "CENTRO"
    },
    "limiteCredito": 1500.00
  }' \
  http://localhost:8080/api/admin/clientes
```

#### Criar pedido em nome do cliente

```bash
curl -u admin@aquadist.com:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 2,
    "itens": [
      { "produtoId": 1, "quantidade": 2, "vasilhamesDevolvidos": 0 }
    ],
    "formaPagamento": "PIX",
    "observacao": "Pedido recebido no balcão",
    "pagamentoConfirmado": true
  }' \
  http://localhost:8080/api/admin/pedidos
```

Regras:

- `pagamentoConfirmado` deve ser `true`; a criação é bloqueada caso contrário.
- Somente `PIX`, `DINHEIRO`, `CARTAO_CREDITO` e `CARTAO_DEBITO` são aceitos nesse fluxo.
- O pedido nasce com `pagamentoConfirmado = true`, `pagoEm` preenchido e um registro em `Pagamento` vinculado ao pedido.
- A atribuição de entregador continua automática.

#### Confirmar pagamento de pedido existente

```bash
curl -u admin@aquadist.com:admin123 \
  -X POST \
  http://localhost:8080/api/admin/pedidos/2/confirmar-pagamento
```

- Marca `pagamentoConfirmado = true` e preenche `pagoEm`.
- Cria um registro em `Pagamento` com `pedidoId`.
- Recusa pedido cancelado, pedido `FATURADO` e pagamento já confirmado.

#### Registrar baixa de saldo devedor

Este endpoint é exclusivo para **saldo devedor** de clientes. Para receber o pagamento de um pedido, use os endpoints acima.

```bash
curl -u admin@aquadist.com:admin123 \
  -H "Content-Type: application/json" \
  -d '{"valor": 100.00, "descricao": "PIX recebido"}' \
  http://localhost:8080/api/admin/clientes/1/pagamentos
```

#### Consultar vasilhames de um cliente

```bash
curl -u admin@aquadist.com:admin123 \
  http://localhost:8080/api/admin/clientes/2/vasilhames
```

Usado pelo modal de novo pedido para limitar a devolução à quantidade de vasilhames válidos em posse do cliente.

#### Repor estoque

```bash
curl -u admin@aquadist.com:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "produtoId": 1,
    "quantidade": 10,
    "dataValidade": "2027-12-31"
  }' \
  http://localhost:8080/api/admin/vasilhames/lote
```

#### Cadastrar entregador

```bash
curl -u admin@aquadist.com:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Carlos Lima",
    "email": "carlos@aquadist.com",
    "senha": "entregador123",
    "telefone": "(11) 96666-0000",
    "zona": "LESTE",
    "capacidadeCarga": 45,
    "veiculoModelo": "Renault Kangoo",
    "veiculoPlaca": "GHI7J89"
  }' \
  http://localhost:8080/api/admin/entregadores
```

## 5. Corpos de requisição

### `CriarPedidoRequest`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `itens` | lista | sim | ao menos um |
| `itens[].produtoId` | `Long` | sim | produto ativo |
| `itens[].quantidade` | `int` | sim | ≥ 1 |
| `itens[].vasilhamesDevolvidos` | `int` | sim | ≥ 0 e ≤ quantidade |
| `formaPagamento` | enum | sim | `FATURADO` só corporativo |
| `observacao` | string | não | até 500 caracteres |

A resposta `PedidoResponse` inclui `pagamentoConfirmado` e `pagoEm`. Pedidos comuns criados pelo cliente nascem com pagamento pendente; o pagamento é confirmado na entrega (formas não `FATURADO`) ou pelo administrador.

### `CriarPedidoAdminRequest`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `clienteId` | `Long` | sim | cliente existente |
| `itens` | lista | sim | ao menos um; mesmas regras do pedido comum |
| `formaPagamento` | enum | sim | `PIX`, `DINHEIRO`, `CARTAO_CREDITO` ou `CARTAO_DEBITO` |
| `observacao` | string | não | até 500 caracteres |
| `pagamentoConfirmado` | `boolean` | sim | deve ser `true`; validado com `@AssertTrue` |

### `ConcluirEntregaRequest`

| Campo | Tipo | Obrigatório |
|---|---|---|
| `codigosVasilhamesDevolvidos` | lista de strings | sim (pode ser `[]`) |
| `formaPagamentoRecebida` | enum | não (mantém a do pedido) |
| `observacao` | string | não |

### `CriarClienteRequest`

```json
{
  "nome": "string",
  "email": "string",
  "senha": "string (6-60)",
  "tipo": "RESIDENCIAL | CORPORATIVO",
  "documento": "string",
  "telefone": "string",
  "endereco": {
    "logradouro": "string",
    "numero": "string",
    "complemento": "string",
    "bairro": "string",
    "cidade": "string",
    "uf": "string",
    "cep": "string",
    "zona": "NORTE | SUL | LESTE | OESTE | CENTRO"
  },
  "limiteCredito": 0.00
}
```

### `CriarEntregadorRequest`

```json
{
  "nome": "string",
  "email": "string",
  "senha": "string",
  "telefone": "string",
  "zona": "ZONA",
  "capacidadeCarga": 40,
  "veiculoModelo": "string",
  "veiculoPlaca": "string"
}
```

### `CriarProdutoRequest`

```json
{
  "codigo": "G15",
  "nome": "Garrafão 15L",
  "volumeLitros": 15,
  "precoUnitario": 10.00,
  "taxaCaucao": 22.00
}
```

## 6. Códigos HTTP

| Código | Uso |
|---|---|
| 200 | Consulta/ação concluída |
| 201 | Pedido, cliente, entregador, produto ou pagamento criado |
| 400 | Validação, JSON inválido, regra de negócio violada |
| 401 | Credenciais ausentes/inválidas |
| 403 | Perfil sem permissão ou recurso de outro usuário |
| 404 | Recurso não encontrado |
| 409 | Conflito de estado/estoque |
| 422 | Limite de crédito excedido (RN01) |
| 500 | Erro inesperado |

## 7. Formato de erro

```json
{
  "type": "https://aquadist.local/errors/409",
  "title": "Estoque insuficiente",
  "status": 409,
  "detail": "Estoque insuficiente para Garrafao 20L. Solicitado: 100, disponivel: 60."
}
```

Erros de validação acrescentam `campos`:

```json
{
  "type": "https://aquadist.local/errors/400",
  "title": "Erro de validacao",
  "status": 400,
  "detail": "Um ou mais campos estao invalidos.",
  "campos": {
    "itens": "o pedido deve ter ao menos um item"
  }
}
```
