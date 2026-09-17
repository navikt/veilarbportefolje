# Migreringsguide for lagrede filter

## Beskrivelse

Hver gang vi endrer på eksisterende filter, må vi migrere dataene i databasen.
Dette er for å sikre at alle brukere får oppdaterte filtervalg, og at filterne ikke lengre kan hentes ut fordi de ikke
lengre stemmer med filtermodellen.

### Enkle migreringer

De mest vanlige og enkle migreringene:

* Endring av filternavn
* Endring av string/enum verdi (både for skalar og for en liste) for et gitt filter
* Sletting av en verdi i et gitt filter
* Sletting av et helt filter

For _enkle_ migreringer er det nok å gjøre endringer i koden, og så migrere i gcp ved hjelp av ferdig definerte
scripts. Kun filtre som ikke kan mappes blir fjernet når man henter alle lagra filter, så veilederne kan fortsatt bruke
de andre filterne sine som før. Skriptene finner
man [her](https://console.cloud.google.com/sql/instances/veilarbportefolje/studio?project=obo-dev-1713), under Queries.

### Komplekse migreringer

For mer _komplekse_ migreringer kan det være lurt å ha mappinger som et mellomsteg til man har migreret over til det nye
formatet.
I tillegg er det lurt å skru av at man kan endre eller opprette nye filtre, ved hjelp av
et [featureflagg](https://obo-unleash-web.iap.nav.cloud.nais.io/projects/default/features/veilarbportefolje.stopp_endring_og_oppretting_av_lagra_filter).
Flagget gjør at frontenden viser en beskjed om teknisk vedlikehold når man henter ut alle lagra filtre, pluss
feilmeldinger ved forsøk på å opprette eller endre filtre. Dette er for å sikre at ingen brukere endrer på filterne mens
migreringen pågår.

Her er noen tips for mapping når man skal gjøre mer omfattende endringer i filterne, og kan være man trenger en
kombinasjon av flere av de:

#### Endringer i navn/enums

* For endringer i **key/filternavn**, må man legge til @JsonAlias("gameltNavn") over filternavnet i AktiveFiltervalg.
* For endringer i **verdier/enums** må man legge til @JsonAlias("GAMMEL_VERDI") eller over enumen i data classen.
* Legge til en test som sjekker at alt blir riktig.

#### Sletting av verdier / hele filtre

Om mulig, oppdater databasen først, så fjern filteret eller enumen fra koden. Da slipper man å lage mapping og unngår at
noen filtre feiler mens man holder på. Om man allikevel trenger mapping som et mellomsteg, er dette fullt mulig, men
litt styrete (se [mapping-guide](#mapping-guide)).

#### Splitting av filtre

Om et filter skal splittes opp, og noen av verdiene skal flyttes over (dette ble gjort for ytelses-filterne i Arena).

1. Lag et nytt filter i koden
2. I gcp, bruk scriptet: Filter: splitt filter.

#### Andre komplekse ting

Det er ingen ferdig-definerte gcp scripts for dette, men man kan bruke
de eksisterende som inspirasjon og lage en custom variant for å oppnå det man ønsker.

### Mapping-guide

1. I AktiveFiltervalg, endre classen til å kunne deserializere "gamle" filtre i databasen.
   Det kan f eks være å endre typen fra en enum til en string (eller list<String>) så den kan deserialiseres.
2. Lag en mapper-funksjon som tar inn filteret i rekonstruerFiltervalgFraAktive.
   Lag custom logikk for det du ønsker å endre.
3. ekstraherAktiveFiltervalg -> mapper om nødvendig så riktig verdi blir sendt inn til AktiveFiltervalg
4. Lag tester for å verifiser at alt blir riktig.
5. RYDD OPP etter migreringen er ferdig.

## GCP-script for migrering (siste oppdatert 17.09.2026)

For å ha en backup av migreringsskriptene så legges de inn i dette repoet, men de skal kjøres i gcp.

1. [Endring av filternavn](#endring-av-filternavn)
2. [Endring av string/enum verdi for skalar](#endring-av-stringenum-verdi-for-skalar)
3. [Endring av string/enum verdi for en liste](#endring-av-stringenum-verdi-for-en-liste)
4. [Sletting av en verdi i et gitt filter](#sletting-av-en-verdi-i-et-gitt-filter)
5. [Sletting av et helt filter](#sletting-av-et-helt-filter)
6. [Splitting av et filter til flere filtre](#splitting-av-et-filter-til-flere-filtre)

### Endring av filternavn

### Endring av string/enum verdi for skalar

### Endring av string/enum verdi for en liste

### Sletting av en verdi i et gitt filter

### Sletting av et helt filter

### Splitting av et filter til flere filtre
