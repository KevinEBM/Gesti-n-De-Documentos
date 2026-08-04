package com.plantarsas.gestiondocumental.roles.service;

import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.roles.dto.RolResponse;
import com.plantarsas.gestiondocumental.roles.entity.Rol;
import com.plantarsas.gestiondocumental.roles.repository.RolRepository;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolServiceImplTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private RolServiceImpl rolServiceImpl;

    @Test
    void listar_debeRetornarTodosLosRolesMapeados() {
        Rol administrador = rolCompletoMock(1L, RolEnum.ADMINISTRADOR, "Rol administrador", true);
        Rol jefeArea = rolCompletoMock(2L, RolEnum.JEFE_AREA, "Rol jefe de area", true);
        when(rolRepository.findAll()).thenReturn(List.of(administrador, jefeArea));

        List<RolResponse> resultado = rolServiceImpl.listar();

        assertThat(resultado).hasSize(2);

        assertThat(resultado.get(0).id()).isEqualTo(1L);
        assertThat(resultado.get(0).nombre()).isEqualTo(RolEnum.ADMINISTRADOR);
        assertThat(resultado.get(0).descripcion()).isEqualTo("Rol administrador");
        assertThat(resultado.get(0).activo()).isTrue();

        assertThat(resultado.get(1).id()).isEqualTo(2L);
        assertThat(resultado.get(1).nombre()).isEqualTo(RolEnum.JEFE_AREA);
        assertThat(resultado.get(1).descripcion()).isEqualTo("Rol jefe de area");
        assertThat(resultado.get(1).activo()).isTrue();

        verify(rolRepository).findAll();
        verifyNoMoreInteractions(rolRepository);
    }

    @Test
    void listar_debeRetornarListaVaciaCuandoNoExistenRoles() {
        when(rolRepository.findAll()).thenReturn(List.of());

        List<RolResponse> resultado = rolServiceImpl.listar();

        assertThat(resultado).isNotNull();
        assertThat(resultado).isEmpty();

        verify(rolRepository).findAll();
        verifyNoMoreInteractions(rolRepository);
    }

    @Test
    void obtenerPorId_debeRetornarRolCuandoExiste() {
        Rol administrativo = rolCompletoMock(3L, RolEnum.ADMINISTRATIVO, "Rol administrativo", true);
        when(rolRepository.findById(3L)).thenReturn(Optional.of(administrativo));

        RolResponse resultado = rolServiceImpl.obtenerPorId(3L);

        assertThat(resultado.id()).isEqualTo(3L);
        assertThat(resultado.nombre()).isEqualTo(RolEnum.ADMINISTRATIVO);
        assertThat(resultado.descripcion()).isEqualTo("Rol administrativo");
        assertThat(resultado.activo()).isTrue();

        verify(rolRepository).findById(3L);
        verifyNoMoreInteractions(rolRepository);
    }

    @Test
    void obtenerPorId_debeLanzarResourceNotFoundCuandoNoExiste() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolServiceImpl.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un rol con id 99");

        verify(rolRepository).findById(99L);
        verifyNoMoreInteractions(rolRepository);
    }

    @Test
    void obtenerActivoPorId_debeRetornarEntidadCuandoExisteYEstaActivo() {
        Rol administrador = mock(Rol.class);
        when(administrador.isActivo()).thenReturn(true);
        when(rolRepository.findById(1L)).thenReturn(Optional.of(administrador));

        Rol resultado = rolServiceImpl.obtenerActivoPorId(1L);

        assertThat(resultado).isSameAs(administrador);

        verify(rolRepository).findById(1L);
        verifyNoMoreInteractions(rolRepository);
    }

    @Test
    void obtenerActivoPorId_debeLanzarResourceNotFoundCuandoNoExiste() {
        when(rolRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolServiceImpl.obtenerActivoPorId(50L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un rol con id 50");

        verify(rolRepository).findById(50L);
        verifyNoMoreInteractions(rolRepository);
    }

    @Test
    void obtenerActivoPorId_debeLanzarBusinessExceptionCuandoEstaInactivo() {
        Rol jefeArea = mock(Rol.class);
        when(jefeArea.isActivo()).thenReturn(false);
        when(jefeArea.getNombre()).thenReturn(RolEnum.JEFE_AREA);
        when(rolRepository.findById(2L)).thenReturn(Optional.of(jefeArea));

        assertThatThrownBy(() -> rolServiceImpl.obtenerActivoPorId(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El rol 'JEFE_AREA' está inactivo y no puede asignarse")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(rolRepository).findById(2L);
        verifyNoMoreInteractions(rolRepository);
    }

    private Rol rolCompletoMock(Long id, RolEnum nombre, String descripcion, boolean activo) {
        Rol rol = mock(Rol.class);
        when(rol.getId()).thenReturn(id);
        when(rol.getNombre()).thenReturn(nombre);
        when(rol.getDescripcion()).thenReturn(descripcion);
        when(rol.isActivo()).thenReturn(activo);
        return rol;
    }
}
