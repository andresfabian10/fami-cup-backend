package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famicup.configuracion.JwtProperties;
import com.famicup.modelo.dto.LoginRequest;
import com.famicup.modelo.entidad.SesionAutenticacion;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoUsuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.modelo.mapper.UsuarioMapper;
import com.famicup.repositorio.SesionAutenticacionRepository;
import com.famicup.repositorio.UsuarioRepository;
import com.famicup.seguridad.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SesionAutenticacionRepository sessionRepository;
    @Mock
    private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-for-famicup-auth-service-with-more-than-64-chars",
                "famicup-test",
                900,
                3600,
                1800);
        authService = new AuthService(
                authenticationManager,
                new JwtService(properties),
                properties,
                usuarioRepository,
                sessionRepository,
                new UsuarioMapper(),
                auditService);
    }

    @Test
    void loginAuthenticatesAndCreatesRefreshSession() {
        Usuario user = new Usuario();
        user.setId(UUID.randomUUID());
        user.setUsername("tia.maria");
        user.setFullName("Tia Maria");
        user.setRole(RolUsuario.PLAYER);
        user.setStatus(EstadoUsuario.ACTIVE);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(usuarioRepository.save(user)).thenReturn(user);
        when(sessionRepository.save(any(SesionAutenticacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.login(new LoginRequest("tia.maria", "123player"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().username()).isEqualTo("tia.maria");
        verify(sessionRepository).save(any(SesionAutenticacion.class));
    }
}
