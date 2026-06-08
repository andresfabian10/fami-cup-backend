package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BannerImageServiceTest {

    @Mock
    private BettingParametersService parametersService;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void downloadsConfiguredBannerImage() throws Exception {
        byte[] imageBytes = new byte[] {(byte) 0x89, 'P', 'N', 'G'};
        int port = startServer("/banner.png", "image/png", imageBytes, 200);
        when(parametersService.getParameters()).thenReturn(parameters("http://localhost:" + port + "/banner.png"));

        BannerImageService.BannerImage image = new BannerImageService(parametersService).loadCurrentBannerImage();

        assertThat(image.content()).containsExactly(imageBytes);
        assertThat(image.contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsResponsesThatAreNotImages() throws Exception {
        byte[] responseBody = "no es imagen".getBytes(StandardCharsets.UTF_8);
        int port = startServer("/banner", "text/html", responseBody, 200);
        when(parametersService.getParameters()).thenReturn(parameters("http://localhost:" + port + "/banner"));

        BannerImageService service = new BannerImageService(parametersService);

        assertThatThrownBy(service::loadCurrentBannerImage)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("imagen valida");
    }

    private int startServer(String path, String contentType, byte[] body, int status) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        return server.getAddress().getPort();
    }

    private ParametrosApuestasResponse parameters(String imageUrl) {
        return new ParametrosApuestasResponse(
                BigDecimal.valueOf(5000),
                3,
                BigDecimal.valueOf(20000),
                10,
                5,
                2,
                50,
                30,
                20,
                5,
                10,
                null,
                "573163353115",
                "Hola",
                "Acceso",
                "Recuperar",
                "Solicitar",
                true,
                imageUrl,
                "",
                "Banner",
                0);
    }
}
