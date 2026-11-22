# JumpLab_api

Back-end de sincronização e autenticação do JumpLab. Regras do projeto na constituição do
[JumpLab_documentacao](https://github.com/ramons02/JumpLab_documentacao) (`.specify/memory/constitution.md`).

## Stack

| Peça | Versão |
|---|---|
| Java | 25 (LTS) |
| Spring Boot | 4.2.0 |
| Build | Maven |
| Banco | PostgreSQL, acesso por `JdbcTemplate` com SQL escrito à mão (sem JPA) |
| Testes | JUnit 5, Mockito, Testcontainers (PostgreSQL real) |

Flyway, Spring Security e JWT entram com a spec `012-conta-e-login`.

## Estrutura

```text
src/main/java/br/com/jumplab/api/
├── JumpLabApiApplication.java
├── core/        # abstrações genéricas: controllers, domains, dtos, exceptions, helpers,
│                # repositories, services, validations
└── business/    # um pacote por módulo de negócio (controller, service, repository, model, dto)
```

O `core/` vem da skill `java-arquitetura`: cada módulo de `business/` herda dele
(`AbstractRepository`, `GenericService`, `GenericController`, `GenericValidation`) e só escreve o que é
do seu domínio. Exclusão é lógica (`ativo = false`), nunca `DELETE`.

## Configuração

Valores sensíveis vêm de variáveis de ambiente, sem valor padrão no `application.yml`:

| Variável | O que é |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL (ex.: `jdbc:postgresql://localhost:5432/jumplab`) |
| `SPRING_DATASOURCE_USERNAME` | usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | senha do banco |

Os valores reais ficam fora do Git (ver `JumpLab_infra`).

## Comandos

```bash
git config core.hooksPath .githooks   # uma vez por clone: liga o hook commit-msg
mvn verify                            # compila e roda testes de unidade e de integração
```

`mvn verify` precisa do JDK 25 e do Docker rodando (os testes `*IT` sobem um PostgreSQL em container).
