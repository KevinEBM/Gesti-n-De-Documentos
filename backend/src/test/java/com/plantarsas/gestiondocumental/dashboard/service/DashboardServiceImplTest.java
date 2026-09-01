package com.plantarsas.gestiondocumental.dashboard.service;

import com.plantarsas.gestiondocumental.areas.repository.AreaRepository;
import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.DashboardAdminResponse;
import com.plantarsas.gestiondocumental.dashboard.mapper.DashboardMapper;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.repository.VersionDocumentoRepository;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private VersionDocumentoRepository versionDocumentoRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    private DashboardServiceImpl dashboardServiceImpl;

    @BeforeEach
    void inicializar() {
        dashboardServiceImpl = new DashboardServiceImpl(
                documentoRepository,
                usuarioRepository,
                areaRepository,
                versionDocumentoRepository,
                dashboardMapper
        );
    }

    // ------------------------------------------------------------------
    // obtenerDashboard()
    // ------------------------------------------------------------------

    @Test
    void obtenerDashboard_debeContarDocumentosConEstadoPublicado() {
        when(documentoRepository.countByEstado(DocumentoEstado.PUBLICADO)).thenReturn(5L);
        when(usuarioRepository.countByEstado(EstadoUsuario.ACTIVO)).thenReturn(3L);
        when(areaRepository.count()).thenReturn(2L);

        DashboardAdminResponse resultado = dashboardServiceImpl.obtenerDashboard();

        assertThat(resultado.documentosPublicados()).isEqualTo(5L);
    }

    @Test
    void obtenerDashboard_debeContarUsuariosConEstadoActivo() {
        when(documentoRepository.countByEstado(DocumentoEstado.PUBLICADO)).thenReturn(5L);
        when(usuarioRepository.countByEstado(EstadoUsuario.ACTIVO)).thenReturn(3L);
        when(areaRepository.count()).thenReturn(2L);

        DashboardAdminResponse resultado = dashboardServiceImpl.obtenerDashboard();

        assertThat(resultado.usuariosActivos()).isEqualTo(3L);
    }

    @Test
    void obtenerDashboard_debeUsarElConteoTotalDeAreaRepository() {
        when(documentoRepository.countByEstado(DocumentoEstado.PUBLICADO)).thenReturn(5L);
        when(usuarioRepository.countByEstado(EstadoUsuario.ACTIVO)).thenReturn(3L);
        when(areaRepository.count()).thenReturn(2L);

        DashboardAdminResponse resultado = dashboardServiceImpl.obtenerDashboard();

        assertThat(resultado.areasRegistradas()).isEqualTo(2L);
    }

    @Test
    void obtenerDashboard_sinDocumentosUsuariosNiAreas_debeRetornarCerosValidos() {
        when(documentoRepository.countByEstado(DocumentoEstado.PUBLICADO)).thenReturn(0L);
        when(usuarioRepository.countByEstado(EstadoUsuario.ACTIVO)).thenReturn(0L);
        when(areaRepository.count()).thenReturn(0L);

        DashboardAdminResponse resultado = dashboardServiceImpl.obtenerDashboard();

        assertThat(resultado.documentosPublicados()).isZero();
        assertThat(resultado.usuariosActivos()).isZero();
        assertThat(resultado.areasRegistradas()).isZero();
    }

    // ------------------------------------------------------------------
    // obtenerActividadReciente()
    // ------------------------------------------------------------------

    @Test
    void obtenerActividadReciente_sinVersiones_debeRetornarListaVacia() {
        when(versionDocumentoRepository.findTop10ByOrderByFechaPublicacionDescIdDesc())
                .thenReturn(List.of());

        List<ActividadDocumentalResponse> resultado = dashboardServiceImpl.obtenerActividadReciente();

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtenerActividadReciente_debeUsarElResultadoDelRepositoryDeTop10() {
        VersionDocumento version = mock(VersionDocumento.class);
        when(versionDocumentoRepository.findTop10ByOrderByFechaPublicacionDescIdDesc())
                .thenReturn(List.of(version));

        dashboardServiceImpl.obtenerActividadReciente();

        verify(versionDocumentoRepository).findTop10ByOrderByFechaPublicacionDescIdDesc();
    }

    @Test
    void obtenerActividadReciente_debeMapearCadaVersionEnElMismoOrdenRecibido() {
        VersionDocumento masReciente = mock(VersionDocumento.class);
        VersionDocumento anterior = mock(VersionDocumento.class);
        ActividadDocumentalResponse respuestaMasReciente = mock(ActividadDocumentalResponse.class);
        ActividadDocumentalResponse respuestaAnterior = mock(ActividadDocumentalResponse.class);

        when(versionDocumentoRepository.findTop10ByOrderByFechaPublicacionDescIdDesc())
                .thenReturn(List.of(masReciente, anterior));
        when(dashboardMapper.toActividad(masReciente)).thenReturn(respuestaMasReciente);
        when(dashboardMapper.toActividad(anterior)).thenReturn(respuestaAnterior);

        List<ActividadDocumentalResponse> resultado = dashboardServiceImpl.obtenerActividadReciente();

        assertThat(resultado).containsExactly(respuestaMasReciente, respuestaAnterior);
    }
}
