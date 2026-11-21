package com.aquadist.service;

import com.aquadist.model.Entregador;
import com.aquadist.model.Pedido;
import com.aquadist.model.StatusPedido;
import com.aquadist.repository.EntregadorRepository;
import com.aquadist.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * RN03: prioriza entregadores da mesma zona e com capacidade de carga.
 * Caso nenhum entregador da zona esteja disponivel, avalia as demais zonas
 * (fallback), mantendo a prioridade por menor carga.
 */
@Service
public class AtribuicaoEntregadorService {

    private static final Logger log = LoggerFactory.getLogger(AtribuicaoEntregadorService.class);
    private static final List<StatusPedido> STATUS_ROTA =
            List.of(StatusPedido.ATRIBUIDO, StatusPedido.EM_TRANSITO);

    private final EntregadorRepository entregadorRepository;
    private final PedidoRepository pedidoRepository;

    public AtribuicaoEntregadorService(EntregadorRepository entregadorRepository,
                                       PedidoRepository pedidoRepository) {
        this.entregadorRepository = entregadorRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public Optional<Entregador> selecionar(Pedido pedido) {
        int garrafoes = pedido.totalGarrafoes();
        if (garrafoes <= 0) {
            return Optional.empty();
        }

        List<Entregador> candidatosZona = new ArrayList<>(
                pedido.getZonaEntrega() == null
                        ? entregadorRepository.findByAtivoTrue()
                        : entregadorRepository.findByAtivoTrueAndZona(pedido.getZonaEntrega()));

        Optional<Entregador> mesmoBairro = escolherComCapacidade(candidatosZona, garrafoes);
        if (mesmoBairro.isPresent()) {
            return mesmoBairro;
        }

        List<Entregador> todos = entregadorRepository.findByAtivoTrue();
        return escolherComCapacidade(todos, garrafoes);
    }

    private Optional<Entregador> escolherComCapacidade(List<Entregador> candidatos, int garrafoes) {
        return candidatos.stream()
                .filter(entregador -> temCapacidade(entregador, garrafoes))
                .sorted(Comparator.comparingLong((Entregador e) -> cargaAtual(e.getId()))
                        .thenComparing(Entregador::getId))
                .findFirst();
    }

    public boolean temCapacidade(Entregador entregador, int garrafoes) {
        return cargaAtual(entregador.getId()) + garrafoes <= entregador.getCapacidadeCarga();
    }

    public long cargaAtual(Long entregadorId) {
        Long carga = pedidoRepository.somarGarrafoesPorEntregador(entregadorId, STATUS_ROTA);
        return carga == null ? 0 : carga;
    }

    public void tentarAtribuir(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getStatus() != StatusPedido.PENDENTE) {
            return;
        }

        Optional<Entregador> escolhido = selecionar(pedido);
        if (escolhido.isEmpty()) {
            log.warn("Pedido {} ficou PENDENTE: nenhum entregador com capacidade disponivel.", pedidoId);
            return;
        }

        Entregador entregador = escolhido.get();
        pedido.setEntregador(entregador);
        pedido.setStatus(StatusPedido.ATRIBUIDO);
        pedidoRepository.save(pedido);
        log.info("Pedido {} atribuido automaticamente ao entregador {} (zona {}).",
                pedidoId, entregador.getUsuario().getNome(), entregador.getZona());
    }
}
