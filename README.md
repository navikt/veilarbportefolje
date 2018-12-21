# Portefølje-serverside

Mikrotjeneste som aggregerer data fra andre tjenester og håndterer oppdatering av indeks.

## Kjør Elastic Search lokalt
```
1. docker network create esnet
2. docker run -d --name elasticsearch --net esnet -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:6.5.2
```

## For å bygge
`mvn clean install`

## URL-er å utføre full hovedindeksering, populering av indeks, oppdatering av ytelser og tiltak
```
http://<host>/veilarbportefolje/internal/totalhovedindeksering
http://<host>/veilarbportefolje/internal/populerindeks
http://<host>/veilarbportefolje/internal/oppdaterytelser
http://<host>/veilarbportefolje/internal/oppdatertiltak

```

!! OBS OBS Om det er gjort endringer i schemaet til indeksen må man kanskje restarte indeksen !!

## Plugin til IntelliJ
Dette prosjektet benytter seg av [lombok](https://projectlombok.org).

Plugin for IntelliJ ligger på følgende path (testet med IntelliJ 2016.3):

## Sjekk at materialiserte views i databasen oppdateres (replikering)

Les i jobbtabellen til oracle for å undersøke statusen på den automatisk refreshingen av materialiserte views 

```
SELECT * from dba_jobs;
```

## Kontakt og spørsmål
Opprett en issue i GitHub for eventuelle spørsmål.

Er du ansatt i NAV kan du stille spørsmål på Slack i kanalen #teamoppfølging.
