package com.plantarsas.gestiondocumental;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled(
        "Requiere un perfil de integración aislado con PostgreSQL y propiedades JWT de prueba"
)
public class GestionDocumentalApplicationTests {

    @Test
    void contextLoads() {
    }

}
