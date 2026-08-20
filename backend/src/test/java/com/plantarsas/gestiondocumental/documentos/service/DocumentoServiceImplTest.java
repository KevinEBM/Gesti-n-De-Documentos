package com.plantarsas.gestiondocumental.documentos.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.service.AreaLookupService;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoActualizacionRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoEstadoActualizacionRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoPublicacionInicialRequest;
import com.plantarsas.gestiondocumental.documentos.dto.NuevaVersionDocumentoRequest;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.documentos.mapper.DocumentoMapper;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoAreaRepository;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.repository.VersionDocumentoRepository;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.exception.UnauthorizedException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.storage.StorageService;
import com.plantarsas.gestiondocumental.storage.StoredFile;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.subprogramas.service.SubprogramaLookupService;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.tiposdocumento.service.TipoDocumentoLookupService;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceImplTest {

    private static final Long AREA_ID = 1L;
    private static final Long SUBPROGRAMA_ID = 2L;
    private static final Long TIPO_DOCUMENTO_ID = 3L;
    private static final Long USUARIO_ID = 4L;
    private static final String RUTA_ALMACENADA = "550e8400-e29b-41d4-a716-446655440000.pdf";

    private static final Long DOCUMENTO_ID = 10L;
    private static final String RUTA_ARCHIVO_ANTERIOR = "ruta-anterior.pdf";
    private static final String RUTA_ARCHIVO_NUEVO = "660e8400-e29b-41d4-a716-446655440111.pdf";

    private static final Long AREA_ADICIONAL_1_ID = 101L;
    private static final Long AREA_ADICIONAL_2_ID = 102L;
    private static final Long AREA_ADICIONAL_3_ID = 103L;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private DocumentoAreaRepository documentoAreaRepository;

    @Mock
    private VersionDocumentoRepository versionDocumentoRepository;

    @Mock
    private AreaLookupService areaLookupService;

    @Mock
    private SubprogramaLookupService subprogramaLookupService;

    @Mock
    private TipoDocumentoLookupService tipoDocumentoLookupService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentoMapper documentoMapper;

    private DocumentoServiceImpl documentoServiceImpl;

    @BeforeEach
    void inicializar() {
        documentoServiceImpl = new DocumentoServiceImpl(
                documentoRepository,
                documentoAreaRepository,
                versionDocumentoRepository,
                areaLookupService,
                subprogramaLookupService,
                tipoDocumentoLookupService,
                usuarioRepository,
                storageService,
                documentoMapper
        );
    }

    private DocumentoPublicacionInicialRequest requestValido() {
        return new DocumentoPublicacionInicialRequest(
                "PROC-001",
                "Título de prueba",
                "Descripción de prueba",
                AREA_ID,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                "Publicación inicial",
                DocumentoAlcance.AREA_RESPONSABLE,
                List.of()
        );
    }

    private DocumentoPublicacionInicialRequest requestConAlcance(
            DocumentoAlcance alcance, List<Long> areasAdicionalesIds
    ) {
        return new DocumentoPublicacionInicialRequest(
                "PROC-001",
                "Título de prueba",
                "Descripción de prueba",
                AREA_ID,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                "Publicación inicial",
                alcance,
                areasAdicionalesIds
        );
    }

    private AuthenticatedUser usuarioAdministrador() {
        return new AuthenticatedUser(USUARIO_ID, "admin@plantarsas.com", RolEnum.ADMINISTRADOR);
    }

    private InputStream contenidoDePrueba() {
        return new ByteArrayInputStream("contenido".getBytes());
    }

    private StoredFile archivoGuardadoDePrueba() {
        return new StoredFile("documento.pdf", RUTA_ALMACENADA, "application/pdf", 9L, "hash");
    }

    /**
     * area.getId() se stubea porque la mayoría de las pruebas que invocan este helper llegan a la
     * comparación de pertenencia (subprograma.getArea().getId() vs area.getId()). area.getNombre()
     * se elimina: DocumentoServiceImpl nunca lo consulta (solo lo usaría DocumentoMapper, que en
     * esta clase de prueba está mockeado).
     */
    private Area areaActivaMock() {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(AREA_ID);
        return area;
    }

    /**
     * subprograma.getArea() se stubea porque siempre se consulta en la comparación de pertenencia.
     * subprograma.getId() se elimina: DocumentoServiceImpl nunca lo consulta. subprograma.getNombre()
     * también se elimina de este helper compartido porque solo lo consume el único caso que construye
     * el mensaje de "no pertenece al área"; esa prueba lo stubea directamente sobre su propio mock.
     */
    private Subprograma subprogramaActivoMock(Area area) {
        Subprograma subprograma = mock(Subprograma.class);
        when(subprograma.getArea()).thenReturn(area);
        return subprograma;
    }

    /** DocumentoServiceImpl nunca consulta getId() ni getNombre() de TipoDocumento directamente. */
    private TipoDocumento tipoDocumentoActivoMock() {
        return mock(TipoDocumento.class);
    }

    /** DocumentoServiceImpl nunca consulta getId() de Usuario directamente. */
    private Usuario usuarioPersistidoMock() {
        return mock(Usuario.class);
    }

    private void stubValidacionesPrevias(
            Area area, Subprograma subprograma, TipoDocumento tipoDocumento, Usuario usuario
    ) {
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerActivoPorId(TIPO_DOCUMENTO_ID)).thenReturn(tipoDocumento);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
    }

    /**
     * documentoMapper.toResponse(...) se elimina de este helper: ninguna de las pruebas que lo
     * invocan usa el valor de retorno de publicarInicial(...), y en la prueba de fallo de flush el
     * mapper nunca se alcanza (la excepción ocurre antes), por lo que ese stub quedaba sin consumir.
     */
    private void stubGuardarYPersistenciaExitosos() throws IOException {
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoGuardadoDePrueba());
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentoAreaRepository.save(any(DocumentoArea.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versionDocumentoRepository.save(any(VersionDocumento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publicarInicial_debeCrearDocumentoAreaYVersionConDatosNormalizados() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        );

        ArgumentCaptor<Documento> documentoCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).save(documentoCaptor.capture());
        assertThat(documentoCaptor.getValue().getCodigo()).isEqualTo("PROC-001");
        assertThat(documentoCaptor.getValue().getCreadoPor()).isEqualTo(usuario);

        ArgumentCaptor<DocumentoArea> documentoAreaCaptor = ArgumentCaptor.forClass(DocumentoArea.class);
        verify(documentoAreaRepository).save(documentoAreaCaptor.capture());
        assertThat(documentoAreaCaptor.getValue().getArea()).isEqualTo(area);

        ArgumentCaptor<VersionDocumento> versionCaptor = ArgumentCaptor.forClass(VersionDocumento.class);
        verify(versionDocumentoRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getPublicadoPor()).isEqualTo(usuario);

        verify(documentoRepository).flush();
    }

    @Test
    void publicarInicial_debeRechazarUsuarioConRolJefeArea() {
        AuthenticatedUser jefeArea = new AuthenticatedUser(USUARIO_ID, "jefe@plantarsas.com", RolEnum.JEFE_AREA);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), jefeArea,
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository, storageService);
    }

    @Test
    void publicarInicial_debeRechazarUsuarioConRolAdministrativo() {
        AuthenticatedUser administrativo = new AuthenticatedUser(USUARIO_ID, "aux@plantarsas.com", RolEnum.ADMINISTRATIVO);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), administrativo,
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository, storageService);
    }

    @Test
    void publicarInicial_debeLanzarConflictoSiCodigoYaExisteConEspaciosYDistintoCasing() {
        DocumentoPublicacionInicialRequest request = new DocumentoPublicacionInicialRequest(
                "  proc-001  ", "Título", "Descripción", AREA_ID, SUBPROGRAMA_ID, TIPO_DOCUMENTO_ID,
                "Publicación inicial", DocumentoAlcance.AREA_RESPONSABLE, List.of()
        );
        when(documentoRepository.existsByCodigoIgnoreCase("proc-001")).thenReturn(true);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                request, usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarNotFoundSiAreaNoExiste() {
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID))
                .thenThrow(new ResourceNotFoundException("No existe un área con id " + AREA_ID));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarBusinessExceptionSiAreaInactiva() {
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID))
                .thenThrow(new BusinessException("El área 'X' está inactiva y no puede utilizarse"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarNotFoundSiSubprogramaNoExiste() {
        // El flujo termina en subprogramaLookupService antes de la comparación de pertenencia:
        // area no necesita ningún getter stubeado.
        Area area = mock(Area.class);
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(SUBPROGRAMA_ID))
                .thenThrow(new ResourceNotFoundException("No existe un subprograma con id " + SUBPROGRAMA_ID));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarBusinessExceptionSiSubprogramaInactivo() {
        // Mismo motivo que en el caso "no existe": el flujo nunca llega a comparar area.getId().
        Area area = mock(Area.class);
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(SUBPROGRAMA_ID))
                .thenThrow(new BusinessException("El subprograma 'X' está inactivo y no puede utilizarse"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarBusinessExceptionSiSubprogramaNoPerteneceAlArea() {
        Area area = areaActivaMock();
        Area otraArea = mock(Area.class);
        when(otraArea.getId()).thenReturn(99L);
        Subprograma subprograma = subprogramaActivoMock(otraArea);
        // getNombre() solo se consume en esta prueba: el mensaje de la excepción lo construye con el nombre.
        when(subprograma.getNombre()).thenReturn("Subprograma de prueba");

        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarNotFoundSiTipoDocumentoNoExiste() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerActivoPorId(TIPO_DOCUMENTO_ID))
                .thenThrow(new ResourceNotFoundException("No existe un tipo de documento con id " + TIPO_DOCUMENTO_ID));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarBusinessExceptionSiTipoDocumentoInactivo() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerActivoPorId(TIPO_DOCUMENTO_ID))
                .thenThrow(new BusinessException("El tipo de documento 'X' está inactivo y no puede utilizarse"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeLanzarNotFoundSiUsuarioAutenticadoYaNoExiste() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerActivoPorId(TIPO_DOCUMENTO_ID)).thenReturn(tipoDocumento);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debePropagarBusinessExceptionSiArchivoVacio() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new BusinessException("El archivo no puede estar vacío"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "vacio.txt", contenidoDePrueba(), "text/plain", 0L
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void publicarInicial_debePropagarBusinessExceptionSiArchivoSuperaQuinceMB() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new BusinessException("El archivo supera el tamaño máximo permitido"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "grande.pdf", contenidoDePrueba(), "application/pdf", 999_999_999L
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void publicarInicial_debeLanzarUncheckedIOExceptionSiFallaElAlmacenamiento() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new IOException("disco lleno"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(UncheckedIOException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void publicarInicial_debeEliminarArchivoSiFallaElSaveDeDocumento() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoGuardadoDePrueba());
        when(documentoRepository.save(any(Documento.class)))
                .thenThrow(new DataIntegrityViolationException("violación de restricción"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).eliminar(RUTA_ALMACENADA);
    }

    @Test
    void publicarInicial_debeEliminarArchivoSiFallaElSaveDeDocumentoArea() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoGuardadoDePrueba());
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentoAreaRepository.save(any(DocumentoArea.class)))
                .thenThrow(new DataIntegrityViolationException("violación de restricción"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).eliminar(RUTA_ALMACENADA);
    }

    @Test
    void publicarInicial_debeEliminarArchivoSiFallaElSaveDeVersionDocumento() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoGuardadoDePrueba());
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentoAreaRepository.save(any(DocumentoArea.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versionDocumentoRepository.save(any(VersionDocumento.class)))
                .thenThrow(new DataIntegrityViolationException("violación de restricción"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).eliminar(RUTA_ALMACENADA);
    }

    @Test
    void publicarInicial_debeEliminarArchivoSiFallaElFlush() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        doThrow(new DataIntegrityViolationException("violación en flush"))
                .when(documentoRepository).flush();

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).eliminar(RUTA_ALMACENADA);
    }

    @Test
    void publicarInicial_debeEliminarArchivoCuandoLaSincronizacionRegistradaSeCompletaConEstadoDistintoDeCommitted()
            throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        TransactionSynchronizationManager.initSynchronization();
        try {
            documentoServiceImpl.publicarInicial(
                    requestValido(), usuarioAdministrador(),
                    "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
            );

            List<TransactionSynchronization> sincronizaciones =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(sincronizaciones).hasSize(1);

            sincronizaciones.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(storageService).eliminar(RUTA_ALMACENADA);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publicarInicial_noDebeEliminarArchivoCuandoLaSincronizacionSeCompletaConCommitted() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        TransactionSynchronizationManager.initSynchronization();
        try {
            documentoServiceImpl.publicarInicial(
                    requestValido(), usuarioAdministrador(),
                    "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
            );

            List<TransactionSynchronization> sincronizaciones =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(sincronizaciones).hasSize(1);

            sincronizaciones.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(storageService, never()).eliminar(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publicarInicial_noDebeInvocarAlmacenamientoCuandoFallaUnaValidacionPrevia() {
        when(documentoRepository.existsByCodigoIgnoreCase("PROC-001")).thenReturn(true);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeConservarEstadoPublicadoVersionUnoVigenteYEsPrincipal() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        documentoServiceImpl.publicarInicial(
                requestValido(), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        );

        ArgumentCaptor<Documento> documentoCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).save(documentoCaptor.capture());
        assertThat(documentoCaptor.getValue().getEstado()).isEqualTo(DocumentoEstado.PUBLICADO);

        ArgumentCaptor<DocumentoArea> documentoAreaCaptor = ArgumentCaptor.forClass(DocumentoArea.class);
        verify(documentoAreaRepository).save(documentoAreaCaptor.capture());
        assertThat(documentoAreaCaptor.getValue().isEsPrincipal()).isTrue();

        ArgumentCaptor<VersionDocumento> versionCaptor = ArgumentCaptor.forClass(VersionDocumento.class);
        verify(versionDocumentoRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getNumeroVersion()).isEqualTo(1);
        assertThat(versionCaptor.getValue().isVigente()).isTrue();
    }

    @Test
    void publicarInicial_conAreaResponsable_debeCrearSoloAsociacionPrincipal() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        documentoServiceImpl.publicarInicial(
                requestConAlcance(DocumentoAlcance.AREA_RESPONSABLE, List.of()), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        );

        ArgumentCaptor<Documento> documentoCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).save(documentoCaptor.capture());
        assertThat(documentoCaptor.getValue().getAlcance()).isEqualTo(DocumentoAlcance.AREA_RESPONSABLE);

        ArgumentCaptor<DocumentoArea> documentoAreaCaptor = ArgumentCaptor.forClass(DocumentoArea.class);
        verify(documentoAreaRepository, times(1)).save(documentoAreaCaptor.capture());
        assertThat(documentoAreaCaptor.getValue().isEsPrincipal()).isTrue();

        verify(areaLookupService, never()).obtenerActivasPorIds(any());
    }

    @Test
    void publicarInicial_conGlobal_debeCrearSoloAsociacionPrincipalSinInsertarTodasLasAreas() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        documentoServiceImpl.publicarInicial(
                requestConAlcance(DocumentoAlcance.GLOBAL, List.of()), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        );

        ArgumentCaptor<Documento> documentoCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).save(documentoCaptor.capture());
        assertThat(documentoCaptor.getValue().getAlcance()).isEqualTo(DocumentoAlcance.GLOBAL);

        ArgumentCaptor<DocumentoArea> documentoAreaCaptor = ArgumentCaptor.forClass(DocumentoArea.class);
        verify(documentoAreaRepository, times(1)).save(documentoAreaCaptor.capture());
        assertThat(documentoAreaCaptor.getValue().isEsPrincipal()).isTrue();

        verify(areaLookupService, never()).obtenerActivasPorIds(any());
    }

    @Test
    void publicarInicial_conAreasEspecificas_debeCrearPrincipalYAdicionales() throws IOException {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);
        stubGuardarYPersistenciaExitosos();

        Area areaAdicional1 = mock(Area.class);
        Area areaAdicional2 = mock(Area.class);
        List<Long> idsAdicionales = List.of(AREA_ADICIONAL_1_ID, AREA_ADICIONAL_2_ID);
        when(areaLookupService.obtenerActivasPorIds(idsAdicionales))
                .thenReturn(List.of(areaAdicional1, areaAdicional2));

        documentoServiceImpl.publicarInicial(
                requestConAlcance(DocumentoAlcance.AREAS_ESPECIFICAS, idsAdicionales), usuarioAdministrador(),
                "documento.pdf", contenidoDePrueba(), "application/pdf", 9L
        );

        verify(areaLookupService).obtenerActivasPorIds(idsAdicionales);

        ArgumentCaptor<DocumentoArea> documentoAreaCaptor = ArgumentCaptor.forClass(DocumentoArea.class);
        verify(documentoAreaRepository, times(3)).save(documentoAreaCaptor.capture());
        List<DocumentoArea> guardadas = documentoAreaCaptor.getAllValues();

        assertThat(guardadas).hasSize(3);
        assertThat(guardadas.get(0).isEsPrincipal()).isTrue();
        assertThat(guardadas.get(0).getArea()).isEqualTo(area);
        assertThat(guardadas.subList(1, 3)).allSatisfy(
                asociacion -> assertThat(asociacion.isEsPrincipal()).isFalse()
        );
        assertThat(guardadas.subList(1, 3).stream().map(DocumentoArea::getArea).toList())
                .containsExactlyInAnyOrder(areaAdicional1, areaAdicional2);

        ArgumentCaptor<DocumentoArea> principalCaptor = ArgumentCaptor.forClass(DocumentoArea.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentoArea>> adicionalesCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentoMapper).toResponse(
                any(Documento.class), principalCaptor.capture(), adicionalesCaptor.capture(), any(VersionDocumento.class)
        );
        assertThat(principalCaptor.getValue().isEsPrincipal()).isTrue();
        assertThat(adicionalesCaptor.getValue()).hasSize(2);
        assertThat(adicionalesCaptor.getValue()).allSatisfy(
                asociacion -> assertThat(asociacion.isEsPrincipal()).isFalse()
        );
    }

    @Test
    void publicarInicial_debeRechazarAreaResponsableConAdicionalesNoVacias() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestConAlcance(
                        DocumentoAlcance.AREA_RESPONSABLE,
                        List.of(AREA_ADICIONAL_1_ID)
                ),
                usuarioAdministrador(),
                "documento.pdf",
                contenidoDePrueba(),
                "application/pdf",
                9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeRechazarGlobalConAdicionalesNoVacias() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestConAlcance(
                        DocumentoAlcance.GLOBAL,
                        List.of(AREA_ADICIONAL_1_ID)
                ),
                usuarioAdministrador(),
                "documento.pdf",
                contenidoDePrueba(),
                "application/pdf",
                9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeRechazarAreasEspecificasSinAdicionales() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of()
                ),
                usuarioAdministrador(),
                "documento.pdf",
                contenidoDePrueba(),
                "application/pdf",
                9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeRechazarAreasEspecificasConIdsDuplicados() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_1_ID, AREA_ADICIONAL_1_ID)
                ),
                usuarioAdministrador(),
                "documento.pdf",
                contenidoDePrueba(),
                "application/pdf",
                9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debeRechazarAreasEspecificasConPrincipalRepetidaComoAdicional() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ID)
                ),
                usuarioAdministrador(),
                "documento.pdf",
                contenidoDePrueba(),
                "application/pdf",
                9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debePropagarNotFoundSiAreaAdicionalNoExiste() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        List<Long> idsAdicionales = List.of(AREA_ADICIONAL_1_ID);

        when(areaLookupService.obtenerActivasPorIds(idsAdicionales))
                .thenThrow(new ResourceNotFoundException(
                        "No existen áreas con id [" + AREA_ADICIONAL_1_ID + "]"
                ));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        idsAdicionales
                ),
                usuarioAdministrador(),
                "documento.pdf",
                contenidoDePrueba(),
                "application/pdf",
                9L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarInicial_debePropagarBusinessExceptionSiAreaAdicionalEstaInactiva() {
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        Usuario usuario = usuarioPersistidoMock();
        stubValidacionesPrevias(area, subprograma, tipoDocumento, usuario);

        List<Long> idsAdicionales = List.of(AREA_ADICIONAL_1_ID);

        when(areaLookupService.obtenerActivasPorIds(idsAdicionales))
                .thenThrow(new BusinessException(
                        "Las siguientes áreas están inactivas y no pueden utilizarse: X"
                ));

        assertThatThrownBy(() -> documentoServiceImpl.publicarInicial(
                requestConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        idsAdicionales
                ),
                usuarioAdministrador(),
                "documento.pdf",
                contenidoDePrueba(),
                "application/pdf",
                9L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    // ------------------------------------------------------------------
    // publicarNuevaVersion
    // ------------------------------------------------------------------

    private NuevaVersionDocumentoRequest requestNuevaVersionValido() {
        return new NuevaVersionDocumentoRequest("  Corrección de erratas  ");
    }

    private Documento documentoPublicadoDePrueba() {
        return new Documento(
                "PROC-001", "Título", "Descripción",
                mock(Subprograma.class), mock(TipoDocumento.class), usuarioPersistidoMock()
        );
    }

    private DocumentoArea documentoAreaDePrueba(Documento documento) {
        return new DocumentoArea(documento, mock(Area.class));
    }

    private VersionDocumento versionVigenteDePrueba(Documento documento) {
        return new VersionDocumento(
                documento, 3, "v1.pdf", RUTA_ARCHIVO_ANTERIOR, RUTA_ARCHIVO_ANTERIOR,
                "application/pdf", 100L, "Descripción anterior", usuarioPersistidoMock()
        );
    }

    private StoredFile archivoNuevoGuardadoDePrueba() {
        return new StoredFile("v2.pdf", RUTA_ARCHIVO_NUEVO, "application/pdf", 20L, "hash-nuevo");
    }

    private void stubBusquedasPreviasNuevaVersion(
            Documento documento, DocumentoArea documentoArea, VersionDocumento vigenteActual, Usuario usuario
    ) {
        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID)).thenReturn(Optional.of(documentoArea));
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID)).thenReturn(List.of());
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(vigenteActual));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
    }

    private void stubAlmacenamientoYPersistenciaExitosaNuevaVersion(int ultimoNumeroVersion) throws IOException {
        when(versionDocumentoRepository.obtenerUltimoNumeroVersion(DOCUMENTO_ID)).thenReturn(ultimoNumeroVersion);
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoNuevoGuardadoDePrueba());
        when(versionDocumentoRepository.save(any(VersionDocumento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publicarNuevaVersion_debePublicarNuevaVersionCorrectamente() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        ArgumentCaptor<VersionDocumento> versionCaptor = ArgumentCaptor.forClass(VersionDocumento.class);
        verify(versionDocumentoRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getNumeroVersion()).isEqualTo(4);
        assertThat(versionCaptor.getValue().isVigente()).isTrue();
        assertThat(vigenteActual.isVigente()).isFalse();
        verify(versionDocumentoRepository, times(2)).flush();
    }

    @Test
    void publicarNuevaVersion_debeRechazarUsuarioNulo() {
        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), null,
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository, storageService);
    }

    @Test
    void publicarNuevaVersion_debeRechazarUsuarioConRolJefeArea() {
        AuthenticatedUser jefeArea = new AuthenticatedUser(USUARIO_ID, "jefe@plantarsas.com", RolEnum.JEFE_AREA);

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), jefeArea,
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository, storageService);
    }

    @Test
    void publicarNuevaVersion_debeRechazarUsuarioConRolAdministrativo() {
        AuthenticatedUser administrativo = new AuthenticatedUser(USUARIO_ID, "aux@plantarsas.com", RolEnum.ADMINISTRATIVO);

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), administrativo,
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository, storageService);
    }

    @Test
    void publicarNuevaVersion_debeLanzarNotFoundSiDocumentoNoExiste() {
        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarNuevaVersion_conDocumentoInactivo_debePermitirNuevaVersionYConservarEstado() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        documento.cambiarEstado(DocumentoEstado.INACTIVO);
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.INACTIVO);
        verify(versionDocumentoRepository).save(any(VersionDocumento.class));
    }

    @Test
    void publicarNuevaVersion_debeLanzarBusinessExceptionSiDocumentoObsoleto() {
        Documento documento = documentoPublicadoDePrueba();
        documento.cambiarEstado(DocumentoEstado.OBSOLETO);
        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(BusinessException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarNuevaVersion_debeLanzarNotFoundSiDocumentoAreaNoExiste() {
        Documento documento = documentoPublicadoDePrueba();
        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarNuevaVersion_debeLanzarIllegalStateExceptionSiNoHayVersionVigente() {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID)).thenReturn(Optional.of(documentoArea));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarNuevaVersion_debeLanzarNotFoundSiUsuarioAutenticadoYaNoExiste() {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID)).thenReturn(Optional.of(documentoArea));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(vigenteActual));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarNuevaVersion_debePropagarBusinessExceptionSiArchivoVacio() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        when(versionDocumentoRepository.obtenerUltimoNumeroVersion(DOCUMENTO_ID)).thenReturn(3);
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new BusinessException("El archivo no puede estar vacío"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "vacio.txt", contenidoDePrueba(), "text/plain", 0L
        )).isInstanceOf(BusinessException.class);

        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
    }

    @Test
    void publicarNuevaVersion_debePropagarBusinessExceptionSiArchivoSuperaQuinceMB() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        when(versionDocumentoRepository.obtenerUltimoNumeroVersion(DOCUMENTO_ID)).thenReturn(3);
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new BusinessException("El archivo supera el tamaño máximo permitido"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "grande.pdf", contenidoDePrueba(), "application/pdf", 999_999_999L
        )).isInstanceOf(BusinessException.class);

        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
    }

    @Test
    void publicarNuevaVersion_debeLanzarUncheckedIOExceptionSiFallaElAlmacenamiento() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        when(versionDocumentoRepository.obtenerUltimoNumeroVersion(DOCUMENTO_ID)).thenReturn(3);
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new IOException("disco lleno"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(UncheckedIOException.class);

        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
    }

    @Test
    void publicarNuevaVersion_debeCalcularNumeroDeVersionComoUltimoMasUno() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(7);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        ArgumentCaptor<VersionDocumento> versionCaptor = ArgumentCaptor.forClass(VersionDocumento.class);
        verify(versionDocumentoRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getNumeroVersion()).isEqualTo(8);
    }

    @Test
    void publicarNuevaVersion_debeMarcarVersionAnteriorComoNoVigente() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        assertThat(vigenteActual.isVigente()).isFalse();
    }

    @Test
    void publicarNuevaVersion_debeCrearNuevaVersionConVigenteVerdadero() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        ArgumentCaptor<VersionDocumento> versionCaptor = ArgumentCaptor.forClass(VersionDocumento.class);
        verify(versionDocumentoRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().isVigente()).isTrue();
    }

    @Test
    void publicarNuevaVersion_debeRegistrarUsuarioAutenticadoComoPublicador() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        ArgumentCaptor<VersionDocumento> versionCaptor = ArgumentCaptor.forClass(VersionDocumento.class);
        verify(versionDocumentoRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getPublicadoPor()).isEqualTo(usuarioPersistido);
    }

    @Test
    void publicarNuevaVersion_debeNormalizarDescripcionCambioConTrim() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        ArgumentCaptor<VersionDocumento> versionCaptor = ArgumentCaptor.forClass(VersionDocumento.class);
        verify(versionDocumentoRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getDescripcionCambio()).isEqualTo("Corrección de erratas");
    }

    @Test
    void publicarNuevaVersion_debeEjecutarLosDosFlushEnElOrdenObligatorio() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        InOrder orden = inOrder(versionDocumentoRepository);
        orden.verify(versionDocumentoRepository).flush();
        orden.verify(versionDocumentoRepository).save(any(VersionDocumento.class));
        orden.verify(versionDocumentoRepository).flush();
    }

    @Test
    void publicarNuevaVersion_debeEliminarArchivoNuevoSiFallaElPrimerFlush() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        when(versionDocumentoRepository.obtenerUltimoNumeroVersion(DOCUMENTO_ID)).thenReturn(3);
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoNuevoGuardadoDePrueba());
        doThrow(new DataIntegrityViolationException("violación en primer flush"))
                .when(versionDocumentoRepository).flush();

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).eliminar(RUTA_ARCHIVO_NUEVO);
        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
    }

    @Test
    void publicarNuevaVersion_debeEliminarArchivoNuevoSiFallaElSaveDeNuevaVersion() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        when(versionDocumentoRepository.obtenerUltimoNumeroVersion(DOCUMENTO_ID)).thenReturn(3);
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoNuevoGuardadoDePrueba());
        when(versionDocumentoRepository.save(any(VersionDocumento.class)))
                .thenThrow(new DataIntegrityViolationException("violación de restricción"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).eliminar(RUTA_ARCHIVO_NUEVO);
    }

    @Test
    void publicarNuevaVersion_debeEliminarArchivoNuevoSiFallaElSegundoFlush() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);
        doNothing()
                .doThrow(new DataIntegrityViolationException("violación en segundo flush"))
                .when(versionDocumentoRepository).flush();

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).eliminar(RUTA_ARCHIVO_NUEVO);
    }

    @Test
    void publicarNuevaVersion_debeEliminarArchivoNuevoCuandoLaSincronizacionSeCompletaConEstadoDistintoDeCommitted()
            throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        TransactionSynchronizationManager.initSynchronization();
        try {
            documentoServiceImpl.publicarNuevaVersion(
                    DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                    "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
            );

            List<TransactionSynchronization> sincronizaciones =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(sincronizaciones).hasSize(1);

            sincronizaciones.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(storageService).eliminar(RUTA_ARCHIVO_NUEVO);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publicarNuevaVersion_noDebeEliminarArchivoNuevoCuandoLaSincronizacionSeCompletaConCommitted()
            throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        TransactionSynchronizationManager.initSynchronization();
        try {
            documentoServiceImpl.publicarNuevaVersion(
                    DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                    "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
            );

            List<TransactionSynchronization> sincronizaciones =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(sincronizaciones).hasSize(1);

            sincronizaciones.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(storageService, never()).eliminar(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publicarNuevaVersion_nuncaDebeEliminarElArchivoDeLaVersionAnterior() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        when(versionDocumentoRepository.obtenerUltimoNumeroVersion(DOCUMENTO_ID)).thenReturn(3);
        when(storageService.guardar(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(archivoNuevoGuardadoDePrueba());
        when(versionDocumentoRepository.save(any(VersionDocumento.class)))
                .thenThrow(new DataIntegrityViolationException("violación de restricción"));

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService, never()).eliminar(RUTA_ARCHIVO_ANTERIOR);
        verify(storageService).eliminar(RUTA_ARCHIVO_NUEVO);
    }

    @Test
    void publicarNuevaVersion_debeConservarCodigoTituloDescripcionAreaSubprogramaYTipoDocumentalDelDocumento()
            throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        String codigoOriginal = documento.getCodigo();
        String tituloOriginal = documento.getTitulo();
        String descripcionOriginal = documento.getDescripcion();
        Subprograma subprogramaOriginal = documento.getSubprograma();
        TipoDocumento tipoDocumentoOriginal = documento.getTipoDocumento();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        Area areaOriginal = documentoArea.getArea();
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        assertThat(documento.getCodigo()).isEqualTo(codigoOriginal);
        assertThat(documento.getTitulo()).isEqualTo(tituloOriginal);
        assertThat(documento.getDescripcion()).isEqualTo(descripcionOriginal);
        assertThat(documento.getSubprograma()).isEqualTo(subprogramaOriginal);
        assertThat(documento.getTipoDocumento()).isEqualTo(tipoDocumentoOriginal);
        assertThat(documentoArea.getArea()).isEqualTo(areaOriginal);
        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void publicarNuevaVersion_debeUsarBusquedaConBloqueoPesimista() throws IOException {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);
        Usuario usuarioPersistido = usuarioPersistidoMock();
        stubBusquedasPreviasNuevaVersion(documento, documentoArea, vigenteActual, usuarioPersistido);
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        verify(documentoRepository).buscarPorIdConBloqueoPesimista(DOCUMENTO_ID);
        verify(documentoRepository, never()).findById(any());
    }

    @Test
    void publicarNuevaVersion_noDebeAlmacenarArchivoCuandoFallaUnaValidacionPrevia() {
        Documento documento = documentoPublicadoDePrueba();
        DocumentoArea documentoArea = documentoAreaDePrueba(documento);
        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID)).thenReturn(Optional.of(documentoArea));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        )).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void publicarNuevaVersion_debeConservarAlcanceYAreasAdicionalesExistentes() throws IOException {
        Usuario usuarioPersistido = usuarioPersistidoMock();
        Documento documento = new Documento(
                "PROC-001",
                "Título",
                "Descripción",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                usuarioPersistido,
                DocumentoAlcance.AREAS_ESPECIFICAS
        );
        DocumentoArea principal = DocumentoArea.principal(documento, mock(Area.class));
        DocumentoArea adicional1 = DocumentoArea.adicional(documento, mock(Area.class));
        DocumentoArea adicional2 = DocumentoArea.adicional(documento, mock(Area.class));
        VersionDocumento vigenteActual = versionVigenteDePrueba(documento);

        when(documentoRepository.buscarPorIdConBloqueoPesimista(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID)).thenReturn(Optional.of(principal));
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID)).thenReturn(List.of(adicional1, adicional2));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID)).thenReturn(Optional.of(vigenteActual));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioPersistido));
        stubAlmacenamientoYPersistenciaExitosaNuevaVersion(3);

        documentoServiceImpl.publicarNuevaVersion(
                DOCUMENTO_ID, requestNuevaVersionValido(), usuarioAdministrador(),
                "v2.pdf", contenidoDePrueba(), "application/pdf", 20L
        );

        assertThat(documento.getAlcance()).isEqualTo(DocumentoAlcance.AREAS_ESPECIFICAS);

        verify(documentoAreaRepository, never()).save(any(DocumentoArea.class));

        verify(documentoAreaRepository, never()).delete(any(DocumentoArea.class));

        verify(versionDocumentoRepository).save(any(VersionDocumento.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentoArea>> adicionalesCaptor = ArgumentCaptor.forClass(List.class);

        verify(documentoMapper).toResponse(
                eq(documento),
                eq(principal),
                adicionalesCaptor.capture(),
                any(VersionDocumento.class)
        );

        assertThat(adicionalesCaptor.getValue()).containsExactlyInAnyOrder(adicional1, adicional2);
    }

    private DocumentoActualizacionRequest requestActualizacionValido() {
        return new DocumentoActualizacionRequest(
                "PROC-001",
                "Título actualizado",
                "Descripción actualizada",
                AREA_ID,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                DocumentoAlcance.AREA_RESPONSABLE,
                List.of()
        );
    }

    private DocumentoActualizacionRequest requestActualizacionConAlcance(
            DocumentoAlcance alcance, List<Long> areasAdicionalesIds
    ) {
        return new DocumentoActualizacionRequest(
                "PROC-001",
                "Título actualizado",
                "Descripción actualizada",
                AREA_ID,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                alcance,
                areasAdicionalesIds
        );
    }

    private DocumentoActualizacionRequest requestActualizacionConCodigo(String codigo) {
        return new DocumentoActualizacionRequest(
                codigo,
                "Título actualizado",
                "Descripción actualizada",
                AREA_ID,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                DocumentoAlcance.AREA_RESPONSABLE,
                List.of()
        );
    }

    private Documento documentoPersistidoDePrueba() {
        Documento documento = documentoPublicadoDePrueba();
        ReflectionTestUtils.setField(documento, "id", DOCUMENTO_ID);
        Subprograma subprogramaAsignado = mock(Subprograma.class);
        lenient().when(subprogramaAsignado.getId()).thenReturn(SUBPROGRAMA_ID);
        TipoDocumento tipoAsignado = mock(TipoDocumento.class);
        lenient().when(tipoAsignado.getId()).thenReturn(TIPO_DOCUMENTO_ID);
        ReflectionTestUtils.setField(documento, "subprograma", subprogramaAsignado);
        ReflectionTestUtils.setField(documento, "tipoDocumento", tipoAsignado);
        return documento;
    }

    private DocumentoArea documentoAreaPrincipalDePrueba(Documento documento, Area area) {
        return DocumentoArea.principal(documento, area);
    }

    private DocumentoArea documentoAreaAdicionalDePrueba(Documento documento, Area area) {
        return DocumentoArea.adicional(documento, area);
    }

    private Area areaAdicionalMock(Long id) {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(id);
        return area;
    }

    private void stubAdicionalesActuales(Documento documento, List<DocumentoArea> adicionales) {
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID))
                .thenReturn(adicionales);
    }

    private void stubActualizacionMetadatosExitosa(
            Documento documento,
            Area area,
            Subprograma subprograma,
            TipoDocumento tipoDocumento,
            DocumentoArea principal
    ) {
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerEntidadPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerEntidadPorId(TIPO_DOCUMENTO_ID)).thenReturn(tipoDocumento);
        when(documentoRepository.save(documento)).thenReturn(documento);
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID))
                .thenReturn(List.of());
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(versionVigenteDePrueba(documento)));
    }

    @Test
    void actualizarMetadatos_debeRechazarUsuarioConRolJefeArea() {
        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionValido(),
                new AuthenticatedUser(USUARIO_ID, "jefe@plantarsas.com", RolEnum.JEFE_AREA)
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository);
    }

    @Test
    void actualizarMetadatos_debeRechazarUsuarioConRolAdministrativo() {
        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionValido(),
                new AuthenticatedUser(USUARIO_ID, "admin@plantarsas.com", RolEnum.ADMINISTRATIVO)
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository);
    }

    @Test
    void actualizarMetadatos_debeLanzarNotFoundSiDocumentoNoExiste() {
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarMetadatos_conDocumentoObsoleto_debeRechazar() {
        Documento documento = documentoPersistidoDePrueba();
        documento.cambiarEstado(DocumentoEstado.OBSOLETO);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "No se puede editar una publicación obsoleta. Actívala nuevamente para modificarla."
                );

        verify(documentoRepository, never()).save(any(Documento.class));
        verify(documentoAreaRepository, never()).deleteAllByDocumento_IdAndEsPrincipalFalse(anyLong());
        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
        verifyNoInteractions(storageService);
    }

    @Test
    void actualizarMetadatos_conDocumentoInactivo_debePermitir() {
        Documento documento = documentoPersistidoDePrueba();
        documento.cambiarEstado(DocumentoEstado.INACTIVO);
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        );

        assertThat(documento.getTitulo()).isEqualTo("Título actualizado");
        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.INACTIVO);
        verify(documentoRepository).save(documento);
        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
        verifyNoInteractions(storageService);
    }

    @Test
    void actualizarMetadatos_debeLanzarBusinessExceptionSiSubprogramaNoPerteneceAlArea() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = mock(Subprograma.class);
        when(subprograma.getNombre()).thenReturn("Sub A");
        when(subprograma.getArea()).thenReturn(mock(Area.class));
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerEntidadPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_debeActualizarMetadatosSinCrearNuevaVersion() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        );

        assertThat(documento.getTitulo()).isEqualTo("Título actualizado");
        verify(documentoRepository).save(documento);
        verify(documentoAreaRepository).deleteAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID);
        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
        verifyNoInteractions(storageService);
    }

    @Test
    void actualizarMetadatos_conGlobal_debeLimpiarAreasAdicionales() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(DocumentoAlcance.GLOBAL, List.of()),
                usuarioAdministrador()
        );

        assertThat(documento.getAlcance()).isEqualTo(DocumentoAlcance.GLOBAL);
        verify(documentoAreaRepository).deleteAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID);
        verify(documentoAreaRepository, never()).save(any(DocumentoArea.class));
    }

    @Test
    void actualizarMetadatos_conAreasEspecificas_debeSincronizarAdicionales() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area adicional = mock(Area.class);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);
        when(areaLookupService.obtenerActivasPorIds(List.of(AREA_ADICIONAL_1_ID))).thenReturn(List.of(adicional));
        when(documentoAreaRepository.save(any(DocumentoArea.class))).thenAnswer(inv -> inv.getArgument(0));

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_1_ID)
                ),
                usuarioAdministrador()
        );

        verify(documentoAreaRepository).deleteAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID);
        verify(documentoAreaRepository).save(any(DocumentoArea.class));
    }

    @Test
    void actualizarMetadatos_debeActualizarCodigoValido() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConCodigo("DOC-002"),
                usuarioAdministrador()
        );

        assertThat(documento.getCodigo()).isEqualTo("DOC-002");
        verify(documentoRepository).existsByCodigoIgnoreCaseAndIdNot("DOC-002", DOCUMENTO_ID);
    }

    @Test
    void actualizarMetadatos_debePermitirGuardarMismoCodigo() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        );

        assertThat(documento.getCodigo()).isEqualTo("PROC-001");
        verify(documentoRepository).existsByCodigoIgnoreCaseAndIdNot("PROC-001", DOCUMENTO_ID);
    }

    @Test
    void actualizarMetadatos_debeNormalizarCodigoConEspacios() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConCodigo("  DOC-002  "),
                usuarioAdministrador()
        );

        assertThat(documento.getCodigo()).isEqualTo("DOC-002");
        verify(documentoRepository).existsByCodigoIgnoreCaseAndIdNot("DOC-002", DOCUMENTO_ID);
    }

    @Test
    void actualizarMetadatos_debeRechazarCodigoDuplicadoDeOtroDocumento() {
        Documento documento = documentoPersistidoDePrueba();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot("DOC-001", DOCUMENTO_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConCodigo("DOC-001"),
                usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_debeRechazarCodigoDuplicadoCaseInsensitive() {
        Documento documento = documentoPersistidoDePrueba();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot("doc-001", DOCUMENTO_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConCodigo("doc-001"),
                usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_debeActualizarCodigoYMetadatosEnMismaTransaccion() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        DocumentoActualizacionRequest request = new DocumentoActualizacionRequest(
                "DOC-002",
                "Nuevo título",
                "Nueva descripción",
                AREA_ID,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                DocumentoAlcance.AREA_RESPONSABLE,
                List.of()
        );

        documentoServiceImpl.actualizarMetadatos(DOCUMENTO_ID, request, usuarioAdministrador());

        assertThat(documento.getCodigo()).isEqualTo("DOC-002");
        assertThat(documento.getTitulo()).isEqualTo("Nuevo título");
        assertThat(documento.getDescripcion()).isEqualTo("Nueva descripción");
        verify(documentoRepository).save(documento);
    }

    @Test
    void actualizarMetadatos_debeRevertirCodigoSiFallaValidacionSubprograma() {
        Documento documento = documentoPersistidoDePrueba();
        String codigoOriginal = documento.getCodigo();
        Area area = areaActivaMock();
        Subprograma subprograma = mock(Subprograma.class);
        when(subprograma.getNombre()).thenReturn("Sub A");
        when(subprograma.getArea()).thenReturn(mock(Area.class));
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot("DOC-002", DOCUMENTO_ID))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerEntidadPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConCodigo("DOC-002"),
                usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        assertThat(documento.getCodigo()).isEqualTo(codigoOriginal);
        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_debeConservarNumeroVersionAlCambiarCodigo() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConCodigo("DOC-002"),
                usuarioAdministrador()
        );

        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
    }

    @Test
    void actualizarMetadatos_debePermitirConservarTipoInactivoYaAsignado() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoInactivo = mock(TipoDocumento.class);
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoInactivo, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        );

        verify(tipoDocumentoLookupService).obtenerEntidadPorId(TIPO_DOCUMENTO_ID);
        verify(tipoDocumentoLookupService, never()).obtenerActivoPorId(any());
    }

    @Test
    void actualizarMetadatos_debePermitirConservarSubprogramaInactivoYaAsignado() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprogramaInactivo = mock(Subprograma.class);
        when(subprogramaInactivo.getArea()).thenReturn(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprogramaInactivo, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        );

        verify(subprogramaLookupService).obtenerEntidadPorId(SUBPROGRAMA_ID);
        verify(subprogramaLookupService, never()).obtenerActivoPorId(any());
    }

    @Test
    void actualizarMetadatos_debePermitirConservarAreaResponsableInactivaYaAsignada() {
        Documento documento = documentoPersistidoDePrueba();
        Area areaInactiva = mock(Area.class);
        when(areaInactiva.getId()).thenReturn(AREA_ID);
        Subprograma subprograma = subprogramaActivoMock(areaInactiva);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, areaInactiva);
        stubActualizacionMetadatosExitosa(documento, areaInactiva, subprograma, tipoDocumento, principal);

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, requestActualizacionValido(), usuarioAdministrador()
        );

        verify(areaLookupService).obtenerEntidadPorId(AREA_ID);
        verify(areaLookupService, never()).obtenerActivaPorId(any());
    }

    @Test
    void actualizarMetadatos_debeRechazarCambioATipoInactivo() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerEntidadPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerActivoPorId(99L))
                .thenThrow(new BusinessException("El tipo de documento 'X' está inactivo y no puede utilizarse"));

        DocumentoActualizacionRequest request = new DocumentoActualizacionRequest(
                "PROC-001",
                "Título actualizado",
                "Descripción actualizada",
                AREA_ID,
                SUBPROGRAMA_ID,
                99L,
                DocumentoAlcance.AREA_RESPONSABLE,
                List.of()
        );

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, request, usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_debeRechazarCambioASubprogramaInactivo() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerActivoPorId(99L))
                .thenThrow(new BusinessException("El subprograma 'X' está inactivo y no puede utilizarse"));

        DocumentoActualizacionRequest request = new DocumentoActualizacionRequest(
                "PROC-001",
                "Título actualizado",
                "Descripción actualizada",
                AREA_ID,
                99L,
                TIPO_DOCUMENTO_ID,
                DocumentoAlcance.AREA_RESPONSABLE,
                List.of()
        );

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, request, usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_debeRechazarCambioAAreaInactiva() {
        Documento documento = documentoPersistidoDePrueba();
        Area areaActual = areaActivaMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, areaActual);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(areaLookupService.obtenerActivaPorId(99L))
                .thenThrow(new BusinessException("El área 'X' está inactiva y no puede utilizarse"));

        DocumentoActualizacionRequest request = new DocumentoActualizacionRequest(
                "PROC-001",
                "Título actualizado",
                "Descripción actualizada",
                99L,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                DocumentoAlcance.AREA_RESPONSABLE,
                List.of()
        );

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID, request, usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
        verify(areaLookupService, never()).obtenerEntidadPorId(99L);
    }

    @Test
    void actualizarMetadatos_debeConservarAreaAdicionalInactivaYaAsociada() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area calidadInactiva = areaAdicionalMock(AREA_ADICIONAL_1_ID);
        Area produccion = areaAdicionalMock(AREA_ADICIONAL_2_ID);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);
        stubAdicionalesActuales(documento, List.of(
                documentoAreaAdicionalDePrueba(documento, calidadInactiva),
                documentoAreaAdicionalDePrueba(documento, produccion)
        ));
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_1_ID)).thenReturn(calidadInactiva);
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_2_ID)).thenReturn(produccion);
        when(documentoAreaRepository.save(any(DocumentoArea.class))).thenAnswer(inv -> inv.getArgument(0));

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_1_ID, AREA_ADICIONAL_2_ID)
                ),
                usuarioAdministrador()
        );

        verify(areaLookupService).obtenerEntidadPorId(AREA_ADICIONAL_1_ID);
        verify(areaLookupService).obtenerEntidadPorId(AREA_ADICIONAL_2_ID);
        verify(areaLookupService, never()).obtenerActivasPorIds(any());
    }

    @Test
    void actualizarMetadatos_debePermitirEditarTituloConservandoAreaAdicionalInactiva() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area calidadInactiva = areaAdicionalMock(AREA_ADICIONAL_1_ID);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);
        stubAdicionalesActuales(documento, List.of(
                documentoAreaAdicionalDePrueba(documento, calidadInactiva)
        ));
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_1_ID)).thenReturn(calidadInactiva);
        when(documentoAreaRepository.save(any(DocumentoArea.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoActualizacionRequest request = new DocumentoActualizacionRequest(
                "PROC-001",
                "Solo cambia el título",
                "Descripción actualizada",
                AREA_ID,
                SUBPROGRAMA_ID,
                TIPO_DOCUMENTO_ID,
                DocumentoAlcance.AREAS_ESPECIFICAS,
                List.of(AREA_ADICIONAL_1_ID)
        );

        documentoServiceImpl.actualizarMetadatos(DOCUMENTO_ID, request, usuarioAdministrador());

        assertThat(documento.getTitulo()).isEqualTo("Solo cambia el título");
        verify(areaLookupService).obtenerEntidadPorId(AREA_ADICIONAL_1_ID);
        verify(areaLookupService, never()).obtenerActivasPorIds(any());
    }

    @Test
    void actualizarMetadatos_debePermitirEliminarAreaAdicionalInactiva() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area calidadInactiva = areaAdicionalMock(AREA_ADICIONAL_1_ID);
        Area produccion = areaAdicionalMock(AREA_ADICIONAL_2_ID);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);
        stubAdicionalesActuales(documento, List.of(
                documentoAreaAdicionalDePrueba(documento, calidadInactiva),
                documentoAreaAdicionalDePrueba(documento, produccion)
        ));
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_2_ID)).thenReturn(produccion);
        when(documentoAreaRepository.save(any(DocumentoArea.class))).thenAnswer(inv -> inv.getArgument(0));

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_2_ID)
                ),
                usuarioAdministrador()
        );

        verify(areaLookupService).obtenerEntidadPorId(AREA_ADICIONAL_2_ID);
        verify(areaLookupService, never()).obtenerEntidadPorId(AREA_ADICIONAL_1_ID);
        verify(documentoAreaRepository).deleteAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID);
        verify(documentoAreaRepository).save(any(DocumentoArea.class));
    }

    @Test
    void actualizarMetadatos_debeRechazarReagregarAreaAdicionalInactivaEliminada() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area produccion = areaAdicionalMock(AREA_ADICIONAL_2_ID);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        stubAdicionalesActuales(documento, List.of(
                documentoAreaAdicionalDePrueba(documento, produccion)
        ));
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerEntidadPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerEntidadPorId(TIPO_DOCUMENTO_ID)).thenReturn(tipoDocumento);
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_2_ID)).thenReturn(produccion);
        when(areaLookupService.obtenerActivasPorIds(List.of(AREA_ADICIONAL_1_ID)))
                .thenThrow(new BusinessException(
                        "Las siguientes áreas están inactivas y no pueden utilizarse: Calidad"
                ));

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_2_ID, AREA_ADICIONAL_1_ID)
                ),
                usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_debeConservarInactivaExistenteYAgregarActiva() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area calidadInactiva = areaAdicionalMock(AREA_ADICIONAL_1_ID);
        Area produccion = areaAdicionalMock(AREA_ADICIONAL_2_ID);
        Area administracion = areaAdicionalMock(AREA_ADICIONAL_3_ID);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);
        stubAdicionalesActuales(documento, List.of(
                documentoAreaAdicionalDePrueba(documento, calidadInactiva),
                documentoAreaAdicionalDePrueba(documento, produccion)
        ));
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_1_ID)).thenReturn(calidadInactiva);
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_2_ID)).thenReturn(produccion);
        when(areaLookupService.obtenerActivasPorIds(List.of(AREA_ADICIONAL_3_ID)))
                .thenReturn(List.of(administracion));
        when(documentoAreaRepository.save(any(DocumentoArea.class))).thenAnswer(inv -> inv.getArgument(0));

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_1_ID, AREA_ADICIONAL_2_ID, AREA_ADICIONAL_3_ID)
                ),
                usuarioAdministrador()
        );

        verify(areaLookupService).obtenerActivasPorIds(List.of(AREA_ADICIONAL_3_ID));
        verify(documentoAreaRepository, times(3)).save(any(DocumentoArea.class));
    }

    @Test
    void actualizarMetadatos_debeRechazarAgregarNuevaAreaAdicionalInactiva() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area calidadInactiva = areaAdicionalMock(AREA_ADICIONAL_1_ID);
        Area produccion = areaAdicionalMock(AREA_ADICIONAL_2_ID);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        stubAdicionalesActuales(documento, List.of(
                documentoAreaAdicionalDePrueba(documento, calidadInactiva),
                documentoAreaAdicionalDePrueba(documento, produccion)
        ));
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerEntidadPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerEntidadPorId(TIPO_DOCUMENTO_ID)).thenReturn(tipoDocumento);
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_1_ID)).thenReturn(calidadInactiva);
        when(areaLookupService.obtenerEntidadPorId(AREA_ADICIONAL_2_ID)).thenReturn(produccion);
        when(areaLookupService.obtenerActivasPorIds(List.of(AREA_ADICIONAL_3_ID)))
                .thenThrow(new BusinessException(
                        "Las siguientes áreas están inactivas y no pueden utilizarse: Bodega"
                ));

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_1_ID, AREA_ADICIONAL_2_ID, AREA_ADICIONAL_3_ID)
                ),
                usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    @Test
    void actualizarMetadatos_conAdicionalInactiva_debePermitirCambioAGlobal() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Area calidadInactiva = areaAdicionalMock(AREA_ADICIONAL_1_ID);
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        stubActualizacionMetadatosExitosa(documento, area, subprograma, tipoDocumento, principal);
        stubAdicionalesActuales(documento, List.of(
                documentoAreaAdicionalDePrueba(documento, calidadInactiva)
        ));

        documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(DocumentoAlcance.GLOBAL, List.of()),
                usuarioAdministrador()
        );

        assertThat(documento.getAlcance()).isEqualTo(DocumentoAlcance.GLOBAL);
        verify(areaLookupService, never()).obtenerActivasPorIds(any());
        verify(documentoAreaRepository).deleteAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID);
    }

    @Test
    void actualizarMetadatos_debeRechazarCambioAAreasEspecificasConInactivaNueva() {
        Documento documento = documentoPersistidoDePrueba();
        Area area = areaActivaMock();
        Subprograma subprograma = subprogramaActivoMock(area);
        TipoDocumento tipoDocumento = tipoDocumentoActivoMock();
        DocumentoArea principal = documentoAreaPrincipalDePrueba(documento, area);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        when(documentoRepository.existsByCodigoIgnoreCaseAndIdNot(anyString(), eq(DOCUMENTO_ID)))
                .thenReturn(false);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        stubAdicionalesActuales(documento, List.of());
        when(areaLookupService.obtenerEntidadPorId(AREA_ID)).thenReturn(area);
        when(subprogramaLookupService.obtenerEntidadPorId(SUBPROGRAMA_ID)).thenReturn(subprograma);
        when(tipoDocumentoLookupService.obtenerEntidadPorId(TIPO_DOCUMENTO_ID)).thenReturn(tipoDocumento);
        when(areaLookupService.obtenerActivasPorIds(List.of(AREA_ADICIONAL_1_ID)))
                .thenThrow(new BusinessException(
                        "Las siguientes áreas están inactivas y no pueden utilizarse: Calidad"
                ));

        assertThatThrownBy(() -> documentoServiceImpl.actualizarMetadatos(
                DOCUMENTO_ID,
                requestActualizacionConAlcance(
                        DocumentoAlcance.AREAS_ESPECIFICAS,
                        List.of(AREA_ADICIONAL_1_ID)
                ),
                usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
    }

    private void stubCambioEstadoExitoso(Documento documento, VersionDocumento versionVigente) {
        DocumentoArea principal = DocumentoArea.principal(documento, mock(Area.class));
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));
        lenient().when(documentoRepository.save(documento)).thenReturn(documento);
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID))
                .thenReturn(List.of());
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(versionVigente));
    }

    @Test
    void cambiarEstado_dePublicadoAInactivo_debeActualizarEstadoSinNuevaVersion() {
        Documento documento = documentoPersistidoDePrueba();
        VersionDocumento versionVigente = versionVigenteDePrueba(documento);
        stubCambioEstadoExitoso(documento, versionVigente);

        documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.INACTIVO),
                usuarioAdministrador()
        );

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.INACTIVO);
        verify(documentoRepository).save(documento);
        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
        verifyNoInteractions(storageService);
        assertThat(versionVigente.getNumeroVersion()).isEqualTo(3);
    }

    @Test
    void cambiarEstado_dePublicadoAObsoleto_debeActualizarEstado() {
        Documento documento = documentoPersistidoDePrueba();
        VersionDocumento versionVigente = versionVigenteDePrueba(documento);
        stubCambioEstadoExitoso(documento, versionVigente);

        documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.OBSOLETO),
                usuarioAdministrador()
        );

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.OBSOLETO);
    }

    @Test
    void cambiarEstado_deInactivoAPublicado_debeReactivarSinNuevaVersion() {
        Documento documento = documentoPersistidoDePrueba();
        documento.cambiarEstado(DocumentoEstado.INACTIVO);
        VersionDocumento versionVigente = versionVigenteDePrueba(documento);
        stubCambioEstadoExitoso(documento, versionVigente);

        documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.PUBLICADO),
                usuarioAdministrador()
        );

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.PUBLICADO);
        verify(versionDocumentoRepository, never()).save(any(VersionDocumento.class));
    }

    @Test
    void cambiarEstado_deObsoletoAPublicado_debeReactivar() {
        Documento documento = documentoPersistidoDePrueba();
        documento.cambiarEstado(DocumentoEstado.OBSOLETO);
        VersionDocumento versionVigente = versionVigenteDePrueba(documento);
        stubCambioEstadoExitoso(documento, versionVigente);

        documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.PUBLICADO),
                usuarioAdministrador()
        );

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.PUBLICADO);
    }

    @Test
    void cambiarEstado_deObsoletoAInactivo_debeRechazarTransicion() {
        Documento documento = documentoPersistidoDePrueba();
        documento.cambiarEstado(DocumentoEstado.OBSOLETO);
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.INACTIVO),
                usuarioAdministrador()
        )).isInstanceOf(BusinessException.class);

        verify(documentoRepository, never()).save(any(Documento.class));
        verifyNoInteractions(storageService);
    }

    @Test
    void cambiarEstado_conMismoEstado_debeSerIdempotente() {
        Documento documento = documentoPersistidoDePrueba();
        VersionDocumento versionVigente = versionVigenteDePrueba(documento);
        stubCambioEstadoExitoso(documento, versionVigente);

        documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.PUBLICADO),
                usuarioAdministrador()
        );

        verify(documentoRepository, never()).save(any(Documento.class));
        verifyNoInteractions(storageService);
    }

    @Test
    void cambiarEstado_debeRechazarUsuarioNoAdministrador() {
        assertThatThrownBy(() -> documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.INACTIVO),
                new AuthenticatedUser(USUARIO_ID, "jefe", RolEnum.JEFE_AREA)
        )).isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(documentoRepository);
    }

    @Test
    void cambiarEstado_noDebeModificarArchivoDeVersionVigente() {
        Documento documento = documentoPersistidoDePrueba();
        VersionDocumento versionVigente = versionVigenteDePrueba(documento);
        String rutaOriginal = versionVigente.getRutaArchivo();
        String nombreOriginal = versionVigente.getNombreArchivoOriginal();
        stubCambioEstadoExitoso(documento, versionVigente);

        documentoServiceImpl.cambiarEstado(
                DOCUMENTO_ID,
                new DocumentoEstadoActualizacionRequest(DocumentoEstado.INACTIVO),
                usuarioAdministrador()
        );

        assertThat(versionVigente.getRutaArchivo()).isEqualTo(rutaOriginal);
        assertThat(versionVigente.getNombreArchivoOriginal()).isEqualTo(nombreOriginal);
        verifyNoInteractions(storageService);
    }
}
