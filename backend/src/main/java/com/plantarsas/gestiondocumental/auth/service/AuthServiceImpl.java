package com.plantarsas.gestiondocumental.auth.service;

import com.plantarsas.gestiondocumental.auth.dto.LoginRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginResponse;
import com.plantarsas.gestiondocumental.exception.AuthenticationFailedException;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAreaRepository usuarioAreaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioAreaRepository usuarioAreaRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioAreaRepository = usuarioAreaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    private AuthenticationFailedException credencialesInvalidas() {
        return new AuthenticationFailedException("Credenciales inválidas");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request == null
                || request.correo() == null
                || request.contrasena() == null) {
            throw credencialesInvalidas();
        }

        String correoNormalizado =
                request.correo().trim().toLowerCase(Locale.ROOT);

        if (correoNormalizado.isBlank() || request.contrasena().isBlank()) {
            throw credencialesInvalidas();
        }

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(correoNormalizado)
                .orElseThrow(this::credencialesInvalidas);

        if (usuario.getEstado() != EstadoUsuario.ACTIVO
                || !usuario.coincideConPassword(
                        request.contrasena(),
                        passwordEncoder
                )) {
            throw credencialesInvalidas();
        }

        if (usuario.getId() == null
                || usuario.getRol() == null
                || usuario.getRol().getNombre() == null
                || usuario.getCorreo() == null
                || usuario.getCorreo().isBlank()) {
            throw credencialesInvalidas();
        }

        RolEnum rol = usuario.getRol().getNombre();

        String token = jwtService.generarToken(
                usuario.getId(),
                usuario.getCorreo(),
                rol
        );

        Long areaPrincipalId = null;
        String areaPrincipalNombre = null;
        var areaPrincipal = usuarioAreaRepository
                .findFirstByUsuario_IdAndEsPrincipalTrue(usuario.getId());
        if (areaPrincipal.isPresent()) {
            UsuarioArea asignacion = areaPrincipal.get();
            areaPrincipalId = asignacion.getArea().getId();
            areaPrincipalNombre = asignacion.getArea().getNombre();
        }

        return new LoginResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getNombres(),
                usuario.getApellidos(),
                rol,
                areaPrincipalId,
                areaPrincipalNombre
        );
    }
}
