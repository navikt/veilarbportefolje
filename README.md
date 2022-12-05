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

Settings for index er definert i filen:
```
src/main/resources/opensearch_settings.json
```

### Fremgangsmåte ved endringer i OpenSearch settings
Når det gjøres endringer i `src/main/resources/opensearch_settings.json`, så er oppsettet slik at det må lages en ny
indeks. Dvs. man oppdaterer ikke den eksisterende indeksen. Den nye indeksen må populeres med data før den kan erstatte
den gamle.

#### Fremgangsmåte
1. Gjør endringer i `src/main/resources/opensearch_settings.json`, commit, push og deploy
2. Gå til pto-admin i riktig miljø (dev/prod) og velg "Veilarbportefolje" i dropdown
3. Utfør en "Hovedindeksering". Bruk referansen i response til å følge med i loggene. Denne jobben gjør oppdatering av
alle brukere i eksisterende indeks. Formålet er å se hvor lang tid det tar å indeksere alle brukerne.
4. Når man oppretter ny indeks (neste steg), så vil ikke endringer som kommer underveis
oppdatere den gamle indeksen som fortsatt er i bruk. Endringene vil først bli synlige i Oversikten når den nye indeksen
er ferdig indeksert, og den gamle indeksen er slettet. Se derfor hvor lang tid indekseringen i steg 3 tok, og vurder
tidspunktet på dagen neste steg bør gjøres. Normalt sett tar indekseringen 10-15min. Dersom det tar mye lenger tid
bør det undersøkes om nye endringer har ført til dette, f.eks. manglende databaseindeks.
5. Utfør "Hovedindeksering: Nytt alias", som oppretter ny indeks og indekserer alle brukere på den. Samtidig blir
gjeldende indeks satt til read-only. Når indeksering er ferdig tas den nye indeksen i bruk og den gamle slettes.
Bruk referansen i response til å følge med i loggene. Dersom jobben feiler, så skal den nye indeksen bli slettet, og
den gamle brukes videre (read-only modus fjernes). Skulle jobben feile, så bør man kjøre en vanlig hovedindeksering
igjen (steg 3), siden endringer som kom inn mens jobben kjørte, før den feilet, kun blir skrivet til den nye indeksen.

## Sjekk at databaselink fra arena oppdateres

Les i jobbtabellen til oracle for å undersøke statusen på den automatisk oppdateringen databaselinken til arena

```
select * from dba_scheduler_jobs;
```


## PostgreSQL

Innloggingsinformasjon til databasen:
https://vault.adeo.no/
* Dev: `vault read postgresql/preprod-fss/creds/veilarbportefolje-dev-admin`
* Prod: `vault read postgresql/prod-fss/creds/veilarbportefolje-prod-readonly`

## Plugin til IntelliJ

Dette prosjektet benytter seg av [lombok](https://projectlombok.org).

Plugin kan lastes ned her: https://plugins.jetbrains.com/plugin/6317-lombok

## Kontakt og spørsmål

Opprett en issue i GitHub for eventuelle spørsmål.

Er du ansatt i NAV kan du stille spørsmål på Slack i kanalen #produktområdet_arbeidsoppfølging.

