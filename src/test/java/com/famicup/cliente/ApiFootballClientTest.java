package com.famicup.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.famicup.configuracion.ApiFootballProperties;
import com.famicup.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ApiFootballClientTest {

    @Test
    void callsFixturesEndpointWithApiKeyHeader() {
        ApiFootballProperties properties = properties("test-key");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-apisports-key", properties.apiKey());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ApiFootballClient client = new ApiFootballClient(builder.build(), properties);
        server.expect(requestTo("https://v3.football.api-sports.io/fixtures?league=1&season=2026"))
                .andExpect(header("x-apisports-key", "test-key"))
                .andRespond(withSuccess("{\"response\":[]}", MediaType.APPLICATION_JSON));

        var response = client.fixturesByLeagueAndSeason();

        assertThat(response.path("response").isArray()).isTrue();
        server.verify();
    }

    @Test
    void rejectsCallsWithoutApiKey() {
        ApiFootballClient client = new ApiFootballClient(RestClient.builder().build(), properties(""));

        assertThatThrownBy(client::fixturesByLeagueAndSeason)
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("API_FOOTBALL_KEY");
    }

    private ApiFootballProperties properties(String apiKey) {
        return new ApiFootballProperties(
                "https://v3.football.api-sports.io",
                apiKey,
                10,
                new ApiFootballProperties.WorldCup(1, 2026),
                new ApiFootballProperties.Colombia(123));
    }
}
