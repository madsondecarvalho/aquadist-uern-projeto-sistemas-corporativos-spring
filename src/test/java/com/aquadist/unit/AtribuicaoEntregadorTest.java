package com.aquadist.unit;

import com.aquadist.model.Cliente;
import com.aquadist.model.Endereco;
import com.aquadist.model.Entregador;
import com.aquadist.model.FormaPagamento;
import com.aquadist.model.ItemPedido;
import com.aquadist.model.Pedido;
import com.aquadist.model.Produto;
import com.aquadist.model.StatusPedido;
import com.aquadist.model.TipoCliente;
import com.aquadist.model.Usuario;
import com.aquadist.model.Zona;
import com.aquadist.repository.EntregadorRepository;
import com.aquadist.repository.PedidoRepository;
import com.aquadist.service.AtribuicaoEntregadorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RN03 - Atribuicao automatica de entregador")
class AtribuicaoEntregadorTest {

    @Mock
    private EntregadorRepository entregadorRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    private AtribuicaoEntregadorService service;
    private Produto produto;

    @BeforeEach
    void setUp() {
        service = new AtribuicaoEntregadorService(entregadorRepository, pedidoRepository);
        produto = new Produto("G20", "Garrafao 20L", 20, new BigDecimal("12.00"), new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("prioriza entregador da mesma zona mesmo que outro tenha mais capacidade")
    void priorizaMesmaZona() {
        Entregador ana = entregador(1L, "Ana", Zona.SUL, 20);
        Entregador joao = entregador(2L, "Joao", Zona.CENTRO, 100);
        Pedido pedido = pedido(Zona.SUL, 5);

        when(entregadorRepository.findByAtivoTrueAndZona(Zona.SUL)).thenReturn(List.of(ana));
        when(pedidoRepository.somarGarrafoesPorEntregador(eq(1L), anyList())).thenReturn(0L);

        assertThat(service.selecionar(pedido)).contains(ana);
    }

    @Test
    @DisplayName("usa outra zona quando o entregador da zona nao tem capacidade")
    void fallbackParaOutraZona() {
        Entregador ana = entregador(1L, "Ana", Zona.SUL, 4);
        Entregador joao = entregador(2L, "Joao", Zona.CENTRO, 20);
        Pedido pedido = pedido(Zona.SUL, 5);

        when(entregadorRepository.findByAtivoTrueAndZona(Zona.SUL)).thenReturn(List.of(ana));
        when(entregadorRepository.findByAtivoTrue()).thenReturn(List.of(ana, joao));
        when(pedidoRepository.somarGarrafoesPorEntregador(eq(1L), anyList())).thenReturn(0L);
        when(pedidoRepository.somarGarrafoesPorEntregador(eq(2L), anyList())).thenReturn(0L);

        assertThat(service.selecionar(pedido)).contains(joao);
    }

    @Test
    @DisplayName("nao atribui quando todos os entregadores estao com carga cheia")
    void semEntregadorDisponivel() {
        Entregador ana = entregador(1L, "Ana", Zona.SUL, 4);
        Entregador joao = entregador(2L, "Joao", Zona.CENTRO, 4);
        Pedido pedido = pedido(Zona.SUL, 5);

        when(entregadorRepository.findByAtivoTrueAndZona(Zona.SUL)).thenReturn(List.of(ana));
        when(entregadorRepository.findByAtivoTrue()).thenReturn(List.of(ana, joao));
        when(pedidoRepository.somarGarrafoesPorEntregador(eq(1L), anyList())).thenReturn(4L);
        when(pedidoRepository.somarGarrafoesPorEntregador(eq(2L), anyList())).thenReturn(4L);

        assertThat(service.selecionar(pedido)).isEmpty();
    }

    @Test
    @DisplayName("capacidade considera a carga atual do entregador")
    void validaCapacidadeComCargaAtual() {
        Entregador ana = entregador(1L, "Ana", Zona.SUL, 10);
        when(pedidoRepository.somarGarrafoesPorEntregador(eq(1L), anyList())).thenReturn(6L);

        assertThat(service.temCapacidade(ana, 4)).isTrue();
        assertThat(service.temCapacidade(ana, 5)).isFalse();
    }

    @Test
    @DisplayName("atribuicao altera o pedido para ATRIBUIDO e grava o entregador")
    void atribuiPedidoPendente() {
        Entregador ana = entregador(1L, "Ana", Zona.SUL, 20);
        Pedido pedido = pedido(Zona.SUL, 3);
        pedido.setId(10L);

        when(pedidoRepository.findById(10L)).thenReturn(java.util.Optional.of(pedido));
        when(entregadorRepository.findByAtivoTrueAndZona(Zona.SUL)).thenReturn(List.of(ana));
        when(pedidoRepository.somarGarrafoesPorEntregador(eq(1L), anyList())).thenReturn(0L);

        service.tentarAtribuir(10L);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ATRIBUIDO);
        assertThat(pedido.getEntregador()).isEqualTo(ana);
        verify(pedidoRepository).save(pedido);
    }

    private Entregador entregador(Long id, String nome, Zona zona, int capacidade) {
        Usuario usuario = new Usuario(nome, nome.toLowerCase() + "@aquadist.com", "senha", null);
        Entregador entregador = new Entregador(usuario, null, zona, capacidade, "Fiorino", "ABC1D23");
        entregador.setId(id);
        return entregador;
    }

    private Pedido pedido(Zona zona, int quantidade) {
        Endereco endereco = new Endereco("Rua A", "10", null, "Bairro",
                "Sao Paulo", "SP", "01000-000", zona);
        Cliente cliente = new Cliente(
                new Usuario("Maria", "maria@teste.com", "senha", null),
                TipoCliente.RESIDENCIAL,
                "123",
                null,
                endereco,
                BigDecimal.ZERO
        );
        Pedido pedido = new Pedido(cliente, endereco, FormaPagamento.PIX, null);
        pedido.adicionarItem(new ItemPedido(produto, quantidade, 0));
        return pedido;
    }
}
