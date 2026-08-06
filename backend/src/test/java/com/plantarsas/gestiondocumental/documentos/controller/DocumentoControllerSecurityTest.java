package com.plantarsas.gestiondocumental.documentos.controller;

/*
 * PROPUESTA — no forma parte del código fuente todavía.
 *
 * DocumentoController.java está vacío en este momento (no existe ningún endpoint). Este archivo
 * describe, mediante pruebas MockMvc, el contrato que se espera que implemente el endpoint de
 * publicación inicial multipart, siguiendo el patrón de TipoDocumentoControllerSecurityTest.
 *
 * Contrato asumido para DocumentoController (a implementar):
 *   - POST /api/documentos, multipart/form-data.
 *   - Parte "metadata": JSON que mapea a DocumentoPublicacionInicialRequest, resuelto con
 *     @Valid @RequestPart (obligatoria; su ausencia debe producir 400 mediante el comportamiento
 *     por defecto de MissingServletRequestPartException, que ya trae @ResponseStatus(BAD_REQUEST)).
 *   - Parte "archivo": MultipartFile resuelto con @RequestPart (obligatoria, mismo mecanismo de 400
 *     por ausencia).
 *   - @PreAuthorize("hasRole('ADMINISTRADOR')") en el método.
 *   - El controlador debe rechazar explícitamente un archivo vacío (archivo.isEmpty()) con 400
 *     ANTES de invocar a DocumentoService (a diferencia de DocumentoServiceImpl, que delega esa
 *     validación al StorageService; aquí se exige un fallo rápido sin llegar al servicio).
 *   - Éxito: 201 con ApiResponse.exitosa(DocumentoResponse).
 *
 * Requisito adicional sobre GlobalExceptionHandler (aún no existe):
 *   - @ExceptionHandler(MaxUploadSizeExceededException.class) -> 413 (PAYLOAD_TOO_LARGE).
 *     Sin este handler, esta excepción cae en el manejador genérico de Exception y responde 500.
 *
 * La prueba 11 (MaxUploadSizeExceededException) stubea el servicio para lanzar esa excepción.
 * En producción la excepción normalmente se origina en el resolutor de multipart antes de llegar
 * al controlador, no en el servicio; aquí se usa el mock del servicio únicamente como mecanismo
 * para verificar el mapeo de esa excepción a 413 en GlobalExceptionHandler a través de MockMvc.
 */

import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.service.DocumentoService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.GlobalExceptionHandler;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DocumentoControllerSecurityTest.ConfiguracionSeguridadDocumentosTest.class)
@WebAppConfiguration
class DocumentoControllerSecurityTest {

    private static final String URL_PUBLICACION_INICIAL = "/api/documentos";

    private static final String METADATA_VALIDA_JSON =
            "{\"codigo\":\"PROC-001\",\"titulo\":\"Titulo\",\"descripcion\":\"Descripcion\","
                    + "\"areaId\":1,\"subprogramaId\":2,\"tipoDocumentoId\":3,"
                    + "\"descripcionVersionInicial\":\"Publicacion inicial\"}";

    private static final String METADATA_INVALIDA_JSON =
            "{\"codigo\":\"\",\"titulo\":\"Titulo\",\"descripcion\":\"Descripcion\","
                    + "\"areaId\":1,\"subprogramaId\":2,\"tipoDocumentoId\":3,"
                    + "\"descripcionVersionInicial\":\"Publicacion inicial\"}";

    private static final String METADATA_JSON_MALFORMADO =
            "{\"codigo\":\"PROC-001\", \"titulo\": ";

    @MockitoBean
    private DocumentoService documentoService;

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
        reset(documentoService, jwtService, usuarioRepository);
    }

    private MockMultipartFile metadataValida() {
        return new MockMultipartFile("metadata", "", "application/json", METADATA_VALIDA_JSON.getBytes());
    }

    private MockMultipartFile metadataInvalida() {
        return new MockMultipartFile("metadata", "", "application/json", METADATA_INVALIDA_JSON.getBytes());
    }

    private MockMultipartFile metadataConJsonMalformado() {
        return new MockMultipartFile("metadata", "", "application/json", METADATA_JSON_MALFORMADO.getBytes());
    }

    private MockMultipartFile archivoValido() {
        return new MockMultipartFile("archivo", "documento.pdf", "application/pdf", "contenido".getBytes());
    }

    private MockMultipartFile archivoVacio() {
        return new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);
    }

    private MockMultipartFile archivoQueFallaAlLeer() {
        return new MockMultipartFile("archivo", "documento.pdf", "application/pdf", "contenido".getBytes()) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("Error de lectura simulado");
            }
        };
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor administradorAutenticado() {
        AuthenticatedUser administrador = new AuthenticatedUser(1L, "admin@plantarsas.com", RolEnum.ADMINISTRADOR);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                administrador, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(token);
    }

    private DocumentoResponse respuestaDePrueba() {
        LocalDateTime ahora = LocalDateTime.now();
        return new DocumentoResponse(
                1L, "PROC-001", "Titulo", "Descripcion", DocumentoEstado.PUBLICADO,
                1L, "Area", 2L, "Subprograma", 3L, "TipoDocumento",
                1L, 1, "documento.pdf", "application/pdf", 9L,
                "Publicacion inicial", 1L, ahora, ahora, ahora
        );
    }

    @Test
    void publicar_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoValido()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void publicar_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoValido()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void publicar_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoValido()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicar_conAdministradorYSolicitudValida_debeResponder201() throws Exception {
        when(documentoService.publicarInicial(
                any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenReturn(respuestaDePrueba());

        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isCreated());

        verify(documentoService).publicarInicial(
                any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        );
    }

    @Test
    void publicar_sinMetadata_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicar_sinArchivo_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicar_conMetadataJsonInvalido_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataConJsonMalformado())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicar_conMetadataInvalida_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataInvalida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicar_conArchivoVacio_debeResponder400YNoInvocarServicio() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoVacio())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicar_conBusinessExceptionDelServicio413_debeResponder413() throws Exception {
        when(documentoService.publicarInicial(
                any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenThrow(new BusinessException("El archivo supera el tamaño máximo permitido", HttpStatus.PAYLOAD_TOO_LARGE));

        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void publicar_conMaxUploadSizeExceededException_debeResponder413() throws Exception {
        when(documentoService.publicarInicial(
                any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenThrow(new MaxUploadSizeExceededException(15_728_640L));

        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void publicar_conIOExceptionAlLeerArchivo_debeResponder500() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataValida())
                        .file(archivoQueFallaAlLeer())
                        .with(administradorAutenticado()))
                .andExpect(status().isInternalServerError());

        verifyNoInteractions(documentoService);
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
            DocumentoController.class
    })
    static class ConfiguracionSeguridadDocumentosTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
