package com.plantarsas.gestiondocumental.documentos.controller;

/*
 * PROPUESTA — extiende el archivo real actual, aún no copiada al repositorio.
 *
 * DocumentoController ya implementa el endpoint de publicación inicial (POST /api/documentos).
 * Esta propuesta agrega la cobertura MockMvc para el endpoint de publicación de nuevas versiones:
 *
 *   - POST /api/documentos/{id}/versiones, multipart/form-data.
 *   - Parte "metadata": JSON que mapea a NuevaVersionDocumentoRequest, resuelto con
 *     @Valid @RequestPart (obligatoria; su ausencia produce 400 mediante el comportamiento por
 *     defecto de MissingServletRequestPartException).
 *   - Parte "archivo": MultipartFile resuelto con @RequestPart (obligatoria, mismo mecanismo de 400).
 *   - @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser.
 *   - @PreAuthorize("hasRole('ADMINISTRADOR')") en el método.
 *   - Mismo fast-fail de archivo.isEmpty() -> 400 antes de invocar al servicio, mismo patrón de
 *     try-with-resources e IOException -> UncheckedIOException que publicarInicial.
 *   - Éxito: 201 con ApiResponse.exitosa(DocumentoResponse).
 *
 * A diferencia de la publicación inicial, este endpoint NO requiere ningún handler nuevo en
 * GlobalExceptionHandler: los 9 handlers actuales ya cubren BusinessException (incluye
 * ResourceNotFoundException y el caso de estado inválido), MissingServletRequestPartException,
 * MaxUploadSizeExceededException y el catch-all de Exception (que es, a propósito, el destino
 * correcto de IllegalStateException cuando el servicio detecta la inconsistencia interna de una
 * versión vigente ausente).
 *
 * Etapa 3C agrega la cobertura MockMvc de GET /api/documentos/{id}/descarga: misma política
 * @PreAuthorize que listar/obtenerPorId, mismo GlobalExceptionHandler sin handlers nuevos (404
 * indistinguible para documento inexistente/no visible, 500 para inconsistencia interna e
 * IOException real). Lo nuevo a verificar es exclusivo de la respuesta HTTP binaria: Content-Type,
 * Content-Disposition attachment y Content-Length calculados a partir de DocumentoArchivoDescarga.
 *
 * Etapa 3D agrega la cobertura MockMvc de GET /api/documentos/{id}/versiones (histórico) y
 * GET /api/documentos/{id}/versiones/{versionId}/descarga (descarga histórica). Ambos usan
 * @PreAuthorize("hasAnyRole('ADMINISTRADOR','JEFE_AREA')") — a diferencia de listar/obtenerPorId/
 * descarga vigente, ADMINISTRATIVO NO está incluido, por lo que el propio mecanismo de
 * @PreAuthorize produce 403 sin código nuevo. La descarga histórica reutiliza el mismo helper
 * privado del controller (construirRespuestaDescarga) que ya arma la respuesta de la vigente, así
 * que sus headers se prueban con el mismo criterio, no una implementación distinta.
 *
 * Etapa 4C agrega un único caso de hardening: si el tipoMime persistido en BD llega inválido,
 * nulo o vacío hasta el controller (dato antiguo, corrupción o manipulación manual de BD), el
 * helper privado DocumentoController#resolverMediaType(...) debe evitar que
 * MediaType.parseMediaType(...) lance InvalidMediaTypeException y produzca un 500 — en su lugar
 * la descarga debe completarse igualmente con Content-Type application/octet-stream. El resto de
 * los tests de esta clase no cambia.
 */

import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoActualizacionRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoArchivoDescarga;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoEstadoActualizacionRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoFiltroRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.NuevaVersionDocumentoRequest;
import com.plantarsas.gestiondocumental.documentos.dto.VersionHistoricaResponse;
import com.plantarsas.gestiondocumental.documentos.service.DocumentoConsultaService;
import com.plantarsas.gestiondocumental.documentos.service.DocumentoService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.GlobalExceptionHandler;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DocumentoControllerSecurityTest.ConfiguracionSeguridadDocumentosTest.class)
@WebAppConfiguration
class DocumentoControllerSecurityTest {

    private static final String URL_PUBLICACION_INICIAL = "/api/documentos";

    private static final String URL_DOCUMENTOS = "/api/documentos";

