package com.plantarsas.gestiondocumental.auth.service;

import com.plantarsas.gestiondocumental.auth.dto.LoginRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginResponse;
import com.plantarsas.gestiondocumental.auth.dto.PerfilUsuarioResponse;
import com.plantarsas.gestiondocumental.exception.AuthenticationFailedException;
import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.roles.entity.Rol;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioAreaRepository usuarioAreaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                usuarioRepository,
                usuarioAreaRepository,
                passwordEncoder,
                jwtService
        );
    }

    private Rol mockRolConNombre(RolEnum nombre) {
        Rol rol = mock(Rol.class);
        when(rol.getNombre()).thenReturn(nombre);
        return rol;
    }

    private Usuario mockUsuarioValido() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getCorreo()).thenReturn("correo@ejemplo.com");
        when(usuario.getNombres()).thenReturn("Nombres");
        when(usuario.getApellidos()).thenReturn("Apellidos");
        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        Rol rol = mockRolConNombre(RolEnum.ADMINISTRADOR);
        when(usuario.getRol()).thenReturn(rol);
        return usuario;
    }

    @Test
    void login_debeRetornarRespuestaYTokenCuandoCredencialesSonValidas() {
        Usuario usuario = mockUsuarioValido();
        LoginRequest request = new LoginRequest("  Correo@Ejemplo.COM  ", "miPassword123");

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));
        when(usuario.coincideConPassword(eq("miPassword123"), eq(passwordEncoder)))
                .thenReturn(true);
        when(jwtService.generarToken(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR))
                .thenReturn("token-generado");
        when(usuarioAreaRepository.findFirstByUsuario_IdAndEsPrincipalTrue(1L))
                .thenReturn(Optional.empty());

        LoginResponse respuesta = authService.login(request);

        assertThat(respuesta.token()).isEqualTo("token-generado");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.correo()).isEqualTo("correo@ejemplo.com");
        assertThat(respuesta.nombres()).isEqualTo("Nombres");
        assertThat(respuesta.apellidos()).isEqualTo("Apellidos");
        assertThat(respuesta.rol()).isEqualTo(RolEnum.ADMINISTRADOR);
        assertThat(respuesta.areaPrincipalId()).isNull();
        assertThat(respuesta.areaPrincipalNombre()).isNull();

        verify(usuarioRepository).findByCorreoIgnoreCase("correo@ejemplo.com");
    }

    @Test
    void login_debeEnviarContrasenaSinRecortarNiModificar() {
        Usuario usuario = mockUsuarioValido();
        String contrasenaConEspacios = "  miPassword123  ";
        LoginRequest request = new LoginRequest("correo@ejemplo.com", contrasenaConEspacios);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));
        when(usuario.coincideConPassword(eq(contrasenaConEspacios), eq(passwordEncoder)))
                .thenReturn(true);
        when(jwtService.generarToken(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR))
                .thenReturn("token-generado");
        when(usuarioAreaRepository.findFirstByUsuario_IdAndEsPrincipalTrue(1L))
                .thenReturn(Optional.empty());

        authService.login(request);

        verify(usuario).coincideConPassword(eq(contrasenaConEspacios), eq(passwordEncoder));
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoRequestEsNulo() {
        assertThatThrownBy(() -> authService.login(null))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoCorreoEsNuloOVacio() {
        LoginRequest requestCorreoNulo = new LoginRequest(null, "miPassword123");
        LoginRequest requestCorreoVacio = new LoginRequest("   ", "miPassword123");

        assertThatThrownBy(() -> authService.login(requestCorreoNulo))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        assertThatThrownBy(() -> authService.login(requestCorreoVacio))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoContrasenaEsNulaOVacia() {
        LoginRequest requestContrasenaNula = new LoginRequest("correo@ejemplo.com", null);
        LoginRequest requestContrasenaVacia = new LoginRequest("correo@ejemplo.com", "   ");

        assertThatThrownBy(() -> authService.login(requestContrasenaNula))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        assertThatThrownBy(() -> authService.login(requestContrasenaVacia))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verifyNoInteractions(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoUsuarioNoExiste() {
        LoginRequest request = new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuarioRepository).findByCorreoIgnoreCase("correo@ejemplo.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoContrasenaEsIncorrecta() {
        Usuario usuario = mock(Usuario.class);
        LoginRequest request =
                new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));

        when(usuario.coincideConPassword(
                eq("miPassword123"),
                eq(passwordEncoder)
        )).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuario).coincideConPassword(
                eq("miPassword123"),
                eq(passwordEncoder)
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoUsuarioEstaInactivo() {
        Usuario usuario = mock(Usuario.class);
        LoginRequest request =
                new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuario.getEstado()).thenReturn(EstadoUsuario.INACTIVO);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuario, never()).coincideConPassword(
                anyString(),
                any()
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoUsuarioEstaBloqueado() {
        Usuario usuario = mock(Usuario.class);
        LoginRequest request =
                new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuario.getEstado()).thenReturn(EstadoUsuario.BLOQUEADO);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuario, never()).coincideConPassword(
                anyString(),
                any()
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_debeFallarConMensajeGenericoCuandoEstadoEsNulo() {
        Usuario usuario = mock(Usuario.class);
        LoginRequest request =
                new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuario.getEstado()).thenReturn(null);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuario, never()).coincideConPassword(
                anyString(),
                any()
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_debeFallarCuandoIdEsNulo() {
        Usuario usuario = mock(Usuario.class);
        LoginRequest request =
                new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));

        when(usuario.coincideConPassword(
                "miPassword123",
                passwordEncoder
        )).thenReturn(true);

        when(usuario.getId()).thenReturn(null);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuario).coincideConPassword(
                "miPassword123",
                passwordEncoder
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_debeFallarCuandoRolEsNulo() {
        Usuario usuario = mock(Usuario.class);
        LoginRequest request =
                new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));

        when(usuario.coincideConPassword(
                "miPassword123",
                passwordEncoder
        )).thenReturn(true);

        when(usuario.getId()).thenReturn(1L);
        when(usuario.getRol()).thenReturn(null);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuario).coincideConPassword(
                "miPassword123",
                passwordEncoder
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_debeFallarCuandoNombreDelRolEsNulo() {
        Usuario usuario = mock(Usuario.class);
        Rol rol = mock(Rol.class);
        LoginRequest request =
                new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));

        when(usuario.coincideConPassword(
                "miPassword123",
                passwordEncoder
        )).thenReturn(true);

        when(usuario.getId()).thenReturn(1L);
        when(usuario.getRol()).thenReturn(rol);
        when(rol.getNombre()).thenReturn(null);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuario).coincideConPassword(
                "miPassword123",
                passwordEncoder
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_debeFallarCuandoCorreoDelUsuarioEsNuloOVacio() {
        Usuario usuarioCorreoNulo = mock(Usuario.class);
        Usuario usuarioCorreoVacio = mock(Usuario.class);
        Rol rolCorreoNulo = mock(Rol.class);
        Rol rolCorreoVacio = mock(Rol.class);

        LoginRequest requestCorreoNulo =
                new LoginRequest("nulo@ejemplo.com", "miPassword123");
        LoginRequest requestCorreoVacio =
                new LoginRequest("vacio@ejemplo.com", "miPassword123");

        when(usuarioCorreoNulo.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        when(usuarioCorreoVacio.getEstado()).thenReturn(EstadoUsuario.ACTIVO);

        when(usuarioRepository.findByCorreoIgnoreCase("nulo@ejemplo.com"))
                .thenReturn(Optional.of(usuarioCorreoNulo));
        when(usuarioRepository.findByCorreoIgnoreCase("vacio@ejemplo.com"))
                .thenReturn(Optional.of(usuarioCorreoVacio));

        when(usuarioCorreoNulo.coincideConPassword(
                "miPassword123",
                passwordEncoder
        )).thenReturn(true);

        when(usuarioCorreoVacio.coincideConPassword(
                "miPassword123",
                passwordEncoder
        )).thenReturn(true);

        when(usuarioCorreoNulo.getId()).thenReturn(1L);
        when(usuarioCorreoNulo.getRol()).thenReturn(rolCorreoNulo);
        when(rolCorreoNulo.getNombre()).thenReturn(RolEnum.ADMINISTRADOR);
        when(usuarioCorreoNulo.getCorreo()).thenReturn(null);

        when(usuarioCorreoVacio.getId()).thenReturn(2L);
        when(usuarioCorreoVacio.getRol()).thenReturn(rolCorreoVacio);
        when(rolCorreoVacio.getNombre()).thenReturn(RolEnum.ADMINISTRADOR);
        when(usuarioCorreoVacio.getCorreo()).thenReturn("   ");

        assertThatThrownBy(() -> authService.login(requestCorreoNulo))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        assertThatThrownBy(() -> authService.login(requestCorreoVacio))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Credenciales inválidas");

        verify(usuarioCorreoNulo).coincideConPassword(
                "miPassword123",
                passwordEncoder
        );
        verify(usuarioCorreoVacio).coincideConPassword(
                "miPassword123",
                passwordEncoder
        );
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_conAreaPrincipal_debeIncluirDatosDePresentacionDelArea() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getCorreo()).thenReturn("correo@ejemplo.com");
        when(usuario.getNombres()).thenReturn("Test");
        when(usuario.getApellidos()).thenReturn("Prueba");
        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        Rol rolJefe = mockRolConNombre(RolEnum.JEFE_AREA);
        when(usuario.getRol()).thenReturn(rolJefe);

        LoginRequest request = new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));
        when(usuario.coincideConPassword(eq("miPassword123"), eq(passwordEncoder)))
                .thenReturn(true);
        when(jwtService.generarToken(1L, "correo@ejemplo.com", RolEnum.JEFE_AREA))
                .thenReturn("token-generado");

        UsuarioArea asignacion = mock(UsuarioArea.class);
        Area area = mock(Area.class);
        when(asignacion.getArea()).thenReturn(area);
        when(area.getId()).thenReturn(5L);
        when(area.getNombre()).thenReturn("Aseo");
        when(usuarioAreaRepository.findFirstByUsuario_IdAndEsPrincipalTrue(1L))
                .thenReturn(Optional.of(asignacion));

        LoginResponse respuesta = authService.login(request);

        assertThat(respuesta.areaPrincipalId()).isEqualTo(5L);
        assertThat(respuesta.areaPrincipalNombre()).isEqualTo("Aseo");
    }

    @Test
    void login_sinAreaPrincipal_debeDejarAreaNula() {
        Usuario usuario = mockUsuarioValido();
        LoginRequest request = new LoginRequest("correo@ejemplo.com", "miPassword123");

        when(usuarioRepository.findByCorreoIgnoreCase("correo@ejemplo.com"))
                .thenReturn(Optional.of(usuario));
        when(usuario.coincideConPassword(eq("miPassword123"), eq(passwordEncoder)))
                .thenReturn(true);
        when(jwtService.generarToken(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR))
                .thenReturn("token-generado");
        when(usuarioAreaRepository.findFirstByUsuario_IdAndEsPrincipalTrue(1L))
                .thenReturn(Optional.empty());

        LoginResponse respuesta = authService.login(request);

        assertThat(respuesta.areaPrincipalId()).isNull();
        assertThat(respuesta.areaPrincipalNombre()).isNull();
    }

    @Test
    void obtenerPerfilActual_jefeArea_devuelveAreaVigenteDeBd() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(9L);
        when(usuario.getCorreo()).thenReturn("juan@ejemplo.com");
        when(usuario.getNombres()).thenReturn("Juan");
        when(usuario.getApellidos()).thenReturn("Pérez");
        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        Rol rol = mockRolConNombre(RolEnum.JEFE_AREA);
        when(usuario.getRol()).thenReturn(rol);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));

        UsuarioArea asignacion = mock(UsuarioArea.class);
        Area area = mock(Area.class);
        when(asignacion.getArea()).thenReturn(area);
        when(area.getId()).thenReturn(7L);
        when(area.getNombre()).thenReturn("Gestión de la Calidad");
        when(usuarioAreaRepository.findFirstByUsuario_IdAndEsPrincipalTrue(9L))
                .thenReturn(Optional.of(asignacion));

        PerfilUsuarioResponse perfil = authService.obtenerPerfilActual(
                new AuthenticatedUser(9L, "juan@ejemplo.com", RolEnum.JEFE_AREA)
        );

        assertThat(perfil.areaPrincipalId()).isEqualTo(7L);
        assertThat(perfil.areaPrincipalNombre()).isEqualTo("Gestión de la Calidad");
        assertThat(perfil.nombres()).isEqualTo("Juan");
        assertThat(perfil.rol()).isEqualTo(RolEnum.JEFE_AREA);
        verifyNoInteractions(jwtService);
    }

    @Test
    void obtenerPerfilActual_administrador_devuelveAreaNula() {
        Usuario usuario = mockUsuarioValido();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        PerfilUsuarioResponse perfil = authService.obtenerPerfilActual(
                new AuthenticatedUser(1L, "correo@ejemplo.com", RolEnum.ADMINISTRADOR)
        );

        assertThat(perfil.areaPrincipalId()).isNull();
        assertThat(perfil.areaPrincipalNombre()).isNull();
        verify(usuarioAreaRepository, never()).findFirstByUsuario_IdAndEsPrincipalTrue(any());
    }

    @Test
    void obtenerPerfilActual_usuarioInactivo_lanzaAuthenticationFailed() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getEstado()).thenReturn(EstadoUsuario.INACTIVO);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() ->
                authService.obtenerPerfilActual(
                        new AuthenticatedUser(9L, "juan@ejemplo.com", RolEnum.JEFE_AREA)
                )
        ).isInstanceOf(AuthenticationFailedException.class);

        verifyNoInteractions(jwtService);
    }
}
