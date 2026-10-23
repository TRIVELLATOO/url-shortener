package com.pablo.shortener;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Confere que a página inicial e a documentação da API não são "engolidas" pela rota /{code}. */
@SpringBootTest
@AutoConfigureMockMvc
class WebPagesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void raizServeAPaginaInicial() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void paginaInicialTemOFormulario() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Encurtador de URL")));
    }

    @Test
    void documentacaoOpenApiListaOsEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("URL Shortener API"))
                .andExpect(jsonPath("$.paths['/api/links'].post").exists())
                .andExpect(jsonPath("$.paths['/api/links/{code}/stats'].get").exists());
    }
}
