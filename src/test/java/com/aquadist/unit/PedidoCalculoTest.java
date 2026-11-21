package com.aquadist.unit;

import com.aquadist.model.Cliente;
import com.aquadist.model.Endereco;
import com.aquadist.model.FormaPagamento;
import com.aquadist.model.ItemPedido;
import com.aquadist.model.Pedido;
import com.aquadist.model.Produto;
import com.aquadist.model.StatusPedido;
import com.aquadist.model.TipoCliente;
import com.aquadist.model.Usuario;
import com.aquadist.model.Zona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN02 - Calculo de caução e totais do pedido")
class PedidoCalculoTest {

    @Test
    @DisplayName("cobra caução apenas pelos vasilhames faltantes")
    void calculaCaucaoPorVasilhamesFaltantes() {
        Produto produto = produto("G20", "Garrafao 20L", "12.00", "25.00");

        ItemPedido item = new ItemPedido(produto, 4, 2);

        assertThat(item.getSubtotal()).isEqualByComparingTo("48.00");
        assertThat(item.getQuantidadeCaucao()).isEqualTo(2);
        assertThat(item.getValorCaucao()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("zera a caução quando todos os vasilhames são devolvidos")
    void semCaucaoQuandoTrocaCompleta() {
        ItemPedido item = new ItemPedido(produto("G20", "Garrafao 20L", "12.00", "25.00"), 4, 4);

        assertThat(item.getValorCaucao()).isEqualByComparingTo("0.00");
        assertThat(item.getQuantidadeCaucao()).isZero();
    }

    @Test
    @DisplayName("recalcula a caução após a conferência dos vasilhames na entrega")
    void recalculaAposConferencia() {
        ItemPedido item = new ItemPedido(produto("G20", "Garrafao 20L", "12.00", "25.00"), 4, 0);
        assertThat(item.getValorCaucao()).isEqualByComparingTo("100.00");

        item.recalcular(3);
        assertThat(item.getQuantidadeCaucao()).isEqualTo(1);
        assertThat(item.getValorCaucao()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("soma produtos e cauções de itens distintos no total do pedido")
    void somaTotaisDoPedido() {
        Pedido pedido = new Pedido(cliente(), endereco(), FormaPagamento.PIX, null);
        pedido.adicionarItem(new ItemPedido(produto("G20", "Garrafao 20L", "12.00", "25.00"), 4, 2));
        pedido.adicionarItem(new ItemPedido(produto("G10", "Garrafao 10L", "8.00", "20.00"), 2, 0));

        pedido.recalcularTotais();

        assertThat(pedido.getValorProdutos()).isEqualByComparingTo("64.00");
        assertThat(pedido.getValorCaucao()).isEqualByComparingTo("90.00");
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("154.00");
        assertThat(pedido.totalGarrafoes()).isEqualTo(6);
    }

    @Test
    @DisplayName("RN04 - status anteriores ao despacho permitem cancelamento")
    void statusPermitemCancelamentoAteAtribuido() {
        assertThat(StatusPedido.PENDENTE.podeCancelar()).isTrue();
        assertThat(StatusPedido.ATRIBUIDO.podeCancelar()).isTrue();
        assertThat(StatusPedido.EM_TRANSITO.podeCancelar()).isFalse();
        assertThat(StatusPedido.ENTREGUE.podeCancelar()).isFalse();
        assertThat(StatusPedido.CANCELADO.podeCancelar()).isFalse();
    }

    private Produto produto(String codigo, String nome, String preco, String caucao) {
        return new Produto(codigo, nome, 20, new BigDecimal(preco), new BigDecimal(caucao));
    }

    private Cliente cliente() {
        Usuario usuario = new Usuario("Maria", "maria@teste.com", "senha", null);
        return new Cliente(usuario, TipoCliente.RESIDENCIAL, "123",
                "(11) 99999-0000", endereco(), BigDecimal.ZERO);
    }

    private Endereco endereco() {
        return new Endereco("Rua A", "10", null, "Centro",
                "Sao Paulo", "SP", "01000-000", Zona.SUL);
    }
}
