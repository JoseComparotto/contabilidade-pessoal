# Etapa 1: Build da aplicação
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia os arquivos do projeto
COPY . .

# Executa o build (gera target/*.jar)
RUN mvn clean package -DskipTests

# Etapa 2: Imagem final para rodar a aplicação
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o JAR da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta da API
EXPOSE 8080

# Comando de execução
ENTRYPOINT ["java", "-jar", "app.jar"]