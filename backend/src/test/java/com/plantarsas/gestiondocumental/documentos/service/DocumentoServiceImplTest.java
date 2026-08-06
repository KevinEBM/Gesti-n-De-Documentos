package com.plantarsas.gestiondocumental.documentos.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.service.AreaLookupService;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoPublicacionInicialRequest;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
                "Publicación inicial"
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
                "Publicación inicial"
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
}
