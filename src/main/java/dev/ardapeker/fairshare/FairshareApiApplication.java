package dev.ardapeker.fairshare;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "FairShare API",
        version = "1.0",
        description = "Split shared expenses in a group and settle up with as few payments as possible."))
public class FairshareApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FairshareApiApplication.class, args);
    }
}
