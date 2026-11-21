package com.aquadist.service;

import com.aquadist.dto.ClienteResponse;
import com.aquadist.dto.CriarClienteRequest;
import com.aquadist.dto.PagamentoRequest;
import com.aquadist.dto.PagamentoResponse;
import com.aquadist.dto.VasilhamesClienteResponse;
import com.aquadist.exception.RecursoNaoEncontradoException;
import com.aquadist.exception.RegraDeNegocioException;
import com.aquadist.model.Cliente;
import com.aquadist.model.Pagamento;
import com.aquadist.model.PerfilUsuario;
import com.aquadist.model.TipoCliente;
import com.aquadist.model.Usuario;
import com.aquadist.repository.ClienteRepository;
import com.aquadist.repository.PagamentoRepository;
import com.aquadist.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EstoqueService estoqueService;

    public ClienteService(ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          PagamentoRepository pagamentoRepository,
                          PasswordEncoder passwordEncoder,
                          EstoqueService estoqueService) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.passwordEncoder = passwordEncoder;
        this.estoqueService = estoqueService;
    }

    @Transactional
    public ClienteResponse criar(CriarClienteRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("Ja existe um usuario com este e-mail.");
        }
        if (clienteRepository.existsByDocumento(request.documento().trim())) {
            throw new RegraDeNegocioException("Ja existe um cliente com este documento.");
        }
        if (request.tipo() == TipoCliente.CORPORATIVO
                && (request.limiteCredito() == null
                || request.limiteCredito().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new RegraDeNegocioException("Clientes corporativos exigem limite de credito maior que zero.");
        }

        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                PerfilUsuario.CLIENTE
        );
        usuarioRepository.save(usuario);

        BigDecimal limite = request.tipo() == TipoCliente.CORPORATIVO
                ? request.limiteCredito().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Cliente cliente = new Cliente(
                usuario,
                request.tipo(),
                request.documento().trim(),
                request.telefone(),
                request.endereco().toEmbeddable(),
                limite
        );
        clienteRepository.save(cliente);
        return ClienteResponse.from(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream()
                .sorted(Comparator.comparing(cliente -> cliente.getUsuario().getNome()))
                .map(ClienteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse me(String email) {
        return ClienteResponse.from(carregarPorEmail(email));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return ClienteResponse.from(carregar(id));
    }

    @Transactional(readOnly = true)
    public VasilhamesClienteResponse vasilhames(String email) {
        return estoqueService.vasilhamesDoCliente(carregarPorEmail(email));
    }

    @Transactional(readOnly = true)
    public VasilhamesClienteResponse vasilhamesPorId(Long clienteId) {
        return estoqueService.vasilhamesDoCliente(carregar(clienteId));
    }

    @Transactional
    public ClienteResponse registrarPagamento(Long clienteId, PagamentoRequest request) {
        Cliente cliente = carregar(clienteId);
        BigDecimal valor = request.valor().setScale(2, RoundingMode.HALF_UP);

        if (cliente.getSaldoDevedor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O cliente nao possui saldo devedor em aberto.");
        }
        if (valor.compareTo(cliente.getSaldoDevedor()) > 0) {
            throw new RegraDeNegocioException("O valor do pagamento excede o saldo devedor de R$ "
                    + cliente.getSaldoDevedor().toPlainString() + ".");
        }

        Pagamento pagamento = new Pagamento(cliente, valor,
                request.descricao() == null || request.descricao().isBlank()
                        ? "Pagamento registrado pelo administrador"
                        : request.descricao().trim());
        pagamentoRepository.save(pagamento);
        cliente.registrarPagamento(valor);
        clienteRepository.save(cliente);

        return ClienteResponse.from(cliente);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> pagamentos(Long clienteId) {
        carregar(clienteId);
        return pagamentoRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(PagamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Cliente carregarPorEmail(String email) {
        return clienteRepository.findByUsuarioEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente autenticado nao encontrado."));
    }

    @Transactional(readOnly = true)
    public Cliente carregar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado: " + id));
    }
}
