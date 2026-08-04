package com.plantarsas.gestiondocumental.usuarios.controller;

import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioResponse;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import com.plantarsas.gestiondocumental.usuarios.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UsuarioControllerSecurityTest.ConfiguracionSeguridadUsuariosTest.class)
@WebAppConfiguration
class UsuarioControllerSecurityTest {

    private static final String USUARIO_REQUEST_JSON =
            "{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"correo\":\"ana@empresa.com\"," +
                    "\"password\":\"claveInicial123\",\"rolId\":1,\"areaIds\":[10],\"areaPrincipalId\":10}";

    private static final String USUARIO_UPDATE_REQUEST_JSON =
            "{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"correo\":\"ana@empresa.com\"," +
                    "\"rolId\":1,\"areaIds\":[10],\"areaPrincipalId\":10}";

    private static final String USUARIO_ESTADO_REQUEST_JSON =
            "{\"estado\":\"INACTIVO\"}";

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
        reset(usuarioService, jwtService, usuarioRepository);
    }

    private UsuarioResponse respuestaDePrueba(Long id) {
        return new UsuarioResponse(
                id,
                "Ana",
                "Perez",
                "ana@empresa.com",
                null,
                EstadoUsuario.ACTIVO,
                List.of(),
                null,
                null,
                null
        );
    }

    private void assertRespuestaDeError(MvcResult result, String mensajeEsperado) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.get("exito").asBoolean()).isFalse();
        assertThat(json.get("mensaje").asText()).isEqualTo(mensajeEsperado);
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertRespuestaDeError(result, "Autenticación requerida");
        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listar_conAdministrativo_debeResponder403() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden())
                .andReturn();

        assertRespuestaDeError(result, "No tiene permisos para realizar esta operación");
        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listar_conJefeArea_debeResponder403() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden())
                .andReturn();

        assertRespuestaDeError(result, "No tiene permisos para realizar esta operación");
        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listar_conAdministrador_debeResponder200() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());

        verify(usuarioService).listar();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void obtenerPorId_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void obtenerPorId_conAdministrador_debePermitirAcceso() throws Exception {
        when(usuarioService.obtenerPorId(1L)).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk());

        verify(usuarioService).obtenerPorId(1L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void crear_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crear_conAdministrador_debeResponder201() throws Exception {
        when(usuarioService.crear(any())).thenReturn(respuestaDePrueba(1L));

        MvcResult result = mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        verify(usuarioService).crear(any());

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode datos = json.get("datos");
        assertThat(datos.has("password")).isFalse();
        assertThat(datos.has("passwordHash")).isFalse();
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void actualizar_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_UPDATE_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).actualizar(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void actualizar_conAdministrador_debePermitirAcceso() throws Exception {
        when(usuarioService.actualizar(eq(1L), any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_UPDATE_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(usuarioService).actualizar(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void cambiarEstado_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(patch("/api/usuarios/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_ESTADO_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).cambiarEstado(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void cambiarEstado_conAdministrador_debePermitirAcceso() throws Exception {
        when(usuarioService.cambiarEstado(eq(1L), any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(patch("/api/usuarios/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_ESTADO_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(usuarioService).cambiarEstado(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void endpointsAdministrativos_debenConservarCsrfDesactivado() throws Exception {
        when(usuarioService.crear(any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_REQUEST_JSON))
                .andExpect(status().isCreated());

        verify(usuarioService).crear(any());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            UsuarioController.class
    })
    static class ConfiguracionSeguridadUsuariosTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
