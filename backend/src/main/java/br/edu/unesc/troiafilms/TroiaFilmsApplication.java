package br.edu.unesc.troiafilms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do TroiaFilms.
 *
 * <p>Nesta primeira etapa a aplicação não tem camada web: ao subir, ela
 * aplica as migrations do Flyway, valida o mapeamento das entidades contra
 * as tabelas criadas e encerra. É justamente esse encerramento limpo que
 * prova que o schema e o pacote {@code entity} estão de acordo.</p>
 */
@SpringBootApplication
public class TroiaFilmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TroiaFilmsApplication.class, args);
    }
}
