package com.plantarsas.gestiondocumental.auth.controller;

/*
 * PROPUESTA — nuevo archivo de test, aun no copiado al repositorio.
 *
 * Cubre exclusivamente PUT /api/auth/contrasena (Etapa 4B). El endpoint de
 * login (POST /api/auth/login) no tiene tests de seguridad propios en el
 * proyecto (AuthServiceImplTest ya lo cubre a nivel de servicio), por lo que
 * esta clase se limita al endpoint nuevo.
 *
 * Nota de diseño de estos tests: a diferencia de otros controllers (p. ej.
 * DocumentoController), donde @AuthenticationPrincipal AuthenticatedUser se
 * reenvia intacto al service mockeado sin que el controller lo desreferencie,
 * AuthController.cambiarContrasena llama directamente a
 * usuarioAutenticado.id(). Por eso los tres casos 200 (ADMINISTRADOR,
 * JEFE_AREA, ADMINISTRATIVO) usan un AuthenticatedUser real inyectado via
 * SecurityMockMvcRequestPostProcessors.authentication(...) -- @WithMockUser
 * por si solo no basta aqui, porque su principal no es un AuthenticatedUser
 * y el controller haria NullPointerException al invocar .id() antes de
 * llegar al service mockeado.
 *
 * UsuarioService.cambiarContrasena(...) recibe (Long, String, String), no un
 * CambiarContrasenaRequest -- el DTO HTTP nunca cruza hacia la capa de
 * servicio de usuarios (ver correccion de diseno de Etapa 4B: se evita la
 * dependencia usuarios -> auth). Por eso las verificaciones de delegacion
 * capturan los tres argumentos por separado en vez de comparar un objeto.
 */

import com.plantarsas.gestiondocumental.auth.service.AuthService;
import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.GlobalExceptionHandler;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import com.plantarsas.gestiondocumental.usuarios.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AuthControllerSecurityTest.ConfiguracionSeguridadAuthTest.class)
@WebAppConfiguration
class AuthControllerSecurityTest {

    private static final String URL_CAMBIAR_CONTRASENA = "/api/auth/contrasena";

    private static final Long USUARIO_ID = 7L;

    private static final String REQUEST_VALIDO_JSON =
            "{\"contrasenaActual\":\"actual123\",\"nuevaContrasena\":\"nueva12345\",\"confirmacionContrasena\":\"nueva12345\"}";

    private static final String REQUEST_CONTRASENA_ACTUAL_VACIA_JSON =
            "{\"contrasenaActual\":\"\",\"nuevaContrasena\":\"nueva12345\",\"confirmacionContrasena\":\"nueva12345\"}";

    private static final String REQUEST_NUEVA_CONTRASENA_CORTA_JSON =
            "{\"contrasenaActual\":\"actual123\",\"nuevaContrasena\":\"corta\",\"confirmacionContrasena\":\"corta\"}";

    private static final String REQUEST_CONFIRMACION_DISTINTA_JSON =
            "{\"contrasenaActual\":\"actual123\",\"nuevaContrasena\":\"nueva12345\",\"confirmacionContrasena\":\"otra12345\"}";

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void limpiarMocks() {
        reset(authService, usuarioService, jwtService, usuarioRepository);
    }

    private RequestPostProcessor usuarioAutenticado(RolEnum rol) {
        AuthenticatedUser usuario = new AuthenticatedUser(USUARIO_ID, "usuario@plantarsas.com", rol);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(token);
    }

