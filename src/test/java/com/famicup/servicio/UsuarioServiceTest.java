package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.CambiarPasswordRequest;
import com.famicup.modelo.dto.CrearUsuarioRequest;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.modelo.mapper.UsuarioMapper;
import com.famicup.repositorio.PagoRepository;
import com.famicup.repositorio.PuntosRankingRepository;
import com.famicup.repositorio.UsuarioRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PuntosRankingRepository rankingRepository;
    @Mock
    private BettingParametersService parametersService;
    @Mock
    private AuditService auditService;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(
                usuarioRepository,
                pagoRepository,
                rankingRepository,
                parametersService,
                new BCryptPasswordEncoder(),
                new UsuarioMapper(),
                auditService);
    }

    @Test
    void createsPlayerWithHashedPasswordRankingAndGlobalPayment() {
        when(usuarioRepository.existsByUsernameIgnoreCase("nuevo.player")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(parametersService.getParameters()).thenReturn(parameters());

        var response = usuarioService.createUser(new CrearUsuarioRequest(
                "nuevo.player",
                "123player",
                "Nuevo Player",
                "nuevo@famicup.local",
                null,
                RolUsuario.PLAYER,
                true));

        assertThat(response.username()).isEqualTo("nuevo.player");
        assertThat(response.player()).isTrue();
        assertThat(response.mustChangePassword()).isTrue();
        verify(rankingRepository).save(any());
        verify(pagoRepository).save(any());
    }

    @Test
    void playerCanChangeTemporaryPassword() {
        Usuario user = new Usuario();
        user.setId(UUID.randomUUID());
        user.setUsername("nuevo.player");
        user.setFullName("Nuevo Player");
        user.setRole(RolUsuario.PLAYER);
        user.setMustChangePassword(true);
        when(usuarioRepository.findById(user.getId())).thenReturn(java.util.Optional.of(user));
        when(usuarioRepository.save(user)).thenReturn(user);

        var response = usuarioService.changeCurrentUserPassword(user, new CambiarPasswordRequest("abc"));

        assertThat(response.mustChangePassword()).isFalse();
        assertThat(response.passwordChangedAt()).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("abc", user.getPasswordHash())).isTrue();
        verify(usuarioRepository).save(user);
    }

    @Test
    void rejectsSecondAdmin() {
        when(usuarioRepository.existsByUsernameIgnoreCase("otro.admin")).thenReturn(false);
        when(usuarioRepository.existsByRole(RolUsuario.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.createUser(new CrearUsuarioRequest(
                "otro.admin",
                "123admin",
                "Otro Admin",
                null,
                null,
                RolUsuario.ADMIN,
                false))).isInstanceOf(ReglaNegocioException.class);
    }

    private ParametrosApuestasResponse parameters() {
        return new ParametrosApuestasResponse(
                BigDecimal.valueOf(5000),
                3,
                BigDecimal.valueOf(60000),
                10,
                0,
                2,
                50,
                30,
                20,
                5,
                10,
                java.time.OffsetDateTime.parse("2026-06-11T00:00:00Z"),
                "573163353115",
                "Hola",
                "Acceso",
                "Recuperar",
                "Solicitar",
                false,
                "",
                "",
                "Banner",
                12);
    }
}
