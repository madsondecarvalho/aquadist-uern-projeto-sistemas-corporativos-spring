package com.aquadist.integration;

import com.aquadist.dto.CancelarPedidoRequest;
import com.aquadist.dto.ConcluirEntregaRequest;
import com.aquadist.dto.CriarPedidoAdminRequest;
import com.aquadist.dto.CriarPedidoRequest;
import com.aquadist.dto.ItemPedidoRequest;
import com.aquadist.dto.PedidoResponse;
import com.aquadist.exception.CreditoExcedidoException;
import com.aquadist.exception.OperacaoNaoPermitidaException;
import com.aquadist.model.Cliente;
import com.aquadist.model.FormaPagamento;
import com.aquadist.model.Produto;
import com.aquadist.model.StatusPedido;
import com.aquadist.model.StatusVasilhame;
import com.aquadist.model.Vasilhame;
import com.aquadist.repository.ClienteRepository;
import com.aquadist.repository.PagamentoRepository;
import com.aquadist.repository.ProdutoRepository;
import com.aquadist.repository.VasilhameRepository;
import com.aquadist.service.PedidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integracao - regras de negocio de pedidos com H2 em memoria")
class PedidoServiceIntegrationTest {

    private static final String MARIA = "maria@email.com";
    private static final String MERCADO = "mercado@aquadist.com";
    private static final String ANA = "ana@aquadist.com";

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private VasilhameRepository vasilhameRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Test
    @DisplayName("RN02 + RN03 - caução de vasilhames faltantes e atribuição na mesma zona")
    void calculaCaucaoEAtribuiEntregadorDaZona() {
        Produto g20 = produto20();

        PedidoResponse pedido = pedidoService.criar(pedido(g20, 4, 2, FormaPagamento.PIX), MARIA);

        assertThat(pedido.valorProdutos()).isEqualByComparingTo("48.00");
        assertThat(pedido.valorCaucao()).isEqualByComparingTo("50.00");
        assertThat(pedido.valorTotal()).isEqualByComparingTo("98.00");
        assertThat(pedido.status()).isEqualTo(StatusPedido.ATRIBUIDO.name());
        assertThat(pedido.entregadorNome()).isEqualTo("Ana Souza");
    }

    @Test
    @DisplayName("RN01 - bloqueia pedido corporativo que ultrapassa o limite disponível")
    void bloqueiaPedidoAcimaDoLimite() {
        Cliente mercado = clienteRepository.findByUsuarioEmailIgnoreCase(MERCADO).orElseThrow();
        mercado.setLimiteCredito(new BigDecimal("100.00"));
        clienteRepository.save(mercado);

        Produto g20 = produto20();

        assertThatThrownBy(() -> pedidoService.criar(
                pedido(g20, 10, 0, FormaPagamento.FATURADO), MERCADO))
                .isInstanceOf(CreditoExcedidoException.class)
                .hasMessageContaining("RN01")
                .hasMessageContaining("limite de credito");
    }

    @Test
    @DisplayName("RN04 - permite cancelar antes do despacho e bloqueia depois de Em Trânsito")
    void bloqueiaCancelamentoDepoisDoDespacho() {
        Produto g20 = produto20();

        PedidoResponse paraCancelar = pedidoService.criar(
                pedido(g20, 2, 0, FormaPagamento.PIX), MARIA);
        PedidoResponse cancelado = pedidoService.cancelar(paraCancelar.id(), MARIA,
                new CancelarPedidoRequest("Desisti da compra"));
        assertThat(cancelado.status()).isEqualTo(StatusPedido.CANCELADO.name());

        PedidoResponse despachado = pedidoService.criar(
                pedido(g20, 2, 0, FormaPagamento.PIX), MARIA);
        pedidoService.iniciarTransito(despachado.id(), ANA);

        assertThatThrownBy(() -> pedidoService.cancelar(despachado.id(), MARIA,
                new CancelarPedidoRequest("Tentei cancelar em rota")))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("RN04");
    }

