package ru.itmo.dentistry.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI catalogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dentistry Catalog API")
                        .version("2.0")
                        .description("Reactive catalog of patients, doctors and dental procedures"))
                .addServersItem(new Server().url("/").description("API gateway"));
    }
}
