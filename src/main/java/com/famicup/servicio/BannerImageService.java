package com.famicup.servicio;

import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.modelo.dto.ParametrosApuestasResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BannerImageService {

    private static final Pattern DRIVE_FILE_PATH_PATTERN = Pattern.compile("/file/d/([^/]+)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private final BettingParametersService parametersService;
    private final HttpClient httpClient;

    public BannerImageService(BettingParametersService parametersService) {
        this.parametersService = parametersService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public BannerImage loadCurrentBannerImage() {
        ParametrosApuestasResponse parameters = parametersService.getParameters();

        if (!parameters.interstitialBannerEnabled() || parameters.interstitialBannerImageUrl().isBlank()) {
            throw new RecursoNoEncontradoException("Banner no configurado.");
        }

        URI imageUri = resolveImageUri(parameters.interstitialBannerImageUrl());
        HttpRequest request = HttpRequest.newBuilder(imageUri)
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*,*/*;q=0.8")
                .header("User-Agent", "FamiCup/1.0")
                .GET()
                .build();

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo descargar la imagen del banner.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Se interrumpio la descarga de la imagen del banner.", exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El proveedor de la imagen respondio con estado " + response.statusCode() + ".");
        }

        byte[] body = response.body();
        if (body.length == 0 || body.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La imagen del banner esta vacia o supera el limite permitido.");
        }

        String contentType = response.headers().firstValue("content-type")
                .map(BannerImageService::normalizeContentType)
                .orElse("");

        if (!contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La URL configurada no respondio con una imagen valida.");
        }

        return new BannerImage(body, contentType);
    }

    private URI resolveImageUri(String value) {
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL de imagen del banner no es valida.", exception);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La URL de imagen del banner debe iniciar con http o https.");
        }

        String driveFileId = getGoogleDriveFileId(uri);
        if (driveFileId != null && !driveFileId.isBlank()) {
            return URI.create("https://drive.google.com/thumbnail?id="
                    + URLEncoder.encode(driveFileId, StandardCharsets.UTF_8)
                    + "&sz=w1600");
        }

        return uri;
    }

    private static String getGoogleDriveFileId(URI uri) {
        String host = uri.getHost();
        if (host == null || !host.toLowerCase(Locale.ROOT).endsWith("drive.google.com")) {
            return null;
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        Matcher filePathMatcher = DRIVE_FILE_PATH_PATTERN.matcher(path);
        if (filePathMatcher.find()) {
            return decode(filePathMatcher.group(1));
        }

        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        for (String parameter : rawQuery.split("&")) {
            int separatorIndex = parameter.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }

            String name = decode(parameter.substring(0, separatorIndex));
            if ("id".equals(name)) {
                return decode(parameter.substring(separatorIndex + 1));
            }
        }

        return null;
    }

    private static String normalizeContentType(String value) {
        return value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public record BannerImage(byte[] content, String contentType) {
    }
}
