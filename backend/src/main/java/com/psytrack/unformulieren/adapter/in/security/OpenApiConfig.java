package com.psytrack.unformulieren.adapter.in.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI unformulierenOpenApi() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("PsyTrack API")
                        .version("1.0.0")
                        .description("""
                                Team psychological health monitoring API.

                                ## Authentication Flow

                                ### Employee
                                1. `POST /auth/register/employee` — Create an account providing the **team code**.
                                2. `POST /auth/login` — Authenticate and receive a JWT token.

                                ### Manager / Counselor
                                1. `POST /auth/register/staff` — Create an account with role `PENDING`.
                                2. `POST /auth/claim-role` — Claim your role (`MANAGER` or `COUNSELOR`) and receive a new JWT token with the definitive role.
                                3. `POST /auth/login` — Subsequent authentications.

                                ## Authorization
                                All endpoints (except `/auth/**`) require the header:
                                ```
                                Authorization: Bearer <token>
                                ```
                                """)
                        .contact(new Contact()
                                .name("PsyTrack"))
                        .license(new License()
                                .name("Internal use — TCC")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .tags(List.of(
                        new Tag().name("Authentication").description("Registration, login and role claiming"),
                        new Tag().name("Employees").description("Manage employees linked to teams"),
                        new Tag().name("Teams").description("Create and manage teams"),
                        new Tag().name("Forms").description("Psychological assessment forms"),
                        new Tag().name("Questions").description("Questions associated with forms"),
                        new Tag().name("Form Responses").description("Responses submitted by employees"),
                        new Tag().name("Employee Results").description("Individual psychological health results"),
                        new Tag().name("Team Results").description("Consolidated psychological health results per team")
                ));
    }
}
