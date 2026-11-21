package com.aquadist.service;

import com.aquadist.dto.EstoqueCentralResponse;
import com.aquadist.dto.EstoqueEntregadorResponse;
import com.aquadist.dto.VasilhameResponse;
import com.aquadist.dto.VasilhamesClienteResponse;
import com.aquadist.model.Cliente;
import com.aquadist.model.Entregador;
import com.aquadist.model.Pedido;
import com.aquadist.model.Produto;
import com.aquadist.model.StatusPedido;
import com.aquadist.model.StatusVasilhame;
import com.aquadist.model.Vasilhame;
import com.aquadist.repository.PedidoRepository;
import com.aquadist.repository.ProdutoRepository;
import com.aquadist.repository.VasilhameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EstoqueService {

    private static final List<StatusPedido> STATUS_ROTA =
            List.of(StatusPedido.ATRIBUIDO, StatusPedido.EM_TRANSITO);

    private final ProdutoRepository produtoRepository;
    private final VasilhameRepository vasilhameRepository;
    private final PedidoRepository pedidoRepository;

    public EstoqueService(ProdutoRepository produtoRepository,
                          VasilhameRepository vasilhameRepository,
                          PedidoRepository pedidoRepository) {
        this.produtoRepository = produtoRepository;
        this.vasilhameRepository = vasilhameRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public EstoqueCentralResponse estoqueCentral() {
        LocalDate hoje = LocalDate.now();
        List<Produto> produtos = produtoRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Produto::getVolumeLitros).reversed())
                .toList();

        List<EstoqueCentralResponse.Item> itens = new ArrayList<>();
        long totalDisponiveis = 0;
        long totalEmTransito = 0;
        long totalComClientes = 0;
        long totalVencidos = 0;
        long totalDescartados = 0;

        for (Produto produto : produtos) {
            long disponiveis = contarEstoqueCentral(produto, hoje);
            long emTransito = vasilhameRepository.countByProdutoIdAndStatus(
                    produto.getId(), StatusVasilhame.EM_TRANSITO);
            long comClientes = vasilhameRepository.countByProdutoIdAndStatus(
                    produto.getId(), StatusVasilhame.COM_CLIENTE);
            long vencidos = vasilhameRepository.countByProdutoIdAndStatusAndDataValidadeLessThan(
                    produto.getId(), StatusVasilhame.NA_CENTRAL, hoje)
                    + vasilhameRepository.countByProdutoIdAndStatusAndDataValidadeLessThan(
                    produto.getId(), StatusVasilhame.EM_TRANSITO, hoje)
                    + vasilhameRepository.countByProdutoIdAndStatusAndDataValidadeLessThan(
                    produto.getId(), StatusVasilhame.COM_CLIENTE, hoje);
            long descartados = vasilhameRepository.countByProdutoIdAndStatus(
                    produto.getId(), StatusVasilhame.DESCARTADO);

            itens.add(new EstoqueCentralResponse.Item(
                    produto.getId(),
                    produto.getCodigo(),
                    produto.getNome(),
                    produto.getVolumeLitros(),
                    disponiveis,
                    emTransito,
                    comClientes,
                    vencidos,
                    descartados
            ));

            totalDisponiveis += disponiveis;
            totalEmTransito += emTransito;
            totalComClientes += comClientes;
            totalVencidos += vencidos;
            totalDescartados += descartados;
        }

        return new EstoqueCentralResponse(itens, totalDisponiveis, totalEmTransito,
                totalComClientes, totalVencidos, totalDescartados);
    }

    @Transactional(readOnly = true)
    public EstoqueEntregadorResponse estoqueEntregador(Entregador entregador) {
        LocalDate hoje = LocalDate.now();
        List<Vasilhame> vasilhames = vasilhameRepository.findByEntregadorIdAndStatus(
                        entregador.getId(), StatusVasilhame.EM_TRANSITO)
                .stream()
                .sorted(Comparator.comparing((Vasilhame v) -> v.getProduto().getVolumeLitros()).reversed()
                        .thenComparing(Vasilhame::getCodigo))
                .toList();

        Map<Long, EstoqueEntregadorResponse.Item> porProduto = new LinkedHashMap<>();
        for (Vasilhame vasilhame : vasilhames) {
            Produto produto = vasilhame.getProduto();
            porProduto.compute(produto.getId(), (id, atual) -> atual == null
                    ? new EstoqueEntregadorResponse.Item(produto.getId(), produto.getNome(), 1)
                    : new EstoqueEntregadorResponse.Item(produto.getId(), produto.getNome(),
                    atual.quantidade() + 1));
        }

        Long carga = pedidoRepository.somarGarrafoesPorEntregador(entregador.getId(), STATUS_ROTA);
        long cargaAtual = carga == null ? 0 : carga;

        return new EstoqueEntregadorResponse(
                entregador.getId(),
                entregador.getUsuario().getNome(),
                entregador.getZona(),
                entregador.getCapacidadeCarga(),
                cargaAtual,
                Math.max(0, entregador.getCapacidadeCarga() - (int) cargaAtual),
                new ArrayList<>(porProduto.values()),
                vasilhames.stream().map(v -> VasilhameResponse.from(v, hoje)).toList()
        );
    }

    @Transactional(readOnly = true)
    public VasilhamesClienteResponse vasilhamesDoCliente(Cliente cliente) {
        LocalDate hoje = LocalDate.now();
        List<Vasilhame> vasilhames = vasilhameRepository
                .findByClienteIdAndStatusOrderByDataValidadeAsc(cliente.getId(), StatusVasilhame.COM_CLIENTE)
                .stream()
                .sorted(Comparator.comparing((Vasilhame v) -> v.getProduto().getVolumeLitros()).reversed()
                        .thenComparing(Vasilhame::getDataValidade))
                .toList();

        Map<Long, long[]> contagem = new LinkedHashMap<>();
        Map<Long, Produto> produtos = new LinkedHashMap<>();
        for (Vasilhame vasilhame : vasilhames) {
            produtos.putIfAbsent(vasilhame.getProduto().getId(), vasilhame.getProduto());
            long[] valores = contagem.computeIfAbsent(vasilhame.getProduto().getId(), id -> new long[2]);
            if (vasilhame.estaVencido(hoje)) {
                valores[1]++;
            } else {
                valores[0]++;
            }
        }

        List<VasilhamesClienteResponse.Resumo> resumo = contagem.entrySet().stream()
                .map(entry -> new VasilhamesClienteResponse.Resumo(
                        entry.getKey(),
                        produtos.get(entry.getKey()).getNome(),
                        entry.getValue()[0],
                        entry.getValue()[1]))
                .toList();

        int totalValidos = (int) vasilhames.stream().filter(v -> !v.estaVencido(hoje)).count();
        int totalVencidos = vasilhames.size() - totalValidos;

        return new VasilhamesClienteResponse(
                cliente.getId(),
                cliente.getUsuario().getNome(),
                totalValidos,
                totalVencidos,
                resumo,
                vasilhames.stream().map(v -> VasilhameResponse.from(v, hoje)).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<VasilhameResponse> vasilhamesDoClienteParaPedido(Pedido pedido) {
        LocalDate hoje = LocalDate.now();
        Set<Long> produtosDoPedido = pedido.getItens().stream()
                .map(item -> item.getProduto().getId())
                .collect(Collectors.toSet());

        return vasilhameRepository
                .findByClienteIdAndStatusOrderByDataValidadeAsc(
                        pedido.getCliente().getId(), StatusVasilhame.COM_CLIENTE)
                .stream()
                .filter(v -> produtosDoPedido.contains(v.getProduto().getId()))
                .sorted(Comparator.comparing((Vasilhame v) -> v.getProduto().getVolumeLitros()).reversed()
                        .thenComparing(Vasilhame::getDataValidade))
                .map(v -> VasilhameResponse.from(v, hoje))
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarEstoqueCentral(Produto produto, LocalDate hoje) {
        return vasilhameRepository.countByProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
                produto.getId(), StatusVasilhame.NA_CENTRAL, hoje);
    }

    @Transactional(readOnly = true)
    public long contarVasilhamesValidosDoCliente(Cliente cliente, Produto produto) {
        return vasilhameRepository.countByClienteIdAndProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
                cliente.getId(), produto.getId(), StatusVasilhame.COM_CLIENTE, LocalDate.now());
    }
}
