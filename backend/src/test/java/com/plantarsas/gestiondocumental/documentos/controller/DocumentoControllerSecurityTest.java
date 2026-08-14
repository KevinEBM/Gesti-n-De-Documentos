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
 */

import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.NuevaVersionDocumentoRequest;
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

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final Long DOCUMENTO_ID = 10L;

    private static final String URL_NUEVA_VERSION = "/api/documentos/" + DOCUMENTO_ID + "/versiones";

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
    void publicarNuevaVersion_conDocumentoInactivo_debeResponder400() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenThrow(new BusinessException(
                "El documento 'PROC-001' no permite publicar nuevas versiones en su estado actual"
        ));

        mockMvc.perform(multipart(URL_NUEVA_VERSION)
                        .file(metadataNuevaVersionValida())
                        .file(archivoValido())
                        .with(administradorAutenticado()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicarNuevaVersion_conDocumentoObsoleto_debeResponder400() throws Exception {
        when(documentoService.publicarNuevaVersion(
                anyLong(), any(), any(), anyString(), any(InputStream.class), anyString(), anyLong()
        )).thenThrow(new BusinessException(
                "El documento 'PROC-002' no permite publicar nuevas versiones en su estado actual"
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
