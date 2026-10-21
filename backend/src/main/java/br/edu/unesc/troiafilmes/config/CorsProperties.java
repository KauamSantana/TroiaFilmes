package br.edu.unesc.troiafilmes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Origens autorizadas a consumir a API (o frontend React, em desenvolvimento). */
@Getter
@Setter
@ConfigurationProperties(prefix = "troiafilmes.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:5173");
}
