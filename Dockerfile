FROM docker.pkg.github.com/navikt/pus-nais-java-app/pus-nais-java-app:java8
ADD jvmtop-0.8.0 /jvmtop
ADD /target/veilarbportefolje /app
