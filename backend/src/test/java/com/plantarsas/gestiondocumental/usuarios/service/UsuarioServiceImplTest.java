package com.plantarsas.gestiondocumental.usuarios.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.service.AreaLookupService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.roles.entity.Rol;
import com.plantarsas.gestiondocumental.roles.service.RolLookupService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioEstadoRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioResponse;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioUpdateRequest;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.mapper.UsuarioMapper;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioAreaRepository usuarioAreaRepository;

    @Mock
    private RolLookupService rolLookupService;

    @Mock
    private AreaLookupService areaLookupService;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioServiceImpl usuarioServiceImpl;

    @BeforeEach
    void inicializar() {
        usuarioServiceImpl = new UsuarioServiceImpl(
                usuarioRepository,
                usuarioAreaRepository,
                rolLookupService,
                areaLookupService,
                usuarioMapper,
                passwordEncoder
        );
    }

    private Rol rolMock(RolEnum nombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(nombre);
        return rol;
    }

    private Area areaMock(Long id) {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(id);
        return area;
    }

    private UsuarioResponse respuestaDePrueba(Long id) {
        return new UsuarioResponse(
                id,
                "Ana",
                "Perez",
                "ana@empresa.com",
                null,
                EstadoUsuario.ACTIVO,
                List.of(),
                null,
                null,
                null
        );
    }

    @Test
    void crear_debeCrearUsuarioConPasswordCifrada() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(10L), 10L
        );
        Rol rol = rolMock(RolEnum.ADMINISTRADOR);
        Area area = areaMock(10L);
        UsuarioResponse respuestaEsperada = respuestaDePrueba(1L);

        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L)).thenReturn(rol);
        when(areaLookupService.obtenerActivaPorId(10L)).thenReturn(area);
        when(passwordEncoder.encode("claveInicial123")).thenReturn("hash-generado");
        when(passwordEncoder.matches("claveInicial123", "hash-generado")).thenReturn(true);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(usuarioMapper.toResponse(any(Usuario.class), anyList())).thenReturn(respuestaEsperada);

        UsuarioResponse resultado = usuarioServiceImpl.crear(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuarioGuardado = usuarioCaptor.getValue();

        verify(passwordEncoder).encode("claveInicial123");
        assertThat(usuarioGuardado.coincideConPassword("claveInicial123", passwordEncoder)).isTrue();
        assertThat(usuarioGuardado.getRol()).isEqualTo(rol);
        assertThat(usuarioGuardado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);

        ArgumentCaptor<List<UsuarioArea>> asignacionesCaptor = ArgumentCaptor.forClass(List.class);
        verify(usuarioAreaRepository).saveAll(asignacionesCaptor.capture());
        assertThat(asignacionesCaptor.getValue()).hasSize(1);
        assertThat(asignacionesCaptor.getValue().get(0).isEsPrincipal()).isTrue();

        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void crear_debeNormalizarCorreo() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "  ANA@Empresa.COM  ", "claveInicial123",
                1L, Set.of(10L), 10L
        );
        Rol rol = rolMock(RolEnum.ADMINISTRADOR);
        Area area = areaMock(10L);

        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L)).thenReturn(rol);
        when(areaLookupService.obtenerActivaPorId(10L)).thenReturn(area);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(usuarioMapper.toResponse(any(Usuario.class), anyList())).thenReturn(respuestaDePrueba(1L));

        usuarioServiceImpl.crear(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getCorreo()).isEqualTo("ana@empresa.com");
        verify(usuarioRepository).existsByCorreoIgnoreCase("ana@empresa.com");
    }

    @Test
    void crear_debeRechazarCorreoDuplicado() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(10L), 10L
        );
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ana@empresa.com");

        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(usuarioAreaRepository);
    }

    @Test
    void crear_debeRechazarRolInexistente() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                99L, Set.of(10L), 10L
        );
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(99L))
                .thenThrow(new ResourceNotFoundException("No existe un rol con id 99"));

        assertThatThrownBy(() -> usuarioServiceImpl.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crear_debeRechazarRolInactivo() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(10L), 10L
        );
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L))
                .thenThrow(new BusinessException("El rol 'ADMINISTRADOR' está inactivo y no puede asignarse"));

        assertThatThrownBy(() -> usuarioServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crear_debeRechazarAreaInexistente() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(99L), 99L
        );
        Rol rol = mock(Rol.class);
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L)).thenReturn(rol);
        when(areaLookupService.obtenerActivaPorId(99L))
                .thenThrow(new ResourceNotFoundException("No existe un área con id 99"));

        assertThatThrownBy(() -> usuarioServiceImpl.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crear_debeRechazarAreaInactiva() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(10L), 10L
        );
        Rol rol = mock(Rol.class);
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L)).thenReturn(rol);
        when(areaLookupService.obtenerActivaPorId(10L))
                .thenThrow(new BusinessException("El área 'Compras' está inactiva y no puede utilizarse"));

        assertThatThrownBy(() -> usuarioServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crear_debeExigirAreaPrincipalParaJefeArea() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(10L), null
        );
        Rol rol = rolMock(RolEnum.JEFE_AREA);
        Area area = mock(Area.class);
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L)).thenReturn(rol);
        when(areaLookupService.obtenerActivaPorId(10L)).thenReturn(area);

        assertThatThrownBy(() -> usuarioServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("área principal");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crear_debeExigirAreaPrincipalParaAdministrativo() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(10L), null
        );
        Rol rol = rolMock(RolEnum.ADMINISTRATIVO);
        Area area = mock(Area.class);
        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L)).thenReturn(rol);
        when(areaLookupService.obtenerActivaPorId(10L)).thenReturn(area);

        assertThatThrownBy(() -> usuarioServiceImpl.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("área principal");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void crear_debePermitirAdministradorSinAreaPrincipal() {
        UsuarioRequest request = new UsuarioRequest(
                "Ana", "Perez", "ana@empresa.com", "claveInicial123",
                1L, Set.of(10L), null
        );
        Rol rol = rolMock(RolEnum.ADMINISTRADOR);
        Area area = areaMock(10L);
        UsuarioResponse respuestaEsperada = respuestaDePrueba(1L);

        when(usuarioRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(1L)).thenReturn(rol);
        when(areaLookupService.obtenerActivaPorId(10L)).thenReturn(area);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(usuarioMapper.toResponse(any(Usuario.class), anyList())).thenReturn(respuestaEsperada);

        UsuarioResponse resultado = usuarioServiceImpl.crear(request);

        assertThat(resultado).isEqualTo(respuestaEsperada);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void actualizar_debeActualizarDatosRolYReemplazarAsignacionesDeAreas() {
        Long id = 1L;
        Usuario usuarioExistente = mock(Usuario.class);
        Rol nuevoRol = rolMock(RolEnum.JEFE_AREA);
        Area nuevaArea = areaMock(20L);
        UsuarioUpdateRequest request = new UsuarioUpdateRequest(
                "Ana Nuevo", "Perez Nuevo", "ana.nueva@empresa.com",
                2L, Set.of(20L), 20L
        );
        UsuarioResponse respuestaEsperada = respuestaDePrueba(id);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.existsByCorreoIgnoreCaseAndIdNot("ana.nueva@empresa.com", id)).thenReturn(false);
        when(rolLookupService.obtenerActivoPorId(2L)).thenReturn(nuevoRol);
        when(areaLookupService.obtenerActivaPorId(20L)).thenReturn(nuevaArea);
        when(usuarioMapper.toResponse(eq(usuarioExistente), anyList())).thenReturn(respuestaEsperada);

        UsuarioResponse resultado = usuarioServiceImpl.actualizar(id, request);

        verify(usuarioExistente).actualizarDatos("Ana Nuevo", "Perez Nuevo", "ana.nueva@empresa.com");
        verify(usuarioExistente).cambiarRol(nuevoRol);
        verify(usuarioExistente, never()).actualizarPassword(anyString());
        verify(usuarioAreaRepository).deleteByUsuarioId(id);
        verify(usuarioAreaRepository).saveAll(anyList());
        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void cambiarEstado_debeActualizarEstado() {
        Long id = 1L;
        Usuario usuario = mock(Usuario.class);
        UsuarioEstadoRequest request = new UsuarioEstadoRequest(EstadoUsuario.INACTIVO);
        UsuarioResponse respuestaEsperada = respuestaDePrueba(id);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioAreaRepository.findByUsuario_Id(id)).thenReturn(List.of());
        when(usuarioMapper.toResponse(usuario, List.of())).thenReturn(respuestaEsperada);

        UsuarioResponse resultado = usuarioServiceImpl.cambiarEstado(id, request);

        verify(usuario).cambiarEstado(EstadoUsuario.INACTIVO);
        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void obtenerPorId_debeRetornarUsuarioMapeado() {
        Long id = 1L;
        Usuario usuario = mock(Usuario.class);
        List<UsuarioArea> asignaciones = List.of();
        UsuarioResponse respuestaEsperada = respuestaDePrueba(id);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(usuarioAreaRepository.findByUsuario_Id(id)).thenReturn(asignaciones);
        when(usuarioMapper.toResponse(usuario, asignaciones)).thenReturn(respuestaEsperada);

        UsuarioResponse resultado = usuarioServiceImpl.obtenerPorId(id);

        assertThat(resultado).isEqualTo(respuestaEsperada);
    }

    @Test
    void obtenerPorId_debeLanzarResourceNotFoundSiNoExiste() {
        Long id = 404L;
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioServiceImpl.obtenerPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(usuarioAreaRepository);
        verifyNoInteractions(usuarioMapper);
    }

    @Test
    void listar_debeMapearUsuariosConSusAreas() {
        Usuario usuario1 = mock(Usuario.class);
        Usuario usuario2 = mock(Usuario.class);
        when(usuario1.getId()).thenReturn(1L);
        when(usuario2.getId()).thenReturn(2L);
        List<UsuarioArea> asignaciones1 = List.of();
        List<UsuarioArea> asignaciones2 = List.of();
        UsuarioResponse respuesta1 = respuestaDePrueba(1L);
        UsuarioResponse respuesta2 = respuestaDePrueba(2L);

        when(usuarioRepository.findAll()).thenReturn(List.of(usuario1, usuario2));
        when(usuarioAreaRepository.findByUsuario_Id(1L)).thenReturn(asignaciones1);
        when(usuarioAreaRepository.findByUsuario_Id(2L)).thenReturn(asignaciones2);
        when(usuarioMapper.toResponse(usuario1, asignaciones1)).thenReturn(respuesta1);
        when(usuarioMapper.toResponse(usuario2, asignaciones2)).thenReturn(respuesta2);

        List<UsuarioResponse> resultado = usuarioServiceImpl.listar();

        assertThat(resultado).containsExactly(respuesta1, respuesta2);
    }

    @Test
    void listar_debeRetornarListaVaciaSiNoHayUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of());

        List<UsuarioResponse> resultado = usuarioServiceImpl.listar();

        assertThat(resultado).isEmpty();
        verifyNoInteractions(usuarioAreaRepository, usuarioMapper);
    }
}
