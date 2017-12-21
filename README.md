#Portefølje-serverside

Mikrotjeneste som aggregerer data fra andre tjenester og håndterer oppdatering av indeks.

## Avhengigheter til andre tjenester
- veilarbportefoljeindeks

## For å bygge
1. Gå til rotmappe
2. `mvn clean install`

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

```
F:\programvare\idea\plugin\lombok-plugin-0.14.16
```

## Sjekk at materialiserte views i databasen oppdateres (replikering)

Les i jobbtabellen til oracle for å undersøke statusen på den automatisk refreshingen av materialiserte views 

```
SELECT * from dba_jobs;
```