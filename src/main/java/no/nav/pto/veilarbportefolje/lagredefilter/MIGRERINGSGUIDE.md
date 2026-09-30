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

## GCP-script for migrering (sist oppdatert 17.09.2026)

For å ha en backup av migreringsskriptene så legges de inn i dette repoet, men de skal kjøres i gcp.

1. [Endring av filternavn](#endring-av-filternavn)
2. [Endring av string/enum verdi for skalar](#endring-av-stringenum-verdi-for-skalar)
3. [Endring av string/enum verdi for en liste](#endring-av-stringenum-verdi-for-en-liste)
4. [Sletting av en verdi i et gitt filter](#sletting-av-en-verdi-i-et-gitt-filter)
5. [Sletting av et helt filter](#sletting-av-et-helt-filter)
6. [Splitting av et filter til flere filtre](#splitting-av-et-filter-til-flere-filtre)

### Endring av filternavn

```
-- OPPDATER navn på en NØKKEL

BEGIN;

-- ==========================================
-- CONFIGURATION
-- ==========================================
SET custom.old_key = 'old_key'; -- gammelt nøkkelnavn
SET custom.new_key = 'new_key'; -- nytt nøkkelnavn
-- ==========================================

-- Result 1) Antall filtre med gammel KEY før oppdatering
SELECT current_setting('custom.old_key') AS old_key, count(*) AS antall_med_old_key
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.old_key');

-- Start update transaction using CTE
WITH before_state AS (
-- Hent rader slik de ser ut før oppdatering
SELECT
filter_id,
aktive_filter_valg AS before_val
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.old_key')
),
updated_rows AS (
-- Kjør oppdatering der vi fjerner gammel nøkkel og legger til ny nøkkel med samme verdi
UPDATE lagrede_filter_mine_filter
SET aktive_filter_valg = (aktive_filter_valg - current_setting('custom.old_key')) ||
jsonb_build_object(current_setting('custom.new_key'), aktive_filter_valg -> current_setting('custom.old_key'))
WHERE aktive_filter_valg ? current_setting('custom.old_key')
RETURNING
filter_id,
aktive_filter_valg AS after_val
)
-- Result 2. Sjekk at nøkkelnavnet er oppdatert og at innholdet er 100% identisk som før
SELECT
b.filter_id,
current_setting('custom.old_key') AS old_key,
current_setting('custom.new_key') AS new_key,
b.before_val -> current_setting('custom.old_key') AS value_under_old_key,
u.after_val -> current_setting('custom.new_key') AS value_under_new_key,
b.before_val AS before_full_json,
u.after_val AS after_full_json
FROM before_state b
JOIN updated_rows u ON b.filter_id = u.filter_id
ORDER BY b.filter_id;

-- Result 3. SANITY CHECK: Sjekk at antall filtre som inneholder gammel key nå er null
SELECT count(*) AS antall_med_gammel_key
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.old_key');

-- Result 4. SANITY CHECK: Verifiser at INGEN andre rader i tabellen har blitt uventet endret
SELECT COUNT(*) AS unexpected_modified_rows
FROM lagrede_filter_mine_filter
WHERE xmin::text = (txid_current()::text) -- Endret i denne aktive transaksjonen
-- Ekskluder rader vi forventet å endre (de som nå inneholder den nye key-en):
AND NOT (aktive_filter_valg ? current_setting('custom.new_key'));

-- Kjør COMMIT om alt ser riktig ut (NB! hele skriptet må kjøres på nytt med commit for at det skal fungere i gcp).
-- COMMIT;
```

### Endring av string/enum verdi for skalar

```
-- OPPDATER en STRING value

BEGIN;

-- ==========================================
-- CONFIGURATION
-- ==========================================
SET custom.target_key = 'key'; -- nøkkelnavn
SET custom.old_val = 'old_value'; -- Gammel verdi
SET custom.new_val = 'new_value'; -- Ny verdi
-- ==========================================

-- Result 1) Verifiser at typen er en string (og ikke map, array e.l.)
SELECT
COALESCE(jsonb_typeof(aktive_filter_valg -> current_setting('custom.target_key'))) AS value_type,
COUNT(*) AS row_count
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
GROUP BY 1;

-- Se på dataene først, og få oversikt over antallet og hvor mange som har verdien som skal endres
-- Result 2) Antall filtre med KEY (uansett verdi)
SELECT current_setting('custom.target_key') AS key, count(*) AS antall_med_key
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key');

-- Result 3) Antall filtre der KEY (skalar streng) er LIK VALUE
SELECT count(*) AS antall_med_skalar_verdi
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.old_val');

-- Result 4) Alle distinkte verdier i et SKALAR-felt, med antall
SELECT aktive_filter_valg ->> current_setting('custom.target_key') AS verdi, count(*) AS antall_filtre
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
GROUP BY verdi
ORDER BY antall_filtre DESC;

-- Start update transaction
WITH before_state AS (
-- Hent rader og verdier slik de ser ut før oppdatering
SELECT
filter_id,
aktive_filter_valg AS before_val,
aktive_filter_valg ->> current_setting('custom.target_key') AS before_target_value
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.old_val')
),
updated_rows AS (
-- Kjør oppdatering der vi bygger opp arrayet på nytt og erstatter kun matchende elementer
UPDATE lagrede_filter_mine_filter
SET aktive_filter_valg = jsonb_set(
aktive_filter_valg,
ARRAY[current_setting('custom.target_key')],
to_jsonb(current_setting('custom.new_val'))
)
WHERE aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.old_val')
RETURNING
filter_id,
aktive_filter_valg AS after_val,
aktive_filter_valg ->> current_setting('custom.target_key') AS after_target_value
)
-- Result 5. Vis side-om-side sammenligning av jsonb før og etter endringen
SELECT
b.filter_id,
b.before_target_value,
u.after_target_value,
b.before_val AS full_json_before,
u.after_val AS full_json_after
FROM before_state b
JOIN updated_rows u ON b.filter_id = u.filter_id
ORDER BY b.filter_id;

-- Result 6. SANITY CHECK: Sjekk at antall filtre som inneholder gammel verdi nå er null
SELECT count(*) AS antall_med_gammel_skalar_verdi
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.old_val');

-- Result 7. SANITY CHECK: Verifiser at INGEN andre rader i tabellen har blitt endret
SELECT COUNT(*) AS unexpected_modified_rows
FROM lagrede_filter_mine_filter
WHERE xmin::text = (txid_current()::text) -- Modified in this active transaction
-- Ekskluder rader vi forventet å endre (de som nå inneholder den nye verdien i arrayet):
AND aktive_filter_valg ->> current_setting('custom.target_key') != current_setting('custom.new_val');

-- Kjør COMMIT om alt ser riktig ut (NB! hele skriptet må kjøres på nytt med commit for at det skal fungere i gcp).
-- COMMIT;
```

### Endring av string/enum verdi for en liste

```
-- OPPDATER en ARRAY value

BEGIN;

-- ==========================================
-- CONFIGURATION
-- ==========================================
SET custom.target_key = 'key';
SET custom.old_val = 'old_value';
SET custom.new_val = 'new_value';
-- ==========================================

-- Verifiser at typen er en array (og ikke skalar streng, map e.l.)
-- 1)
SELECT
COALESCE(jsonb_typeof(aktive_filter_valg -> current_setting('custom.target_key')), 'missing') AS value_type,
COUNT(*) AS row_count
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
GROUP BY 1;

-- Se på dataene først, og få oversikt over antallet og hvor mange som har verdien som skal endres
-- 2) Antall filtre med KEY (uansett verdi)
SELECT current_setting('custom.target_key') AS key, count(*) AS antall_med_key
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key');

-- 3) Antall filtre der KEY (array) INNEHOLDER target value
SELECT count(*) AS antall_med_array_verdi
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.old_val'));

-- 4) Alle distinkte verdier INSIDE arrayet, med antall (ekspanderer array-elementer)
SELECT elem AS verdi, count(*) AS antall_filtre
FROM lagrede_filter_mine_filter,
jsonb_array_elements_text(aktive_filter_valg -> current_setting('custom.target_key')) AS elem
WHERE aktive_filter_valg ? current_setting('custom.target_key')
GROUP BY verdi
ORDER BY antall_filtre DESC;

-- Start update transaction using CTE
WITH before_state AS (
-- Hent rader og verdier slik de ser ut før oppdatering
SELECT
filter_id,
aktive_filter_valg AS before_val,
aktive_filter_valg -> current_setting('custom.target_key') AS before_target_array
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.old_val'))
),
updated_rows AS (
-- Kjør oppdatering der vi bygger opp arrayet på nytt og erstatter kun matchende elementer
UPDATE lagrede_filter_mine_filter
SET aktive_filter_valg = jsonb_set(
aktive_filter_valg,
ARRAY[current_setting('custom.target_key')],
COALESCE(
(
SELECT jsonb_agg(
CASE
WHEN elem = to_jsonb(current_setting('custom.old_val'))
THEN to_jsonb(current_setting('custom.new_val'))
ELSE elem
END
)
FROM jsonb_array_elements(aktive_filter_valg -> current_setting('custom.target_key')) AS elem
),
'[]'::jsonb
)
)
WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.old_val'))
RETURNING
filter_id,
aktive_filter_valg AS after_val,
aktive_filter_valg -> current_setting('custom.target_key') AS after_target_array
)
-- 5. Vis side-om-side sammenligning av arrayene før og etter endringen
SELECT
b.filter_id,
b.before_target_array,
u.after_target_array,
b.before_val AS full_json_before,
u.after_val AS full_json_after
FROM before_state b
JOIN updated_rows u ON b.filter_id = u.filter_id
ORDER BY b.filter_id;

-- 6. SANITY CHECK: Sjekk at antall filtre som inneholder gammel verdi nå er null
SELECT count(*) AS antall_med_gammel_array_verdi
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.old_val'));

-- 7. SANITY CHECK: Verifiser at INGEN andre rader i tabellen har blitt endret
SELECT COUNT(*) AS unexpected_modified_rows
FROM lagrede_filter_mine_filter
WHERE xmin::text = (txid_current()::text) -- Endret i denne aktive transaksjonen
-- Ekskluder rader vi forventet å endre (de som nå inneholder den nye verdien i arrayet):
AND NOT (aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.new_val')));

-- Hvis 'unexpected_modified_rows' returnerer 0, er du 100% trygg.
-- Kjør COMMIT om alt ser riktig ut (NB! hele skriptet må kjøres på nytt med commit for at det skal fungere i gcp).
-- COMMIT;
```

### Sletting av en verdi i et gitt filter

```
-- SLETT VERDI for filtervalg av type string eller array (ikke maps)
-- Sletter en gitt verdi under en nøkkel.
-- Om aktive_filter_valg blir tomt som et resultat, så slettes HELE filteret.
-- Om noen filtervalg gjenstår, insertes en kommentar i kolonnen info_om_slettet_filtervalg om dette.

BEGIN;

-- ==========================================
-- CONFIGURATION
-- ==========================================
SET custom.target_key = 'key'; -- Nøkkelen verdien ligger under, f eks "ytelseDagpengerArena"
SET custom.target_value = 'value'; -- Verdien vi ønsker å fjerne/slette. F eks "HAR_DAGPENGER_MED_PERMITTERING"
SET custom.deleted_filter_text = 'Verdi "XXX" i filter for YYY'; -- Navn på verdien og
filteret i frontend som ble slettet, vises rett til veiledere i frontend. F eks "Verdi "Dagpenger under permitering" i
filter for Dagpenger (Arena)".
-- ==========================================

-- Result 1: Antall rader som inneholder nøkkelen og har målverdien FØR sletting
SELECT
current_setting('custom.target_key') AS target_key,
current_setting('custom.target_value') AS target_value,
count(*) AS antall_treff
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
AND (
-- Enten er verdien en streng lik målverdien...
aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.target_value')
OR
-- ...eller så inneholder arrayet målverdien
aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_value'))
);

-- Start slette- og oppdateringstransaksjon med CTE inn i TEMP-tabell
CREATE TEMP TABLE temp_updated_rows ON COMMIT DROP AS -- 'ON COMMIT DROP' sletter tabellen automatisk når transaksjonen
avsluttes
WITH before_state AS (
-- Hent rader slik de ser ut før oppdatering
SELECT
filter_id,
aktive_filter_valg AS before_val,
jsonb_typeof(aktive_filter_valg -> current_setting('custom.target_key')) AS val_type
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
AND (
aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.target_value')
OR
aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_value'))
)
),
calculated_after_state AS (
-- Pre-kalkuler hva ny jsonb-verdi vil bli etter fjerning av målverdi
SELECT
filter_id,
before_val,
val_type,
CASE
-- 1) STRENG-ARRAY SJEKK: Håndter kun array
WHEN val_type = 'array' THEN
COALESCE(
(
SELECT
CASE
-- Hvis det filtrerte arrayet er tomt, slett hele nøkkelen
WHEN jsonb_agg(elem) IS NULL THEN before_val - current_setting('custom.target_key')
-- Ellers oppdater nøkkelen med det nye filtrerte arrayet
ELSE jsonb_set(before_val, ARRAY[current_setting('custom.target_key')], jsonb_agg(elem))
END
FROM jsonb_array_elements(before_val -> current_setting('custom.target_key')) AS elem
WHERE elem != to_jsonb(current_setting('custom.target_value'))
),
before_val - current_setting('custom.target_key')
)

            -- 2) STRENG-SJEKK: Håndter KUN hvis verdien er en ren streng
            WHEN val_type = 'string' THEN
                CASE 
                    -- Slett nøkkelen helt hvis streng-verdien matcher målverdien
                    WHEN before_val -> current_setting('custom.target_key') = to_jsonb(current_setting('custom.target_value'))
                    THEN before_val - current_setting('custom.target_key')
                    ELSE before_val
                END

            -- 3) SIKKERHETS-FALLBACK: Alle andre datatyper (som maps/objekter '{}', tall, boolean) forblir uendret
            ELSE before_val
        END AS after_val
    FROM before_state

),
deleted_rows AS (
-- SLETTING: Slett rader der fjerning av verdien vil gjøre hele JSONB-objektet helt tomt ('{}')
DELETE FROM lagrede_filter_mine_filter
WHERE filter_id IN (
SELECT filter_id
FROM calculated_after_state
WHERE after_val = '{}'::jsonb
)
RETURNING filter_id, '{}'::jsonb AS after_val, info_om_slettet_filtervalg
),
updated_rows AS (
-- OPPDATERING: Oppdater rader der det fortsatt gjenstår andre filtervalg i JSONB
UPDATE lagrede_filter_mine_filter f
SET
aktive_filter_valg = c.after_val,
info_om_slettet_filtervalg = array_append(
coalesce(f.info_om_slettet_filtervalg, '{}'::text[]),
current_setting('custom.deleted_filter_text')
)
FROM calculated_after_state c
WHERE f.filter_id = c.filter_id
-- Ekskluder rader som akkurat ble slettet i steget over
AND c.after_val != '{}'::jsonb
RETURNING f.filter_id, f.aktive_filter_valg AS after_val, f.info_om_slettet_filtervalg
)
-- Slå sammen resultatene fra både slettede og oppdaterte rader inn i TEMP-tabellen
SELECT
b.filter_id,
b.val_type,
b.before_val,
coalesce(u.after_val, d.after_val) AS after_val,
coalesce(u.info_om_slettet_filtervalg, d.info_om_slettet_filtervalg) AS info_om_slettet_filtervalg,
(d.filter_id IS NOT NULL) AS was_deleted -- Flagg som forteller om raden ble slettet helt
FROM before_state b
LEFT JOIN updated_rows u ON b.filter_id = u.filter_id
LEFT JOIN deleted_rows d ON b.filter_id = d.filter_id;

-- Result 2: Verifisering av berørte rader (viser datatype, verdi før, verdi etter, og om raden ble slettet)
SELECT
filter_id,
val_type AS value_type_before,
before_val -> current_setting('custom.target_key') AS value_before,
after_val -> current_setting('custom.target_key') AS value_after,
before_val AS full_json_before,
after_val AS full_json_after,
was_deleted AS was_completely_deleted,
info_om_slettet_filtervalg AS inserta_tekst_om_slettet_filtervalg
FROM temp_updated_rows
ORDER BY filter_id;

-- Result 3: Antall slettet og antall oppdatert
SELECT
COUNT(*) FILTER (WHERE info_om_slettet_filtervalg is not null) AS number_of_updated_rows,
COUNT(*) FILTER (WHERE was_deleted = true) AS number_of_deleted_rows
FROM temp_updated_rows;

-- Result 4: SANITY CHECK: Sjekk at ingen av de berørte radene lenger har målverdien koblet til denne nøkkelen
SELECT count(*) AS gjenstaaende_treff_med_verdi
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
AND (
aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.target_value')
OR
aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_value'))
);

-- Result 5: SANITY CHECK: Sjekk om det gjenstår noen helt tomme JSONB-objekter i tabellen (Skal nå være 0)
SELECT count(*) AS antall_tomme_filtre_igjen
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg = '{}'::jsonb
AND filter_id IN (SELECT filter_id FROM temp_updated_rows);

-- Result 6: SANITY CHECK: Verifiser at INGEN rader modifisert i denne transaksjonen fortsatt inneholder målverdien
under denne nøkkelen
SELECT COUNT(*) AS unexpected_modified_rows
FROM lagrede_filter_mine_filter
WHERE xmin::text = (txid_current()::text) -- Endret i denne aktive transaksjonen
AND (
-- Sjekk om målverdien fortsatt uventet eksisterer under nøkkelen på noen endrede rader
(aktive_filter_valg ? current_setting('custom.target_key')) AND (
aktive_filter_valg ->> current_setting('custom.target_key') = current_setting('custom.target_value')
OR
aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_value'))
)
);

-- Kjør COMMIT om alt ser riktig ut (NB! hele skriptet må kjøres på nytt med commit for at det skal fungere i gcp).
-- COMMIT;
```

### Sletting av et helt filter

```
-- SLETT NØKKEL MED TILHØRENDE INNHOLD
-- Sletter en en hel nøkkel / filtervalg.
-- Om aktive_filter_valg blir tomt, så slettes HELE filteret.
-- Om noen filtervalg gjennstår, insertes en kommentar i kolonnen X om dette.

BEGIN;

-- ==========================================
-- CONFIGURATION
-- ==========================================
SET custom.target_key = 'nøkkelnavn'; -- Sett nøkkelen til filtervalget du ønsker å slette her, f eks "
ytelseDagpengerArena"
SET custom.deleted_filter_text = 'Filtervalgnavn'; -- Navn på filteret i frontend som ble slettet, vises til veilederne
i frontend. F. eks "Dagpenger (Arena)"
-- ==========================================

-- Result 1: Antall filtre som inneholder nøkkelen FØR sletting
SELECT current_setting('custom.target_key') AS target_key, count(*) AS antall_med_key
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key');

-- Start slette- og oppdateringstransaksjon med CTE
CREATE TEMP TABLE temp_updated_rows ON COMMIT DROP AS -- 'ON COMMIT DROP' sletter tabellen automatisk når transaksjonen
avsluttes
WITH before_state AS (
SELECT
filter_id,
aktive_filter_valg AS before_val
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
),
deleted_rows AS (
-- SLETTING: Slett rader der fjerning av nøkkelen vil gjøre JSONB-objektet helt tomt
DELETE FROM lagrede_filter_mine_filter
WHERE filter_id IN (
SELECT filter_id
FROM before_state
WHERE (before_val - current_setting('custom.target_key')) = '{}'::jsonb
)
RETURNING filter_id, '{}'::jsonb AS after_val, info_om_slettet_filtervalg
),
updated_rows AS (
-- OPPDATERING: Oppdater rader der det fortsatt gjenstår andre filtervalg i JSONB
UPDATE lagrede_filter_mine_filter
SET
aktive_filter_valg = aktive_filter_valg - current_setting('custom.target_key'),
info_om_slettet_filtervalg = array_append(
coalesce(info_om_slettet_filtervalg, '{}'::text[]),
current_setting('custom.deleted_filter_text')
)
WHERE aktive_filter_valg ? current_setting('custom.target_key')
-- Ekskluder rader som akkurat ble slettet i steget over
AND filter_id NOT IN (SELECT filter_id FROM deleted_rows)
RETURNING filter_id, aktive_filter_valg AS after_val, info_om_slettet_filtervalg
)
-- Slå sammen resultatene fra både slettede og oppdaterte rader inn i TEMP-tabellen
SELECT
b.filter_id,
b.before_val,
coalesce(u.after_val, d.after_val) AS after_val,
coalesce(u.info_om_slettet_filtervalg, d.info_om_slettet_filtervalg) AS info_om_slettet_filtervalg,
(d.filter_id IS NOT NULL) AS was_deleted -- Flagg som forteller om raden ble slettet helt
FROM before_state b
LEFT JOIN updated_rows u ON b.filter_id = u.filter_id
LEFT JOIN deleted_rows d ON b.filter_id = d.filter_id;

-- Result 2: Verifisering av berørte rader
SELECT
filter_id,
current_setting('custom.target_key') AS deleted_key_name,
before_val AS full_json_before,
after_val AS full_json_after,
(after_val = '{}'::jsonb) AS was_empty_and_deleted,
(after_val != '{}'::jsonb) AS was_updated_with_info_text,
info_om_slettet_filtervalg AS inserta_tekst_om_slettet_filtervalg
FROM temp_updated_rows
ORDER BY filter_id;

-- Result 3: SANITY CHECK: Sjekk at antall filtre som inneholder nøkkelen nå er NULL
SELECT count(*) AS antall_med_slettet_key
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key');

-- Result 4: SANITY CHECK: Sjekk om det gjenstår noen helt tomme JSONB-objekter i tabellen (Skal nå være 0)
SELECT count(*) AS antall_tomme_filtre_igjen
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg = '{}'::jsonb
AND filter_id IN (SELECT filter_id FROM temp_updated_rows);

-- Result 5: SANITY CHECK: Verifiser at INGEN andre rader i tabellen har blitt uventet endret
SELECT COUNT(*) AS unexpected_modified_rows
FROM lagrede_filter_mine_filter
WHERE xmin::text = (txid_current()::text) -- Endret i denne aktive transaksjonen
-- Ekskluder rader vi faktisk håndterte (både oppdaterte og slettede)
AND filter_id NOT IN (SELECT filter_id FROM temp_updated_rows);

-- Kjør COMMIT om alt ser riktig ut (NB! hele skriptet må kjøres på nytt med commit for at det skal fungere i gcp).
-- COMMIT;
```

### Splitting av et filter til flere filtre

```
-- Flytt verdier over til et annet/nytt filter

BEGIN;

-- ==========================================
-- CONFIGURATION
-- ==========================================
SET custom.target_key = 'key_origin'; -- navn på filter som skal splittes
SET custom.target_key_new = 'key_destination'; -- navn på filter som skal motta verdier
SET custom.target_val = 'value'; -- navn på verdien man ønsker å flytte
-- ==========================================

-- Verifiser at typen er en array (og ikke skalar streng, map e.l.)
-- 1) Sjekk kilde-filteret
SELECT
COALESCE(jsonb_typeof(aktive_filter_valg -> current_setting('custom.target_key')), 'missing') AS value_type,
COUNT(*) AS row_count
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key')
GROUP BY 1;

-- 2) Antall filtre med kilde-nøkkel (uansett verdi)
SELECT current_setting('custom.target_key') AS key, count(*) AS antall_med_key
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg ? current_setting('custom.target_key');

-- 3) Antall filtre der kilde-nøkkel (array) INNEHOLDER verdien som skal flyttes
SELECT count(*) AS antall_med_target_verdi
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_val'));

-- 4) Alle distinkte verdier INSIDE det originale arrayet, med antall (ekspanderer array-elementer)
SELECT elem AS verdi, count(*) AS antall_filtre
FROM lagrede_filter_mine_filter,
jsonb_array_elements_text(aktive_filter_valg -> current_setting('custom.target_key')) AS elem
WHERE aktive_filter_valg ? current_setting('custom.target_key')
GROUP BY verdi
ORDER BY antall_filtre DESC;

-- Start oppdateringstransaksjon ved bruk av CTE (Common Table Expressions)
WITH before_state AS (
-- Hent rader og verdier slik de ser ut før oppdatering
SELECT
filter_id,
aktive_filter_valg AS before_val,
aktive_filter_valg -> current_setting('custom.target_key') AS before_source_array,
aktive_filter_valg -> current_setting('custom.target_key_new') AS before_target_array
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_val'))
),
updated_rows AS (
-- Kjør oppdatering
UPDATE lagrede_filter_mine_filter
SET aktive_filter_valg =
jsonb_set(
-- TRINN 1: Rydd opp i kilde-filteret. Hvis arrayet blir tomt, fjern hele nøkkelen.
CASE
WHEN ((aktive_filter_valg -> current_setting('custom.target_key')) - current_setting('custom.target_val')) = '[]'::jsonb
THEN aktive_filter_valg - current_setting('custom.target_key')
ELSE jsonb_set(
aktive_filter_valg,
ARRAY[current_setting('custom.target_key')],
(aktive_filter_valg -> current_setting('custom.target_key')) - current_setting('custom.target_val')
)
END,

            -- TRINN 2: Legg til verdien i destinasjons-filteret (uten å lage duplikater)
            ARRAY[current_setting('custom.target_key_new')],
            CASE 
                -- Sjekk om destinasjons-arrayet allerede inneholder verdien vår
                WHEN COALESCE(aktive_filter_valg -> current_setting('custom.target_key_new'), '[]'::jsonb) @> jsonb_build_array(current_setting('custom.target_val'))
                THEN COALESCE(aktive_filter_valg -> current_setting('custom.target_key_new'), '[]'::jsonb)
                -- Hvis ikke, legg den til (append)
                ELSE COALESCE(aktive_filter_valg -> current_setting('custom.target_key_new'), '[]'::jsonb) || jsonb_build_array(current_setting('custom.target_val'))
            END
        )
    WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_val'))
    RETURNING 
        filter_id, 
        aktive_filter_valg AS after_val,
        aktive_filter_valg -> current_setting('custom.target_key') AS after_source_array,
        aktive_filter_valg -> current_setting('custom.target_key_new') AS after_target_array

)
-- 5. Vis side-om-side sammenligning av arrayene før og etter endringen
SELECT
b.filter_id,
b.before_source_array AS kilde_array_før,
u.after_source_array AS kilde_array_etter,
b.before_target_array AS dest_array_før,
u.after_target_array AS dest_array_etter,
b.before_val AS full_json_before,
u.after_val AS full_json_after
FROM before_state b
JOIN updated_rows u ON b.filter_id = u.filter_id
ORDER BY b.filter_id;

-- 6. SANITY CHECK: Sjekk at antall filtre som inneholder verdien i gammel kilde nå er null
SELECT count(*) AS antall_igjen_i_gammel_kilde
FROM lagrede_filter_mine_filter
WHERE aktive_filter_valg -> current_setting('custom.target_key') @> to_jsonb(current_setting('custom.target_val'));

-- 7. SANITY CHECK: Verifiser at INGEN andre rader i tabellen har blitt uventet endret
SELECT COUNT(*) AS unexpected_modified_rows
FROM lagrede_filter_mine_filter
WHERE xmin::text = (txid_current()::text) -- Endret i denne aktive transaksjonen
-- Ekskluder rader vi forventet å endre (de som nå har fått verdien i det nye arrayet):
AND NOT (aktive_filter_valg -> current_setting('custom.target_key_new') @> to_jsonb(current_setting('
custom.target_val')));

-- Hvis 'unexpected_modified_rows' returnerer 0, er du helt trygg.
-- Kjør COMMIT om alt ser riktig ut i verifikasjonsstegene.
-- COMMIT; 
```