    private static final String METADATA_VALIDA_JSON =
            "{\"codigo\":\"PROC-001\",\"titulo\":\"Titulo\",\"descripcion\":\"Descripcion\","
                    + "\"areaId\":1,\"subprogramaId\":2,\"tipoDocumentoId\":3,"
                    + "\"descripcionVersionInicial\":\"Publicacion inicial\","
                    + "\"alcance\":\"AREA_RESPONSABLE\",\"areasAdicionalesIds\":[]}";

    private static final String METADATA_INVALIDA_JSON =
            "{\"codigo\":\"\",\"titulo\":\"Titulo\",\"descripcion\":\"Descripcion\","
                    + "\"areaId\":1,\"subprogramaId\":2,\"tipoDocumentoId\":3,"
                    + "\"descripcionVersionInicial\":\"Publicacion inicial\"}";

    private static final String METADATA_JSON_MALFORMADO =
            "{\"codigo\":\"PROC-001\", \"titulo\": ";

    private static final String METADATA_SIN_ALCANCE_JSON =
            "{\"codigo\":\"PROC-001\",\"titulo\":\"Titulo\",\"descripcion\":\"Descripcion\","
                    + "\"areaId\":1,\"subprogramaId\":2,\"tipoDocumentoId\":3,"
                    + "\"descripcionVersionInicial\":\"Publicacion inicial\","
                    + "\"areasAdicionalesIds\":[]}";

    private static final String METADATA_SIN_AREAS_ADICIONALES_IDS_JSON =
            "{\"codigo\":\"PROC-001\",\"titulo\":\"Titulo\",\"descripcion\":\"Descripcion\","
                    + "\"areaId\":1,\"subprogramaId\":2,\"tipoDocumentoId\":3,"
                    + "\"descripcionVersionInicial\":\"Publicacion inicial\","
                    + "\"alcance\":\"AREA_RESPONSABLE\"}";

    private static final String METADATA_ACTUALIZACION_VALIDA_JSON =
            "{\"codigo\":\"PROC-001\",\"titulo\":\"Titulo actualizado\",\"descripcion\":\"Descripcion actualizada\","
                    + "\"areaId\":1,\"subprogramaId\":2,\"tipoDocumentoId\":3,"
                    + "\"alcance\":\"AREA_RESPONSABLE\",\"areasAdicionalesIds\":[]}";

    private static final Long DOCUMENTO_ID = 10L;

    private static final String URL_ACTUALIZACION = "/api/documentos/" + DOCUMENTO_ID;

    private static final String URL_ESTADO = "/api/documentos/" + DOCUMENTO_ID + "/estado";

    private static final String ESTADO_INACTIVO_REQUEST_JSON = "{\"estado\":\"INACTIVO\"}";

    private static final String ESTADO_REQUEST_INVALIDO_JSON = "{\"estado\":null}";

    private static final String URL_NUEVA_VERSION = "/api/documentos/" + DOCUMENTO_ID + "/versiones";

    private static final String URL_DESCARGA = "/api/documentos/" + DOCUMENTO_ID + "/descarga";

    private static final Long VERSION_ID = 55L;

    private static final String URL_DESCARGA_HISTORICA = URL_NUEVA_VERSION + "/" + VERSION_ID + "/descarga";

    private static final String METADATA_NUEVA_VERSION_VALIDA_JSON =
            "{\"descripcionCambio\":\"Corrección de erratas\"}";

    private static final String METADATA_NUEVA_VERSION_DESCRIPCION_VACIA_JSON =
            "{\"descripcionCambio\":\"\"}";

    private static final String METADATA_NUEVA_VERSION_DESCRIPCION_DEMASIADO_LARGA_JSON =
            "{\"descripcionCambio\":\"" + "a".repeat(501) + "\"}";

    private static final String METADATA_NUEVA_VERSION_JSON_MALFORMADO =
            "{\"descripcionCambio\": ";

    @MockitoBean
    private DocumentoService documentoService;

    @MockitoBean
    private DocumentoConsultaService documentoConsultaService;

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
        reset(documentoService, documentoConsultaService, jwtService, usuarioRepository);
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

    private MockMultipartFile metadataSinAlcance() {
        return new MockMultipartFile("metadata", "", "application/json", METADATA_SIN_ALCANCE_JSON.getBytes());
    }

