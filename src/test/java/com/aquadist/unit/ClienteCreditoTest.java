package com.aquadist.unit;

import com.aquadist.model.Cliente;
import com.aquadist.model.Endereco;
import com.aquadist.model.TipoCliente;
import com.aquadist.model.Usuario;
import com.aquadist.model.Zona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN01 - Limite de credito do cliente corporativo")
class ClienteCreditoTest {

    @Test
    @DisplayName("permite compra enquanto saldo devedor + pedidos abertos + novo pedido cabe no limite")
    void permiteCompraDentroDoLimite() {
        Cliente cliente = corporativo(new BigDecimal("1000.00"));
        cliente.setSaldoDevedor(new BigDecimal("200.00"));

        assertThat(cliente.permiteCompra(new BigDecimal("300.00"), new BigDecimal("400.00"))).isTrue();
        assertThat(cliente.permiteCompra(new BigDecimal("400.00"), new BigDecimal("400.00"))).isTrue();
    }

    @Test
    @DisplayName("bloqueia compra que ultrapassa o limite considerado")
    void bloqueiaCompraAcimaDoLimite() {
        Cliente cliente = corporativo(new BigDecimal("1000.00"));
        cliente.setSaldoDevedor(new BigDecimal("200.00"));

        assertThat(cliente.permiteCompra(new BigDecimal("400.01"), new BigDecimal("400.00"))).isFalse();
    }

    @Test
    @DisplayName("cliente residencial nao possui bloqueio de credito")
    void residencialSemprePermite() {
        Cliente cliente = new Cliente(
                new Usuario("Maria", "maria@teste.com", "senha", null),
                TipoCliente.RESIDENCIAL,
                "123",
                null,
                endereco(),
                BigDecimal.ZERO
        );
        cliente.setSaldoDevedor(new BigDecimal("500.00"));

        assertThat(cliente.permiteCompra(new BigDecimal("9999.00"), new BigDecimal("9999.00"))).isTrue();
    }

    @Test
    @DisplayName("calcula limite disponivel sem ficar negativo")
    void calculaLimiteDisponivel() {
        Cliente cliente = corporativo(new BigDecimal("1000.00"));
        cliente.setSaldoDevedor(new BigDecimal("250.00"));
        assertThat(cliente.getLimiteDisponivel()).isEqualByComparingTo("750.00");

        cliente.setSaldoDevedor(new BigDecimal("1500.00"));
        assertThat(cliente.getLimiteDisponivel()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("pagamento reduz o saldo devedor e nunca fica negativo")
    void pagamentoReduzSaldo() {
        Cliente cliente = corporativo(new BigDecimal("1000.00"));
        cliente.setSaldoDevedor(new BigDecimal("300.00"));

        cliente.registrarPagamento(new BigDecimal("100.00"));
        assertThat(cliente.getSaldoDevedor()).isEqualByComparingTo("200.00");

        cliente.registrarPagamento(new BigDecimal("500.00"));
        assertThat(cliente.getSaldoDevedor()).isEqualByComparingTo("0.00");
    }

    private Cliente corporativo(BigDecimal limite) {
        return new Cliente(
                new Usuario("Mercado", "mercado@teste.com", "senha", null),
                TipoCliente.CORPORATIVO,
                "12.345.678/0001-90",
                null,
                endereco(),
                limite
        );
    }

    private Endereco endereco() {
        return new Endereco("Avenida Central", "500", null, "Centro",
                "Sao Paulo", "SP", "01000-000", Zona.CENTRO);
    }
}