    @Test
    @DisplayName("RN05 - recusa vasilhame vencido, cobra caução e atualiza estoque")
    void recusaVasilhameVencidoNaTroca() {
        Cliente maria = clienteRepository.findByUsuarioEmailIgnoreCase(MARIA).orElseThrow();
        Produto g20 = produto20();
        LocalDate hoje = LocalDate.now();

        List<Vasilhame> emPosse = vasilhameRepository
                .findByClienteIdAndStatusOrderByDataValidadeAsc(maria.getId(), StatusVasilhame.COM_CLIENTE)
                .stream()
                .filter(v -> v.getProduto().getId().equals(g20.getId()))
                .toList();

        Vasilhame valido = emPosse.stream()
                .filter(v -> !v.estaVencido(hoje))
                .findFirst()
                .orElseThrow();
        Vasilhame vencido = emPosse.stream()
                .filter(v -> v.estaVencido(hoje))
                .findFirst()
                .orElseThrow();

        PedidoResponse pedido = pedidoService.criar(
                pedido(g20, 2, 0, FormaPagamento.PIX), MARIA);
        assertThat(pedido.valorCaucao()).isEqualByComparingTo("50.00");

        pedidoService.iniciarTransito(pedido.id(), ANA);

        PedidoResponse entregue = pedidoService.concluirEntrega(pedido.id(), ANA,
                new ConcluirEntregaRequest(
                        List.of(valido.getCodigo(), vencido.getCodigo()),
                        FormaPagamento.PIX,
                        "Conferido no local"));

        assertThat(entregue.status()).isEqualTo(StatusPedido.ENTREGUE.name());
        assertThat(entregue.itens().getFirst().vasilhamesValidosRecebidos()).isEqualTo(1);
        assertThat(entregue.itens().getFirst().vasilhamesRecusadosVencidos()).isEqualTo(1);
        assertThat(entregue.valorCaucao()).isEqualByComparingTo("25.00");
        assertThat(entregue.valorTotal()).isEqualByComparingTo("49.00");

        assertThat(vasilhameRepository.findById(vencido.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusVasilhame.DESCARTADO);
        assertThat(vasilhameRepository.findById(valido.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusVasilhame.NA_CENTRAL);
    }

    @Test
    @DisplayName("Admin cria pedido para cliente exigindo confirmacao de pagamento")
    void adminCriaPedidoComPagamentoConfirmado() {
        Cliente maria = clienteRepository.findByUsuarioEmailIgnoreCase(MARIA).orElseThrow();
        Produto g20 = produto20();

        CriarPedidoAdminRequest request = new CriarPedidoAdminRequest(
                maria.getId(),
                List.of(new ItemPedidoRequest(g20.getId(), 2, 0)),
                FormaPagamento.PIX,
                "Pedido criado no balcao",
                true
        );

        PedidoResponse pedido = pedidoService.criarComoAdmin(request);

        assertThat(pedido.status()).isEqualTo(StatusPedido.ATRIBUIDO.name());
        assertThat(pedido.valorTotal()).isEqualByComparingTo("74.00");
        assertThat(pedido.pagamentoConfirmado()).isTrue();
        assertThat(pedido.pagoEm()).isNotNull();
        assertThat(pagamentoRepository.findByPedidoId(pedido.id()))
                .singleElement()
                .satisfies(pagamento -> {
                    assertThat(pagamento.getValor()).isEqualByComparingTo("74.00");
                    assertThat(pagamento.getDescricao()).contains("pedido #" + pedido.id());
                });
    }

    @Test
    @DisplayName("Admin confirma pagamento de pedido existente e gera registro")
    void adminConfirmaPagamentoDePedidoExistente() {
        Produto g20 = produto20();
        PedidoResponse pedido = pedidoService.criar(
                pedido(g20, 2, 0, FormaPagamento.PIX), MARIA);

        assertThat(pedido.pagamentoConfirmado()).isFalse();

        PedidoResponse confirmado = pedidoService.confirmarPagamento(pedido.id());

        assertThat(confirmado.pagamentoConfirmado()).isTrue();
        assertThat(confirmado.pagoEm()).isNotNull();
        assertThat(pagamentoRepository.findByPedidoId(pedido.id()))
                .singleElement()
                .satisfies(pagamento -> assertThat(pagamento.getValor())
                        .isEqualByComparingTo("74.00"));
    }

    private CriarPedidoRequest pedido(Produto produto, int quantidade, int devolvidos,
                                      FormaPagamento formaPagamento) {
        return new CriarPedidoRequest(
                List.of(new ItemPedidoRequest(produto.getId(), quantidade, devolvidos)),
                formaPagamento,
                null
        );
    }

    private Produto produto20() {
        return produtoRepository.findByCodigoIgnoreCase("G20").orElseThrow();
    }
}
