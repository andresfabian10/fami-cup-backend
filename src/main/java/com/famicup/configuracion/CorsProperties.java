package com.famicup.configuracion;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "famicup.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
