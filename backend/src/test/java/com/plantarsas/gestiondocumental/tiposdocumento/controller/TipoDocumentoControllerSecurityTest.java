package com.plantarsas.gestiondocumental.tiposdocumento.controller;

import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.service.TipoDocumentoService;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
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
@ContextConfiguration(classes = TipoDocumentoControllerSecurityTest.ConfiguracionSeguridadTiposDocumentoTest.class)
@WebAppConfiguration
class TipoDocumentoControllerSecurityTest {

    private static final String TIPO_DOCUMENTO_REQUEST_JSON =
            "{\"codigo\":\"MA\",\"nombre\":\"Tipo A\",\"descripcion\":\"Descripcion\"}";

    private static final String TIPO_DOCUMENTO_REQUEST_INVALIDO_JSON =
            "{\"codigo\":\"MA\",\"nombre\":\"\",\"descripcion\":\"Descripcion\"}";

    private static final String TIPO_DOCUMENTO_UPDATE_REQUEST_JSON =
            "{\"codigo\":\"MA\",\"nombre\":\"Tipo actualizado\",\"descripcion\":\"Descripcion actualizada\"}";

    private static final String TIPO_DOCUMENTO_UPDATE_REQUEST_INVALIDO_JSON =
            "{\"codigo\":\"MA\",\"nombre\":\"\",\"descripcion\":\"Descripcion\"}";

    private static final String TIPO_DOCUMENTO_ESTADO_REQUEST_JSON =
            "{\"activo\":false}";

    private static final String TIPO_DOCUMENTO_ESTADO_REQUEST_INVALIDO_JSON =
            "{}";

    @MockitoBean
    private TipoDocumentoService tipoDocumentoService;

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
        reset(tipoDocumentoService, jwtService, usuarioRepository);
    }

    private TipoDocumentoResponse respuestaDePrueba(Long id) {
        return new TipoDocumentoResponse(
                id,
                "MA",
                "Tipo A",
                "Descripcion",
                true,
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
    void crear_sinAutenticacion_debeResponder401() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tipos-documento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_REQUEST_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertRespuestaDeError(result, "Autenticación requerida");
        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void crear_conAdministrativo_debeResponder403() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tipos-documento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        assertRespuestaDeError(result, "No tiene permisos para realizar esta operación");
        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void crear_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(post("/api/tipos-documento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crear_conAdministrador_debeResponder201() throws Exception {
        when(tipoDocumentoService.crear(any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(post("/api/tipos-documento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_REQUEST_JSON))
                .andExpect(status().isCreated());

        verify(tipoDocumentoService).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crear_conNombreInvalido_debeResponder400() throws Exception {
        mockMvc.perform(post("/api/tipos-documento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_REQUEST_INVALIDO_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crear_conCodigoDemasiadoLargo_debeResponder400() throws Exception {
        mockMvc.perform(post("/api/tipos-documento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ABCDEFGHIJKLMNOPQRSTU\",\"nombre\":\"Tipo A\",\"descripcion\":\"Descripcion\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crear_conCodigoInvalido_debeResponder400() throws Exception {
        mockMvc.perform(post("/api/tipos-documento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"Tipo A\",\"descripcion\":\"Descripcion\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get("/api/tipos-documento"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listar_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get("/api/tipos-documento"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listar_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(get("/api/tipos-documento"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listarParaConsulta_conJefeArea_debeResponder200() throws Exception {
        when(tipoDocumentoService.listar()).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/tipos-documento/consulta"))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).listar();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listarParaConsulta_conAdministrativo_debeResponder200() throws Exception {
        when(tipoDocumentoService.listar()).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/tipos-documento/consulta"))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).listar();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listar_conAdministrador_debeResponder200() throws Exception {
        when(tipoDocumentoService.listar()).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/tipos-documento"))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).listar();
    }

    @Test
    void obtenerPorId_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get("/api/tipos-documento/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void obtenerPorId_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get("/api/tipos-documento/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void obtenerPorId_conAdministrador_debePermitirAcceso() throws Exception {
        when(tipoDocumentoService.obtenerPorId(1L)).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(get("/api/tipos-documento/1"))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).obtenerPorId(1L);
    }

    @Test
    void actualizar_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(put("/api/tipos-documento/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_UPDATE_REQUEST_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void actualizar_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(put("/api/tipos-documento/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_UPDATE_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(tipoDocumentoService, never()).actualizar(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void actualizar_conAdministrador_debePermitirAcceso() throws Exception {
        when(tipoDocumentoService.actualizar(eq(1L), any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(put("/api/tipos-documento/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_UPDATE_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).actualizar(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void actualizar_conNombreInvalido_debeResponder400() throws Exception {
        mockMvc.perform(put("/api/tipos-documento/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_UPDATE_REQUEST_INVALIDO_JSON))
                .andExpect(status().isBadRequest());

        verify(tipoDocumentoService, never()).actualizar(anyLong(), any());
    }

    @Test
    void cambiarEstado_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(patch("/api/tipos-documento/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_ESTADO_REQUEST_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tipoDocumentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void cambiarEstado_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(patch("/api/tipos-documento/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_ESTADO_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(tipoDocumentoService, never()).cambiarEstado(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void cambiarEstado_conAdministrador_debePermitirAcceso() throws Exception {
        when(tipoDocumentoService.cambiarEstado(eq(1L), any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(patch("/api/tipos-documento/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_ESTADO_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).cambiarEstado(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void cambiarEstado_conActivoNulo_debeResponder400() throws Exception {
        mockMvc.perform(patch("/api/tipos-documento/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TIPO_DOCUMENTO_ESTADO_REQUEST_INVALIDO_JSON))
                .andExpect(status().isBadRequest());

        verify(tipoDocumentoService, never()).cambiarEstado(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listarActivos_conAdministrador_debeResponder200() throws Exception {
        when(tipoDocumentoService.listarActivos()).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/tipos-documento/activos"))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).listarActivos();
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listarActivos_conJefeArea_debeResponder200() throws Exception {
        when(tipoDocumentoService.listarActivos()).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/tipos-documento/activos"))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).listarActivos();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listarActivos_conAdministrativo_debeResponder200() throws Exception {
        when(tipoDocumentoService.listarActivos()).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/tipos-documento/activos"))
                .andExpect(status().isOk());

        verify(tipoDocumentoService).listarActivos();
    }

    @Test
    void listarActivos_sinAutenticacion_debeResponder401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tipos-documento/activos"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertRespuestaDeError(result, "Autenticación requerida");
        verifyNoInteractions(tipoDocumentoService);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            TipoDocumentoController.class
    })
    static class ConfiguracionSeguridadTiposDocumentoTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
