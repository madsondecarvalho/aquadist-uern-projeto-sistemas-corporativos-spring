package com.aquadist.config;

import com.aquadist.model.Cliente;
import com.aquadist.model.Endereco;
import com.aquadist.model.Entregador;
import com.aquadist.model.PerfilUsuario;
import com.aquadist.model.Produto;
import com.aquadist.model.TipoCliente;
import com.aquadist.model.Usuario;
import com.aquadist.model.Vasilhame;
import com.aquadist.model.Zona;
import com.aquadist.repository.ClienteRepository;
import com.aquadist.repository.EntregadorRepository;
import com.aquadist.repository.ProdutoRepository;
import com.aquadist.repository.UsuarioRepository;
import com.aquadist.repository.VasilhameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EntregadorRepository entregadorRepository;
    private final ProdutoRepository produtoRepository;
    private final VasilhameRepository vasilhameRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Integer> contadores = new HashMap<>();

    public DataInitializer(UsuarioRepository usuarioRepository,
                           ClienteRepository clienteRepository,
                           EntregadorRepository entregadorRepository,
                           ProdutoRepository produtoRepository,
                           VasilhameRepository vasilhameRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.entregadorRepository = entregadorRepository;
        this.produtoRepository = produtoRepository;
        this.vasilhameRepository = vasilhameRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("Base de dados ja possui dados. Seed ignorado.");
            return;
        }

        log.info("Criando dados de demonstracao da AquaDist...");

        Usuario admin = usuario("Administrador AquaDist", "admin@aquadist.com",
                "admin123", PerfilUsuario.ADMIN);
        usuarioRepository.save(admin);

        Usuario usuarioMercado = usuario("Mercado Bom Preco", "mercado@aquadist.com",
                "cliente123", PerfilUsuario.CLIENTE);
        usuarioRepository.save(usuarioMercado);
        Cliente mercado = new Cliente(
                usuarioMercado,
                TipoCliente.CORPORATIVO,
                "12.345.678/0001-90",
                "(11) 3333-1000",
                new Endereco("Avenida Central", "500", "Loja 2", "Centro",
                        "Sao Paulo", "SP", "01000-000", Zona.CENTRO),
                new BigDecimal("2000.00")
        );
        mercado.setSaldoDevedor(new BigDecimal("300.00"));
        clienteRepository.save(mercado);

        Usuario usuarioMaria = usuario("Maria Silva", "maria@email.com",
                "cliente123", PerfilUsuario.CLIENTE);
        usuarioRepository.save(usuarioMaria);
        Cliente maria = new Cliente(
                usuarioMaria,
                TipoCliente.RESIDENCIAL,
                "123.456.789-00",
                "(11) 99999-2000",
                new Endereco("Rua das Flores", "120", "Apto 34", "Vila Mariana",
                        "Sao Paulo", "SP", "04100-000", Zona.SUL),
                BigDecimal.ZERO
        );
        clienteRepository.save(maria);

        Usuario usuarioJoao = usuario("Joao Pereira", "joao@aquadist.com",
                "entregador123", PerfilUsuario.ENTREGADOR);
        usuarioRepository.save(usuarioJoao);
        Entregador joao = new Entregador(usuarioJoao, "(11) 98888-1000",
                Zona.CENTRO, 60, "Fiat Fiorino", "ABC1D23");
        entregadorRepository.save(joao);

        Usuario usuarioAna = usuario("Ana Souza", "ana@aquadist.com",
                "entregador123", PerfilUsuario.ENTREGADOR);
        usuarioRepository.save(usuarioAna);
        Entregador ana = new Entregador(usuarioAna, "(11) 98888-2000",
                Zona.SUL, 50, "VW Saveiro", "DEF4G56");
        entregadorRepository.save(ana);

        Produto g20 = produto("G20", "Garrafao 20L", 20, "12.00", "25.00");
        Produto g10 = produto("G10", "Garrafao 10L", 10, "8.00", "20.00");
        Produto g5 = produto("G5", "Garrafao 5L", 5, "6.00", "15.00");
        produtoRepository.saveAll(List.of(g20, g10, g5));

        LocalDate hoje = LocalDate.now();

        // Estoque central (vasilhames cheios e disponiveis para rota).
        criarLote(g20, 60, hoje.plusDays(365), null);
        criarLote(g10, 40, hoje.plusDays(300), null);
        criarLote(g5, 20, hoje.plusDays(240), null);

        // Vasilhames em posse dos clientes (para demonstracao da RN02 e RN05).
        criarLote(g20, 8, hoje.plusDays(270), maria);
        criarLote(g20, 2, hoje.minusDays(5), maria);   // vencidos
        criarLote(g10, 3, hoje.plusDays(200), maria);

        criarLote(g20, 10, hoje.plusDays(300), mercado);
        criarLote(g10, 2, hoje.minusDays(20), mercado); // vencidos

        log.info("Seed concluido. Base em ./data/aquadist.db");
        log.info("Usuarios de demonstracao:");
        log.info("  ADMIN       admin@aquadist.com / admin123");
        log.info("  CLIENTE     mercado@aquadist.com / cliente123 (corporativo, limite R$ 2.000,00)");
        log.info("  CLIENTE     maria@email.com / cliente123 (residencial)");
        log.info("  ENTREGADOR  joao@aquadist.com / entregador123 (zona CENTRO)");
        log.info("  ENTREGADOR  ana@aquadist.com / entregador123 (zona SUL)");
    }

    private Usuario usuario(String nome, String email, String senha, PerfilUsuario perfil) {
        return new Usuario(nome, email.toLowerCase(Locale.ROOT),
                passwordEncoder.encode(senha), perfil);
    }

    private Produto produto(String codigo, String nome, int volume, String preco, String caucao) {
        return new Produto(codigo, nome, volume, new BigDecimal(preco), new BigDecimal(caucao));
    }

    private void criarLote(Produto produto, int quantidade, LocalDate validade, Cliente cliente) {
        List<Vasilhame> lote = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            Vasilhame vasilhame = new Vasilhame(proximoCodigo(produto), produto, validade);
            if (cliente != null) {
                vasilhame.entregarAoCliente(cliente);
            }
            lote.add(vasilhame);
        }
        vasilhameRepository.saveAll(lote);
    }

    private String proximoCodigo(Produto produto) {
        int numero = contadores.merge(produto.getCodigo(), 1, Integer::sum);
        return String.format("%s-%04d", produto.getCodigo(), numero);
    }
}
