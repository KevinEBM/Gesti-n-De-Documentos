package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceImplTest {

    private static final String SECRET_BASE64 =
            Base64.getEncoder().encodeToString("a".repeat(40).getBytes(StandardCharsets.UTF_8));

    private static final SecretKey SECRET_KEY =
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));

    private static final long EXPIRATION_MS = 60_000L;

    private static final Instant INSTANTE_FIJO = Instant.parse("2025-01-01T10:00:00Z");
    private static final Clock CLOCK_FIJO = Clock.fixed(INSTANTE_FIJO, ZoneOffset.UTC);

    private JwtServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new JwtServiceImpl(SECRET_BASE64, EXPIRATION_MS, CLOCK_FIJO);
    }

    private String construirToken(String subject, String correo, String rol) {
        var builder = Jwts.builder();
        if (subject != null) {
            builder.subject(subject);
        }
        if (correo != null) {
            builder.claim("correo", correo);
        }
        if (rol != null) {
            builder.claim("rol", rol);
        }
        return builder
                .issuedAt(Date.from(INSTANTE_FIJO))
                .expiration(Date.from(INSTANTE_FIJO.plusSeconds(60)))
                .signWith(SECRET_KEY)
                .compact();
    }

    @Test
    void generarToken_debeCrearTokenValido() {
        String token = servicio.generarToken(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(servicio.esTokenValido(token)).isTrue();
    }

    @Test
    void generarToken_debeNormalizarCorreoConTrimYMinusculas() {
        String token = servicio.generarToken(1L, "  Correo@Ejemplo.COM  ", RolEnum.ADMINISTRADOR);

        assertThat(servicio.obtenerCorreo(token)).isEqualTo("correo@ejemplo.com");
    }

    @Test
    void obtenerIdUsuario_debeDevolverIdCorrecto() {
        String token = servicio.generarToken(42L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR);

        assertThat(servicio.obtenerIdUsuario(token)).isEqualTo(42L);
    }

    @Test
    void obtenerCorreo_debeDevolverCorreoNormalizado() {
        String token = servicio.generarToken(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR);

        assertThat(servicio.obtenerCorreo(token)).isEqualTo("correo@ejemplo.com");
    }

    @Test
    void obtenerRol_debeDevolverRolCorrecto() {
        String token = servicio.generarToken(1L, "correo@ejemplo.com", RolEnum.JEFE_AREA);

        assertThat(servicio.obtenerRol(token)).isEqualTo(RolEnum.JEFE_AREA);
    }

    @Test
    void esTokenValido_debeRetornarTrueParaTokenValido() {
        String token = servicio.generarToken(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR);

        assertThat(servicio.esTokenValido(token)).isTrue();
    }

    @Test
    void esTokenValido_debeRetornarFalseParaNuloYVacio() {
        assertThat(servicio.esTokenValido(null)).isFalse();
        assertThat(servicio.esTokenValido("")).isFalse();
        assertThat(servicio.esTokenValido("   ")).isFalse();
    }

    @Test
    void esTokenValido_debeRetornarFalseParaTokenAlterado() {
        String token = servicio.generarToken(
                1L,
                "correo@ejemplo.com",
                RolEnum.ADMINISTRADOR
        );

        String[] partes = token.split("\\.");
        String firma = partes[2];
        int posicion = firma.length() / 2;
        char caracterActual = firma.charAt(posicion);
        char reemplazo = caracterActual == 'A' ? 'B' : 'A';

        String firmaAlterada = firma.substring(0, posicion)
                + reemplazo
                + firma.substring(posicion + 1);

        String tokenAlterado =
                partes[0] + "." + partes[1] + "." + firmaAlterada;

        assertThat(servicio.esTokenValido(tokenAlterado)).isFalse();
    }

    @Test
    void esTokenValido_debeRetornarFalseParaTokenExpirado() {
        JwtServiceImpl servicioEmision = new JwtServiceImpl(SECRET_BASE64, 1_000L, CLOCK_FIJO);
        String token = servicioEmision.generarToken(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR);

        Clock clockPosterior = Clock.fixed(INSTANTE_FIJO.plusSeconds(2), ZoneOffset.UTC);
        JwtServiceImpl servicioValidacion = new JwtServiceImpl(SECRET_BASE64, 1_000L, clockPosterior);

        assertThat(servicioValidacion.esTokenValido(token)).isFalse();
    }

    @Test
    void esTokenValido_debeRetornarFalseSiFaltaSubject() {
        String token = construirToken(null, "correo@ejemplo.com", RolEnum.ADMINISTRADOR.name());

        assertThat(servicio.esTokenValido(token)).isFalse();
    }

    @Test
    void esTokenValido_debeRetornarFalseSiSubjectNoEsNumericoONoEsPositivo() {
        String tokenNoNumerico = construirToken("abc", "correo@ejemplo.com", RolEnum.ADMINISTRADOR.name());
        String tokenNoPositivo = construirToken("0", "correo@ejemplo.com", RolEnum.ADMINISTRADOR.name());
        String tokenNegativo = construirToken("-5", "correo@ejemplo.com", RolEnum.ADMINISTRADOR.name());

        assertThat(servicio.esTokenValido(tokenNoNumerico)).isFalse();
        assertThat(servicio.esTokenValido(tokenNoPositivo)).isFalse();
        assertThat(servicio.esTokenValido(tokenNegativo)).isFalse();
    }

    @Test
    void esTokenValido_debeRetornarFalseSiFaltaCorreo() {
        String token = construirToken("1", null, RolEnum.ADMINISTRADOR.name());

        assertThat(servicio.esTokenValido(token)).isFalse();
    }

    @Test
    void esTokenValido_debeRetornarFalseSiFaltaRol() {
        String token = construirToken("1", "correo@ejemplo.com", null);

        assertThat(servicio.esTokenValido(token)).isFalse();
    }

    @Test
    void esTokenValido_debeRetornarFalseSiRolNoPerteneceARolEnum() {
        String token = construirToken("1", "correo@ejemplo.com", "SUPERADMIN");

        assertThat(servicio.esTokenValido(token)).isFalse();
    }

    @Test
    void constructor_debeRechazarSecretoNuloOVacio() {
        assertThatThrownBy(() -> new JwtServiceImpl(null, EXPIRATION_MS, CLOCK_FIJO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtServiceImpl("", EXPIRATION_MS, CLOCK_FIJO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtServiceImpl("   ", EXPIRATION_MS, CLOCK_FIJO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_debeRechazarSecretoNoBase64Valido() {
        assertThatThrownBy(() -> new JwtServiceImpl("%%%no_es_base64%%%", EXPIRATION_MS, CLOCK_FIJO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_debeRechazarSecretoDecodificadoMenorA32Bytes() {
        String secretoCorto = Base64.getEncoder().encodeToString("a".repeat(10).getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new JwtServiceImpl(secretoCorto, EXPIRATION_MS, CLOCK_FIJO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_debeRechazarExpirationMsMenorOIgualACero() {
        assertThatThrownBy(() -> new JwtServiceImpl(SECRET_BASE64, 0L, CLOCK_FIJO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtServiceImpl(SECRET_BASE64, -1L, CLOCK_FIJO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_debeRechazarClockNulo() {
        assertThatThrownBy(() -> new JwtServiceImpl(SECRET_BASE64, EXPIRATION_MS, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generarToken_debeRechazarIdInvalidoCorreoVacioYRolNulo() {
        assertThatThrownBy(() -> servicio.generarToken(null, "correo@ejemplo.com", RolEnum.ADMINISTRADOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.generarToken(0L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.generarToken(-1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.generarToken(1L, null, RolEnum.ADMINISTRADOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.generarToken(1L, "   ", RolEnum.ADMINISTRADOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.generarToken(1L, "correo@ejemplo.com", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
