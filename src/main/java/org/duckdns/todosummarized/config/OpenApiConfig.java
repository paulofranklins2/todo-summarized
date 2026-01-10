package org.duckdns.todosummarized.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation with JWT security.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String API_TITLE = "Todo Insight API";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION =
            "A backend-first Todo API that stores daily todos, exposes a clean REST API, " +
                    "and generates end-of-day summaries using deterministic metrics plus optional AI narratives. " +
                    "Authentication is handled via JWT tokens.";

    private static final String CONTACT_NAME = "Todo Insight";
    private static final String CONTACT_URL = "https://todo-insight.duckdns.org";

    private static final String LICENSE_NAME = "MIT License";
    private static final String LICENSE_URL = "https://opensource.org/licenses/MIT";

    private static final String SECURITY_DESCRIPTION =
            "JWT Bearer Token Authentication. Obtain token from /api/auth/signin or /api/auth/signup endpoint.";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(bearerSecurityRequirement())
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title(API_TITLE)
                .version(API_VERSION)
                .description(API_DESCRIPTION)
                .contact(new Contact()
                        .name(CONTACT_NAME)
                        .url(CONTACT_URL))
                .license(new License()
                        .name(LICENSE_NAME)
                        .url(LICENSE_URL));
    }

    private SecurityRequirement bearerSecurityRequirement() {
        return new SecurityRequirement().addList(BEARER_AUTH);
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(SECURITY_DESCRIPTION);
    }
}
