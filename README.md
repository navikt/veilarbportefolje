[![CircleCI](https://circleci.com/gh/navikt/veilarbportefolje.svg?style=svg)](https://circleci.com/gh/navikt/veilarbportefolje)

# Portefølje-serverside

Mikrotjeneste som aggregerer data fra andre tjenester og håndterer oppdatering av søkeindeks.

## For å bygge
`mvn clean install`

## Elasticsearch

Denne applikasjonen går mot et elasticsearch-cluster for indeksering av data om oppfølgingsbrukere.

##### Oppsett av Postman
Importer denne filen i Postman:
```
elastic_postman.json
``` 
Dette gir deg en samling av endepunkter man kan bruke i testing og feilsøk av
Elasticsearch. 

Noen av disse requestene benytter seg av Postman-miljøvariabler, importer disse ved å trykke på tannhjulet øverst til
høyre i Postman og velg `Import`. Importer disse filene:
```
preprod.postman_environment
prod.postman_environment
``` 

Disse environment-ene inneholder en auth-variabel. For å sette denne må du hente oidc-token i Fasit. Last ned filen du finner under enten `veilarbsecret_preprod` eller
`veilarbsecret_prod` og bruk tokenet som ligger i følgende del av fila:

```
stringData:
    client_pwd: <token>
```

##### Integrasjonstester
Integrasjonstestene vil kjøre mot preprod i utviklerimage og på byggserver. Om man vil kjøre de på egen laptop kjør:
```.env
docker-compose up
```
##### Oppsett av Elastic
Elastic er satt opp via et "Infrastructure as code"-repo (IAC) for tredjepartsapplikasjoner på NAIS:

https://github.com/navikt/nais-tpa/

Selve clusteret er basert på følgende koderepo:

https://www.github.com/navikt/pam-elasticsearch

Innstillinger i clusteret er definert i filen:

```
elastic_settings.json
```


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
