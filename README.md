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
| Migrações | Flyway (`src/main/resources/db/migration/`, `V<HU>_<n>__descricao.sql`) |
| Segurança | Spring Security com JWT HS256 (resource server do próprio Spring), senhas em BCrypt |
| Testes | JUnit 5, Mockito, Testcontainers (PostgreSQL real) |

## Endpoints

| Rota | Acesso |
|---|---|
| `POST /api/auth/cadastro` | pública; cria a conta e já devolve a sessão |
| `POST /api/auth/entrar` | pública |
| `POST /api/auth/renovar` | pública; troca o token de renovação por um par novo |
| `POST /api/auth/sair` | pública; revoga o token de renovação |
| `POST /api/sync/enviar` | protegida; grava até 100 pacientes e avaliações do aparelho (vence a alteração mais recente; até 2 MB) |
| `GET /api/sync/receber?desde=&limite=` | protegida; devolve os registros da conta mudados depois do cursor, em ordem de versão |
| qualquer outra | `Authorization: Bearer <token de acesso>` |

Contrato completo em `specs/012-conta-e-login/contracts/api.md` do `JumpLab_documentacao`. Toda resposta,
inclusive de erro, usa o envelope `ApiResponse` (`sucesso`, `mensagem`, `dados`, `erros`).

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
| `JUMPLAB_JWT_SEGREDO` | segredo que assina os tokens; pelo menos 32 bytes, senão a API não sobe |
| `JUMPLAB_JWT_ACESSO_MINUTOS` | validade do token de acesso (padrão 15) |
| `JUMPLAB_JWT_RENOVACAO_DIAS` | validade do token de renovação (padrão 30) |

Os valores reais ficam fora do Git (ver `JumpLab_infra`).

## Comandos

```bash
git config core.hooksPath .githooks   # uma vez por clone: liga o hook commit-msg
mvn verify                            # compila e roda testes de unidade e de integração
```

`mvn verify` precisa do JDK 25 e do Docker rodando (os testes `*IT` sobem um PostgreSQL em container,
um só para a suíte inteira, e o Flyway roda as migrações reais nele).

Para subir a API com o banco, use o `docker-compose.yml` do `JumpLab_infra` (ele constrói a imagem pelo
`Dockerfile` daqui).
