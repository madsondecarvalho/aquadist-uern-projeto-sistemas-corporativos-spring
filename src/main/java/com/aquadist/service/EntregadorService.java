package com.aquadist.service;

import com.aquadist.dto.CriarEntregadorRequest;
import com.aquadist.dto.EntregadorResponse;
import com.aquadist.dto.EstoqueEntregadorResponse;
import com.aquadist.dto.PedidoResponse;
import com.aquadist.dto.VasilhameResponse;
import com.aquadist.exception.AcessoNegadoException;
import com.aquadist.exception.OperacaoNaoPermitidaException;
import com.aquadist.exception.RecursoNaoEncontradoException;
import com.aquadist.exception.RegraDeNegocioException;
import com.aquadist.model.Entregador;
import com.aquadist.model.Pedido;
import com.aquadist.model.PerfilUsuario;
import com.aquadist.model.StatusPedido;
import com.aquadist.model.Usuario;
import com.aquadist.repository.EntregadorRepository;
import com.aquadist.repository.PedidoRepository;
import com.aquadist.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class EntregadorService {

    private static final List<StatusPedido> STATUS_ROTA =
            List.of(StatusPedido.ATRIBUIDO, StatusPedido.EM_TRANSITO);

    private final EntregadorRepository entregadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final EstoqueService estoqueService;
    private final PasswordEncoder passwordEncoder;

    public EntregadorService(EntregadorRepository entregadorRepository,
                             UsuarioRepository usuarioRepository,
                             PedidoRepository pedidoRepository,
                             EstoqueService estoqueService,
                             PasswordEncoder passwordEncoder) {
        this.entregadorRepository = entregadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.estoqueService = estoqueService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EntregadorResponse criar(CriarEntregadorRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("Ja existe um usuario com este e-mail.");
        }

        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                PerfilUsuario.ENTREGADOR
        );
        usuarioRepository.save(usuario);

        Entregador entregador = new Entregador(
                usuario,
                request.telefone(),
                request.zona(),
                request.capacidadeCarga(),
                request.veiculoModelo(),
                request.veiculoPlaca()
        );
        entregadorRepository.save(entregador);
        return EntregadorResponse.from(entregador, 0);
    }

    @Transactional(readOnly = true)
    public List<EntregadorResponse> listar() {
        return entregadorRepository.findAll().stream()
                .sorted(Comparator.comparing(entregador -> entregador.getUsuario().getNome()))
                .map(entregador -> EntregadorResponse.from(entregador, cargaAtual(entregador.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EntregadorResponse me(String email) {
        Entregador entregador = carregarPorEmail(email);
        return EntregadorResponse.from(entregador, cargaAtual(entregador.getId()));
    }

    @Transactional(readOnly = true)
    public EntregadorResponse buscar(Long id) {
        Entregador entregador = carregar(id);
        return EntregadorResponse.from(entregador, cargaAtual(entregador.getId()));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> pedidos(String email) {
        Entregador entregador = carregarPorEmail(email);
        return pedidoRepository.findByEntregadorIdAndStatusInOrderByCriadoEmAsc(
                        entregador.getId(), STATUS_ROTA)
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstoqueEntregadorResponse estoque(String email) {
        return estoqueService.estoqueEntregador(carregarPorEmail(email));
    }

    @Transactional(readOnly = true)
    public List<VasilhameResponse> vasilhamesClienteDoPedido(Long pedidoId, String email) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido nao encontrado: " + pedidoId));
        Entregador entregador = carregarPorEmail(email);

        if (pedido.getEntregador() == null
                || !pedido.getEntregador().getId().equals(entregador.getId())) {
            throw new AcessoNegadoException("Este pedido nao esta atribuido a voce.");
        }
        if (pedido.getStatus() != StatusPedido.ATRIBUIDO && pedido.getStatus() != StatusPedido.EM_TRANSITO) {
            throw new OperacaoNaoPermitidaException(
                    "A lista de vasilhames do cliente so esta disponivel para pedidos em rota.");
        }

        return estoqueService.vasilhamesDoClienteParaPedido(pedido);
    }

    @Transactional(readOnly = true)
    public Entregador carregarPorEmail(String email) {
        return entregadorRepository.findByUsuarioEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entregador autenticado nao encontrado."));
    }

    @Transactional(readOnly = true)
    public Entregador carregar(Long id) {
        return entregadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entregador nao encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public long cargaAtual(Long entregadorId) {
        Long carga = pedidoRepository.somarGarrafoesPorEntregador(entregadorId, STATUS_ROTA);
        return carga == null ? 0 : carga;
    }
}
