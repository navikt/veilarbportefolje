# Portefølje-serverside

Mikrotjeneste som aggregerer data fra andre tjenester og håndterer oppdatering av søkeindeks.

## For å bygge
`mvn clean install`

## Elasticsearch

Denne applikasjonen går mot et elasticsearch-cluster for indeksering av data om oppfølgingsbrukere.


### Integrasjonstester

For å kjøre integrasjonstester mot en lokal instans av elasticsearch må man sette elasticOnLocalhost=true i `ElasticServiceIntegrationTest.kt`
og kjøre `docker-compose up` i terminalen.

## URL-er å utføre full hovedindeksering, populering av indeks, oppdatering av ytelser og tiltak
```
http://<host>/veilarbportefolje/internal/totalhovedindeksering
http://<host>/veilarbportefolje/internal/populerindeks
http://<host>/veilarbportefolje/internal/oppdaterytelser
http://<host>/veilarbportefolje/internal/oppdatertiltak
http://<host>/veilarbportefolje/internal/populer_elastic

```

!! OBS OBS Om det er gjort endringer i schemaet til indeksen må man kanskje restarte indeksen !!

## Plugin til IntelliJ
Dette prosjektet benytter seg av [lombok](https://projectlombok.org).

Plugin for IntelliJ ligger på følgende path (testet med IntelliJ 2016.3):

## Sjekk at materialiserte views i databasen oppdateres (replikering)

Les i jobbtabellen til oracle for å undersøke statusen på den automatisk refreshingen av materialiserte views 

```
select * from all_scheduler_jobs;
```

## Kontakt og spørsmål
Opprett en issue i GitHub for eventuelle spørsmål.

Er du ansatt i NAV kan du stille spørsmål på Slack i kanalen #team-biff.
