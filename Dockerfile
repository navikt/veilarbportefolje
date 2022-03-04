FROM  docker.pkg.github.com/navikt/pus-nais-java-app/pus-nais-java-app:java17

ENV APPD_ENABLED=true
ENV APP_NAME=veilarbfilter

COPY /target/veilarbportefolje.jar app.jar