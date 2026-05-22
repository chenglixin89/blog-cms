package com.blog.config;

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
 * Customises the OpenAPI document springdoc auto-generates from the controllers.
 *
 * <p>Two things are worth pointing out:</p>
 *
 * <ul>
 *   <li>The {@code BearerAuth} security scheme is declared as a global default,
 *       so every operation in the generated spec is documented as requiring
 *       a {@code Authorization: Bearer &lt;jwt&gt;} header. Public endpoints
 *       (e.g. {@code /api/admin/login}, {@code /api/front/**}) can override
 *       this with {@code @SecurityRequirements} on the controller method when
 *       we want the spec to be more accurate.</li>
 *   <li>This class only affects the generated documentation. It does not enable
 *       or enforce authentication at runtime; that is the job of
 *       {@code AdminAuthInterceptor} (see PR #1).</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI blogOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Admin JWT issued by POST /api/admin/login");

        return new OpenAPI()
            .info(new Info()
                .title("Blog CMS Backend API")
                .version("v1")
                .description("Personal blog CMS &mdash; admin and public-facing endpoints.")
                .contact(new Contact().name("blog-cms").url("https://github.com/chenglixin89/blog-cms"))
                .license(new License().name("MIT")))
            .components(new Components().addSecuritySchemes("BearerAuth", bearerScheme))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
