package com.aquadist.unit;

import com.aquadist.model.Produto;
import com.aquadist.model.StatusVasilhame;
import com.aquadist.model.Vasilhame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN05 - Validade do vasilhame")
class VasilhameValidadeTest {

    private final Produto produto = new Produto("G20", "Garrafao 20L", 20,
            new BigDecimal("12.00"), new BigDecimal("25.00"));

    @Test
    @DisplayName("identifica vasilhame vencido na data de referencia")
    void identificaVencido() {
        LocalDate hoje = LocalDate.of(2026, 11, 21);

        assertThat(new Vasilhame("V1", produto, hoje.minusDays(1)).estaVencido(hoje)).isTrue();
        assertThat(new Vasilhame("V2", produto, hoje).estaVencido(hoje)).isFalse();
        assertThat(new Vasilhame("V3", produto, hoje.plusDays(1)).estaVencido(hoje)).isFalse();
    }

    @Test
    @DisplayName("apenas vasilhame valido em posse do cliente pode ser usado na troca simples")
    void disponibilidadeParaTroca() {
        LocalDate hoje = LocalDate.of(2026, 11, 21);
        Vasilhame valido = new Vasilhame("V1", produto, hoje.plusDays(10));
        Vasilhame vencido = new Vasilhame("V2", produto, hoje.minusDays(10));

        valido.retornarACentral();
        assertThat(valido.disponivelParaTroca(hoje)).isFalse();

        valido.entregarAoCliente(null);
        assertThat(valido.disponivelParaTroca(hoje)).isTrue();

        vencido.entregarAoCliente(null);
        assertThat(vencido.disponivelParaTroca(hoje)).isFalse();
    }

    @Test
    @DisplayName("transicoes de estoque atualizam status e vinculos")
    void transicoesDeStatus() {
        Vasilhame vasilhame = new Vasilhame("V1", produto, LocalDate.now().plusDays(30));

        assertThat(vasilhame.getStatus()).isEqualTo(StatusVasilhame.NA_CENTRAL);

        vasilhame.carregarParaTransito(null, null);
        assertThat(vasilhame.getStatus()).isEqualTo(StatusVasilhame.EM_TRANSITO);

        vasilhame.entregarAoCliente(null);
        assertThat(vasilhame.getStatus()).isEqualTo(StatusVasilhame.COM_CLIENTE);

        vasilhame.retornarACentral();
        assertThat(vasilhame.getStatus()).isEqualTo(StatusVasilhame.NA_CENTRAL);

        vasilhame.descartar();
        assertThat(vasilhame.getStatus()).isEqualTo(StatusVasilhame.DESCARTADO);
    }
}
