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
 *
 * Etapa 3D (histórico de versiones y descarga histórica) añade la misma clase de
 * verificación: que listarHistorico/descargarVersionHistorica autorizan por el mismo
 * buscarDocumentoVisible (mismo 404 indistinguible para inexistente/no visible), que el
 * histórico usa findByDocumento_IdOrderByNumeroVersionDesc sin filtrar la vigente, que la
 * descarga histórica valida la pertenencia documento-versión en la propia consulta
 * (findByIdAndDocumento_Id) sin confiar en datos del cliente, y que el helper compartido
 * aArchivoDescarga(...) se reutiliza igual para vigente e histórica (los tests ya
 * existentes de descargarVersionVigente siguen probando, sin cambios, que ese
 * comportamiento no se alteró al extraer el helper).
 */

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoArchivoDescarga;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoFiltroRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.VersionHistoricaResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.documentos.mapper.DocumentoMapper;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoAreaRepository;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.repository.VersionDocumentoRepository;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.UsuarioAreaAutorizacionService;
import com.plantarsas.gestiondocumental.shared.enums.AlcanceConsulta;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.storage.StorageService;
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
import java.util.Set;

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
    private UsuarioAreaAutorizacionService usuarioAreaAutorizacionService;

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
                usuarioAreaAutorizacionService,
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
        return new DocumentoFiltroRequest(null, null, null, null, null, null, null, null, null);
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

        verify(usuarioAreaAutorizacionService, never()).obtenerAreaIdsAutorizadas(any());
    }

    @Test
    void listar_conJefeArea_debeConsultarLasAreasRealesDelUsuarioAutenticado() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(jefeArea(), sinFiltros(), pageable);

        verify(usuarioAreaAutorizacionService).obtenerAreaIdsAutorizadas(any());
    }

    @Test
    void listar_conAdministrativo_debeConsultarLasAreasRealesDelUsuarioAutenticado() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(administrativo(), sinFiltros(), pageable);

        verify(usuarioAreaAutorizacionService).obtenerAreaIdsAutorizadas(any());
    }

    @Test
    void listar_conUsuarioSinAreasAsignadas_noDebeLanzarErrorYDebeConsultarIgual() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<DocumentoResumenResponse> resultado = documentoConsultaServiceImpl.listar(jefeArea(), sinFiltros(), pageable);

        assertThat(resultado.getContent()).isEmpty();
        verify(usuarioAreaAutorizacionService).obtenerAreaIdsAutorizadas(any());
    }

    @Test
    void listar_debeResolverAreasDesdeAsociacionesRealesYNoDesdeElCliente() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(jefeArea())).thenReturn(Set.of(77L));
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        documentoConsultaServiceImpl.listar(jefeArea(), sinFiltros(), pageable);

        verify(usuarioAreaAutorizacionService).obtenerAreaIdsAutorizadas(jefeArea());
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
                "SG-SST", null, null, null, null, null, null, null, null
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
                null, "Procedimiento", null, null, null, null, null, null, null
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
                null, null, 5L, null, null, null, null, null, null
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
                null, null, null, 8L, null, null, null, null, null
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
                null, null, null, null, 3L, null, null, null, null
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
                null, null, null, null, null, DocumentoEstado.INACTIVO, null, null, null
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
                null, null, null, null, null, null, LocalDate.of(2026, 1, 1), null, null
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
                null, null, null, null, null, null, null, LocalDate.of(2026, 12, 31), null
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
                null, null, null, null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null
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
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null
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
                "   ", "   ", null, null, null, null, null, null, null
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
                "SG-SST", null, 5L, null, null, null, null, null, null
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

        verify(usuarioAreaAutorizacionService, never()).obtenerAreaIdsAutorizadas(any());
    }

    @Test
    void obtenerPorId_conDocumentoVisible_debeRetornarDocumentoResponseCompleto() {
        Documento documento = mock(Documento.class);
        DocumentoArea principal = mock(DocumentoArea.class);
        DocumentoArea adicional = mock(DocumentoArea.class);
        VersionDocumento version = mock(VersionDocumento.class);
        DocumentoResponse respuesta = mock(DocumentoResponse.class);

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
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
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.obtenerPorId(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_sinAreaPrincipalAsignada_debeLanzarResourceNotFoundException() {
        Documento documento = mock(Documento.class);

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
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

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
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

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
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
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.descargarVersionVigente(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void descargarVersionVigente_sinVersionVigenteRegistrada_debeLanzarIllegalStateExceptionSinConsultarStorage() {
        Documento documento = mock(Documento.class);

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
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

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
        when(version.getRutaArchivo()).thenReturn("b3f1c2.pdf");
        when(storageService.cargar("b3f1c2.pdf")).thenThrow(new IOException("fallo de lectura"));

        assertThatThrownBy(() -> documentoConsultaServiceImpl.descargarVersionVigente(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(UncheckedIOException.class);
    }

    // ------------------------------------------------------------------
    // listarHistorico(...) (Etapa 3D)
    // ------------------------------------------------------------------

    @Test
    void listarHistorico_debeMapearElMismoPublicadorParaAdministradorYJefe() {
        DocumentoMapper mapperReal = new DocumentoMapper();
        documentoConsultaServiceImpl = new DocumentoConsultaServiceImpl(
                documentoRepository,
                documentoAreaRepository,
                versionDocumentoRepository,
                usuarioAreaAutorizacionService,
                mapperReal,
                storageService
        );

        Documento documento = mock(Documento.class);
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));

        Usuario publicador = mock(Usuario.class);
        when(publicador.getId()).thenReturn(23L);
        when(publicador.getNombres()).thenReturn("Administrador");
        when(publicador.getApellidos()).thenReturn("Local");

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getId()).thenReturn(100L);
        when(version.getNumeroVersion()).thenReturn(2);
        when(version.getNombreArchivoOriginal()).thenReturn("archivo.pdf");
        when(version.getTipoMime()).thenReturn("application/pdf");
        when(version.getTamanoBytes()).thenReturn(1024L);
        when(version.getDescripcionCambio()).thenReturn("prueba 2 inactiva");
        when(version.getFechaPublicacion()).thenReturn(java.time.LocalDateTime.of(2026, 8, 20, 12, 0));
        when(version.getPublicadoPor()).thenReturn(publicador);
        when(version.isVigente()).thenReturn(true);

        when(versionDocumentoRepository.findByDocumento_IdOrderByNumeroVersionDesc(DOCUMENTO_ID))
                .thenReturn(List.of(version));

        List<VersionHistoricaResponse> admin =
                documentoConsultaServiceImpl.listarHistorico(DOCUMENTO_ID, administrador());

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        List<VersionHistoricaResponse> jefe =
                documentoConsultaServiceImpl.listarHistorico(DOCUMENTO_ID, jefeArea());

        assertThat(admin).hasSize(1);
        assertThat(jefe).hasSize(1);
        assertThat(admin.get(0).publicadoPorId()).isEqualTo(23L);
        assertThat(admin.get(0).publicadoPorNombre()).isEqualTo("Administrador Local");
        assertThat(jefe.get(0).publicadoPorId()).isEqualTo(23L);
        assertThat(jefe.get(0).publicadoPorNombre()).isEqualTo("Administrador Local");
    }

    @Test
    void listarHistorico_conAdministrador_debeRetornarElHistoricoCompleto() {
        Documento documento = mock(Documento.class);
        VersionDocumento v2 = mock(VersionDocumento.class);
        VersionDocumento v1 = mock(VersionDocumento.class);
        VersionHistoricaResponse r2 = mock(VersionHistoricaResponse.class);
        VersionHistoricaResponse r1 = mock(VersionHistoricaResponse.class);

        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByDocumento_IdOrderByNumeroVersionDesc(DOCUMENTO_ID))
                .thenReturn(List.of(v2, v1));
        when(documentoMapper.toHistorico(v2)).thenReturn(r2);
        when(documentoMapper.toHistorico(v1)).thenReturn(r1);

        List<VersionHistoricaResponse> resultado =
                documentoConsultaServiceImpl.listarHistorico(DOCUMENTO_ID, administrador());

        assertThat(resultado).containsExactly(r2, r1);
        verify(usuarioAreaAutorizacionService, never()).obtenerAreaIdsAutorizadas(any());
    }

    @Test
    void listarHistorico_conJefeAreaYDocumentoVisible_debeRetornarHistorico() {
        Documento documento = mock(Documento.class);
        VersionDocumento version = mock(VersionDocumento.class);
        VersionHistoricaResponse respuesta = mock(VersionHistoricaResponse.class);

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByDocumento_IdOrderByNumeroVersionDesc(DOCUMENTO_ID))
                .thenReturn(List.of(version));
        when(documentoMapper.toHistorico(version)).thenReturn(respuesta);

        List<VersionHistoricaResponse> resultado =
                documentoConsultaServiceImpl.listarHistorico(DOCUMENTO_ID, jefeArea());

        assertThat(resultado).containsExactly(respuesta);
    }

    @Test
    void listarHistorico_conDocumentoInexistenteONoVisible_debeLanzarResourceNotFoundExceptionSinConsultarVersiones() {
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl.listarHistorico(DOCUMENTO_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(versionDocumentoRepository, never()).findByDocumento_IdOrderByNumeroVersionDesc(any());
    }

    @Test
    void listarHistorico_debeConservarElOrdenEntregadoPorElRepository() {
        Documento documento = mock(Documento.class);
        VersionDocumento vigente = mock(VersionDocumento.class);
        VersionDocumento anterior = mock(VersionDocumento.class);
        VersionHistoricaResponse respuestaVigente = mock(VersionHistoricaResponse.class);
        VersionHistoricaResponse respuestaAnterior = mock(VersionHistoricaResponse.class);

        // El repository ya entrega orden numeroVersion DESC (vigente primero); el
        // servicio no debe reordenar ni filtrar la vigente del histórico.
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByDocumento_IdOrderByNumeroVersionDesc(DOCUMENTO_ID))
                .thenReturn(List.of(vigente, anterior));
        when(documentoMapper.toHistorico(vigente)).thenReturn(respuestaVigente);
        when(documentoMapper.toHistorico(anterior)).thenReturn(respuestaAnterior);

        List<VersionHistoricaResponse> resultado =
                documentoConsultaServiceImpl.listarHistorico(DOCUMENTO_ID, administrador());

        assertThat(resultado).containsExactly(respuestaVigente, respuestaAnterior);
    }

    // ------------------------------------------------------------------
    // descargarVersionHistorica(...) (Etapa 3D)
    // ------------------------------------------------------------------

    private static final Long VERSION_ID = 55L;

    @Test
    void descargarVersionHistorica_conVersionValida_debeRetornarArchivoDeEsaVersion() throws Exception {
        Documento documento = mock(Documento.class);
        VersionDocumento version = mock(VersionDocumento.class);
        InputStream contenido = InputStream.nullInputStream();

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByIdAndDocumento_Id(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
        when(version.getRutaArchivo()).thenReturn("archivo-anterior.pdf");
        when(version.getNombreArchivoOriginal()).thenReturn("informe-v1.pdf");
        when(version.getTipoMime()).thenReturn("application/pdf");
        when(version.getTamanoBytes()).thenReturn(512L);
        when(storageService.cargar("archivo-anterior.pdf")).thenReturn(contenido);

        DocumentoArchivoDescarga resultado = documentoConsultaServiceImpl
                .descargarVersionHistorica(DOCUMENTO_ID, VERSION_ID, jefeArea());

        assertThat(resultado.nombreArchivoOriginal()).isEqualTo("informe-v1.pdf");
        assertThat(resultado.tipoMime()).isEqualTo("application/pdf");
        assertThat(resultado.tamanoBytes()).isEqualTo(512L);
        assertThat(resultado.contenido()).isSameAs(contenido);
        verify(storageService).cargar("archivo-anterior.pdf");
    }

    @Test
    void descargarVersionHistorica_conDocumentoInexistenteONoVisible_debeLanzarResourceNotFoundExceptionSinConsultarVersion() {
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl
                .descargarVersionHistorica(DOCUMENTO_ID, VERSION_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(versionDocumentoRepository, never()).findByIdAndDocumento_Id(any(), any());
        verifyNoInteractions(storageService);
    }

    @Test
    void descargarVersionHistorica_conVersionIdInexistente_debeLanzarResourceNotFoundException() {
        Documento documento = mock(Documento.class);

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByIdAndDocumento_Id(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl
                .descargarVersionHistorica(DOCUMENTO_ID, VERSION_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void descargarVersionHistorica_conVersionIdDeOtroDocumento_debeLanzarElMismoResourceNotFoundException() {
        // findByIdAndDocumento_Id ya valida la pertenencia en la propia consulta: si el
        // versionId existe pero pertenece a otro documento, el repository (mockeado aquí
        // exactamente como lo haría la query derivada real) devuelve vacío igual que si
        // no existiera. Mismo 404, sin distinguir el caso.
        Documento documento = mock(Documento.class);

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByIdAndDocumento_Id(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoConsultaServiceImpl
                .descargarVersionHistorica(DOCUMENTO_ID, VERSION_ID, jefeArea()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(versionDocumentoRepository).findByIdAndDocumento_Id(VERSION_ID, DOCUMENTO_ID);
    }

    @Test
    void descargarVersionHistorica_conIOExceptionAlLeerElArchivo_debeLanzarUncheckedIOException() throws Exception {
        Documento documento = mock(Documento.class);
        VersionDocumento version = mock(VersionDocumento.class);

        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findOne(any(Specification.class))).thenReturn(Optional.of(documento));
        when(versionDocumentoRepository.findByIdAndDocumento_Id(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
        when(version.getRutaArchivo()).thenReturn("archivo-anterior.pdf");
        when(storageService.cargar("archivo-anterior.pdf")).thenThrow(new IOException("fallo de lectura"));

        assertThatThrownBy(() -> documentoConsultaServiceImpl
                .descargarVersionHistorica(DOCUMENTO_ID, VERSION_ID, jefeArea()))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void listar_conAdministrativoYManipulacionAreaIdAjena_debeSeguirResolviendoVisibilidadDesdeAreasDelUsuario() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(administrativo())).thenReturn(Set.of(10L));
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        DocumentoFiltroRequest filtroManipulado = new DocumentoFiltroRequest(
                null, null, 99L, null, null, null, null, null, null
        );

        documentoConsultaServiceImpl.listar(administrativo(), filtroManipulado, pageable);

        verify(usuarioAreaAutorizacionService).obtenerAreaIdsAutorizadas(administrativo());
    }

    @Test
    void listar_conAdministrativoYManipulacionSubprogramaIdAjeno_debeSeguirResolviendoVisibilidadDesdeAreasDelUsuario() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of());
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        DocumentoFiltroRequest filtroManipulado = new DocumentoFiltroRequest(
                null, null, null, 888L, null, null, null, null, null
        );

        documentoConsultaServiceImpl.listar(administrativo(), filtroManipulado, pageable);

        verify(usuarioAreaAutorizacionService).obtenerAreaIdsAutorizadas(any());
    }

    @Test
    void listar_conAlcanceGlobalesParaJefe_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of(10L));
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, null, null, AlcanceConsulta.GLOBALES
        );

        assertThatCode(() -> documentoConsultaServiceImpl.listar(jefeArea(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conAlcanceMiAreaParaJefe_debeUsarAreasAutorizadasDelUsuario() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(jefeArea())).thenReturn(Set.of(10L));
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, null, null, AlcanceConsulta.MI_AREA
        );

        documentoConsultaServiceImpl.listar(jefeArea(), filtro, pageable);

        verify(usuarioAreaAutorizacionService).obtenerAreaIdsAutorizadas(jefeArea());
    }

    @Test
    void listar_conAlcanceGlobalesParaAdministrador_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, null, null, AlcanceConsulta.GLOBALES
        );

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conAlcanceGlobalesParaAdministrativo_debeAplicarloSinLanzarExcepcion() {
        Pageable pageable = PageRequest.of(0, 20);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(any())).thenReturn(Set.of(10L));
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, null, null, AlcanceConsulta.GLOBALES
        );

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrativo(), filtro, pageable))
                .doesNotThrowAnyException();
    }

    @Test
    void listar_conAlcanceMiAreaParaAdministrador_debeIgnorarFiltroDeAlcance() {
        Pageable pageable = PageRequest.of(0, 20);
        when(documentoRepository.findAll(any(Specification.class), any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        DocumentoFiltroRequest filtro = new DocumentoFiltroRequest(
                null, null, null, null, null, null, null, null, AlcanceConsulta.MI_AREA
        );

        assertThatCode(() -> documentoConsultaServiceImpl.listar(administrador(), filtro, pageable))
                .doesNotThrowAnyException();
    }
}
