package com.aquadist.service;

import com.aquadist.dto.AtribuirEntregadorRequest;
import com.aquadist.dto.CancelarPedidoRequest;
import com.aquadist.dto.ConcluirEntregaRequest;
import com.aquadist.dto.CriarPedidoAdminRequest;
import com.aquadist.dto.CriarPedidoRequest;
import com.aquadist.dto.ItemPedidoRequest;
import com.aquadist.dto.PedidoResponse;
import com.aquadist.dto.SimulacaoRequest;
import com.aquadist.dto.SimulacaoResponse;
import com.aquadist.event.PedidoCriadoEvent;
import com.aquadist.exception.AcessoNegadoException;
import com.aquadist.exception.CreditoExcedidoException;
import com.aquadist.exception.EstoqueInsuficienteException;
import com.aquadist.exception.OperacaoNaoPermitidaException;
import com.aquadist.exception.RecursoNaoEncontradoException;
import com.aquadist.exception.RegraDeNegocioException;
import com.aquadist.model.Cliente;
import com.aquadist.model.Entregador;
import com.aquadist.model.FormaPagamento;
import com.aquadist.model.ItemPedido;
import com.aquadist.model.Pagamento;
import com.aquadist.model.Pedido;
import com.aquadist.model.PerfilUsuario;
import com.aquadist.model.Produto;
import com.aquadist.model.StatusPedido;
import com.aquadist.model.StatusVasilhame;
import com.aquadist.model.Usuario;
import com.aquadist.model.Vasilhame;
import com.aquadist.repository.ClienteRepository;
import com.aquadist.repository.EntregadorRepository;
import com.aquadist.repository.PagamentoRepository;
import com.aquadist.repository.PedidoRepository;
import com.aquadist.repository.ProdutoRepository;
import com.aquadist.repository.UsuarioRepository;
import com.aquadist.repository.VasilhameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);
    private static final List<StatusPedido> STATUS_EM_ABERTO =
            List.of(StatusPedido.PENDENTE, StatusPedido.ATRIBUIDO, StatusPedido.EM_TRANSITO);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Set<FormaPagamento> FORMAS_PAGAMENTO_IMEDIATO = Set.of(
            FormaPagamento.PIX,
            FormaPagamento.DINHEIRO,
            FormaPagamento.CARTAO_CREDITO,
            FormaPagamento.CARTAO_DEBITO
    );

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final VasilhameRepository vasilhameRepository;
    private final EntregadorRepository entregadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AtribuicaoEntregadorService atribuicaoService;
    private final ApplicationEventPublisher eventPublisher;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         ProdutoRepository produtoRepository,
                         VasilhameRepository vasilhameRepository,
                         EntregadorRepository entregadorRepository,
                         UsuarioRepository usuarioRepository,
                         PagamentoRepository pagamentoRepository,
                         AtribuicaoEntregadorService atribuicaoService,
                         ApplicationEventPublisher eventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.vasilhameRepository = vasilhameRepository;
        this.entregadorRepository = entregadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.atribuicaoService = atribuicaoService;
        this.eventPublisher = eventPublisher;
    }

    // ------------------------------------------------------------------
    // UC01 - Realizar pedido
    // ------------------------------------------------------------------

    @Transactional
    public PedidoResponse criar(CriarPedidoRequest request, String emailCliente) {
        Cliente cliente = clientePorEmail(emailCliente);
        Pedido pedido = criarPedido(cliente, request.itens(), request.formaPagamento(), request.observacao());
        return PedidoResponse.from(pedido);
    }

    /**
     * Cria um pedido em nome do cliente pelo administrador.
     * O pagamento e recebido no ato; o pedido ja nasce com pagamento confirmado.
     */
    @Transactional
    public PedidoResponse criarComoAdmin(CriarPedidoAdminRequest request) {
        if (!request.pagamentoConfirmado()) {
            throw new RegraDeNegocioException(
                    "O administrador deve confirmar o recebimento do pagamento antes de criar o pedido.");
        }
        if (!FORMAS_PAGAMENTO_IMEDIATO.contains(request.formaPagamento())) {
            throw new RegraDeNegocioException(
                    "Pedidos criados pelo administrador exigem pagamento imediato "
                            + "(PIX, dinheiro ou cartao). FATURADO e BOLETO devem ser tratados na conta do cliente.");
        }

        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente nao encontrado: " + request.clienteId()));

        Pedido pedido = criarPedido(cliente, request.itens(), request.formaPagamento(), request.observacao());
        confirmarPagamentoDoPedido(pedido, "Pagamento recebido na criacao");
        pedidoRepository.save(pedido);

        log.info("Pedido {} criado pelo administrador para {} com pagamento confirmado no valor de R$ {}.",
                pedido.getId(), cliente.getUsuario().getNome(), pedido.getValorTotal().toPlainString());

        return PedidoResponse.from(pedido);
    }

    @Transactional
    public PedidoResponse confirmarPagamento(Long pedidoId) {
        Pedido pedido = porId(pedidoId);
        confirmarPagamentoDoPedido(pedido, "Pagamento confirmado pelo administrador");
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    private Pedido criarPedido(Cliente cliente, List<ItemPedidoRequest> itens,
                               FormaPagamento formaPagamento, String observacao) {
        validarClienteParaPedido(cliente);
        validarFormaPagamento(cliente, formaPagamento);

        Pedido pedido = new Pedido(cliente, cliente.getEndereco(), formaPagamento, observacao);

        montarItens(pedido, itens, cliente, true);
        pedido.recalcularTotais();
        verificarCredito(cliente, pedido.getValorTotal());

        pedidoRepository.save(pedido);
        eventPublisher.publishEvent(new PedidoCriadoEvent(pedido.getId()));

        log.info("Pedido {} criado para o cliente {} no valor de R$ {}.",
                pedido.getId(), cliente.getUsuario().getNome(), pedido.getValorTotal().toPlainString());

        return pedido;
    }

    private void confirmarPagamentoDoPedido(Pedido pedido, String origem) {
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new OperacaoNaoPermitidaException("Pedidos cancelados nao podem receber confirmacao de pagamento.");
        }
        if (pedido.getFormaPagamento() == FormaPagamento.FATURADO) {
            throw new RegraDeNegocioException(
                    "Pedidos FATURADO sao cobrados pelo saldo devedor. Registre o pagamento na conta do cliente.");
        }
        if (pedido.isPagamentoConfirmado()) {
            throw new OperacaoNaoPermitidaException(
                    "O pagamento deste pedido ja foi confirmado em " + pedido.getPagoEm() + ".");
        }

        pedido.setPagamentoConfirmado(true);
        pedido.setPagoEm(LocalDateTime.now());
        pagamentoRepository.save(new Pagamento(
                pedido.getCliente(),
                pedido,
                pedido.getValorTotal(),
                origem + " - pedido #" + pedido.getId() + " (" + pedido.getFormaPagamento() + ")"
        ));
    }

    @Transactional(readOnly = true)
    public SimulacaoResponse simular(SimulacaoRequest request, String emailCliente) {
        Cliente cliente = clientePorEmail(emailCliente);
        validarClienteParaPedido(cliente);

        LocalDate hoje = LocalDate.now();
        List<SimulacaoResponse.ItemSimulacao> itens = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Set<Long> produtos = new HashSet<>();
        BigDecimal totalProdutos = ZERO;
        BigDecimal totalCaucao = ZERO;

        for (ItemPedidoRequest itemRequest : request.itens()) {
            Produto produto = produtoAtivo(itemRequest.produtoId());
            if (!produtos.add(produto.getId())) {
                throw new RegraDeNegocioException(
                        "O produto " + produto.getNome() + " aparece mais de uma vez no pedido.");
            }
            if (itemRequest.vasilhamesDevolvidos() > itemRequest.quantidade()) {
                throw new RegraDeNegocioException(
                        "A quantidade de vasilhames devolvidos nao pode ser maior que a quantidade de garrafoes do item.");
            }

            long disponiveisTroca = vasilhameRepository
                    .countByClienteIdAndProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
                            cliente.getId(), produto.getId(), StatusVasilhame.COM_CLIENTE, hoje);
            if (itemRequest.vasilhamesDevolvidos() > disponiveisTroca) {
                throw new RegraDeNegocioException("Voce informou " + itemRequest.vasilhamesDevolvidos()
                        + " vasilhames para troca do produto " + produto.getNome()
                        + ", mas possui apenas " + disponiveisTroca + " vasilhame(s) valido(s) em posse.");
            }

            ItemPedido item = new ItemPedido(produto, itemRequest.quantidade(),
                    itemRequest.vasilhamesDevolvidos());
            totalProdutos = totalProdutos.add(item.getSubtotal());
            totalCaucao = totalCaucao.add(item.getValorCaucao());

            long estoqueCentral = vasilhameRepository
                    .countByProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
                            produto.getId(), StatusVasilhame.NA_CENTRAL, hoje);
            if (estoqueCentral < itemRequest.quantidade()) {
                avisos.add("Estoque central insuficiente para " + produto.getNome()
                        + ": solicitado " + itemRequest.quantidade()
                        + ", disponivel " + estoqueCentral + ".");
            }

            itens.add(new SimulacaoResponse.ItemSimulacao(
                    produto.getId(),
                    produto.getNome(),
                    itemRequest.quantidade(),
                    itemRequest.vasilhamesDevolvidos(),
                    disponiveisTroca,
                    item.getQuantidadeCaucao(),
                    item.getValorCaucao(),
                    item.getSubtotal()
            ));
        }

        BigDecimal total = totalProdutos.add(totalCaucao).setScale(2, RoundingMode.HALF_UP);
        BigDecimal emAberto = valorEmAberto(cliente.getId());
        boolean limiteSuficiente = cliente.permiteCompra(total, emAberto);

        BigDecimal limiteDisponivel = cliente.getLimiteCredito()
                .subtract(cliente.getSaldoDevedor())
                .subtract(emAberto)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        if (!limiteSuficiente) {
            avisos.add("Limite de credito insuficiente. Disponivel para este pedido: R$ "
                    + limiteDisponivel.toPlainString() + ".");
        }

        return new SimulacaoResponse(
                totalProdutos.setScale(2, RoundingMode.HALF_UP),
                totalCaucao.setScale(2, RoundingMode.HALF_UP),
                total,
                limiteSuficiente,
                cliente.getLimiteCredito(),
                cliente.getSaldoDevedor(),
                emAberto,
                limiteDisponivel,
                itens,
                avisos
        );
    }

    // ------------------------------------------------------------------
    // Consultas e cancelamento
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarDoCliente(String emailCliente) {
        Cliente cliente = clientePorEmail(emailCliente);
        return pedidoRepository.findByClienteIdOrderByCriadoEmDesc(cliente.getId()).stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos(StatusPedido status) {
        List<Pedido> pedidos = status == null
                ? pedidoRepository.findAllByOrderByCriadoEmDesc()
                : pedidoRepository.findByStatusOrderByCriadoEmDesc(status);
        return pedidos.stream().map(PedidoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long id, String email) {
        Pedido pedido = porId(id);
        Usuario usuario = usuarioPorEmail(email);

        boolean autorizado = switch (usuario.getPerfil()) {
            case ADMIN -> true;
            case CLIENTE -> pedido.getCliente().getUsuario().getEmail()
                    .equalsIgnoreCase(usuario.getEmail());
            case ENTREGADOR -> pedido.getEntregador() != null
                    && pedido.getEntregador().getUsuario().getEmail().equalsIgnoreCase(usuario.getEmail());
        };

        if (!autorizado) {
            throw new AcessoNegadoException("Voce nao tem permissao para visualizar este pedido.");
        }
        return PedidoResponse.from(pedido);
    }

    @Transactional
    public PedidoResponse cancelar(Long id, String emailCliente, CancelarPedidoRequest request) {
        Pedido pedido = porId(id);
        if (!pedido.getCliente().getUsuario().getEmail().equalsIgnoreCase(emailCliente)) {
            throw new AcessoNegadoException("Este pedido nao pertence ao cliente autenticado.");
        }
        if (!pedido.podeCancelar()) {
            throw new OperacaoNaoPermitidaException(
                    "RN04: o pedido nao pode mais ser cancelado, pois ja saiu para entrega (status "
                            + pedido.getStatus() + ").");
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceladoEm(LocalDateTime.now());
        pedido.setMotivoCancelamento(request == null || request.motivo() == null
                || request.motivo().isBlank()
                ? "Cancelado pelo cliente"
                : request.motivo().trim());

        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    // ------------------------------------------------------------------
    // Administracao / atribuicao manual
    // ------------------------------------------------------------------

    @Transactional
    public PedidoResponse atribuirEntregador(Long id, AtribuirEntregadorRequest request) {
        Pedido pedido = porId(id);
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new OperacaoNaoPermitidaException(
                    "Somente pedidos PENDENTES podem receber atribuicao manual.");
        }

        Entregador entregador = entregadorRepository.findById(request.entregadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entregador nao encontrado."));
        if (!entregador.isAtivo()) {
            throw new RegraDeNegocioException("O entregador selecionado esta inativo.");
        }
        if (!atribuicaoService.temCapacidade(entregador, pedido.totalGarrafoes())) {
            throw new RegraDeNegocioException("O entregador selecionado nao possui capacidade de carga disponivel.");
        }

        pedido.setEntregador(entregador);
        pedido.setStatus(StatusPedido.ATRIBUIDO);
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    // ------------------------------------------------------------------
    // UC02 - Atualizar status da entrega
    // ------------------------------------------------------------------

    @Transactional
    public PedidoResponse iniciarTransito(Long id, String emailEntregador) {
        Pedido pedido = porId(id);
        Entregador entregador = entregadorPorEmail(emailEntregador);
        validarEntregadorDoPedido(pedido, entregador);

        if (pedido.getStatus() != StatusPedido.ATRIBUIDO) {
            throw new OperacaoNaoPermitidaException(
                    "O pedido precisa estar com status ATRIBUIDO para iniciar o transito. Status atual: "
                            + pedido.getStatus() + ".");
        }

        LocalDate hoje = LocalDate.now();
        List<Vasilhame> carregados = new ArrayList<>();

        for (ItemPedido item : pedido.getItens()) {
            List<Vasilhame> disponiveis = vasilhameRepository
                    .findByProdutoIdAndStatusAndDataValidadeGreaterThanEqualOrderByDataValidadeAsc(
                            item.getProduto().getId(), StatusVasilhame.NA_CENTRAL, hoje,
                            PageRequest.of(0, item.getQuantidade()));

            if (disponiveis.size() < item.getQuantidade()) {
                throw new EstoqueInsuficienteException(
                        "Estoque central insuficiente para " + item.getProduto().getNome()
                                + ". Necessario: " + item.getQuantidade()
                                + ", disponivel: " + disponiveis.size() + ".");
            }

            for (Vasilhame vasilhame : disponiveis) {
                vasilhame.carregarParaTransito(entregador, pedido);
                carregados.add(vasilhame);
            }
        }

        vasilhameRepository.saveAll(carregados);
        pedido.setStatus(StatusPedido.EM_TRANSITO);
        pedido.setDespachadoEm(LocalDateTime.now());
        pedidoRepository.save(pedido);

        return PedidoResponse.from(pedido);
    }

    @Transactional
    public PedidoResponse concluirEntrega(Long id, String emailEntregador, ConcluirEntregaRequest request) {
        Pedido pedido = porId(id);
        Entregador entregador = entregadorPorEmail(emailEntregador);
        validarEntregadorDoPedido(pedido, entregador);

        if (pedido.getStatus() != StatusPedido.EM_TRANSITO) {
            throw new OperacaoNaoPermitidaException(
                    "Somente pedidos EM_TRANSITO podem ser concluidos. Status atual: "
                            + pedido.getStatus() + ".");
        }

        LocalDate hoje = LocalDate.now();
        Cliente cliente = pedido.getCliente();

        // Normaliza e valida os codigos informados pelo entregador.
        LinkedHashSet<String> codigos = new LinkedHashSet<>();
        for (String codigo : request.codigosVasilhamesDevolvidos()) {
            if (codigo == null || codigo.isBlank()) {
                throw new RegraDeNegocioException("A lista de vasilhames devolvidos contem um codigo vazio.");
            }
            codigos.add(codigo.trim().toUpperCase(Locale.ROOT));
        }

        List<Vasilhame> encontrados = codigos.isEmpty()
                ? List.of()
                : vasilhameRepository.findByCodigoIn(codigos);
        if (encontrados.size() != codigos.size()) {
            Set<String> localizados = encontrados.stream()
                    .map(Vasilhame::getCodigo)
                    .collect(Collectors.toSet());
            List<String> faltantes = codigos.stream()
                    .filter(codigo -> !localizados.contains(codigo))
                    .toList();
            throw new RegraDeNegocioException("Vasilhame(s) nao encontrado(s): " + String.join(", ", faltantes));
        }

        Map<Long, ItemPedido> itemPorProduto = new HashMap<>();
        for (ItemPedido item : pedido.getItens()) {
            itemPorProduto.put(item.getProduto().getId(), item);
        }

        Map<Long, List<Vasilhame>> devolucoesPorProduto = new HashMap<>();
        for (Vasilhame vasilhame : encontrados) {
            if (vasilhame.getStatus() != StatusVasilhame.COM_CLIENTE
                    || vasilhame.getCliente() == null
                    || !vasilhame.getCliente().getId().equals(cliente.getId())) {
                throw new RegraDeNegocioException("O vasilhame " + vasilhame.getCodigo()
                        + " nao pertence ao cliente deste pedido ou nao esta em posse dele.");
            }
            if (!itemPorProduto.containsKey(vasilhame.getProduto().getId())) {
                throw new RegraDeNegocioException("O vasilhame " + vasilhame.getCodigo()
                        + " e de um produto que nao faz parte deste pedido.");
            }
            devolucoesPorProduto
                    .computeIfAbsent(vasilhame.getProduto().getId(), chave -> new ArrayList<>())
                    .add(vasilhame);
        }

        List<Vasilhame> alterados = new ArrayList<>();

        for (ItemPedido item : pedido.getItens()) {
            List<Vasilhame> devolucoes = devolucoesPorProduto
                    .getOrDefault(item.getProduto().getId(), List.of());
            if (devolucoes.size() > item.getQuantidade()) {
                throw new RegraDeNegocioException("Foram informados " + devolucoes.size()
                        + " vasilhames do produto " + item.getProduto().getNome()
                        + ", mas o pedido possui apenas " + item.getQuantidade() + " garrafoes.");
            }

            int validos = 0;
            int vencidos = 0;
            for (Vasilhame vasilhame : devolucoes) {
                if (vasilhame.estaVencido(hoje)) {
                    vencidos++;
                    vasilhame.descartar();
                } else {
                    validos++;
                    vasilhame.retornarACentral();
                }
                alterados.add(vasilhame);
            }

            item.setVasilhamesValidosRecebidos(validos);
            item.setVasilhamesRecusadosVencidos(vencidos);
            item.recalcular(validos);

            List<Vasilhame> carregados = vasilhameRepository.findByPedidoIdAndStatusAndProdutoId(
                    pedido.getId(), StatusVasilhame.EM_TRANSITO, item.getProduto().getId());
            if (carregados.size() != item.getQuantidade()) {
                throw new OperacaoNaoPermitidaException(
                        "Inconsistencia no estoque do veiculo para o produto "
                                + item.getProduto().getNome() + ". Esperado: " + item.getQuantidade()
                                + ", encontrado: " + carregados.size() + ".");
            }
            for (Vasilhame vasilhame : carregados) {
                vasilhame.entregarAoCliente(cliente);
                alterados.add(vasilhame);
            }
        }

        pedido.recalcularTotais();

        FormaPagamento formaRecebida = request.formaPagamentoRecebida() == null
                ? pedido.getFormaPagamento()
                : request.formaPagamentoRecebida();
        validarFormaPagamento(cliente, formaRecebida);
        pedido.setFormaPagamento(formaRecebida);

        if (formaRecebida == FormaPagamento.FATURADO) {
            if (pedido.isPagamentoConfirmado()) {
                throw new RegraDeNegocioException(
                        "Pedido com pagamento confirmado nao pode ser alterado para FATURADO na entrega.");
            }
            cliente.adicionarDebito(pedido.getValorTotal());
            clienteRepository.save(cliente);
            if (cliente.getSaldoDevedor().compareTo(cliente.getLimiteCredito()) > 0) {
                log.warn("Cliente {} ultrapassou o limite de credito apos ajuste de caucao. Saldo: R$ {}.",
                        cliente.getId(), cliente.getSaldoDevedor().toPlainString());
            }
        } else if (!pedido.isPagamentoConfirmado()) {
            confirmarPagamentoDoPedido(pedido, "Pagamento recebido na entrega");
        }

        if (request.observacao() != null && !request.observacao().isBlank()) {
            String anterior = pedido.getObservacao() == null || pedido.getObservacao().isBlank()
                    ? ""
                    : pedido.getObservacao() + " | ";
            pedido.setObservacao(anterior + "Entrega: " + request.observacao().trim());
        }

        pedido.setStatus(StatusPedido.ENTREGUE);
        pedido.setEntregueEm(LocalDateTime.now());

        vasilhameRepository.saveAll(alterados);
        pedidoRepository.save(pedido);

        return PedidoResponse.from(pedido);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private void montarItens(Pedido pedido, List<ItemPedidoRequest> itens, Cliente cliente,
                             boolean verificarEstoque) {
        LocalDate hoje = LocalDate.now();
        Set<Long> produtos = new HashSet<>();

        for (ItemPedidoRequest itemRequest : itens) {
            Produto produto = produtoAtivo(itemRequest.produtoId());
            if (!produtos.add(produto.getId())) {
                throw new RegraDeNegocioException(
                        "O produto " + produto.getNome() + " aparece mais de uma vez no pedido.");
            }
            if (itemRequest.vasilhamesDevolvidos() > itemRequest.quantidade()) {
                throw new RegraDeNegocioException(
                        "A quantidade de vasilhames devolvidos nao pode ser maior que a quantidade de garrafoes do item.");
            }

            long disponiveisTroca = vasilhameRepository
                    .countByClienteIdAndProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
                            cliente.getId(), produto.getId(), StatusVasilhame.COM_CLIENTE, hoje);
            if (itemRequest.vasilhamesDevolvidos() > disponiveisTroca) {
                throw new RegraDeNegocioException("Voce informou " + itemRequest.vasilhamesDevolvidos()
                        + " vasilhames para troca do produto " + produto.getNome()
                        + ", mas possui apenas " + disponiveisTroca + " vasilhame(s) valido(s) em posse.");
            }

            if (verificarEstoque) {
                long estoqueCentral = vasilhameRepository
                        .countByProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
                                produto.getId(), StatusVasilhame.NA_CENTRAL, hoje);
                if (estoqueCentral < itemRequest.quantidade()) {
                    throw new EstoqueInsuficienteException("Estoque insuficiente para "
                            + produto.getNome() + ". Solicitado: " + itemRequest.quantidade()
                            + ", disponivel: " + estoqueCentral + ".");
                }
            }

            pedido.adicionarItem(new ItemPedido(produto, itemRequest.quantidade(),
                    itemRequest.vasilhamesDevolvidos()));
        }
    }

    private void verificarCredito(Cliente cliente, BigDecimal valorTotal) {
        BigDecimal emAberto = valorEmAberto(cliente.getId());
        if (!cliente.permiteCompra(valorTotal, emAberto)) {
            throw new CreditoExcedidoException(
                    "RN01: pedido bloqueado por limite de credito. Limite: R$ "
                            + cliente.getLimiteCredito().toPlainString()
                            + ", saldo devedor: R$ " + cliente.getSaldoDevedor().toPlainString()
                            + ", pedidos em aberto: R$ " + emAberto.toPlainString()
                            + ", total deste pedido: R$ " + valorTotal.toPlainString() + ".");
        }
    }

    private BigDecimal valorEmAberto(Long clienteId) {
        BigDecimal valor = pedidoRepository.somarValorPorStatus(clienteId, STATUS_EM_ABERTO);
        return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarClienteParaPedido(Cliente cliente) {
        if (!cliente.isAtivo()) {
            throw new RegraDeNegocioException("O cadastro do cliente esta inativo.");
        }
        var endereco = cliente.getEndereco();
        if (endereco == null
                || vazio(endereco.getLogradouro())
                || vazio(endereco.getNumero())
                || vazio(endereco.getBairro())
                || vazio(endereco.getCidade())
                || vazio(endereco.getUf())
                || vazio(endereco.getCep())
                || endereco.getZona() == null) {
            throw new RegraDeNegocioException(
                    "O cliente precisa ter um endereco completo com zona cadastrado antes de realizar pedidos.");
        }
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private void validarFormaPagamento(Cliente cliente, FormaPagamento formaPagamento) {
        if (formaPagamento == FormaPagamento.FATURADO && !cliente.isCorporativo()) {
            throw new RegraDeNegocioException(
                    "A forma de pagamento FATURADO e exclusiva para clientes corporativos.");
        }
    }

    private void validarEntregadorDoPedido(Pedido pedido, Entregador entregador) {
        if (pedido.getEntregador() == null
                || !pedido.getEntregador().getId().equals(entregador.getId())) {
            throw new AcessoNegadoException("Este pedido nao esta atribuido ao entregador autenticado.");
        }
    }

    private Produto produtoAtivo(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto ativo nao encontrado: " + id));
    }

    private Cliente clientePorEmail(String email) {
        return clienteRepository.findByUsuarioEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente autenticado nao encontrado."));
    }

    private Entregador entregadorPorEmail(String email) {
        return entregadorRepository.findByUsuarioEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entregador autenticado nao encontrado."));
    }

    private Usuario usuarioPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado."));
    }

    private Pedido porId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido nao encontrado: " + id));
    }
}
