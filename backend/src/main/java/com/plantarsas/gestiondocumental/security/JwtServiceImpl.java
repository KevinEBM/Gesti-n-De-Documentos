package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
@Service
public class JwtServiceImpl implements JwtService {

    private static final int LONGITUD_MINIMA_SECRETO_BYTES = 32;
    private static final String CLAIM_CORREO = "correo";
    private static final String CLAIM_ROL = "rol";

    private final SecretKey secretKey;
    private final long expirationMs;
    private final Clock clock;
    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            Clock clock) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret es obligatorio");
        }
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("jwt.expiration-ms debe ser mayor que cero");
        }
        if (clock == null) {
            throw new IllegalArgumentException("El reloj no puede ser nulo");
        }
        byte[] secretDecodificado;
        try {
            secretDecodificado = Decoders.BASE64.decode(secret);
        } catch (DecodingException | IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "jwt.secret debe ser una cadena Base64 valida",
                    e
            );
        }
        if (secretDecodificado.length < LONGITUD_MINIMA_SECRETO_BYTES) {
            throw new IllegalArgumentException("jwt.secret debe decodificar a al menos 32 bytes para HS256");
        }

        this.secretKey = Keys.hmacShaKeyFor(secretDecodificado);
        this.expirationMs = expirationMs;
        this.clock = clock;
    }

    @Override
    public String generarToken(Long id, String correo, RolEnum rol) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El id no puede ser nulo ni menor o igual a cero");
        }
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo no puede ser nulo ni vacio");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo");
        }

        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        Instant instanteActual = clock.instant();

        return Jwts.builder()
                .subject(id.toString())
                .claim(CLAIM_CORREO, correoNormalizado)
                .claim(CLAIM_ROL, rol.name())
                .issuedAt(Date.from(instanteActual))
                .expiration(Date.from(instanteActual.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    private Claims parsearClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token no puede ser nulo ni vacio");
        }

        return Jwts.parser()
                .clock(() -> Date.from(clock.instant()))
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long extraerId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El token no contiene un subject valido");
        }

        Long id;
        try {
            id = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El subject del token no es un id numerico valido", e);
        }
        if (id <= 0) {
            throw new IllegalArgumentException("El id del token debe ser mayor que cero");
        }
        return id;
    }

    private String extraerCorreo(Claims claims) {
        String correo = claims.get(CLAIM_CORREO, String.class);
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El token no contiene un correo valido");
        }
        return correo;
    }

    private RolEnum extraerRol(Claims claims) {
        String rol = claims.get(CLAIM_ROL, String.class);

        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException("El token no contiene un rol valido");
        }

        try {
            return RolEnum.valueOf(rol);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El rol del token no es valido", e);
        }
    }

    @Override
    public Long obtenerIdUsuario(String token) {
        Claims claims = parsearClaims(token);
        return extraerId(claims);
    }

    @Override
    public String obtenerCorreo(String token) {
        Claims claims = parsearClaims(token);
        return extraerCorreo(claims);
    }

    @Override
    public RolEnum obtenerRol(String token) {
        Claims claims = parsearClaims(token);
        return extraerRol(claims);
    }

    @Override
    public boolean esTokenValido(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = parsearClaims(token);
            extraerId(claims);
            extraerCorreo(claims);
            extraerRol(claims);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
