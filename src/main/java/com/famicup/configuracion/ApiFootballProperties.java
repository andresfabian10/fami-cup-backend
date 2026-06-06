package com.famicup.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api.football")
public record ApiFootballProperties(
        String baseUrl,
        String apiKey,
        int timeoutSeconds,
        WorldCup worldCup,
        Colombia colombia) {

    public record WorldCup(Integer leagueId, Integer season) {
    }

    public record Colombia(Integer teamId) {
    }
}
