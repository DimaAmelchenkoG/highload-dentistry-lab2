package ru.itmo.dentistry.appointment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI appointmentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dentistry Appointment API")
                        .version("2.0")
                        .description("Reactive appointment service. Catalog data is loaded through Feign and a circuit breaker"))
                .addServersItem(new Server().url("/").description("API gateway"));
    }
}
