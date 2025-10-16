package com.book.bookhost.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Booking Houses Reservation API",
                version = "1.0",
                description = "Reservation API for managing bookings and blocks.",
                contact = @Contact(
                        name = "David Alves França",
                        url = "https://www.hostfully.com",
                        email = "dafranca1981@gmail.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local server")
        }
)

@Configuration
public class OpenApiConfig {
}