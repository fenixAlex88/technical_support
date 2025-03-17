package by.baes.gatewayservice.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class SwaggerConfig {

    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    public SwaggerConfig(DiscoveryClient discoveryClient, SwaggerUiConfigProperties swaggerUiConfigProperties) {
        this.discoveryClient = discoveryClient;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        configureSwaggerUi(); // Вызываем конфигурацию при создании объекта
    }

    private void configureSwaggerUi() {
        Set<SwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        discoveryClient.getServices().forEach(serviceId -> {
            if (!serviceId.equals("gateway-service") && !serviceId.equals("eureka-server")) {
                urls.add(new SwaggerUiConfigProperties.SwaggerUrl(
                        serviceId,
                        "/v3/api-docs/" + serviceId,
                        serviceId + " API"
                ));
            }
        });
        swaggerUiConfigProperties.setUrls(urls);
    }
}