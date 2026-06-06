package com.famicup.configuracion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI famiCupOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FamiCup API")
                        .version("0.1.0")
                        .description("API backend para autenticacion, apuestas Colombia Especial, Polla Global, pagos, ranking y sincronizacion controlada con API-Football."))
                .components(new Components().addSecuritySchemes(
                        "bearer-jwt",
                        new SecurityScheme()
                                .name("bearer-jwt")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
