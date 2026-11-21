package com.aquadist.controller;

import com.aquadist.dto.AtribuirEntregadorRequest;
import com.aquadist.dto.ClienteResponse;
import com.aquadist.dto.CriarClienteRequest;
import com.aquadist.dto.CriarEntregadorRequest;
import com.aquadist.dto.CriarPedidoAdminRequest;
import com.aquadist.dto.CriarProdutoRequest;
import com.aquadist.dto.EntregadorResponse;
import com.aquadist.dto.EstoqueCentralResponse;
import com.aquadist.dto.PagamentoRequest;
import com.aquadist.dto.PagamentoResponse;
import com.aquadist.dto.PedidoResponse;
import com.aquadist.dto.ProdutoResponse;
import com.aquadist.dto.ReporEstoqueRequest;
import com.aquadist.dto.VasilhameResponse;
import com.aquadist.dto.VasilhamesClienteResponse;
import com.aquadist.model.StatusPedido;
import com.aquadist.service.ClienteService;
import com.aquadist.service.EntregadorService;
import com.aquadist.service.PedidoService;
import com.aquadist.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ClienteService clienteService;
    private final EntregadorService entregadorService;
    private final PedidoService pedidoService;
    private final ProdutoService produtoService;

    public AdminController(ClienteService clienteService,
                           EntregadorService entregadorService,
                           PedidoService pedidoService,
                           ProdutoService produtoService) {
        this.clienteService = clienteService;
        this.entregadorService = entregadorService;
        this.pedidoService = pedidoService;
        this.produtoService = produtoService;
    }

    @GetMapping("/pedidos")
    public List<PedidoResponse> pedidos(@RequestParam(required = false) StatusPedido status) {
        return pedidoService.listarTodos(status);
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criarPedido(@RequestBody @Valid CriarPedidoAdminRequest request) {
        return pedidoService.criarComoAdmin(request);
    }

    @PostMapping("/pedidos/{id}/confirmar-pagamento")
    public PedidoResponse confirmarPagamento(@PathVariable Long id) {
        return pedidoService.confirmarPagamento(id);
    }

    @PostMapping("/pedidos/{id}/atribuir")
    public PedidoResponse atribuir(@PathVariable Long id,
                                   @RequestBody @Valid AtribuirEntregadorRequest request) {
        return pedidoService.atribuirEntregador(id, request);
    }

    @GetMapping("/clientes")
    public List<ClienteResponse> clientes() {
        return clienteService.listar();
    }

    @GetMapping("/clientes/{id}")
    public ClienteResponse cliente(@PathVariable Long id) {
        return clienteService.buscar(id);
    }

    @GetMapping("/clientes/{id}/vasilhames")
    public VasilhamesClienteResponse vasilhamesCliente(@PathVariable Long id) {
        return clienteService.vasilhamesPorId(id);
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse criarCliente(@RequestBody @Valid CriarClienteRequest request) {
        return clienteService.criar(request);
    }

    @PostMapping("/clientes/{id}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse registrarPagamento(@PathVariable Long id,
                                              @RequestBody @Valid PagamentoRequest request) {
        return clienteService.registrarPagamento(id, request);
    }

    @GetMapping("/clientes/{id}/pagamentos")
    public List<PagamentoResponse> pagamentos(@PathVariable Long id) {
        return clienteService.pagamentos(id);
    }

    @GetMapping("/entregadores")
    public List<EntregadorResponse> entregadores() {
        return entregadorService.listar();
    }

    @PostMapping("/entregadores")
    @ResponseStatus(HttpStatus.CREATED)
    public EntregadorResponse criarEntregador(@RequestBody @Valid CriarEntregadorRequest request) {
        return entregadorService.criar(request);
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criarProduto(@RequestBody @Valid CriarProdutoRequest request) {
        return produtoService.criar(request);
    }

    @PostMapping("/vasilhames/lote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<VasilhameResponse> reporEstoque(@RequestBody @Valid ReporEstoqueRequest request) {
        return produtoService.reporEstoque(request);
    }

    @GetMapping("/estoque")
    public EstoqueCentralResponse estoque() {
        return produtoService.estoqueCentral();
    }
}
