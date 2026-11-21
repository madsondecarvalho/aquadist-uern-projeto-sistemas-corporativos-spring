package com.aquadist.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("E2E - seguranca, RBAC e paginas estaticas")
class SegurancaE2ETest {

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
    @DisplayName("API sem credenciais retorna 401")
    void apiSemCredenciaisRetorna401() throws Exception {
        mockMvc.perform(get("/api/pedidos/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Nao autenticado")));
    }

    @Test
    @DisplayName("credenciais invalidas retornam 401")
    void credenciaisInvalidasRetornam401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(httpBasic("maria@email.com", "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("cliente nao acessa endpoints de administrador (403)")
    void clienteNaoAcessaAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/pedidos")
                        .with(httpBasic("maria@email.com", "cliente123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/clientes")
                        .with(httpBasic("ana@aquadist.com", "entregador123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("paginas HTML e assets do Tailwind sao publicos e carregam")
    void paginasEstaticasSaoPublicas() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Entrar no sistema")));

        mockMvc.perform(get("/cliente.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Montar novo pedido")));

        mockMvc.perform(get("/entregador.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pedidos da minha rota")));

        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AquaDist")));

        mockMvc.perform(get("/assets/js/api.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("requireRole")));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Redirecionando")));
    }

    @Test
    @DisplayName("admin acessa painel com sucesso")
    void adminAcessaPainel() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .with(httpBasic("admin@aquadist.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"perfil\":\"ADMIN\"")));

        mockMvc.perform(get("/api/admin/pedidos")
                        .with(httpBasic("admin@aquadist.com", "admin123")))
                .andExpect(status().isOk());
    }
}
