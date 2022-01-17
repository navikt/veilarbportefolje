![](https://github.com/navikt/veilarbportefolje/workflows/Build,%20push%20and%20deploy/badge.svg)

# Beskrivelse


Tjeneste som aggregerer data fra andre baksystemer og håndterer oppdatering av søkeindeks brukt i modia oversikten.

Følgende data aggregeres:

* Informasjon om brukere under oppfølging via databaselink til Arena (flyttet til veilarbindexer)
* Informasjon om løpende ytelser fra arena via Kafka
* Informasjon om brukeraktiviteter fra arena via Kafka
* Reservering mot digitale henvendelser i KRR via dkif (flyttet til veilarbindexer) (ikke fungerende)
* Veiledertilordninger fra `veilarboppfolging` via Kafka
* Dialoger fra `veilarbdialog` (aktivitetsplan) via Kafka
* Aktiviteter fra `veilarbaktivitet` (aktivitetsplan) via Kafka
* Informasjon om delt cv fra arbeidsplassen via Kafka
* Informasjon om endring av mål fra `veilarboppfolging` via Kafka

## Hvordan bygge

Kjør `mvn clean install`

## Oppsett av Opensearch

Opensearch er satt opp via "Aiven infrastructure as code"-repo (aiven-iac) administert av på NAIS:

https://github.com/navikt/aiven-iac

Mere info om:
https://aiven.io/opensearch

Innstillinger for index er definert i filen:

```
src/main/resources/opensearch_settings.json
```

## Sjekk at databaselink fra arena oppdateres

Les i jobbtabellen til oracle for å undersøke statusen på den automatisk oppdateringen databaselinken til arena

```
select * from dba_scheduler_jobs;
```


## PostgreSQL

Innloggingsinformasjon til databasen:

* Dev: `vault read postgresql/preprod-fss/creds/veilarbportefolje-dev-admin`
* Prod: `vault read postgresql/prod-fss/creds/veilarbportefolje-prod-readonly`

## Plugin til IntelliJ

Dette prosjektet benytter seg av [lombok](https://projectlombok.org).

Plugin kan lastes ned her: https://plugins.jetbrains.com/plugin/6317-lombok

## Kontakt og spørsmål

Opprett en issue i GitHub for eventuelle spørsmål.

Er du ansatt i NAV kan du stille spørsmål på Slack i kanalen #produktområdet_arbeidsoppfølging.