    private MockMultipartFile metadataSinAreasAdicionalesIds() {
        return new MockMultipartFile(
                "metadata", "", "application/json", METADATA_SIN_AREAS_ADICIONALES_IDS_JSON.getBytes()
        );
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
                "Publicacion inicial", 1L, ahora, ahora, ahora,
                DocumentoAlcance.AREA_RESPONSABLE, List.of()
        );
    }

    private DocumentoResumenResponse resumenDePrueba() {
        return new DocumentoResumenResponse(
                1L, "PROC-001", "Titulo", DocumentoEstado.PUBLICADO, DocumentoAlcance.AREA_RESPONSABLE,
                "Subprograma", "TipoDocumento", LocalDateTime.now()
        );
    }

    private DocumentoArchivoDescarga archivoDescargaDePrueba() {
        return new DocumentoArchivoDescarga(
                "informe.pdf", "application/pdf", 9L, new ByteArrayInputStream("contenido".getBytes())
        );
    }

    private DocumentoArchivoDescarga archivoDescargaConMimeInvalidoDePrueba() {
        return new DocumentoArchivoDescarga(
                "informe.pdf", "mime invalido", 9L, new ByteArrayInputStream("contenido".getBytes())
        );
    }

    private VersionHistoricaResponse versionHistoricaDePrueba() {
        return new VersionHistoricaResponse(
                2L, 2, "informe-v2.pdf", "application/pdf", 9L,
                "Corrección de erratas", LocalDateTime.now(), 1L, "Administrador Local", true
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
    void publicarInicial_sinAlcance_debeResponderBadRequest() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataSinAlcance())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarInicial_sinAreasAdicionalesIds_debeResponderBadRequest() throws Exception {
        mockMvc.perform(multipart(URL_PUBLICACION_INICIAL)
                        .file(metadataSinAreasAdicionalesIds())
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

    // ------------------------------------------------------------------
    // POST /api/documentos/{id}/versiones
    // ------------------------------------------------------------------

    private MockMultipartFile metadataNuevaVersionValida() {
        return new MockMultipartFile(
                "metadata", "", "application/json", METADATA_NUEVA_VERSION_VALIDA_JSON.getBytes()
        );
    }

    private MockMultipartFile metadataNuevaVersionDescripcionVacia() {
        return new MockMultipartFile(
                "metadata", "", "application/json", METADATA_NUEVA_VERSION_DESCRIPCION_VACIA_JSON.getBytes()
        );
    }

    private MockMultipartFile metadataNuevaVersionDescripcionDemasiadoLarga() {
        return new MockMultipartFile(
                "metadata", "", "application/json",
                METADATA_NUEVA_VERSION_DESCRIPCION_DEMASIADO_LARGA_JSON.getBytes()
        );
    }

    private MockMultipartFile metadataNuevaVersionConJsonMalformado() {
        return new MockMultipartFile(
                "metadata", "", "application/json", METADATA_NUEVA_VERSION_JSON_MALFORMADO.getBytes()
        );
    }

    @Test
    void publicarNuevaVersion_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void publicarNuevaVersion_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void publicarNuevaVersion_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_conAdministradorYSolicitudValida_debeResponder201() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenReturn(respuestaDePrueba());

        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isCreated());

        verify(documentoService).publicarNuevaVersion(
                eq(DOCUMENTO_ID), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        );
    }

    @Test
    void publicarNuevaVersion_sinMetadata_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_sinArchivo_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_conMetadataJsonInvalido_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionConJsonMalformado())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_conDescripcionCambioVacia_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionDescripcionVacia())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_conDescripcionCambioDemasiadoLarga_debeResponder400() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionDescripcionDemasiadoLarga())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_conArchivoVacio_debeResponder400YNoInvocarServicio() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoVacio())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_conDocumentoInexistente_debeResponder404() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenThrow(new ResourceNotFoundException("No existe un documento con id " + DOCUMENTO_ID));

        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicarNuevaVersion_conDocumentoInactivo_debePermitirAcceso() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenReturn(respuestaDePrueba());

        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isCreated());
    }

    @Test
    void publicarNuevaVersion_conDocumentoObsoleto_debeResponder400() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenThrow(new BusinessException(
                "El documento 'PROC-002' no permite publicar nuevas versiones mientras esté obsoleto"
        ));

        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicarNuevaVersion_conArchivoRechazadoPorTamano_debeResponder413() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenThrow(new BusinessException("El archivo supera el tamaño máximo permitido", HttpStatus.PAYLOAD_TOO_LARGE));

        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void publicarNuevaVersion_conIOExceptionAlLeerArchivo_debeResponder500() throws Exception {
        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoQueFallaAlLeer())
                        .with(administradorAutenticado()))
                .andExpect(status().isInternalServerError());

        verifyNoInteractions(documentoService);
    }

    @Test
    void publicarNuevaVersion_debeEnviarLosSieteArgumentosCorrectosYLeerElArchivoAntesDeCerrarlo()
            throws Exception {
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<NuevaVersionDocumentoRequest> metadataCaptor =
                ArgumentCaptor.forClass(NuevaVersionDocumentoRequest.class);
        ArgumentCaptor<AuthenticatedUser> usuarioCaptor = ArgumentCaptor.forClass(AuthenticatedUser.class);
        ArgumentCaptor<String> nombreCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mimeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> tamanoCaptor = ArgumentCaptor.forClass(Long.class);
        byte[][] bytesLeidosDentroDelServicio = new byte[1][];

        when(documentoService.publicarNuevaVersion(
                idCaptor.capture(), metadataCaptor.capture(), usuarioCaptor.capture(),
                nombreCaptor.capture(), any(InputStream.class), mimeCaptor.capture(), tamanoCaptor.capture()
        )).thenAnswer(invocacion -> {
            InputStream contenidoArchivo = invocacion.getArgument(4);
            bytesLeidosDentroDelServicio[0] = contenidoArchivo.readAllBytes();
            return respuestaDePrueba();
        });

        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isCreated());

        assertThat(idCaptor.getValue()).isEqualTo(DOCUMENTO_ID);
        assertThat(metadataCaptor.getValue().descripcionCambio()).isEqualTo("Corrección de erratas");
        assertThat(usuarioCaptor.getValue().rol()).isEqualTo(RolEnum.ADMINISTRADOR);
        assertThat(nombreCaptor.getValue()).isEqualTo("documento.pdf");
        assertThat(mimeCaptor.getValue()).isEqualTo("application/pdf");
        assertThat(tamanoCaptor.getValue()).isEqualTo((long) "contenido".getBytes().length);
        assertThat(bytesLeidosDentroDelServicio[0]).isEqualTo("contenido".getBytes());
    }

    @Test
    void publicarNuevaVersion_conExito_debeResponderConEstructuraApiResponseValida() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenReturn(respuestaDePrueba());

        MvcResult result = mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("exito").asBoolean()).isTrue();
        assertThat(json.get("mensaje").isNull()).isTrue();
        assertThat(json.get("datos").get("codigo").asText()).isEqualTo("PROC-001");
        assertThat(json.get("errores").isNull()).isTrue();
        assertThat(json.get("fechaHora").asText()).isNotBlank();
    }

    // ------------------------------------------------------------------
    // GET /api/documentos
    // ------------------------------------------------------------------

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conAdministrador_debeResponder200() throws Exception {
        when(documentoConsultaService.listar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resumenDePrueba())));

        mockMvc.perform(get(URL_DOCUMENTOS).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listar_conJefeArea_debeResponder200() throws Exception {
        when(documentoConsultaService.listar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resumenDePrueba())));

        mockMvc.perform(get(URL_DOCUMENTOS))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listar_conAdministrativo_debeResponder200() throws Exception {
        when(documentoConsultaService.listar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resumenDePrueba())));

        mockMvc.perform(get(URL_DOCUMENTOS))
                .andExpect(status().isOk());
    }

    @Test
    void listar_conPageNegativo_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("page", "-1").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conSizeCero_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("size", "0").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conSizeMayorAlMaximoPermitido_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("size", "101").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    // ------------------------------------------------------------------
    // GET /api/documentos — filtros (Etapa 3B)
    // ------------------------------------------------------------------

    @Test
    void listar_conAreaIdCero_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("areaId", "0").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conAreaIdNegativo_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("areaId", "-1").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conSubprogramaIdInvalido_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("subprogramaId", "0").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conTipoDocumentoIdInvalido_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("tipoDocumentoId", "-5").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conFechaDesdePosteriorAFechaHasta_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS)
                        .param("fechaDesde", "2026-12-31")
                        .param("fechaHasta", "2026-01-01")
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conEstadoInvalido_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("estado", "NO_EXISTE").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_conFechaMalFormada_debeResponder400() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS).param("fechaDesde", "no-es-una-fecha").with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listar_debeTransferirTodosLosParametrosDeFiltroAlDTO() throws Exception {
        ArgumentCaptor<DocumentoFiltroRequest> filtroCaptor = ArgumentCaptor.forClass(DocumentoFiltroRequest.class);
        when(documentoConsultaService.listar(any(), filtroCaptor.capture(), any()))
                .thenReturn(new PageImpl<>(List.of(resumenDePrueba())));

        mockMvc.perform(get(URL_DOCUMENTOS)
                        .param("codigo", "SG-SST")
                        .param("titulo", "Procedimiento")
                        .param("areaId", "5")
                        .param("subprogramaId", "8")
                        .param("tipoDocumentoId", "3")
                        .param("estado", "PUBLICADO")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-12-31")
                        .with(administradorAutenticado()))
                .andExpect(status().isOk());

        DocumentoFiltroRequest filtro = filtroCaptor.getValue();
        assertThat(filtro.codigo()).isEqualTo("SG-SST");
        assertThat(filtro.titulo()).isEqualTo("Procedimiento");
        assertThat(filtro.areaId()).isEqualTo(5L);
        assertThat(filtro.subprogramaId()).isEqualTo(8L);
        assertThat(filtro.tipoDocumentoId()).isEqualTo(3L);
        assertThat(filtro.estado()).isEqualTo(DocumentoEstado.PUBLICADO);
        assertThat(filtro.fechaDesde()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(filtro.fechaHasta()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void listar_sinParametrosDeFiltro_debeTransferirDTOVacio() throws Exception {
        ArgumentCaptor<DocumentoFiltroRequest> filtroCaptor = ArgumentCaptor.forClass(DocumentoFiltroRequest.class);
        when(documentoConsultaService.listar(any(), filtroCaptor.capture(), any()))
                .thenReturn(new PageImpl<>(List.of(resumenDePrueba())));

        mockMvc.perform(get(URL_DOCUMENTOS).with(administradorAutenticado()))
                .andExpect(status().isOk());

        DocumentoFiltroRequest filtro = filtroCaptor.getValue();
        assertThat(filtro.codigo()).isNull();
        assertThat(filtro.titulo()).isNull();
        assertThat(filtro.areaId()).isNull();
        assertThat(filtro.subprogramaId()).isNull();
        assertThat(filtro.tipoDocumentoId()).isNull();
        assertThat(filtro.estado()).isNull();
        assertThat(filtro.fechaDesde()).isNull();
        assertThat(filtro.fechaHasta()).isNull();
    }

    // ------------------------------------------------------------------
    // GET /api/documentos/{id}
    // ------------------------------------------------------------------

    @Test
    void obtenerPorId_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_DOCUMENTOS + "/" + DOCUMENTO_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void obtenerPorId_conAdministrador_debeResponder200() throws Exception {
        when(documentoConsultaService.obtenerPorId(eq(DOCUMENTO_ID), any()))
                .thenReturn(respuestaDePrueba());

        mockMvc.perform(get(URL_DOCUMENTOS + "/" + DOCUMENTO_ID).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void obtenerPorId_conJefeArea_debeResponder200() throws Exception {
        when(documentoConsultaService.obtenerPorId(eq(DOCUMENTO_ID), any()))
                .thenReturn(respuestaDePrueba());

        mockMvc.perform(get(URL_DOCUMENTOS + "/" + DOCUMENTO_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void obtenerPorId_conAdministrativo_debeResponder200() throws Exception {
        when(documentoConsultaService.obtenerPorId(eq(DOCUMENTO_ID), any()))
                .thenReturn(respuestaDePrueba());

        mockMvc.perform(get(URL_DOCUMENTOS + "/" + DOCUMENTO_ID))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorId_conDocumentoInexistenteONoVisible_debeResponder404() throws Exception {
        when(documentoConsultaService.obtenerPorId(eq(DOCUMENTO_ID), any()))
                .thenThrow(new ResourceNotFoundException("No existe un documento con id " + DOCUMENTO_ID));

        mockMvc.perform(get(URL_DOCUMENTOS + "/" + DOCUMENTO_ID).with(administradorAutenticado()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // GET /api/documentos/{id}/descarga (Etapa 3C)
    // ------------------------------------------------------------------

    @Test
    void descargarVersionVigente_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_DESCARGA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void descargarVersionVigente_conAdministrador_debeResponder200() throws Exception {
        when(documentoConsultaService.descargarVersionVigente(eq(DOCUMENTO_ID), any()))
                .thenReturn(archivoDescargaDePrueba());

        mockMvc.perform(get(URL_DESCARGA).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void descargarVersionVigente_conJefeArea_debeResponder200() throws Exception {
        when(documentoConsultaService.descargarVersionVigente(eq(DOCUMENTO_ID), any()))
                .thenReturn(archivoDescargaDePrueba());

        mockMvc.perform(get(URL_DESCARGA))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void descargarVersionVigente_conAdministrativo_debeResponder200() throws Exception {
        when(documentoConsultaService.descargarVersionVigente(eq(DOCUMENTO_ID), any()))
                .thenReturn(archivoDescargaDePrueba());

        mockMvc.perform(get(URL_DESCARGA))
                .andExpect(status().isOk());
    }

    @Test
    void descargarVersionVigente_conExito_debeIncluirHeadersDeDescargaYCuerpoCorrecto() throws Exception {
        when(documentoConsultaService.descargarVersionVigente(eq(DOCUMENTO_ID), any()))
                .thenReturn(archivoDescargaDePrueba());

        mockMvc.perform(get(URL_DESCARGA).with(administradorAutenticado()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("informe.pdf")))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 9L))
                .andExpect(content().bytes("contenido".getBytes()));
    }

    @Test
    void descargarVersionVigente_conDocumentoInexistenteONoVisible_debeResponder404() throws Exception {
        when(documentoConsultaService.descargarVersionVigente(eq(DOCUMENTO_ID), any()))
                .thenThrow(new ResourceNotFoundException("No existe un documento con id " + DOCUMENTO_ID));

        mockMvc.perform(get(URL_DESCARGA).with(administradorAutenticado()))
                .andExpect(status().isNotFound());
    }

    @Test
    void descargarVersionVigente_conMimeInvalidoAlmacenado_debeResponderOctetStreamSinFallar() throws Exception {
        when(documentoConsultaService.descargarVersionVigente(eq(DOCUMENTO_ID), any()))
                .thenReturn(archivoDescargaConMimeInvalidoDePrueba());

        mockMvc.perform(get(URL_DESCARGA).with(administradorAutenticado()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/octet-stream"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    // ------------------------------------------------------------------
    // GET /api/documentos/{id}/versiones (Etapa 3D — histórico)
    // ------------------------------------------------------------------

    @Test
    void listarHistorico_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_NUEVA_VERSION))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listarHistorico_conAdministrador_debeResponder200() throws Exception {
        when(documentoConsultaService.listarHistorico(eq(DOCUMENTO_ID), any()))
                .thenReturn(List.of(versionHistoricaDePrueba()));

        mockMvc.perform(get(URL_NUEVA_VERSION).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listarHistorico_conJefeArea_debeResponder200() throws Exception {
        when(documentoConsultaService.listarHistorico(eq(DOCUMENTO_ID), any()))
                .thenReturn(List.of(versionHistoricaDePrueba()));

        mockMvc.perform(get(URL_NUEVA_VERSION))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listarHistorico_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_NUEVA_VERSION))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void listarHistorico_conDocumentoInexistenteONoVisible_debeResponder404() throws Exception {
        when(documentoConsultaService.listarHistorico(eq(DOCUMENTO_ID), any()))
                .thenThrow(new ResourceNotFoundException("No existe un documento con id " + DOCUMENTO_ID));

        mockMvc.perform(get(URL_NUEVA_VERSION).with(administradorAutenticado()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // GET /api/documentos/{id}/versiones/{versionId}/descarga (Etapa 3D)
    // ------------------------------------------------------------------

    @Test
    void descargarVersionHistorica_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_DESCARGA_HISTORICA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void descargarVersionHistorica_conAdministrador_debeResponder200() throws Exception {
        when(documentoConsultaService.descargarVersionHistorica(eq(DOCUMENTO_ID), eq(VERSION_ID), any()))
                .thenReturn(archivoDescargaDePrueba());

        mockMvc.perform(get(URL_DESCARGA_HISTORICA).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void descargarVersionHistorica_conJefeArea_debeResponder200() throws Exception {
        when(documentoConsultaService.descargarVersionHistorica(eq(DOCUMENTO_ID), eq(VERSION_ID), any()))
                .thenReturn(archivoDescargaDePrueba());

        mockMvc.perform(get(URL_DESCARGA_HISTORICA))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void descargarVersionHistorica_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_DESCARGA_HISTORICA))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoConsultaService);
    }

    @Test
    void descargarVersionHistorica_conExito_debeIncluirLosMismosHeadersQueLaDescargaVigente() throws Exception {
        when(documentoConsultaService.descargarVersionHistorica(eq(DOCUMENTO_ID), eq(VERSION_ID), any()))
                .thenReturn(archivoDescargaDePrueba());

        mockMvc.perform(get(URL_DESCARGA_HISTORICA).with(administradorAutenticado()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("informe.pdf")))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 9L))
                .andExpect(content().bytes("contenido".getBytes()));
    }

    @Test
    void descargarVersionHistorica_conVersionInexistenteODeOtroDocumento_debeResponder404() throws Exception {
        when(documentoConsultaService.descargarVersionHistorica(eq(DOCUMENTO_ID), eq(VERSION_ID), any()))
                .thenThrow(new ResourceNotFoundException(
                        "No existe la versión con id " + VERSION_ID + " para el documento con id " + DOCUMENTO_ID
                ));

        mockMvc.perform(get(URL_DESCARGA_HISTORICA).with(administradorAutenticado()))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarMetadatos_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(put(URL_ACTUALIZACION)
                        .contentType("application/json")
                        .content(METADATA_ACTUALIZACION_VALIDA_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void actualizarMetadatos_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(put(URL_ACTUALIZACION)
                        .contentType("application/json")
                        .content(METADATA_ACTUALIZACION_VALIDA_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void actualizarMetadatos_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(put(URL_ACTUALIZACION)
                        .contentType("application/json")
                        .content(METADATA_ACTUALIZACION_VALIDA_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentoService);
    }

    @Test
    void actualizarMetadatos_conAdministradorYSolicitudValida_debeResponder200() throws Exception {
        when(documentoService.actualizarMetadatos(eq(DOCUMENTO_ID), any(), any()))
                .thenReturn(respuestaDePrueba());

        mockMvc.perform(put(URL_ACTUALIZACION)
                        .contentType("application/json")
                        .content(METADATA_ACTUALIZACION_VALIDA_JSON)
                        .with(administradorAutenticado()))
                .andExpect(status().isOk());

        verify(documentoService).actualizarMetadatos(eq(DOCUMENTO_ID), any(), any());
    }

    @Test
    void actualizarMetadatos_conDocumentoObsoleto_debeResponder400() throws Exception {
        when(documentoService.actualizarMetadatos(eq(DOCUMENTO_ID), any(), any()))
                .thenThrow(new BusinessException(
                        "No se puede editar una publicación obsoleta. Actívala nuevamente para modificarla."
                ));

        mockMvc.perform(put(URL_ACTUALIZACION)
                        .contentType("application/json")
                        .content(METADATA_ACTUALIZACION_VALIDA_JSON)
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // PATCH /api/documentos/{id}/estado (E1)
    // ------------------------------------------------------------------

    @Test
    void cambiarEstado_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(patch(URL_ESTADO)
                        .contentType("application/json")
                        .content(ESTADO_INACTIVO_REQUEST_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(documentoService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void cambiarEstado_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(patch(URL_ESTADO)
                        .contentType("application/json")
                        .content(ESTADO_INACTIVO_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(documentoService, never()).cambiarEstado(anyLong(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void cambiarEstado_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(patch(URL_ESTADO)
                        .contentType("application/json")
                        .content(ESTADO_INACTIVO_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(documentoService, never()).cambiarEstado(anyLong(), any(), any());
    }

    @Test
    void cambiarEstado_conAdministrador_debePermitirAcceso() throws Exception {
        when(documentoService.cambiarEstado(eq(DOCUMENTO_ID), any(), any())).thenReturn(respuestaDePrueba());

        mockMvc.perform(patch(URL_ESTADO)
                        .contentType("application/json")
                        .content(ESTADO_INACTIVO_REQUEST_JSON)
                        .with(administradorAutenticado()))
                .andExpect(status().isOk());

        verify(documentoService).cambiarEstado(eq(DOCUMENTO_ID), any(), any());
    }

    @Test
    void cambiarEstado_conEstadoNulo_debeResponder400() throws Exception {
        mockMvc.perform(patch(URL_ESTADO)
                        .contentType("application/json")
                        .content(ESTADO_REQUEST_INVALIDO_JSON)
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());

        verify(documentoService, never()).cambiarEstado(anyLong(), any(), any());
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
