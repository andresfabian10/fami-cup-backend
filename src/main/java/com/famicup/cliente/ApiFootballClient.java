package com.famicup.cliente;

import com.fasterxml.jackson.databind.JsonNode;
import com.famicup.configuracion.ApiFootballProperties;
import com.famicup.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class ApiFootballClient {

    private final RestClient apiFootballRestClient;
    private final ApiFootballProperties properties;

    public ApiFootballClient(RestClient apiFootballRestClient, ApiFootballProperties properties) {
        this.apiFootballRestClient = apiFootballRestClient;
        this.properties = properties;
    }

    public JsonNode fixturesByLeagueAndSeason() {
        ensureApiKey();
        return apiFootballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", properties.worldCup().leagueId())
                        .queryParam("season", properties.worldCup().season())
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode fixtureById(Long fixtureId) {
        ensureApiKey();
        return apiFootballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("id", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    private void ensureApiKey() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new ReglaNegocioException("API_FOOTBALL_KEY no esta configurada.");
        }
    }
}
