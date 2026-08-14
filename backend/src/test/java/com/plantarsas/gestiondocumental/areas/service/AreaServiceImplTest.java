package com.plantarsas.gestiondocumental.areas.service;

import com.plantarsas.gestiondocumental.areas.dto.AreaEstadoRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.mapper.AreaMapper;
import com.plantarsas.gestiondocumental.areas.repository.AreaRepository;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AreaServiceImplTest {

    @Mock
    private AreaRepository areaRepository;

    private AreaServiceImpl areaServiceImpl;

    @BeforeEach
    void inicializar() {
        areaServiceImpl = new AreaServiceImpl(
                areaRepository,
                new AreaMapper()
        );
    }

    @Test
    void crear_debeCrearAreaConDatosNormalizadosYRetornarRespuesta() {
        AreaRequest request = new AreaRequest("  adm  ", "  Recursos Humanos  ", "  Gestion de personal  ");

        when(areaRepository.existsByCodigoIgnoreCase("ADM")).thenReturn(false);
        when(areaRepository.existsByNombreIgnoreCase("Recursos Humanos")).thenReturn(false);

        Area areaGuardada = areaMock(1L, "ADM", "Recursos Humanos", "Gestion de personal", true);
        when(areaRepository.save(any(Area.class))).thenReturn(areaGuardada);

        AreaResponse resultado = areaServiceImpl.crear(request);

        ArgumentCaptor<Area> captor = ArgumentCaptor.forClass(Area.class);
        verify(areaRepository).save(captor.capture());
        Area capturada = captor.getValue();

        assertThat(capturada.getCodigo()).isEqualTo("ADM");
        assertThat(capturada.getNombre()).isEqualTo("Recursos Humanos");
        assertThat(capturada.getDescripcion()).isEqualTo("Gestion de personal");

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.codigo()).isEqualTo("ADM");
        assertThat(resultado.nombre()).isEqualTo("Recursos Humanos");
        assertThat(resultado.descripcion()).isEqualTo("Gestion de personal");
        assertThat(resultado.activo()).isTrue();

        verify(areaRepository).existsByCodigoIgnoreCase("ADM");
        verify(areaRepository).existsByNombreIgnoreCase("Recursos Humanos");
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void crear_debeLanzarConflictoCuandoCodigoDuplicado() {
        AreaRequest request = new AreaRequest("adm", "Recursos Humanos", null);
        when(areaRepository.existsByCodigoIgnoreCase("ADM")).thenReturn(true);

        assertThatThrownBy(() -> areaServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un área con el código 'ADM'")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(areaRepository).existsByCodigoIgnoreCase("ADM");
        verify(areaRepository, never()).existsByNombreIgnoreCase(any());
        verify(areaRepository, never()).save(any());
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void crear_debeLanzarConflictoCuandoNombreDuplicado() {
        AreaRequest request = new AreaRequest("ADM", "Recursos Humanos", null);
        when(areaRepository.existsByCodigoIgnoreCase("ADM")).thenReturn(false);
        when(areaRepository.existsByNombreIgnoreCase("Recursos Humanos")).thenReturn(true);

        assertThatThrownBy(() -> areaServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un área con el nombre 'Recursos Humanos'")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(areaRepository).existsByCodigoIgnoreCase("ADM");
        verify(areaRepository).existsByNombreIgnoreCase("Recursos Humanos");
        verify(areaRepository, never()).save(any());
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void listar_debeRetornarTodasLasAreasMapeadas() {
        Area area1 = areaMock(1L, "ADM", "Administracion", "desc1", true);
        Area area2 = areaMock(2L, "FIN", "Finanzas", "desc2", false);
        when(areaRepository.findAll()).thenReturn(List.of(area1, area2));

        List<AreaResponse> resultado = areaServiceImpl.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
        assertThat(resultado.get(0).activo()).isTrue();
        assertThat(resultado.get(1).id()).isEqualTo(2L);
        assertThat(resultado.get(1).activo()).isFalse();

        verify(areaRepository).findAll();
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void listar_debeRetornarListaVaciaCuandoNoExistenAreas() {
        when(areaRepository.findAll()).thenReturn(List.of());

        List<AreaResponse> resultado = areaServiceImpl.listar();

        assertThat(resultado).isNotNull();
        assertThat(resultado).isEmpty();

        verify(areaRepository).findAll();
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerPorId_debeRetornarAreaCuandoExiste() {
        Area area = areaMock(1L, "ADM", "Administracion", "desc", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        AreaResponse resultado = areaServiceImpl.obtenerPorId(1L);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.codigo()).isEqualTo("ADM");
        assertThat(resultado.nombre()).isEqualTo("Administracion");
        assertThat(resultado.descripcion()).isEqualTo("desc");
        assertThat(resultado.activo()).isTrue();

        verify(areaRepository).findById(1L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerPorId_debeLanzarResourceNotFoundCuandoNoExiste() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> areaServiceImpl.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un área con id 99");

        verify(areaRepository).findById(99L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void actualizar_debeActualizarDatosCorrectamente() {
        Area area = areaMock(1L, "FIN", "Finanzas", "Nueva descripcion", true);
        AreaRequest request = new AreaRequest("  fin  ", "  Finanzas  ", "Nueva descripcion");

        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.existsByCodigoIgnoreCaseAndIdNot("fin", 1L)).thenReturn(false);
        when(areaRepository.existsByNombreIgnoreCaseAndIdNot("Finanzas", 1L)).thenReturn(false);

        AreaResponse resultado = areaServiceImpl.actualizar(1L, request);

        verify(area).actualizarDatos(request.codigo(), request.nombre(), request.descripcion());

        assertThat(resultado.codigo()).isEqualTo("FIN");
        assertThat(resultado.nombre()).isEqualTo("Finanzas");
        assertThat(resultado.descripcion()).isEqualTo("Nueva descripcion");

        verify(areaRepository).findById(1L);
        verify(areaRepository).existsByCodigoIgnoreCaseAndIdNot("fin", 1L);
        verify(areaRepository).existsByNombreIgnoreCaseAndIdNot("Finanzas", 1L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void actualizar_debeLanzarConflictoCuandoCodigoDuplicado() {
        Area area = mock(Area.class);
        AreaRequest request = new AreaRequest("FIN", "Administracion", "desc");

        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.existsByCodigoIgnoreCaseAndIdNot("FIN", 1L)).thenReturn(true);

        assertThatThrownBy(() -> areaServiceImpl.actualizar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un área con el código 'FIN'")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(area, never()).actualizarDatos(any(), any(), any());
        verify(areaRepository).findById(1L);
        verify(areaRepository).existsByCodigoIgnoreCaseAndIdNot("FIN", 1L);
        verify(areaRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void actualizar_debeLanzarConflictoCuandoNombreDuplicado() {
        Area area = mock(Area.class);
        AreaRequest request = new AreaRequest("ADM", "Finanzas", "desc");

        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.existsByCodigoIgnoreCaseAndIdNot("ADM", 1L)).thenReturn(false);
        when(areaRepository.existsByNombreIgnoreCaseAndIdNot("Finanzas", 1L)).thenReturn(true);

        assertThatThrownBy(() -> areaServiceImpl.actualizar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ya existe un área con el nombre 'Finanzas'")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(area, never()).actualizarDatos(any(), any(), any());
        verify(areaRepository).findById(1L);
        verify(areaRepository).existsByCodigoIgnoreCaseAndIdNot("ADM", 1L);
        verify(areaRepository).existsByNombreIgnoreCaseAndIdNot("Finanzas", 1L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void cambiarEstado_debeActivarArea() {
        Area area = areaMock(1L, "ADM", "Administracion", "desc", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        AreaResponse resultado = areaServiceImpl.cambiarEstado(1L, new AreaEstadoRequest(true));

        verify(area).activar();
        verify(area, never()).desactivar();
        assertThat(resultado.activo()).isTrue();

        verify(areaRepository).findById(1L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void cambiarEstado_debeDesactivarArea() {
        Area area = areaMock(1L, "ADM", "Administracion", "desc", false);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        AreaResponse resultado = areaServiceImpl.cambiarEstado(1L, new AreaEstadoRequest(false));

        verify(area).desactivar();
        verify(area, never()).activar();
        assertThat(resultado.activo()).isFalse();

        verify(areaRepository).findById(1L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerActivaPorId_debeRetornarEntidadCuandoEstaActiva() {
        Area area = mock(Area.class);
        when(area.isActivo()).thenReturn(true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        Area resultado = areaServiceImpl.obtenerActivaPorId(1L);

        assertThat(resultado).isSameAs(area);

        verify(areaRepository).findById(1L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerActivaPorId_debeLanzarBusinessExceptionCuandoEstaInactiva() {
        Area area = mock(Area.class);
        when(area.isActivo()).thenReturn(false);
        when(area.getNombre()).thenReturn("Recursos Humanos");
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        assertThatThrownBy(() -> areaServiceImpl.obtenerActivaPorId(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El área 'Recursos Humanos' está inactiva y no puede utilizarse")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(areaRepository).findById(1L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerActivaPorId_debeLanzarResourceNotFoundCuandoNoExiste() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> areaServiceImpl.obtenerActivaPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un área con id 99");

        verify(areaRepository).findById(99L);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerActivasPorIds_debeRetornarAreasActivasEnOrdenSolicitado() {
        Area area1 = mock(Area.class);
        when(area1.getId()).thenReturn(1L);
        when(area1.isActivo()).thenReturn(true);

        Area area2 = mock(Area.class);
        when(area2.getId()).thenReturn(2L);
        when(area2.isActivo()).thenReturn(true);

        List<Long> idsSolicitados = List.of(2L, 1L);
        when(areaRepository.findByIdIn(idsSolicitados)).thenReturn(List.of(area1, area2));

        List<Area> resultado = areaServiceImpl.obtenerActivasPorIds(idsSolicitados);

        assertThat(resultado).containsExactly(area2, area1);

        verify(areaRepository).findByIdIn(idsSolicitados);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerActivasPorIds_debeLanzarNotFoundSiFaltaAlgunaArea() {
        Area area1 = mock(Area.class);
        when(area1.getId()).thenReturn(1L);

        List<Long> idsSolicitados = List.of(1L, 2L);
        when(areaRepository.findByIdIn(idsSolicitados)).thenReturn(List.of(area1));

        assertThatThrownBy(() -> areaServiceImpl.obtenerActivasPorIds(idsSolicitados))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(areaRepository).findByIdIn(idsSolicitados);
        verifyNoMoreInteractions(areaRepository);
    }

    @Test
    void obtenerActivasPorIds_debeLanzarBusinessExceptionSiAlgunaAreaEstaInactiva() {
        Area area1 = mock(Area.class);
        when(area1.getId()).thenReturn(1L);
        when(area1.isActivo()).thenReturn(true);

        Area area2 = mock(Area.class);
        when(area2.getId()).thenReturn(2L);
        when(area2.isActivo()).thenReturn(false);
        when(area2.getNombre()).thenReturn("Área 2");

        List<Long> idsSolicitados = List.of(1L, 2L);
        when(areaRepository.findByIdIn(idsSolicitados)).thenReturn(List.of(area1, area2));

        assertThatThrownBy(() -> areaServiceImpl.obtenerActivasPorIds(idsSolicitados))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Las siguientes áreas están inactivas y no pueden utilizarse: Área 2");

        verify(areaRepository).findByIdIn(idsSolicitados);
        verifyNoMoreInteractions(areaRepository);
    }

    private Area areaMock(Long id, String codigo, String nombre, String descripcion, boolean activo) {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(id);
        when(area.getCodigo()).thenReturn(codigo);
        when(area.getNombre()).thenReturn(nombre);
        when(area.getDescripcion()).thenReturn(descripcion);
        when(area.isActivo()).thenReturn(activo);
        return area;
    }
}
