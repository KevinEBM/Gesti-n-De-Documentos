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
import org.springframework.web.bind.annotation.RequestHeader;
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
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final String HEADER_STATUS = "X-Test-Status";

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

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reloj.reiniciar();
        PruebaController.llamadasLogin.set(0);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void cinco401_debenRegistrarCincoFallosYPermitirse() throws Exception {
        String ip = "203.0.113.10";

        for (int intento = 1; intento <= 5; intento++) {
            postLogin(ip, 401);
        }

        assertThat(PruebaController.llamadasLogin.get()).isEqualTo(5);
        assertThat(loginRateLimiter.estaBloqueado(ip)).isTrue();
    }

    @Test
    void sextoIntentoTrasCinco401_debeResponder429SinLlamarAlLogin() throws Exception {
        String ip = "203.0.113.11";

        for (int intento = 0; intento < 5; intento++) {
            postLogin(ip, 401);
        }

        MvcResult result = mockMvc.perform(post(URL_LOGIN).with(ipDe(ip)))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertRespuestaRateLimit(result);
        assertThat(PruebaController.llamadasLogin.get()).isEqualTo(5);
    }

    @Test
    void login2xx_debeLimpiarFallosAnteriores() throws Exception {
        String ip = "203.0.113.12";

        for (int intento = 0; intento < 3; intento++) {
            postLogin(ip, 401);
        }

        postLogin(ip, 200);

        for (int intento = 1; intento <= 5; intento++) {
            postLogin(ip, 401);
        }

        mockMvc.perform(post(URL_LOGIN).with(ipDe(ip)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void variosLoginsExitosos_noDebenGenerarBloqueo() throws Exception {
        String ip = "203.0.113.13";

        for (int intento = 0; intento < 10; intento++) {
            postLogin(ip, 200);
        }

        postLogin(ip, 200);
        assertThat(loginRateLimiter.estaBloqueado(ip)).isFalse();
    }

    @Test
    void respuesta400_noDebeIncrementarElContador() throws Exception {
        String ip = "203.0.113.14";

        for (int intento = 0; intento < 8; intento++) {
            postLogin(ip, 400);
        }

        postLogin(ip, 401);
        assertThat(loginRateLimiter.estaBloqueado(ip)).isFalse();
    }

    @Test
    void respuesta403_noDebeIncrementarElContador() throws Exception {
        String ip = "203.0.113.15";

        for (int intento = 0; intento < 8; intento++) {
            postLogin(ip, 403);
        }

        postLogin(ip, 401);
        assertThat(loginRateLimiter.estaBloqueado(ip)).isFalse();
    }

    @Test
    void respuesta500_noDebeIncrementarElContador() throws Exception {
        String ip = "203.0.113.16";

        for (int intento = 0; intento < 8; intento++) {
            postLogin(ip, 500);
        }

        postLogin(ip, 401);
        assertThat(loginRateLimiter.estaBloqueado(ip)).isFalse();
    }

    @Test
    void ipA_noDebeAfectarIpB() throws Exception {
        String ipA = "203.0.113.20";
        String ipB = "203.0.113.21";

        for (int intento = 0; intento < 5; intento++) {
            postLogin(ipA, 401);
        }

        mockMvc.perform(post(URL_LOGIN).with(ipDe(ipA)))
                .andExpect(status().isTooManyRequests());

        postLogin(ipB, 401);
        postLogin(ipB, 200);
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
        String ip = "203.0.113.40";

        for (int intento = 0; intento < 10; intento++) {
            mockMvc.perform(get(URL_LOGIN).with(ipDe(ip)))
                    .andExpect(status().isUnauthorized());
        }

        postLogin(ip, 200);
        assertThat(loginRateLimiter.estaBloqueado(ip)).isFalse();
    }

    @Test
    void trasVencerLaVentana_debeVolverAPermitirSolicitudes() throws Exception {
        String ip = "203.0.113.50";

        for (int intento = 0; intento < 5; intento++) {
            postLogin(ip, 401);
        }

        mockMvc.perform(post(URL_LOGIN).with(ipDe(ip)))
                .andExpect(status().isTooManyRequests());

        reloj.avanzar(Duration.ofSeconds(61));

        postLogin(ip, 401);
    }

    @Test
    void getProtegido_noDebeConsultarJwtServiceCuandoNoHayBearer() throws Exception {
        mockMvc.perform(get("/api/prueba/protegida"))
                .andExpect(status().isUnauthorized());

        verify(jwtService, never()).esTokenValido(anyString());
    }

    private void postLogin(String ip, int status) throws Exception {
        mockMvc.perform(post(URL_LOGIN)
                        .header(HEADER_STATUS, status)
                        .with(ipDe(ip)))
                .andExpect(status().is(status));
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
                        "Demasiados intentos de inicio de sesión. Intenta nuevamente en 5 minutos."
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

        static final AtomicInteger llamadasLogin = new AtomicInteger();

        @PostMapping(URL_LOGIN)
        public ResponseEntity<Void> login(
                @RequestHeader(value = HEADER_STATUS, defaultValue = "200") int status
        ) {
            llamadasLogin.incrementAndGet();
            return ResponseEntity.status(status).build();
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
