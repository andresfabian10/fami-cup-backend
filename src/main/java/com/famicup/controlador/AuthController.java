package com.famicup.controlador;

import com.famicup.modelo.dto.AuthResponse;
import com.famicup.modelo.dto.LoginRequest;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.modelo.dto.RefreshTokenRequest;
import com.famicup.modelo.dto.UsuarioResponse;
import com.famicup.servicio.AuthService;
import com.famicup.servicio.BannerImageService;
import com.famicup.servicio.BettingParametersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Autenticacion con JWT, refresh token persistido y cierre de sesion.")
public class AuthController {

    private final AuthService authService;
    private final BettingParametersService parametersService;
    private final BannerImageService bannerImageService;

    public AuthController(AuthService authService, BettingParametersService parametersService, BannerImageService bannerImageService) {
        this.authService = authService;
        this.parametersService = parametersService;
        this.bannerImageService = bannerImageService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Valida usuario y contraseña, retorna access token JWT de vida corta y refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion iniciada."),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas."),
            @ApiResponse(responseCode = "400", description = "Payload invalido.")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar sesion", description = "Rota el refresh token activo si no ha expirado ni caducado por inactividad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nueva pareja de tokens generada."),
            @ApiResponse(responseCode = "400", description = "Refresh token invalido, expirado o inactivo.")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar sesion", description = "Revoca el refresh token enviado. El access token vence por su propia expiracion corta.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesion revocada."),
            @ApiResponse(responseCode = "400", description = "Payload invalido.")
    })
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @GetMapping("/public-parameters")
    @Operation(summary = "Parametros publicos", description = "Retorna parametros necesarios antes del inicio de sesion, como ayudas de acceso y banner inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parametros publicos disponibles.")
    })
    public ParametrosApuestasResponse publicParameters() {
        return parametersService.getParameters();
    }

    @GetMapping("/banner-image")
    @Operation(summary = "Imagen publica del banner", description = "Sirve la imagen configurada del banner inicial para evitar bloqueos de proveedores externos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen del banner disponible."),
            @ApiResponse(responseCode = "404", description = "Banner no configurado."),
            @ApiResponse(responseCode = "502", description = "No se pudo descargar la imagen externa.")
    })
    public ResponseEntity<byte[]> bannerImage() {
        BannerImageService.BannerImage image = bannerImageService.loadCurrentBannerImage();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario autenticado", description = "Retorna la sesion actual y rol a partir del Bearer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado."),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido.")
    })
    public UsuarioResponse me(Authentication authentication) {
        return authService.me(authentication);
    }
}
