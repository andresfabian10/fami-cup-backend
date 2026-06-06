package com.famicup.servicio;

import com.famicup.configuracion.JwtProperties;
import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.AuthResponse;
import com.famicup.modelo.dto.LoginRequest;
import com.famicup.modelo.dto.RefreshTokenRequest;
import com.famicup.modelo.dto.UsuarioResponse;
import com.famicup.modelo.entidad.SesionAutenticacion;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.mapper.UsuarioMapper;
import com.famicup.repositorio.SesionAutenticacionRepository;
import com.famicup.repositorio.UsuarioRepository;
import com.famicup.seguridad.JwtService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UsuarioRepository usuarioRepository;
    private final SesionAutenticacionRepository sessionRepository;
    private final UsuarioMapper usuarioMapper;
    private final AuditService auditService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            JwtProperties jwtProperties,
            UsuarioRepository usuarioRepository,
            SesionAutenticacionRepository sessionRepository,
            UsuarioMapper usuarioMapper,
            AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.usuarioRepository = usuarioRepository;
        this.sessionRepository = sessionRepository;
        this.usuarioMapper = usuarioMapper;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException exception) {
            auditService.recordAnonymous(username, "LOGIN_FAILED", "AUTH", username, "Intento de login fallido", "Credenciales invalidas");
            throw exception;
        }
        Usuario user = (Usuario) authentication.getPrincipal();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        user.setLastLoginAt(now);
        usuarioRepository.save(user);
        auditService.record(user, "LOGIN_SUCCESS", "AUTH", user.getId().toString(), "Login exitoso", "Sesion iniciada");
        return buildAuthResponse(user, createRefreshSession(user, now));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String tokenHash = jwtService.hashToken(request.refreshToken());
        SesionAutenticacion session = sessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new ReglaNegocioException("Refresh token invalido."));

        if (!session.isActive(now)) {
            throw new ReglaNegocioException("La sesion expiro o fue revocada.");
        }
        if (session.getLastUsedAt().plusSeconds(jwtProperties.refreshInactivityExpirationSeconds()).isBefore(now)) {
            session.setRevokedAt(now);
            throw new ReglaNegocioException("La sesion expiro por inactividad.");
        }

        session.setRevokedAt(now);
        session.setLastUsedAt(now);
        return buildAuthResponse(session.getUser(), createRefreshSession(session.getUser(), now));
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = jwtService.hashToken(request.refreshToken());
        sessionRepository.findByRefreshTokenHash(tokenHash).ifPresent(session -> {
            session.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
        });
    }

    @Transactional(readOnly = true)
    public UsuarioResponse me(Authentication authentication) {
        Usuario user = usuarioRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ReglaNegocioException("Usuario autenticado no encontrado."));
        return usuarioMapper.toResponse(user);
    }

    private AuthResponse buildAuthResponse(Usuario user, String refreshToken) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshToken,
                "Bearer",
                jwtService.accessTokenExpirationSeconds(),
                usuarioMapper.toResponse(user));
    }

    private String createRefreshSession(Usuario user, OffsetDateTime now) {
        String refreshToken = jwtService.generateRefreshToken();
        SesionAutenticacion session = new SesionAutenticacion();
        session.setUser(user);
        session.setRefreshTokenHash(jwtService.hashToken(refreshToken));
        session.setLastUsedAt(now);
        session.setExpiresAt(now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds()));
        sessionRepository.save(session);
        return refreshToken;
    }
}
