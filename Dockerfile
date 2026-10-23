# syntax=docker/dockerfile:1

# ---------- Estágio 1: build ----------
# Imagem com o JDK completo (compilador + Maven Wrapper). Ela só existe durante o build
# e não vai para a imagem final.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copia primeiro só o que define as dependências. Enquanto o pom.xml não mudar,
# o Docker reaproveita esta camada e não baixa tudo de novo a cada build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Agora o código-fonte. Os testes não rodam aqui porque precisam do Postgres e do Redis.
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests && cp target/shortener-*.jar app.jar

# ---------- Estágio 2: execução ----------
# Só o JRE (sem compilador): imagem menor e com menos coisas que possam ter falhas de segurança.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Roda como um usuário comum, e não como root.
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/app.jar app.jar
USER app

# Memória da JVM pensada para um contêiner de 512 MB:
# - MaxRAMPercentage=50: o heap usa no máximo metade da RAM do contêiner (~256 MB);
#   o resto fica para metaspace, threads, code cache e buffers de rede.
# - MaxMetaspaceSize / ReservedCodeCacheSize / MaxDirectMemorySize: põem teto nas áreas fora do heap.
# - Xss512k: pilha menor por thread (o padrão é 1 MB).
# - UseSerialGC: o coletor de lixo mais econômico em memória, bom para 1 CPU.
# - TieredStopAtLevel=1: compila só com o compilador rápido (C1): menos memória e inicialização mais rápida.
# JAVA_TOOL_OPTIONS é lida pela JVM automaticamente e pode ser sobrescrita no painel do Render.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=48m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Só documenta a porta padrão; no Render, a aplicação usa a variável PORT.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
