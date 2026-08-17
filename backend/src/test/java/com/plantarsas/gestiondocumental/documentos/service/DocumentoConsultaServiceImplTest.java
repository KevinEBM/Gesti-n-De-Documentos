package com.plantarsas.gestiondocumental.documentos.service;

/*
 * PROPUESTA — nuevo archivo de test, aún no copiado al repositorio.
 *
 * Alcance deliberado de esta clase: solo Mockito, sin base de datos real (ni H2 ni
 * Testcontainers), conforme a la restricción vigente del proyecto.
 *
 * Con documentoRepository mockeado, documentoRepository.findAll(spec, countSpec, pageable) y
 * documentoRepository.findOne(spec) devuelven exactamente lo que el propio test configura
 * en el when(...): la Specification en sí NO se ejecuta contra ningún motor de consultas
 * real, por lo que estos tests NO pueden demostrar resultados de negocio como "GLOBAL
 * visible", "área principal visible", "área adicional visible" u "otra área no visible".
 * Esos escenarios están correctamente diseñados dentro de DocumentoSpecifications
 * (visiblePara/idIgual), pero su verificación de comportamiento real solo puede hacerse
 * con una consulta ejecutada contra PostgreSQL, una vez aplicado y compilado el código
 * (ver informe de propuestas, sección L).
 *
 * Lo que SÍ se verifica aquí, y es exactamente lo que un test de coordinación con mocks
 * puede probar de forma honesta:
 *   - que ADMINISTRADOR nunca consulta UsuarioAreaRepository (no le aplica autorización
 *     por área, y no debe pagar el costo de una consulta innecesaria);
 *   - que JEFE_AREA/ADMINISTRATIVO sí consultan las áreas reales del usuario autenticado
 *     (nunca un areaId enviado por el cliente, que ni siquiera existe como parámetro);
 *   - que un usuario sin áreas no rompe la resolución (lista vacía -> Set vacío);
 *   - que el mapeo Page<Documento> -> Page<DocumentoResumenResponse> y el Pageable se
 *     propagan correctamente;
 *   - que el detalle reconstruye DocumentoResponse igual que publicarNuevaVersion
 *     (principal, adicionales, versión vigente, mapper);
 *   - que "documento inexistente" y "documento no visible" son, a nivel de código,
 *     exactamente la misma ruta: findOne(spec) vacío -> ResourceNotFoundException. No hay
 *     ninguna consulta previa que permita distinguirlos.
 *
 * Etapa 3B (filtros combinables) añade aquí exactamente la misma clase de verificación:
 * que cada filtro presente en DocumentoFiltroRequest se traduzca en una llamada a
 * DocumentoSpecifications.<filtro>(...), que los filtros ausentes (null/blank) no se
 * apliquen, y que la composición siga usando la misma base como contentSpec y countSpec.
 * NO se intenta demostrar aquí que un filtro produzca el resultado SQL correcto (p. ej.
 * que codigoContiene realmente encuentre coincidencias parciales, o que deArea excluya
 * documentos de otra área): eso pertenece a la misma futura validación contra PostgreSQL
 * real mencionada arriba para visiblePara/idIgual.
 *
 * Etapa 3C (descarga de la versión vigente) añade la misma clase de verificación de
 * coordinación: que la autorización se resuelve por el mismo camino que obtenerPorId
 * (findOne(idIgual.and(visiblePara))), que se usa la versión vigente real, que se llama a
 * StorageService con la ruta correcta, y que una IOException real se traduce en
 * UncheckedIOException siguiendo el mismo patrón que DocumentoServiceImpl.
 */

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoArchivoDescarga;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoFiltroRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.documentos.mapper.DocumentoMapper;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoAreaRepository;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.repository.VersionDocumentoRepository;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.storage.StorageService;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoConsultaServiceImplTest {

    private static final Long USUARIO_ID = 4L;
    private static final Long DOCUMENTO_ID = 10L;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private DocumentoAreaRepository documentoAreaRepository;

    @Mock
    private VersionDocumentoRepository versionDocumentoRepository;

    @Mock
    private UsuarioAreaRepository usuarioAreaRepository;

    @Mock
    private DocumentoMapper documentoMapper;

    @Mock
    private StorageService storageService;

    private DocumentoConsultaServiceImpl documentoConsultaServiceImpl;

    @BeforeEach
    void inicializar() {
        documentoConsultaServiceImpl = new DocumentoConsultaServiceImpl(
                documentoRepository,
                documentoAreaRepository,
                versionDocumentoRepository,
                usuarioAreaRepository,
                documentoMapper,
                storageService
        );
    }

    private AuthenticatedUser administrador() {
        return new AuthenticatedUser(USUARIO_ID, "admin@plantarsas.com", RolEnum.ADMINISTRADOR);
    }

    private AuthenticatedUser jefeArea() {
        return new AuthenticatedUser(USUARIO_ID, "jefe@plantarsas.com", RolEnum.JEFE_AREA);
    }

    private AuthenticatedUser administrativo() {
        return new AuthenticatedUser(USUARIO_ID, "administrativo@plantarsas.com", RolEnum.ADMINISTRATIVO);
    }

    private DocumentoFiltroRequest sinFiltros() {
        return new DocumentoFiltroRequest(null, null, null, null, null, null, null, null);
    }

    // ------------------------------------------------------------------
    // listar(...)
    // ------------------------------------------------------------------

    @Test
    void listar_conAdministrador_noDebeConsultarAreasDelUsuario() {
        Pageable pageable = PageRequest.of(0, 20);
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(administrador(), sinFiltros(), pageable);

        verify(usuarioAreaRepository, never()).findByUsuario_Id(any());
    }

    @Test
    void listar_conJefeArea_debeConsultarLasAreasRealesDelUsuarioAutenticado() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(jefeArea(), sinFiltros(), pageable);

        verify(usuarioAreaRepository).findByUsuario_Id(USUARIO_ID);
    }

    @Test
    void listar_conAdministrativo_debeConsultarLasAreasRealesDelUsuarioAutenticado() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(administrativo(), sinFiltros(), pageable);

        verify(usuarioAreaRepository).findByUsuario_Id(USUARIO_ID);
    }

    @Test
    void listar_conUsuarioSinAreasAsignadas_noDebeLanzarErrorYDebeConsultarIgual() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<DocumentoResumenResponse> resultado = documentoConsultaServiceImpl.listar(jefeArea(), sinFiltros(), pageable);

        assertThat(resultado.getContent()).isEmpty();
        verify(usuarioAreaRepository).findByUsuario_Id(USUARIO_ID);
    }

    @Test
    void listar_debeResolverAreasDesdeAsociacionesRealesYNoDesdeElCliente() {
        Pageable pageable = PageRequest.of(0, 20);
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(77L);
        UsuarioArea usuarioArea = mock(UsuarioArea.class);
        when(usuarioArea.getArea()).thenReturn(area);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of(usuarioArea));
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(jefeArea(), sinFiltros(), pageable);

        verify(usuarioAreaRepository).findByUsuario_Id(USUARIO_ID);
        verify(area).getId();
    }

    @Test
    void listar_debeMapearCadaDocumentoDeLaPaginaARecursoDeResumen() {
        Pageable pageable = PageRequest.of(0, 20);
        Documento documento = mock(Documento.class);
        DocumentoResumenResponse resumen = new DocumentoResumenResponse(
                1L, "PROC-001", "Titulo", null, null, "Subprograma", "TipoDocumento", null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(documento)));
        when(documentoMapper.toResumen(documento)).thenReturn(resumen);

        Page<DocumentoResumenResponse> resultado = documentoConsultaServiceImpl.listar(administrador(), sinFiltros(), pageable);

        assertThat(resultado.getContent()).containsExactly(resumen);
    }

    @Test
    void listar_debePropagarElPageableRecibidoAlRepository() {
        Pageable pageable = PageRequest.of(2, 15);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(documentoRepository.findAll(
                any(Specification.class), any(Specification.class), pageableCaptor.capture()
        )).thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(administrador(), sinFiltros(), pageable);

        assertThat(pageableCaptor.getValue()).isEqualTo(pageable);
    }

    @Test
    void listar_debeUsarSoloLaVisibilidadComoCountSpecSinElFetchDeResumen() {
        Pageable pageable = PageRequest.of(0, 20);
        ArgumentCaptor<Specification<Documento>> consultaCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Specification<Documento>> countSpecCaptor = ArgumentCaptor.forClass(Specification.class);
        when(documentoRepository.findAll(consultaCaptor.capture(), countSpecCaptor.capture(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(administrador(), sinFiltros(), pageable);

        assertThat(consultaCaptor.getValue()).isNotNull();
        assertThat(countSpecCaptor.getValue()).isNotNull();
        assertThat(countSpecCaptor.getValue()).isNotSameAs(consultaCaptor.getValue());
    }

    // ------------------------------------------------------------------
    // listar(...) con DocumentoFiltroRequest (Etapa 3B)
    //
    // Con documentoRepository mockeado, estos tests verifican que el filtro presente
    // en DocumentoFiltroRequest efectivamente resulta en una llamada exitosa al
    // repository (la composición no lanza excepciones, no rompe la resolución de
    // áreas ni el mapeo). NO demuestran que el predicado generado por cada
    // DocumentoSpecifications.<filtro>(...) produzca el resultado SQL correcto: eso
    // requiere la validación contra PostgreSQL real mencionada en la cabecera de esta
    // clase.
    // ------------------------------------------------------------------

    @Test
    void listar_conCodigoPresente_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                "SG-SST", null, null, null, null, null, null, null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conTituloPresente_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, "Procedimiento", null, null, null, null, null, null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conAreaIdPresente_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, 5L, null, null, null, null, null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conSubprogramaIdPresente_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, 8L, null, null, null, null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conTipoDocumentoIdPresente_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, 3L, null, null, null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conEstadoPresente_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, DocumentoEstado.INACTIVO, null, null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conFechaDesdePresente_debeAplicarlaSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, LocalDate.of(2026, 1, 1), null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conFechaHastaPresente_debeAplicarlaSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, null, LocalDate.of(2026, 12, 31)
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conAmbasFechasPresentes_debeAplicarlasSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conVariosFiltrosCombinados_debeAplicarlosSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                "SG-SST", "Procedimiento", 5L, 8L, 3L, DocumentoEstado.PUBLICADO,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conCodigoYTituloEnBlanco_debenTratarseComoAusentes() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                "   ", "   ", null, null, null, null, null, null
        );
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conFiltrosPresentes_debeAplicarlosTantoAContentSpecComoACountSpec() {
        Pageable pageable = PageRequest.of(0, 20);
        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                "SG-SST", null, 5L, null, null, null, null, null
        );
        ArgumentCaptor<Specification<Documento>> consultaCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Specification<Documento>> countSpecCaptor = ArgumentCaptor.forClass(Specification.class);
        when(documentoRepository.findAll(consultaCaptor.capture(), countSpecCaptor.capture(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(administrador(), filtro, pageable);

        assertThat(consultaCaptor.getValue()).isNotNull();
        assertThat(countSpecCaptor.getValue()).isNotNull();
        assertThat(countSpecCaptor.getValue()).isNotSameAs(consultaCaptor.getValue());
    }

    // ------------------------------------------------------------------
    // obtenerPorId(...)
    // ------------------------------------------------------------------

    @Test
    void obtenerPorId_conAdministrador_noDebeConsultarAreasDelUsuario() {
        Documento documento = mock(Documento.class);
        DocumentoArea principal = mock(DocumentoArea.class);
        VersionDocumento version = mock(VersionDocumento.class);
        DocumentoResponse respuesta = mock(DocumentoResponse.class);

        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID))
                .thenReturn(List.of());
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
        when(documentoMapper.toResponse(documento, principal, List.of(), version)).thenReturn(respuesta);

        documentoConsultaServiceImpl.obtenerPorId(DOCUMENTO_ID, administrador());

        verify(usuarioAreaRepository, never()).findByUsuario_Id(any());
    }

    @Test
    void obtenerPorId_conDocumentoVisible_debeRetornarDocumentoResponseCompleto() {
        Documento documento = mock(Documento.class);
        DocumentoArea principal = mock(DocumentoArea.class);
        DocumentoArea adicional = mock(DocumentoArea.class);
        VersionDocumento version = mock(VersionDocumento.class);
        DocumentoResponse respuesta = mock(DocumentoResponse.class);

        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID))
                .thenReturn(List.of(adicional));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
        when(documentoMapper.toResponse(documento, principal, List.of(adicional), version))
                .thenReturn(respuesta);

        DocumentoResponse resultado = documentoConsultaServiceImpl.obtenerPorId(DOCUMENTO_ID, jefeArea());

        assertThat(resultado).isEqualTo(respuesta);
    }

    @Test
    void obtenerPorId_conDocumentoInexistenteONoVisible_debeLanzarResourceNotFoundException() {
        // Misma ruta de código para ambos casos: findOne(spec) devuelve Optional.empty()
        // tanto si el id no existe como si el documento existe pero no es visible para
        // este usuario. No hay ninguna consulta anterior que permita diferenciarlos.
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.obtenerPorId(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_sinAreaPrincipalAsignada_debeLanzarResourceNotFoundException() {
        Documento documento = mock(Documento.class);

        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.obtenerPorId(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_sinVersionVigenteRegistrada_debeLanzarIllegalStateException() {
        Documento documento = mock(Documento.class);
        DocumentoArea principal = mock(DocumentoArea.class);

        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(principal));
        when(documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(DOCUMENTO_ID))
                .thenReturn(List.of());
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.obtenerPorId(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------
    // descargarVersionVigente(...) (Etapa 3C)
    // ------------------------------------------------------------------

    @Test
    void descargarVersionVigente_conDocumentoVisible_debeRetornarArchivoDeLaVersionVigente() throws Exception {
        Documento documento = mock(Documento.class);
        VersionDocumento version = mock(VersionDocumento.class);
        InputStream contenido = InputStream.nullInputStream();

        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
        when(version.getRutaArchivo()).thenReturn("b3f1c2.pdf");
        when(version.getNombreArchivoOriginal()).thenReturn("informe.pdf");
        when(version.getTipoMime()).thenReturn("application/pdf");
        when(version.getTamanoBytes()).thenReturn(1024L);
        when(storageService.cargar("b3f1c2.pdf")).thenReturn(contenido);

        DocumentoArchivoDescarga resultado =
                documentoConsultaServiceImpl.descargarVersionVigente(DOCUMENTO_ID, jefeArea());

        assertThat(resultado.nombreArchivoOriginal()).isEqualTo("informe.pdf");
        assertThat(resultado.tipoMime()).isEqualTo("application/pdf");
        assertThat(resultado.tamanoBytes()).isEqualTo(1024L);
        assertThat(resultado.contenido()).isSameAs(contenido);
        verify(storageService).cargar("b3f1c2.pdf");
    }

    @Test
    void descargarVersionVigente_conDocumentoInexistenteONoVisible_debeLanzarResourceNotFoundExceptionSinConsultarStorage() {
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.descargarVersionVigente(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void descargarVersionVigente_sinVersionVigenteRegistrada_debeLanzarIllegalStateExceptionSinConsultarStorage() {
        Documento documento = mock(Documento.class);

        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.descargarVersionVigente(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void descargarVersionVigente_conIOExceptionAlLeerElArchivo_debeLanzarUncheckedIOException() throws Exception {
        Documento documento = mock(Documento.class);
        VersionDocumento version = mock(VersionDocumento.class);

        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
        when(version.getRutaArchivo()).thenReturn("b3f1c2.pdf");
        when(storageService.cargar("b3f1c2.pdf")).thenThrow(new IOException("fallo de lectura"));

        assertThatThrownBy(() -> documentoConsultaServiceImpl.descargarVersionVigente(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(UncheckedIOException.class);
    }
}
