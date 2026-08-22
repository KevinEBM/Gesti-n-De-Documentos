package com.plantarsas.gestiondocumental.subprogramas.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.service.AreaLookupService;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.exception.UnauthorizedException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.UsuarioAreaAutorizacionService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaEstadoRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaResponse;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaUpdateRequest;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.subprogramas.mapper.SubprogramaMapper;
import com.plantarsas.gestiondocumental.subprogramas.repository.SubprogramaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubprogramaServiceImplTest {

    @Mock
    private SubprogramaRepository subprogramaRepository;

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private AreaLookupService areaLookupService;

    @Mock
    private SubprogramaMapper subprogramaMapper;

    @Mock
    private UsuarioAreaAutorizacionService usuarioAreaAutorizacionService;

    private SubprogramaServiceImpl subprogramaServiceImpl;

    private AuthenticatedUser administrador() {
        return new AuthenticatedUser(1L, "admin@plantarsas.com", RolEnum.ADMINISTRADOR);
    }

    private AuthenticatedUser administrativo() {
        return new AuthenticatedUser(4L, "administrativo@plantarsas.com", RolEnum.ADMINISTRATIVO);
    }

    @BeforeEach
    void inicializar() {
        subprogramaServiceImpl = new SubprogramaServiceImpl(
                subprogramaRepository,
                documentoRepository,
                areaLookupService,
                subprogramaMapper,
                usuarioAreaAutorizacionService
        );
    }

    private Area areaMock(Long id) {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(id);
        return area;
    }

    private SubprogramaResponse respuestaDePrueba(Long id) {
        return new SubprogramaResponse(
                id,
                "Subprograma A",
                "Descripcion",
                null,
                true,
                null,
                null
        );
    }

    @Test
    void crear_debeCrearSubprogramaConAreaValida() {
        SubprogramaRequest request = new SubprogramaRequest("Subprograma A", "Descripcion", 1L);
        Area area = areaMock(1L);
        SubprogramaResponse respuestaEsperada = respuestaDePrueba(1L);

        when(areaLookupService.obtenerActivaPorId(1L)).thenReturn(area);
        when(subprogramaRepository.existsByAreaIdAndNombreIgnoreCase(1L, "Subprograma A")).thenReturn(false);
        when(subprogramaRepository.save(any(Subprograma.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(subprogramaMapper.toResponse(any(Subprograma.class))).thenReturn(respuestaEsperada);

        SubprogramaResponse resultado = subprogramaServiceImpl.crear(request);

        ArgumentCaptor<Subprograma> captor = ArgumentCaptor.forClass(Subprograma.class);
        verify(subprogramaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Subprograma A");
        assertThat(captor.getValue().getArea()).isEqualTo(area);
        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void crear_debeRechazarAreaInexistente() {
        SubprogramaRequest request = new SubprogramaRequest("Subprograma A", "Descripcion", 99L);
        when(areaLookupService.obtenerActivaPorId(99L))
                .thenThrow(new ResourceNotFoundException("No existe un área con id 99"));

        assertThatThrownBy(() -> subprogramaServiceImpl.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subprogramaRepository, never()).save(any());
        verifyNoInteractions(subprogramaMapper);
    }

    @Test
    void crear_debeRechazarAreaInactiva() {
        SubprogramaRequest request = new SubprogramaRequest("Subprograma A", "Descripcion", 1L);
        when(areaLookupService.obtenerActivaPorId(1L))
                .thenThrow(new BusinessException("El área 'Compras' está inactiva y no puede utilizarse"));

        assertThatThrownBy(() -> subprogramaServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class);

        verify(subprogramaRepository, never()).save(any());
        verifyNoInteractions(subprogramaMapper);
    }

    @Test
    void crear_debeRechazarNombreDuplicadoEnMismaArea() {
        SubprogramaRequest request = new SubprogramaRequest("Subprograma A", "Descripcion", 1L);
        Area area = areaMock(1L);

        when(areaLookupService.obtenerActivaPorId(1L)).thenReturn(area);
        when(subprogramaRepository.existsByAreaIdAndNombreIgnoreCase(1L, "Subprograma A")).thenReturn(true);

        assertThatThrownBy(() -> subprogramaServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Subprograma A");

        verify(subprogramaRepository, never()).save(any());
    }

    @Test
    void crear_debePermitirMismoNombreEnAreaDiferente() {
        SubprogramaRequest requestAreaUno = new SubprogramaRequest("Subprograma A", "Descripcion", 1L);
        SubprogramaRequest requestAreaDos = new SubprogramaRequest("Subprograma A", "Descripcion", 2L);
        Area areaUno = areaMock(1L);
        Area areaDos = areaMock(2L);
        SubprogramaResponse respuesta1 = respuestaDePrueba(1L);
        SubprogramaResponse respuesta2 = respuestaDePrueba(2L);

        when(areaLookupService.obtenerActivaPorId(1L)).thenReturn(areaUno);
        when(areaLookupService.obtenerActivaPorId(2L)).thenReturn(areaDos);
        when(subprogramaRepository.existsByAreaIdAndNombreIgnoreCase(1L, "Subprograma A")).thenReturn(false);
        when(subprogramaRepository.existsByAreaIdAndNombreIgnoreCase(2L, "Subprograma A")).thenReturn(false);
        when(subprogramaRepository.save(any(Subprograma.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(subprogramaMapper.toResponse(any(Subprograma.class))).thenReturn(respuesta1, respuesta2);

        SubprogramaResponse resultado1 = subprogramaServiceImpl.crear(requestAreaUno);
        SubprogramaResponse resultado2 = subprogramaServiceImpl.crear(requestAreaDos);

        assertThat(resultado1).isEqualTo(respuesta1);
        assertThat(resultado2).isEqualTo(respuesta2);
        verify(subprogramaRepository).existsByAreaIdAndNombreIgnoreCase(1L, "Subprograma A");
        verify(subprogramaRepository).existsByAreaIdAndNombreIgnoreCase(2L, "Subprograma A");
    }

    @Test
    void actualizar_debeActualizarNombreYDescripcionSinCambiarArea() {
        Long id = 1L;
        Subprograma subprograma = mock(Subprograma.class);
        Area area = areaMock(1L);
        when(subprograma.getArea()).thenReturn(area);
        SubprogramaUpdateRequest request = new SubprogramaUpdateRequest("Nuevo nombre", "Nueva descripcion", 1L);
        SubprogramaResponse respuestaEsperada = respuestaDePrueba(id);

        when(subprogramaRepository.findById(id)).thenReturn(Optional.of(subprograma));
        when(subprogramaRepository.existsByAreaIdAndNombreIgnoreCaseAndIdNot(1L, "Nuevo nombre", id)).thenReturn(false);
        when(subprogramaMapper.toResponse(subprograma)).thenReturn(respuestaEsperada);

        SubprogramaResponse resultado = subprogramaServiceImpl.actualizar(id, request);

        verify(subprograma).actualizarDatos("Nuevo nombre", "Nueva descripcion", area);
        verify(documentoRepository, never()).existsBySubprograma_Id(any());
        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void actualizar_conCambioDeAreaSinDocumentos_debeActualizarArea() {
        Long id = 1L;
        Subprograma subprograma = mock(Subprograma.class);
        Area areaActual = areaMock(1L);
        Area areaNueva = areaMock(2L);
        when(subprograma.getArea()).thenReturn(areaActual);
        SubprogramaUpdateRequest request = new SubprogramaUpdateRequest("Nuevo nombre", "Nueva descripcion", 2L);
        SubprogramaResponse respuestaEsperada = respuestaDePrueba(id);

        when(subprogramaRepository.findById(id)).thenReturn(Optional.of(subprograma));
        when(documentoRepository.existsBySubprograma_Id(id)).thenReturn(false);
        when(areaLookupService.obtenerActivaPorId(2L)).thenReturn(areaNueva);
        when(subprogramaRepository.existsByAreaIdAndNombreIgnoreCaseAndIdNot(2L, "Nuevo nombre", id)).thenReturn(false);
        when(subprogramaMapper.toResponse(subprograma)).thenReturn(respuestaEsperada);

        SubprogramaResponse resultado = subprogramaServiceImpl.actualizar(id, request);

        verify(subprograma).actualizarDatos("Nuevo nombre", "Nueva descripcion", areaNueva);
        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void actualizar_conCambioDeAreaConDocumentos_debeRechazar() {
        Long id = 1L;
        Subprograma subprograma = mock(Subprograma.class);
        Area areaActual = areaMock(1L);
        when(subprograma.getArea()).thenReturn(areaActual);
        SubprogramaUpdateRequest request = new SubprogramaUpdateRequest("Nuevo nombre", "Nueva descripcion", 2L);

        when(subprogramaRepository.findById(id)).thenReturn(Optional.of(subprograma));
        when(documentoRepository.existsBySubprograma_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> subprogramaServiceImpl.actualizar(id, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "No se puede cambiar el área responsable porque el subproceso ya está asociado a documentos."
                );

        verify(subprograma, never()).actualizarDatos(any(), any(), any());
        verify(areaLookupService, never()).obtenerActivaPorId(any());
    }

    @Test
    void actualizar_debeRechazarNombreDuplicadoEnMismaArea() {
        Long id = 1L;
        Subprograma subprograma = mock(Subprograma.class);
        Area area = areaMock(1L);
        when(subprograma.getArea()).thenReturn(area);
        SubprogramaUpdateRequest request = new SubprogramaUpdateRequest("Nombre repetido", "Descripcion", 1L);

        when(subprogramaRepository.findById(id)).thenReturn(Optional.of(subprograma));
        when(subprogramaRepository.existsByAreaIdAndNombreIgnoreCaseAndIdNot(1L, "Nombre repetido", id)).thenReturn(true);

        assertThatThrownBy(() -> subprogramaServiceImpl.actualizar(id, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nombre repetido");

        verify(subprograma, never()).actualizarDatos(any(), any(), any());
    }

    @Test
    void cambiarEstado_debeActivarSubprograma() {
        Long id = 1L;
        Subprograma subprograma = mock(Subprograma.class);
        SubprogramaEstadoRequest request = new SubprogramaEstadoRequest(true);
        SubprogramaResponse respuestaEsperada = respuestaDePrueba(id);

        when(subprogramaRepository.findById(id)).thenReturn(Optional.of(subprograma));
        when(subprogramaMapper.toResponse(subprograma)).thenReturn(respuestaEsperada);

        SubprogramaResponse resultado = subprogramaServiceImpl.cambiarEstado(id, request);

        verify(subprograma).activar();
        verify(subprograma, never()).desactivar();
        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void cambiarEstado_debeDesactivarSubprograma() {
        Long id = 1L;
        Subprograma subprograma = mock(Subprograma.class);
        SubprogramaEstadoRequest request = new SubprogramaEstadoRequest(false);
        SubprogramaResponse respuestaEsperada = respuestaDePrueba(id);

        when(subprogramaRepository.findById(id)).thenReturn(Optional.of(subprograma));
        when(subprogramaMapper.toResponse(subprograma)).thenReturn(respuestaEsperada);

        SubprogramaResponse resultado = subprogramaServiceImpl.cambiarEstado(id, request);

        verify(subprograma).desactivar();
        verify(subprograma, never()).activar();
        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void obtenerPorId_debeRetornarSubprogramaMapeado() {
        Long id = 1L;
        Subprograma subprograma = mock(Subprograma.class);
        SubprogramaResponse respuestaEsperada = respuestaDePrueba(id);

        when(subprogramaRepository.findById(id)).thenReturn(Optional.of(subprograma));
        when(subprogramaMapper.toResponse(subprograma)).thenReturn(respuestaEsperada);

        SubprogramaResponse resultado = subprogramaServiceImpl.obtenerPorId(id);

        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void obtenerPorId_debeLanzarResourceNotFoundSiNoExiste() {
        Long id = 404L;
        when(subprogramaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subprogramaServiceImpl.obtenerPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(subprogramaMapper);
    }

    @Test
    void listar_debeMapearTodosLosSubprogramas() {
        Subprograma subprograma1 = mock(Subprograma.class);
        Subprograma subprograma2 = mock(Subprograma.class);
        SubprogramaResponse respuesta1 = respuestaDePrueba(1L);
        SubprogramaResponse respuesta2 = respuestaDePrueba(2L);

        when(subprogramaRepository.findAll()).thenReturn(List.of(subprograma1, subprograma2));
        when(subprogramaMapper.toResponse(subprograma1)).thenReturn(respuesta1);
        when(subprogramaMapper.toResponse(subprograma2)).thenReturn(respuesta2);

        List<SubprogramaResponse> resultado = subprogramaServiceImpl.listar();

        assertThat(resultado).containsExactly(respuesta1, respuesta2);
    }

    @Test
    void listarActivosPorArea_debeRetornarSoloActivosOrdenadosPorNombre() {
        Long areaId = 1L;
        Area area = mock(Area.class);
        Subprograma subprograma1 = mock(Subprograma.class);
        SubprogramaResponse respuestaEsperada = respuestaDePrueba(1L);

        when(areaLookupService.obtenerActivaPorId(areaId)).thenReturn(area);
        when(subprogramaRepository.findByAreaIdAndActivoTrueOrderByNombreAsc(areaId))
                .thenReturn(List.of(subprograma1));
        when(subprogramaMapper.toResponse(subprograma1)).thenReturn(respuestaEsperada);
        doNothing().when(usuarioAreaAutorizacionService).validarAccesoArea(administrador(), areaId);

        List<SubprogramaResponse> resultado = subprogramaServiceImpl.listarActivosPorArea(areaId, administrador());

        assertThat(resultado).containsExactly(respuestaEsperada);
        verify(subprogramaRepository).findByAreaIdAndActivoTrueOrderByNombreAsc(areaId);
        verify(usuarioAreaAutorizacionService).validarAccesoArea(administrador(), areaId);
    }

    @Test
    void listarActivosPorArea_debeRechazarAreaNoAutorizada() {
        Long areaId = 99L;
        doThrow(new UnauthorizedException("No tiene permisos para consultar el área solicitada"))
                .when(usuarioAreaAutorizacionService).validarAccesoArea(administrativo(), areaId);

        assertThatThrownBy(() -> subprogramaServiceImpl.listarActivosPorArea(areaId, administrativo()))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(subprogramaRepository);
        verify(areaLookupService, never()).obtenerActivaPorId(any());
    }

    @Test
    void listarParaUsuario_conAdministrativo_debeRetornarSoloSubprogramasDeSusAreas() {
        Subprograma subprograma = mock(Subprograma.class);
        SubprogramaResponse respuesta = respuestaDePrueba(1L);
        when(usuarioAreaAutorizacionService.esAdministrador(administrativo())).thenReturn(false);
        when(usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(administrativo())).thenReturn(Set.of(10L));
        when(subprogramaRepository.findByArea_IdInOrderByNombreAsc(Set.of(10L))).thenReturn(List.of(subprograma));
        when(subprogramaMapper.toResponse(subprograma)).thenReturn(respuesta);

        List<SubprogramaResponse> resultado = subprogramaServiceImpl.listarParaUsuario(administrativo());

        assertThat(resultado).containsExactly(respuesta);
        verify(subprogramaRepository).findByArea_IdInOrderByNombreAsc(Set.of(10L));
        verify(subprogramaRepository, never()).findAll();
    }

    @Test
    void listarActivosPorArea_debeRechazarAreaInexistente() {
        Long areaId = 99L;
        doNothing().when(usuarioAreaAutorizacionService).validarAccesoArea(administrador(), areaId);
        when(areaLookupService.obtenerActivaPorId(areaId))
                .thenThrow(new ResourceNotFoundException("No existe un área con id 99"));

        assertThatThrownBy(() -> subprogramaServiceImpl.listarActivosPorArea(areaId, administrador()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(subprogramaRepository);
    }

    @Test
    void listarActivosPorArea_debeRechazarAreaInactiva() {
        Long areaId = 1L;
        doNothing().when(usuarioAreaAutorizacionService).validarAccesoArea(administrador(), areaId);
        when(areaLookupService.obtenerActivaPorId(areaId))
                .thenThrow(new BusinessException("El área 'Compras' está inactiva y no puede utilizarse"));

        assertThatThrownBy(() -> subprogramaServiceImpl.listarActivosPorArea(areaId, administrador()))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(subprogramaRepository);
    }
}
