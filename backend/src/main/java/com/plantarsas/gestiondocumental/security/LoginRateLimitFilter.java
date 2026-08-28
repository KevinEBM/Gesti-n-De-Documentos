package com.plantarsas.gestiondocumental.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Limita intentos de {@code POST /api/auth/login} por IP del cliente.
 * <p>
 * La IP se obtiene con {@link HttpServletRequest#getRemoteAddr()}. En producción,
 * {@code server.forward-headers-strategy=native} activa el RemoteIpValve de Tomcat,
 * que reescribe {@code getRemoteAddr()} a partir de cabeceras del proxy confiable
 * (Nginx), sin leer {@code X-Forwarded-For} a mano en la aplicación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String MENSAJE_LIMITE =
            "Demasiados intentos de inicio de sesión. Intenta nuevamente en 5 minutos.";

    private final LoginRateLimiter loginRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!esLoginPost(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        if (loginRateLimiter.estaBloqueado(clientIp)) {
            log.warn(
                    "Rate limit de login excedido: ip={}, ruta={}, momento={}",
                    clientIp,
                    LOGIN_PATH,
                    Instant.now()
            );
            responderDemasiadasSolicitudes(response);
            return;
        }

        filterChain.doFilter(request, response);

        int status = response.getStatus();
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            loginRateLimiter.registrarFallo(clientIp);
        } else if (status >= 200 && status < 300) {
            loginRateLimiter.limpiar(clientIp);
        }
    }

    private boolean esLoginPost(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return false;
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        return LOGIN_PATH.equals(path);
    }

    private void responderDemasiadasSolicitudes(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(MENSAJE_LIMITE));
    }
}
