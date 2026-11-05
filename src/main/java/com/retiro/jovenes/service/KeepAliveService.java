package com.retiro.jovenes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Servicio Actuador Keep-Alive para Render.
 * Render suspende servicios gratuitos tras 15 minutos sin peticiones HTTP entrantes.
 * Este actuador realiza una petición externa a la propia URL pública de la aplicación
 * cada 10 minutos para resetear el contador de inactividad del balanceador de Render.
 */
@Service
public class KeepAliveService {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveService.class);

    // Variable estándar inyectada automáticamente por Render en tiempo de ejecución
    @Value("${RENDER_EXTERNAL_URL:#{null}}")
    private String renderExternalUrl;

    // URL configurable opcional (por si se especifica manualmente en Render o properties)
    @Value("${app.keep-alive.url:#{null}}")
    private String customKeepAliveUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Se ejecuta 2 minutos después del arranque y luego cada 10 minutos (600,000 ms)
    @Scheduled(initialDelay = 120000, fixedRate = 600000)
    public void pingSelf() {
        String targetBaseUrl = (customKeepAliveUrl != null && !customKeepAliveUrl.isBlank())
                ? customKeepAliveUrl
                : renderExternalUrl;

        if (targetBaseUrl == null || targetBaseUrl.isBlank()) {
            // En entorno local no hay URL de Render, no es necesario hacer ping
            return;
        }

        try {
            String pingUrl = targetBaseUrl.replaceAll("/+$", "") + "/api/ping";
            log.info("Enviando auto-ping keep-alive a Render: {}", pingUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pingUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Spring-Boot-Render-Actuator/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Auto-ping Render completado con éxito (status: {})", response.statusCode());
        } catch (Exception e) {
            log.warn("No se pudo completar el auto-ping en Render: {}", e.getMessage());
        }
    }
}
