FROM docker.pkg.github.com/navikt/pus-nais-java-app/pus-nais-java-app:java11
ADD jvmtop-0.8.0 /jvmtop
COPY /target/veilarbportefolje.jar app.jar