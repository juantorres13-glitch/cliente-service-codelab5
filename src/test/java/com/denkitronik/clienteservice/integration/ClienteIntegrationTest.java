
package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClienteIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String url;

    private static Long clienteId;

    @BeforeEach
    void configurar() {
        url = "http://localhost:" + port
                + "/api/v1/cliente-service/clientes";

        // Verificar que exista la región necesaria para crear clientes.
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM regiones WHERE id = 1",
                Integer.class
        );

        // Crear la región si no existe.
        if (cantidad == null || cantidad == 0) {
            jdbcTemplate.update(
                    "INSERT INTO regiones (id, nombre) VALUES (?, ?)",
                    1L,
                    "Suramerica"
            );
        }
    }

    @Test
    @Order(1)
    void crearCliente_correoInvalido_debeRetornar400() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNombre("Juan");
        cliente.setApellido("Torres");
        cliente.setEmail("correo-invalido");

        ResponseEntity<String> respuesta =
                restTemplate.postForEntity(
                        url,
                        cliente,
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());

        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());

        assertTrue(cuerpo.has("errors"));
        assertTrue(cuerpo.get("errors").isArray());
    }

    @Test
    @Order(2)
    void crearCliente_nombreVacio_debeRetornar400() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNombre("");
        cliente.setApellido("Torres");
        cliente.setEmail("juan.vacio@example.com");

        ResponseEntity<String> respuesta =
                restTemplate.postForEntity(
                        url,
                        cliente,
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());

        JsonNode cuerpo = objectMapper.readTree(respuesta.getBody());

        assertTrue(cuerpo.has("errors"));
        assertTrue(cuerpo.get("errors").isArray());
    }

    @Test
    @Order(3)
    void crearCliente_debeRetornar201() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Juan");
        cliente.setApellido("Torres");
        cliente.setEmail("juan.integration@example.com");

        Region region = new Region();
        region.setId(1L);
        cliente.setRegion(region);

        ResponseEntity<Cliente> respuesta =
                restTemplate.postForEntity(
                        url,
                        cliente,
                        Cliente.class
                );

        assertEquals(
                HttpStatus.CREATED,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());

        clienteId = respuesta.getBody().getId();

        assertNotNull(clienteId);
        assertEquals("Juan", respuesta.getBody().getNombre());
    }

    @Test
    @Order(4)
    void consultarClienteCreado_debeRetornar200() {
        assertNotNull(
                clienteId,
                "Primero debe crearse un cliente"
        );

        ResponseEntity<Cliente> respuesta =
                restTemplate.getForEntity(
                        url + "/" + clienteId,
                        Cliente.class
                );

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());
        assertEquals("Juan", respuesta.getBody().getNombre());
        assertEquals("Torres", respuesta.getBody().getApellido());
    }

    @Test
    @Order(5)
    void actualizarCliente_debeRetornar201() {
        assertNotNull(
                clienteId,
                "Primero debe crearse un cliente"
        );

        Cliente actualizado = new Cliente();
        actualizado.setNombre("Juan David");
        actualizado.setApellido("Torres");
        actualizado.setEmail("juan.integration@example.com");

        Region region = new Region();
        region.setId(1L);
        actualizado.setRegion(region);

        ResponseEntity<Cliente> respuesta =
                restTemplate.exchange(
                        url + "/" + clienteId,
                        HttpMethod.PUT,
                        new HttpEntity<>(actualizado),
                        Cliente.class
                );

        assertEquals(
                HttpStatus.CREATED,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());
        assertEquals("Juan David", respuesta.getBody().getNombre());
    }

    @Test
    @Order(6)
    void consultarClienteInexistente_debeRetornar404() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(
                        url + "/99999",
                        String.class
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                respuesta.getStatusCode()
        );
    }

    @Test
    @Order(7)
    void eliminarCliente_debeRetornar204_yLuego404() {
        assertNotNull(
                clienteId,
                "Primero debe crearse un cliente"
        );

        ResponseEntity<Void> respuestaEliminar =
                restTemplate.exchange(
                        url + "/" + clienteId,
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Void.class
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                respuestaEliminar.getStatusCode()
        );

        ResponseEntity<String> respuestaConsultar =
                restTemplate.getForEntity(
                        url + "/" + clienteId,
                        String.class
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                respuestaConsultar.getStatusCode()
        );
    }
}
