package com.aquadist.service;

import com.aquadist.dto.CriarProdutoRequest;
import com.aquadist.dto.EstoqueCentralResponse;
import com.aquadist.dto.ProdutoResponse;
import com.aquadist.dto.ReporEstoqueRequest;
import com.aquadist.dto.VasilhameResponse;
import com.aquadist.exception.RecursoNaoEncontradoException;
import com.aquadist.exception.RegraDeNegocioException;
import com.aquadist.model.Produto;
import com.aquadist.model.Vasilhame;
import com.aquadist.repository.ProdutoRepository;
import com.aquadist.repository.VasilhameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final VasilhameRepository vasilhameRepository;
    private final EstoqueService estoqueService;

    public ProdutoService(ProdutoRepository produtoRepository,
                          VasilhameRepository vasilhameRepository,
                          EstoqueService estoqueService) {
        this.produtoRepository = produtoRepository;
        this.vasilhameRepository = vasilhameRepository;
        this.estoqueService = estoqueService;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        LocalDate hoje = LocalDate.now();
        return produtoRepository.findAll().stream()
                .sorted(Comparator.comparing(Produto::isAtivo).reversed()
                        .thenComparing(Comparator.comparingInt(Produto::getVolumeLitros).reversed()))
                .map(produto -> ProdutoResponse.from(
                        produto,
                        estoqueService.contarEstoqueCentral(produto, hoje),
                        vasilhameRepository.countByProdutoIdAndStatusAndDataValidadeLessThan(
                                produto.getId(), com.aquadist.model.StatusVasilhame.NA_CENTRAL, hoje)))
                .toList();
    }

    @Transactional
    public ProdutoResponse criar(CriarProdutoRequest request) {
        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (produtoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new RegraDeNegocioException("Ja existe um produto com o codigo " + codigo + ".");
        }

        Produto produto = new Produto(
                codigo,
                request.nome().trim(),
                request.volumeLitros(),
                request.precoUnitario().setScale(2, RoundingMode.HALF_UP),
                request.taxaCaucao().setScale(2, RoundingMode.HALF_UP)
        );
        produtoRepository.save(produto);
        return ProdutoResponse.from(produto, 0, 0);
    }

    @Transactional
    public List<VasilhameResponse> reporEstoque(ReporEstoqueRequest request) {
        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto nao encontrado."));

        if (request.dataValidade().isBefore(LocalDate.now())) {
            throw new RegraDeNegocioException("Nao e possivel repor o estoque com vasilhames vencidos.");
        }

        List<Vasilhame> novos = new ArrayList<>();
        for (int i = 0; i < request.quantidade(); i++) {
            String codigo = produto.getCodigo().toUpperCase(Locale.ROOT) + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            novos.add(new Vasilhame(codigo, produto, request.dataValidade()));
        }
        vasilhameRepository.saveAll(novos);

        LocalDate hoje = LocalDate.now();
        return novos.stream().map(v -> VasilhameResponse.from(v, hoje)).toList();
    }

    @Transactional(readOnly = true)
    public EstoqueCentralResponse estoqueCentral() {
        return estoqueService.estoqueCentral();
    }

    @Transactional(readOnly = true)
    public Produto buscarAtivo(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto ativo nao encontrado: " + id));
    }

    public BigDecimal normalizarValor(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
