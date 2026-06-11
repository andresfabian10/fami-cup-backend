package com.famicup.servicio;

import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.dto.ActualizarUsuarioRequest;
import com.famicup.modelo.dto.CambiarPasswordRequest;
import com.famicup.modelo.dto.CrearUsuarioRequest;
import com.famicup.modelo.dto.UsuarioResponse;
import com.famicup.modelo.entidad.Pago;
import com.famicup.modelo.entidad.PuntosRanking;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.EstadoUsuario;
import com.famicup.modelo.enumeracion.RolUsuario;
import com.famicup.modelo.enumeracion.SistemaPago;
import com.famicup.modelo.mapper.UsuarioMapper;
import com.famicup.modelo.validacion.PasswordPolicy;
import com.famicup.repositorio.PagoRepository;
import com.famicup.repositorio.PuntosRankingRepository;
import com.famicup.repositorio.UsuarioRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final PuntosRankingRepository rankingRepository;
    private final BettingParametersService parametersService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final AuditService auditService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PagoRepository pagoRepository,
            PuntosRankingRepository rankingRepository,
            BettingParametersService parametersService,
            PasswordEncoder passwordEncoder,
            UsuarioMapper usuarioMapper,
            AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.rankingRepository = rankingRepository;
        this.parametersService = parametersService;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listUsers() {
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse createUser(CrearUsuarioRequest request) {
        String username = request.username().trim().toLowerCase();
        String password = PasswordPolicy.normalizeAndValidate(request.password());
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese username.");
        }
        if (request.role() == RolUsuario.ADMIN && usuarioRepository.existsByRole(RolUsuario.ADMIN)) {
            throw new ReglaNegocioException("FamiCup permite un unico usuario ADMIN.");
        }

        Usuario user = new Usuario();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(EstadoUsuario.ACTIVE);
        user.setMustChangePassword(request.role() == RolUsuario.PLAYER);
        user.setPasswordChangedAt(request.role() == RolUsuario.PLAYER ? null : OffsetDateTime.now(ZoneOffset.UTC));
        Usuario saved = usuarioRepository.save(user);

        if (saved.getRole() == RolUsuario.PLAYER) {
            createRankingRow(saved);
            createGlobalPayment(saved, request.hasPaidGlobalRegistration());
        }

        auditService.record(
                null,
                "USER_CREATE",
                "USER",
                saved.getId().toString(),
                "Creo usuario " + saved.getUsername() + " con rol " + saved.getRole(),
                "Usuario creado");
        return usuarioMapper.toResponse(saved);
    }

    @Transactional
    public UsuarioResponse updateUser(UUID id, ActualizarUsuarioRequest request) {
        Usuario user = getById(id);
        if (request.username() != null && !request.username().isBlank()) {
            String username = request.username().trim().toLowerCase();
            usuarioRepository.findByUsernameIgnoreCase(username)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new ReglaNegocioException("Ya existe un usuario con ese username.");
                    });
            user.setUsername(username);
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        String password = PasswordPolicy.normalizeOptionalAndValidate(request.password());
        if (password != null) {
            user.setPasswordHash(passwordEncoder.encode(password));
            if (user.getRole() == RolUsuario.PLAYER) {
                user.setMustChangePassword(true);
                user.setPasswordChangedAt(null);
            } else {
                user.setMustChangePassword(false);
                user.setPasswordChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
            }
        }
        if (request.status() != null) {
            if (user.getRole() == RolUsuario.ADMIN && request.status() != EstadoUsuario.ACTIVE) {
                throw new ReglaNegocioException("El unico ADMIN no puede quedar inactivo.");
            }
            user.setStatus(request.status());
        }
        auditService.record(
                null,
                "USER_UPDATE",
                "USER",
                user.getId().toString(),
                "Actualizo usuario " + user.getUsername(),
                "Usuario actualizado");
        return usuarioMapper.toResponse(user);
    }

    @Transactional
    public UsuarioResponse changeCurrentUserPassword(Usuario user, CambiarPasswordRequest request) {
        String newPassword = PasswordPolicy.normalizeAndValidate(request.newPassword());

        Usuario managedUser = usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));
        managedUser.setPasswordHash(passwordEncoder.encode(newPassword));
        managedUser.setMustChangePassword(false);
        managedUser.setPasswordChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        auditService.record(
                managedUser,
                "PASSWORD_CHANGE",
                "USER",
                managedUser.getId().toString(),
                "Cambio de contraseña propia",
                "Contraseña actualizada");
        return usuarioMapper.toResponse(usuarioRepository.save(managedUser));
    }

    @Transactional
    public void deleteUser(UUID id) {
        Usuario user = getById(id);
        if (user.getRole() == RolUsuario.ADMIN && usuarioRepository.countByRole(RolUsuario.ADMIN) <= 1) {
            throw new ReglaNegocioException("No se puede eliminar el unico ADMIN.");
        }
        auditService.record(
                null,
                "USER_DELETE",
                "USER",
                user.getId().toString(),
                "Elimino usuario " + user.getUsername(),
                "Usuario eliminado");
        usuarioRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Usuario getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RecursoNoEncontradoException("Usuario autenticado no encontrado.");
        }
        return usuarioRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));
    }

    @Transactional(readOnly = true)
    public Usuario getById(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }

    private void createRankingRow(Usuario user) {
        PuntosRanking ranking = new PuntosRanking();
        ranking.setUser(user);
        ranking.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        rankingRepository.save(ranking);
    }

    private void createGlobalPayment(Usuario user, boolean paid) {
        Pago payment = new Pago();
        payment.setUser(user);
        payment.setSystem(SistemaPago.GLOBAL);
        payment.setAmountCop(parametersService.getParameters().globalRegistrationAmount());
        payment.setStatus(paid ? EstadoPago.PAID : EstadoPago.PENDING);
        payment.setPaymentMethod("Registro inicial");
        payment.setPaidAt(paid ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        pagoRepository.save(payment);
    }
}
