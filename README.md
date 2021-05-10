![](https://github.com/navikt/veilarbportefolje/workflows/Build,%20push%20and%20deploy/badge.svg)

# Beskrivelse

Tjeneste som aggregerer data fra andre baksystemer og håndterer oppdatering av søkeindeks brukt i modia oversikten.

Følgende data aggregeres:
* Informasjon om brukere under oppfølging via databaselink til Arena (flyttet til veilarbindexer)
* Informasjon om løpende ytelser fra arena via sftp (flyttet til veilarbindexer)
* Informasjon om brukeraktiviteter fra arena via sftp (flyttet til veilarbindexer)
* Reservering mot digitale henvendelser i KRR via dkif (flyttet til veilarbindexer)
* Veiledertilordninger fra `veilarboppfolging` via Kafka
* Dialoger fra `veilarbdialog` (aktivitetsplan) via Kafka
* Aktiviteter fra `veilarbaktivitet` (aktivitetsplan) via Kafka
* Informasjon om delt cv fra arbeidsplassen via Kafka
* Informasjon om endring av mål fra `veilarboppfolging` via Kafka

## Hvordan bygge

Kjør `mvn clean install`

## Oppsett av Postman
Importer denne filen i Postman:
```
postman/config.json
``` 
Dette gir deg en samling av endepunkter man kan bruke i testing og feilsøk av
Elasticsearch. 

Noen av disse requestene benytter seg av Postman-miljøvariabler, importer disse ved å trykke på tannhjulet øverst til
høyre i Postman og velg `Import`. Importer disse filene: 
```
postman/preprod_environment
postman/prod_environment
``` 

Disse environment-ene inneholder en auth-variabel. For å sette denne må du hente oidc-token i Fasit. Last ned filen du finner under enten `veilarbsecret_preprod` eller
`veilarbsecret_prod` og bruk tokenet som ligger i følgende del av fila:


```
stringData:
    client_pwd: <token>
```

## Oppsett av Elasticsearch
Elastic er satt opp via et "Infrastructure as code"-repo (IAC) for tredjepartsapplikasjoner på NAIS:

https://github.com/navikt/nais-tpa/

Selve clusteret er basert på følgende koderepo:

https://www.github.com/navikt/pam-elasticsearch

Innstillinger i clusteret er definert i filen:

```
src/main/resources/elastic_settings.json
```

## Sjekk at databaselink fra arena oppdateres

Les i jobbtabellen til oracle for å undersøke statusen på den automatisk oppdateringen databaselinken til arena 
```
select * from dba_scheduler_jobs;
```

## PostgreSQL
Innloggingsinformasjon til databasen:  
Dev: `vault read postgresql/preprod-fss/creds/veilarbportefolje-dev-admin` 
Prod: `vault read postgresql/preprod-fss/creds/veilarbportefolje-prod-admin` 

## Plugin til IntelliJ
Dette prosjektet benytter seg av [lombok](https://projectlombok.org).

Plugin kan lastes ned her: https://plugins.jetbrains.com/plugin/6317-lombok

## Kontakt og spørsmål
Opprett en issue i GitHub for eventuelle spørsmål.


Er du ansatt i NAV kan du stille spørsmål på Slack i kanalen #pto.
