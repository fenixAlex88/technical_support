package by.baes.gatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashSet;
import java.util.Set;

@Configuration
@Slf4j
public class SwaggerConfig {

    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    public SwaggerConfig(DiscoveryClient discoveryClient, SwaggerUiConfigProperties swaggerUiConfigProperties) {
        this.discoveryClient = discoveryClient;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        configureSwaggerUi(); // Начальная конфигурация
    }

    private void configureSwaggerUi() {
        Set<SwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        discoveryClient.getServices().forEach(serviceId -> {
            if (!serviceId.equalsIgnoreCase("gateway-service") && !serviceId.equalsIgnoreCase("eureka-server")) {
                String url = "/v3/api-docs/" + serviceId;
                urls.add(new SwaggerUiConfigProperties.SwaggerUrl(
                        serviceId,
                        url,
                        serviceId + " API"
                ));
                log.info("Added Swagger URL for service: {} -> {}", serviceId, url);
            }
        });
        swaggerUiConfigProperties.setUrls(urls);
        swaggerUiConfigProperties.setTryItOutEnabled(true);
    }

    @Scheduled(fixedRate = 30000) // Обновляем каждые 30 секунд
    public void refreshSwaggerUi() {
        log.debug("Refreshing Swagger UI configuration");
        configureSwaggerUi();
    }
}