    @Test
    void cambiarContrasena_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_VALIDO_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarContrasena_conAdministrador_debeResponder200() throws Exception {
        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_VALIDO_JSON)
                        .with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isOk());

        verify(usuarioService).cambiarContrasena(eq(USUARIO_ID), anyString(), anyString());
    }

    @Test
    void cambiarContrasena_conJefeArea_debeResponder200() throws Exception {
        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_VALIDO_JSON)
                        .with(usuarioAutenticado(RolEnum.JEFE_AREA)))
                .andExpect(status().isOk());

        verify(usuarioService).cambiarContrasena(eq(USUARIO_ID), anyString(), anyString());
    }

    @Test
    void cambiarContrasena_conAdministrativo_debeResponder200() throws Exception {
        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_VALIDO_JSON)
                        .with(usuarioAutenticado(RolEnum.ADMINISTRATIVO)))
                .andExpect(status().isOk());

        verify(usuarioService).cambiarContrasena(eq(USUARIO_ID), anyString(), anyString());
    }

    @Test
    void cambiarContrasena_debeDelegarConElIdYLosValoresExactosDelRequest() throws Exception {
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> actualCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nuevaCaptor = ArgumentCaptor.forClass(String.class);

        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_VALIDO_JSON)
                        .with(usuarioAutenticado(RolEnum.ADMINISTRATIVO)))
                .andExpect(status().isOk());

        verify(usuarioService).cambiarContrasena(
                idCaptor.capture(), actualCaptor.capture(), nuevaCaptor.capture()
        );
        assertThat(idCaptor.getValue()).isEqualTo(USUARIO_ID);
        assertThat(actualCaptor.getValue()).isEqualTo("actual123");
        assertThat(nuevaCaptor.getValue()).isEqualTo("nueva12345");
    }

    @Test
    void cambiarContrasena_conContrasenaActualVacia_debeResponder400() throws Exception {
        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_CONTRASENA_ACTUAL_VACIA_JSON)
                        .with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarContrasena_conNuevaContrasenaDemasiadoCorta_debeResponder400() throws Exception {
        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_NUEVA_CONTRASENA_CORTA_JSON)
                        .with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarContrasena_conConfirmacionDistinta_debeResponder400() throws Exception {
        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_CONFIRMACION_DISTINTA_JSON)
                        .with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarContrasena_conContrasenaActualIncorrectaSegunElServicio_debeResponder400() throws Exception {
        doThrow(new BusinessException("La contraseña actual es incorrecta."))
                .when(usuarioService).cambiarContrasena(eq(USUARIO_ID), anyString(), anyString());

        mockMvc.perform(put(URL_CAMBIAR_CONTRASENA)
                        .contentType("application/json")
                        .content(REQUEST_VALIDO_JSON)
                        .with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerPerfil_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    void obtenerPerfil_conJefeArea_debeDelegarAlServicio() throws Exception {
        when(authService.obtenerPerfilActual(any(AuthenticatedUser.class)))
                .thenReturn(new com.plantarsas.gestiondocumental.auth.dto.PerfilUsuarioResponse(
                        USUARIO_ID, "usuario@plantarsas.com", "Usuario", "Prueba",
                        RolEnum.JEFE_AREA, 3L, "Gestión Ambiental"
                ));

        mockMvc.perform(get("/api/auth/me").with(usuarioAutenticado(RolEnum.JEFE_AREA)))
                .andExpect(status().isOk());

        verify(authService).obtenerPerfilActual(any(AuthenticatedUser.class));
    }

    @Test
    void obtenerPerfil_conAdministrador_debeDelegarAlServicio() throws Exception {
        when(authService.obtenerPerfilActual(any(AuthenticatedUser.class)))
                .thenReturn(new com.plantarsas.gestiondocumental.auth.dto.PerfilUsuarioResponse(
                        USUARIO_ID, "usuario@plantarsas.com", "Usuario", "Prueba",
                        RolEnum.ADMINISTRADOR, null, null
                ));

        mockMvc.perform(get("/api/auth/me").with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isOk());

        verify(authService).obtenerPerfilActual(any(AuthenticatedUser.class));
    }

    @Test
    void obtenerPerfil_conAdministrativo_debeDelegarAlServicio() throws Exception {
        when(authService.obtenerPerfilActual(any(AuthenticatedUser.class)))
                .thenReturn(new com.plantarsas.gestiondocumental.auth.dto.PerfilUsuarioResponse(
                        USUARIO_ID, "usuario@plantarsas.com", "Usuario", "Prueba",
                        RolEnum.ADMINISTRATIVO, 3L, "Gestión Ambiental"
                ));

        mockMvc.perform(get("/api/auth/me").with(usuarioAutenticado(RolEnum.ADMINISTRATIVO)))
                .andExpect(status().isOk());

        verify(authService).obtenerPerfilActual(any(AuthenticatedUser.class));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            GlobalExceptionHandler.class,
            AuthController.class
    })
    static class ConfiguracionSeguridadAuthTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
