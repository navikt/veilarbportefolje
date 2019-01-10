FROM docker.adeo.no:5000/pus/maven as builder
ADD / /source
WORKDIR /source
RUN mvn package -DskipTests

FROM navikt/java:8-appdynamics
COPY --from=builder /source/target/veilarbportefolje /app
