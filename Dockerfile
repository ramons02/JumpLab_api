# Compila sem rodar os testes (eles rodam no mvn verify, antes do PR) e executa só com o JRE.
FROM maven:3.9-eclipse-temurin-25 AS compilacao
WORKDIR /fonte
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
RUN groupadd --system jumplab && useradd --system --gid jumplab --no-create-home jumplab
WORKDIR /app
COPY --from=compilacao /fonte/target/jumplab-api-*.jar app.jar
USER jumplab
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
