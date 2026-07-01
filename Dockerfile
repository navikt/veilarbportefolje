FROM gcr.io/distroless/java21-debian13
ENV TZ="Europe/Oslo"
COPY /target/veilarbportefolje.jar app.jar
CMD ["app.jar"]