package com.plantarsas.gestiondocumental.auth.controller;

import com.plantarsas.gestiondocumental.auth.dto.CambiarContrasenaRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginRequest;
import com.plantarsas.gestiondocumental.auth.dto.LoginResponse;
import com.plantarsas.gestiondocumental.auth.service.AuthService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import com.plantarsas.gestiondocumental.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.exitosa(authService.login(request));
    }

    @PutMapping("/contrasena")
    public ApiResponse<Void> cambiarContrasena(
            @Valid @RequestBody CambiarContrasenaRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuarioAutenticado
    ) {
        if (!request.nuevaContrasena().equals(request.confirmacionContrasena())) {
            throw new BusinessException("Las contraseñas no coinciden.");
        }

        usuarioService.cambiarContrasena(
                usuarioAutenticado.id(),
                request.contrasenaActual(),
                request.nuevaContrasena()
        );
        return ApiResponse.exitosa(null);
    }
}
