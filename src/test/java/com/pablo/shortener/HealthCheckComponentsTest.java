package com.pablo.shortener;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Liga a exibição dos componentes só neste teste, para conferir O QUE o health verifica:
 * o banco entra (sem ele a aplicação não funciona); o Redis não (a aplicação funciona sem ele).
 */
@SpringBootTest(properties = "management.endpoint.health.show-components=always")
@AutoConfigureMockMvc
class HealthCheckComponentsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthVerificaOBancoMasNaoORedis() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.redis").doesNotExist());
    }
}
