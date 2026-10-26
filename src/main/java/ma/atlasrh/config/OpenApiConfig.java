package ma.atlasrh.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI atlasRHOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AtlasRH API")
                        .description("API REST de gestion des ressources humaines — Livrable 1 : "
                                + "Gestion des départements, employés et contrats")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AtlasRH")
                                .email("contact@atlasrh.ma")));
    }
}
