package com.aquadist.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("E2E - fluxo HTTP completo cliente -> entregador (H2 em memoria)")
class FluxoPedidoE2ETest {

    private static final String MARIA = "maria@email.com";
    private static final String ANA = "ana@aquadist.com";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("cliente cria pedido, entregador inicia trânsito, cancelamento falha e entrega é concluída")
    void fluxoCompletoDePedido() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic(MARIA, "cliente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));

        String corpoPedido = mockMvc.perform(post("/api/pedidos")
                        .with(httpBasic(MARIA, "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"produtoId": 1, "quantidade": 4, "vasilhamesDevolvidos": 2}
                                  ],
                                  "formaPagamento": "PIX",
                                  "observacao": "Teste E2E"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATRIBUIDO"))
                .andExpect(jsonPath("$.valorCaucao").value(50.0))
                .andExpect(jsonPath("$.valorTotal").value(98.0))
                .andExpect(jsonPath("$.entregadorNome").value("Ana Souza"))
                .andReturn().getResponse().getContentAsString();

        Number idNumber = JsonPath.read(corpoPedido, "$.id");
        long pedidoId = idNumber.longValue();

        mockMvc.perform(get("/api/entregador/pedidos").with(httpBasic(ANA, "entregador123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedidoId));

        mockMvc.perform(post("/api/entregador/pedidos/{id}/iniciar-transito", pedidoId)
                        .with(httpBasic(ANA, "entregador123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_TRANSITO"));

        mockMvc.perform(post("/api/pedidos/{id}/cancelar", pedidoId)
                        .with(httpBasic(MARIA, "cliente123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Tentativa em rota\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Operacao nao permitida"));

        String corpoVasilhames = mockMvc.perform(get(
                        "/api/entregador/pedidos/{id}/vasilhames-cliente", pedidoId)
                        .with(httpBasic(ANA, "entregador123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> vasilhames = JsonPath.read(corpoVasilhames, "$");
        List<String> codigos = vasilhames.stream()
                .filter(v -> Boolean.FALSE.equals(v.get("vencido")))
                .limit(2)
                .map(v -> String.valueOf(v.get("codigo")))
                .toList();

        String corpoConclusao = """
                {
                  "codigosVasilhamesDevolvidos": ["%s", "%s"],
                  "formaPagamentoRecebida": "PIX",
                  "observacao": "Entrega E2E"
                }
                """.formatted(codigos.get(0), codigos.get(1));

        mockMvc.perform(post("/api/entregador/pedidos/{id}/concluir", pedidoId)
                        .with(httpBasic(ANA, "entregador123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoConclusao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.valorCaucao").value(50.0))
                .andExpect(jsonPath("$.valorTotal").value(98.0))
                .andExpect(jsonPath("$.itens[0].vasilhamesValidosRecebidos").value(2))
                .andExpect(jsonPath("$.itens[0].vasilhamesRecusadosVencidos").value(0));

        mockMvc.perform(get("/api/pedidos/me").with(httpBasic(MARIA, "cliente123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ENTREGUE"));
    }
}
