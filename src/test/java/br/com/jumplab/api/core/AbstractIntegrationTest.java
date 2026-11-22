package br.com.jumplab.api.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integracao (*IT). Um unico PostgreSQL real em container serve a suite inteira:
 * o contexto do Spring fica em cache entre as classes de teste, entao o container nao pode parar ao fim
 * de cada classe. O @ServiceConnection liga o datasource a ele e o Flyway roda as migracoes reais.
 * O RestTestClient ja aponta para a porta aleatoria do servidor: basta passar o caminho ("/api/...").
 */
@AutoConfigureRestTestClient
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "jumplab.jwt.segredo=" + AbstractIntegrationTest.SEGREDO_JWT_DE_TESTE)
public abstract class AbstractIntegrationTest {

    /** Segredo so para os testes; o real vem de JUMPLAB_JWT_SEGREDO no servidor. */
    protected static final String SEGREDO_JWT_DE_TESTE = "segredo-de-teste-com-mais-de-32-bytes-ok";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
    }

    @Autowired
    protected RestTestClient restClient;

    @Autowired
    protected JdbcTemplate jdbc;
}
