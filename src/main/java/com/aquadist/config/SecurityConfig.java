package com.aquadist.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/login.html",
                                "/cliente.html", "/entregador.html", "/admin.html",
                                "/assets/**", "/favicon.ico", "/error"
                        ).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exception) ->
                                escreverErro(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Nao autenticado", "Informe credenciais Basic validas."))
                        .accessDeniedHandler((request, response, exception) ->
                                escreverErro(response, HttpServletResponse.SC_FORBIDDEN,
                                        "Acesso negado", "Seu perfil nao tem permissao para esta operacao.")));

        return http.build();
    }

    private void escreverErro(HttpServletResponse response, int status, String titulo, String detalhe)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":%d,"title":"%s","detail":"%s"}
                """.formatted(status, titulo, detalhe));
    }
}
