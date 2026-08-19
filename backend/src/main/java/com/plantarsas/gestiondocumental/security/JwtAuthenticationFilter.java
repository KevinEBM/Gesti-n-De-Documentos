package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        autenticarSiCorresponde(request);
        filterChain.doFilter(request, response);
    }

    private void autenticarSiCorresponde(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String token = extraerToken(request);
        if (token == null) {
            return;
        }

        if (!jwtService.esTokenValido(token)) {
            return;
        }

        Long id;
        try {
            id = jwtService.obtenerIdUsuario(token);
        } catch (JwtException | IllegalArgumentException e) {
            return;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findWithRolById(id);
        if (usuarioOpt.isEmpty()) {
            return;
        }

        Usuario usuario = usuarioOpt.get();
        if (!esUsuarioValido(usuario)) {
            return;
        }

        RolEnum rolEnum = usuario.getRol().getNombre();

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        usuario.getId(),
                        usuario.getCorreo(),
                        rolEnum
                );

        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + rolEnum.name());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        List.of(authority)
                );
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private boolean esUsuarioValido(Usuario usuario) {
        if (usuario.getId() == null) {
            return false;
        }
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            return false;
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            return false;
        }
        if (usuario.getRol() == null) {
            return false;
        }
        if (usuario.getRol().getNombre() == null) {
            return false;
        }
        return true;
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length());
        if (token.isBlank()) {
            return null;
        }
        return token;
    }
}
