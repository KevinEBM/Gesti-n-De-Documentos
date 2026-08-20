package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.exception.UnauthorizedException;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAreaAutorizacionServiceTest {

    private static final Long USUARIO_ID = 4L;

    @Mock
    private UsuarioAreaRepository usuarioAreaRepository;

    private UsuarioAreaAutorizacionService usuarioAreaAutorizacionService;

    @BeforeEach
    void inicializar() {
        usuarioAreaAutorizacionService = new UsuarioAreaAutorizacionService(usuarioAreaRepository);
    }

    private AuthenticatedUser administrador() {
        return new AuthenticatedUser(1L, "admin@plantarsas.com", RolEnum.ADMINISTRADOR);
    }

    private AuthenticatedUser jefeArea() {
        return new AuthenticatedUser(USUARIO_ID, "jefe@plantarsas.com", RolEnum.JEFE_AREA);
    }

    private AuthenticatedUser administrativo() {
        return new AuthenticatedUser(USUARIO_ID, "administrativo@plantarsas.com", RolEnum.ADMINISTRATIVO);
    }

    @Test
    void obtenerAreaIdsAutorizadas_conAdministrativo_debeRetornarSoloElAreaPrincipal() {
        UsuarioArea principal = asignacionPrincipalMock(10L);
        UsuarioArea secundaria = mock(UsuarioArea.class);
        when(secundaria.isEsPrincipal()).thenReturn(false);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of(principal, secundaria));

        Set<Long> resultado = usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(administrativo());

        assertThat(resultado).containsExactly(10L);
        verify(usuarioAreaRepository).findByUsuario_Id(USUARIO_ID);
    }

    @Test
    void obtenerAreaIdsAutorizadas_conJefeArea_debeRetornarSoloElAreaPrincipal() {
        UsuarioArea principal = asignacionPrincipalMock(10L);
        UsuarioArea secundariaLegacy = mock(UsuarioArea.class);
        when(secundariaLegacy.isEsPrincipal()).thenReturn(false);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of(principal, secundariaLegacy));

        Set<Long> resultado = usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(jefeArea());

        assertThat(resultado).containsExactly(10L);
    }

    @Test
    void obtenerAreaIdsAutorizadas_conUsuarioSinPrincipal_debeRetornarConjuntoVacio() {
        UsuarioArea secundaria = mock(UsuarioArea.class);
        when(secundaria.isEsPrincipal()).thenReturn(false);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of(secundaria));

        Set<Long> resultado = usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(jefeArea());

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtenerAreaIdsAutorizadas_conAdministrador_debeRetornarConjuntoVacio() {
        Set<Long> resultado = usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(administrador());

        assertThat(resultado).isEmpty();
    }

    @Test
    void validarAccesoArea_conAdministrador_noDebeLanzarExcepcion() {
        assertThatCode(() -> usuarioAreaAutorizacionService.validarAccesoArea(administrador(), 99L))
                .doesNotThrowAnyException();
    }

    @Test
    void validarAccesoArea_conAreaAutorizada_noDebeLanzarExcepcion() {
        UsuarioArea principal = asignacionPrincipalMock(10L);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of(principal));

        assertThatCode(() -> usuarioAreaAutorizacionService.validarAccesoArea(administrativo(), 10L))
                .doesNotThrowAnyException();
    }

    @Test
    void validarAccesoArea_conAreaAjena_debeLanzarUnauthorizedException() {
        UsuarioArea principal = asignacionPrincipalMock(10L);
        when(usuarioAreaRepository.findByUsuario_Id(USUARIO_ID)).thenReturn(List.of(principal));

        assertThatThrownBy(() -> usuarioAreaAutorizacionService.validarAccesoArea(administrativo(), 99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    private UsuarioArea asignacionPrincipalMock(Long areaId) {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(areaId);
        UsuarioArea usuarioArea = mock(UsuarioArea.class);
        when(usuarioArea.isEsPrincipal()).thenReturn(true);
        when(usuarioArea.getArea()).thenReturn(area);
        return usuarioArea;
    }
}
