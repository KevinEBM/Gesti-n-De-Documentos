package com.plantarsas.gestiondocumental.auth.controller;

import com.plantarsas.gestiondocumental.auth.dto.CambiarContrasenaRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginResponse;
import com.plantarsas.gestiondocumental.auth.dto.PerfilUsuarioResponse;
import com.plantarsas.gestiondocumental.auth.service.AuthService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ContrasenaActualIncorrectaException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.PasswordChangeRateLimiter;
import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import com.plantarsas.gestiondocumental.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String MENSAJE_LIMITE_CONTRASENA =
            "Demasiados intentos de cambio de contraseña. Intenta nuevamente en 1 hora.";

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final PasswordChangeRateLimiter passwordChangeRateLimiter;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.exitosa(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<PerfilUsuarioResponse> obtenerPerfilActual(
            @AuthenticationPrincipal AuthenticatedUser usuarioAutenticado
    ) {
        return ApiResponse.exitosa(authService.obtenerPerfilActual(usuarioAutenticado));
    }

    @PutMapping("/contrasena")
    public ApiResponse<Void> cambiarContrasena(
            @Valid @RequestBody CambiarContrasenaRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuarioAutenticado
    ) {
        if (!request.nuevaContrasena().equals(request.confirmacionContrasena())) {
            throw new BusinessException("Las contraseñas no coinciden.");
        }

        Long usuarioId = usuarioAutenticado.id();
        if (passwordChangeRateLimiter.estaBloqueado(usuarioId)) {
            throw new BusinessException(MENSAJE_LIMITE_CONTRASENA, HttpStatus.TOO_MANY_REQUESTS);
        }

        try {
            usuarioService.cambiarContrasena(
                    usuarioId,
                    request.contrasenaActual(),
                    request.nuevaContrasena()
            );
        } catch (ContrasenaActualIncorrectaException ex) {
            passwordChangeRateLimiter.registrarFallo(usuarioId);
            throw ex;
        }

        passwordChangeRateLimiter.limpiar(usuarioId);
        return ApiResponse.exitosa(null);
    }
}
