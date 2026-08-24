package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.config.SecurityTestConfiguration;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LoginRateLimitFilterTest.ConfiguracionTest.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "security.login-rate-limit.max-attempts=5",
        "security.login-rate-limit.window-seconds=60"
})
@WebAppConfiguration
class LoginRateLimitFilterTest {

    private static final String URL_LOGIN = "/api/auth/login";
    private static final String URL_OTRA = "/api/prueba/otra";

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RelojAjustable reloj;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reloj.reiniciar();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void primerIntentoPostLogin_noDebeResponder429() throws Exception {
        mockMvc.perform(post(URL_LOGIN).with(ipDe("203.0.113.10")))
                .andExpect(status().isOk());
    }

    @Test
    void cincoIntentosPostLoginDentroDeLaVentana_debenPermitirse() throws Exception {
        for (int intento = 1; intento <= 5; intento++) {
            mockMvc.perform(post(URL_LOGIN).with(ipDe("203.0.113.11")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void sextoIntentoPostLoginDesdeLaMismaIp_debeResponder429() throws Exception {
        String ip = "203.0.113.12";

        for (int intento = 0; intento < 5; intento++) {
            mockMvc.perform(post(URL_LOGIN).with(ipDe(ip))).andExpect(status().isOk());
        }

        MvcResult result = mockMvc.perform(post(URL_LOGIN).with(ipDe(ip)))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertRespuestaRateLimit(result);
    }

    @Test
    void otraIp_debeConservarSuPropioContador() throws Exception {
        String ipBloqueada = "203.0.113.20";
        String ipLibre = "203.0.113.21";

        for (int intento = 0; intento < 5; intento++) {
            mockMvc.perform(post(URL_LOGIN).with(ipDe(ipBloqueada))).andExpect(status().isOk());
        }

        mockMvc.perform(post(URL_LOGIN).with(ipDe(ipBloqueada)))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(post(URL_LOGIN).with(ipDe(ipLibre)))
                .andExpect(status().isOk());
    }

    @Test
    void endpointDistinto_noDebeEstarLimitado() throws Exception {
        for (int intento = 0; intento < 10; intento++) {
            mockMvc.perform(post(URL_OTRA).with(ipDe("203.0.113.30")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void metodoDistintoSobreLogin_noDebeConsumirElLimite() throws Exception {
        for (int intento = 0; intento < 10; intento++) {
            mockMvc.perform(get(URL_LOGIN).with(ipDe("203.0.113.40")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post(URL_LOGIN).with(ipDe("203.0.113.40")))
                .andExpect(status().isOk());
    }

    @Test
    void trasVencerLaVentana_debeVolverAPermitirSolicitudes() throws Exception {
        String ip = "203.0.113.50";

        for (int intento = 0; intento < 5; intento++) {
            mockMvc.perform(post(URL_LOGIN).with(ipDe(ip))).andExpect(status().isOk());
        }

        mockMvc.perform(post(URL_LOGIN).with(ipDe(ip)))
                .andExpect(status().isTooManyRequests());

        reloj.avanzar(Duration.ofSeconds(61));

        mockMvc.perform(post(URL_LOGIN).with(ipDe(ip)))
                .andExpect(status().isOk());
    }

    @Test
    void getProtegido_noDebeConsultarJwtServiceCuandoNoHayBearer() throws Exception {
        mockMvc.perform(get("/api/prueba/protegida"))
                .andExpect(status().isUnauthorized());

        verify(jwtService, never()).esTokenValido(anyString());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor ipDe(String ip) {
        return req -> {
            req.setRemoteAddr(ip);
            return req;
        };
    }

    private void assertRespuestaRateLimit(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.get("exito").asBoolean()).isFalse();
        assertThat(json.get("mensaje").asText())
                .isEqualTo(
                        "Demasiados intentos de inicio de sesión. Intenta nuevamente en unos segundos."
                );
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityTestConfiguration.class,
            PruebaController.class
    })
    static class ConfiguracionTest {

        private final RelojAjustable reloj = new RelojAjustable(
                Instant.parse("2026-01-01T12:00:00Z")
        );

        @Bean
        @Primary
        Clock clock() {
            return reloj;
        }

        @Bean
        RelojAjustable relojAjustable() {
            return reloj;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @RestController
    static class PruebaController {

        @PostMapping(URL_LOGIN)
        public ResponseEntity<Void> login() {
            return ResponseEntity.ok().build();
        }

        @PostMapping(URL_OTRA)
        public ResponseEntity<Void> otraPost() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/prueba/protegida")
        public ResponseEntity<Void> protegida() {
            return ResponseEntity.ok().build();
        }

        @GetMapping(URL_LOGIN)
        public ResponseEntity<Void> loginGet() {
            throw new AccessDeniedException("no permitido");
        }
    }

    static final class RelojAjustable extends Clock {

        private Instant instante;

        private RelojAjustable(Instant instante) {
            this.instante = instante;
        }

        private void reiniciar() {
            instante = Instant.parse("2026-01-01T12:00:00Z");
        }

        private void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
