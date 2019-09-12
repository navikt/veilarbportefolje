FROM navikt/java:8-appdynamics
ADD /target/veilarbportefolje /app
ENV APPD_ENABLED=true