# Etterlevelsesgjennomgang — «Oppfølging mot arbeid: Oversikten»

**Type:** Full gjennomgang (deep)
**Etterlevelsesdokument:** `2edfe1e1-a34e-469f-8a61-3f284c74e58e`
**Team:** Team OBO (`team-obo-arbeidsoppfølging`)
**Behandling:** B555 «Oversikten» (Modia Arbeidsrettet oppfølging)
**Systemer vurdert:**
- Backend: `navikt/veilarbportefolje` (Kotlin/Java, Spring Boot)
- Frontend: `navikt/veilarbportefoljeflatefs` (React, intern veilederflate)
- Støttetjeneste: `navikt/veilarbfilter` (Kotlin/Java, lagrer veilederes filtervalg)

> ⚠️ **Dette er et forslag til gjennomgang.** Ingenting er lastet opp til etterlevelsesløsningen.
> Begrunnelsene må kvalitetssikres av teamet før eventuell opplasting.

---

## 1. Sammendrag

| Mål | Antall |
|-----|--------|
| Gjeldende krav (AKTIVE) | **54** |
| Utfylte etterlevelser på gjeldende versjon | 38 |
| Krav uten etterlevelse (helt ubesvart) | **16** |
| — herav: ny kravversjon, gammel begrunnelse finnes (migreres) | 3 (K103, K205, K230) |
| — herav: helt nye krav | 13 |
| Etterlevelser liggende på **utgått** versjon (ignoreres / migreres) | 5 (103.1, 108.1, 196.3, 205.1, 230.1) |

**Hovedinntrykk:** Dokumentasjonen er i hovedsak godt utfylt for personvern- og rettsgrunnlagskrav,
men har flere materielle svakheter:

1. **K253 (oppslagslogg) mangler helt** — og kodegjennomgangen fant **ingen oppslagslogg (Arcsight/CEF)**
   i backend. Dette er det mest alvorlige funnet.
2. **Tomme OPPFYLT-begrunnelser** på sentrale krav (K102.3, K107.2, K191.1, K190.2) — en OPPFYLT-status
   uten begrunnelse er aldri holdbar.
3. **Faktuelt misvisende begrunnelse** på K191.1 SK3 («det lagres ikke noe informasjon i Oversikten»)
   — backend lagrer faktisk persondata (huskelapp, fargekategori, tiltakshendelse, enslige forsørgere).
4. **For kategoriske IKKE_RELEVANT-begrunnelser** på K115/K188 (automatisering/profilering) — systemet
   konsumerer og viser profileringsresultat fra arbeidssøkerregisteret (`paw.arbeidssoker-profilering-v1`).
5. **13 nye arkiv-/sikkerhetskrav** (K245, K251, K255, K262, K267–K274) er ennå ikke vurdert.
6. **PVK mangler i etterlevelsesløsningen.** B555 har `needForDpia=true` og art. 9-opplysninger;
   `dpia.refToDpia` peker til en ekstern PVK «under arbeid» (P360 recno 206636), men ingen PVK er
   koblet i etterlevelsesløsningen.

---

## 2. Handlingspunkter (prioritert)

| # | Prioritet | Krav | Tiltak |
|---|-----------|------|--------|
| 1 | 🔴 Kritisk | **K253.1** | Avklar oppslagslogg med Team Auditlogging Arcsight. Etabler oppslagslogging for enkeltpersonrettede funksjoner (huskelapp, fargekategori). Kodegjennomgang fant ingen oppslagslogg i backend. |
| 2 | 🔴 Kritisk | **K255.1** | Dokumentér adressebeskyttelse. Tilgangskontroll finnes (poao-tilgang, kode 6/7), men beskyttelse i sammenstilte oversikter (SK5/SK10) må verifiseres for listevisningen. |
| 3 | 🔴 Kritisk | **PVK / K114** | Ferdigstill PVK og koble den til etterlevelsesdokumentet. Art. 9-data + `needForDpia=true`. |
| 4 | 🟠 Høy | **K191.1** | Rett opp faktuelt feil SK3-begrunnelse. Fyll tomme OPPFYLT (SK1/SK2). Dokumentér faktisk lagret persondata + kassasjon (120 mnd, B555). |
| 5 | 🟠 Høy | **K107.2** | Erstatt «Det er ikke tolkningstvil» (SK3) med faktisk dokumentert rettsgrunnlag fra B555 (Art 6(1)e + Art 9(2)b). Fyll tom SK1. |
| 6 | 🟠 Høy | **K102.3** | Fyll tom OPPFYLT SK1 med formålet «Oppfølging mot arbeid» (B555). |
| 7 | 🟠 Høy | **K190.2** | Fyll tom SK1. Avklar behandlingsansvar NAV-loven §14a (SK3 under arbeid). |
| 8 | 🟠 Høy | **K115/K188** | Omformulér IKKE_RELEVANT-begrunnelsene: systemet profilerer ikke selv, men viser profileringsresultat. |
| 9 | 🟡 Medium | **K196.6** | WCAG: status IKKE_OPPFYLT («ikke prioritert»). Frontend bruker Aksel 8.11.0 med god UU-støtte — vurder ny UU-gjennomgang, status kan trolig heves. |
| 10 | 🟡 Medium | **K245.1** | Dokumentér risikovurdering (ROS/TryggNok). |
| 11 | 🟡 Medium | **K267.1** | Dokumentér sikkerhetsnivå (sårbarhetshåndtering, secrets, tilgangskontroll, logging). Mye er på plass. |
| 12 | 🟡 Medium | **K251, K262, K268** | Vurdér og dokumentér (barnets beste, rett til å protestere, EØS). |
| 13 | 🟢 Lav | **K103.2, K205.2, K230.2** | Migrér begrunnelser fra gammel versjon til ny (se §6). |
| 14 | 🟢 Lav | **K269–K274** | Arkivkrav: vurdér relevans (Oversikten er et visningsverktøy, ikke arkiv). |

---

## 3. Risikofunn fra kodegjennomgang

### 🔴 Ingen oppslagslogg (Arcsight/CEF) i backend
Kodegjennomgang av `veilarbportefolje` fant **ingen** `AuditLogger`/`@AuthorizeFnr`/Arcsight/CEF-logging.
Kun `secureLog` (feil-/teknisk logging) og en inkludert `logback-naudit.xml`. Kontrollere som håndterer
enkeltpersoners data på `fnr`:
- `HuskelappController.java` (CRUD på huskelapp pr fnr)
- `FargekategoriController.java` (fargekategori pr fnr)
- `VeilederController.java`, `EnhetController.java`, `UngdomsprogramController.kt`

**Nyanse (K253 SK3):** Ordinær listevisning av porteføljen skal *ikke* oppslagslogges. Men de
enkeltpersonrettede funksjonene (huskelapp/fargekategori) kan være «visning/oppslag» som utløser
loggplikt. Dette må avklares — ikke settes IKKE_RELEVANT.

> **Forbehold:** Verifiser at logging ikke skjer via felles filter, sidecar/proxy eller bibliotek
> utenfor de søkte mønstrene før endelig konklusjon.

### 🟠 Profileringsresultat vises i veilederflate
Backend konsumerer `paw.arbeidssoker-profilering-v1` og frontend viser «Resultat profilering
arbeidssøkere» samt innsatsgruppe (`vedtak14a`). Selve profileringen skjer i arbeidssøkerregisteret
(PAW), men Oversikten *behandler og viser* resultatet. Påvirker K115/K188.

### 🟠 Egen lagring av persondata (inkl. art. 9-nær)
Flyway-migrasjoner i backend viser egne tabeller med persondata:
- `Huskelapp(fnr, kommentar [fritekst], frist, enhet_id, …)`
- `fargekategori(fnr, sist_endret_av_veilederident)`
- `TILTAKSHENDELSE(fnr, tekst, lenke, …)`
- `enslige_forsorgere(personIdent, …)` / `enslige_forsorgere_barn(fnr, termindato)` — med typer som
  `FORSØRGER_ER_SYK`, `BARNET_ER_SYKT` (helseindikasjon)

Ingen tydelig tidsbasert kassasjonsjobb funnet (kun hendelsesdrevet opprydding ved inaktiv oppfølging).
Påvirker K191 (lagringstid/kassasjon).

### 🟢 Sporing i frontend — maskering på plass
- **Umami** (`umami.nav.no`): sporer `filtervalg`, `knapp klikket`, `lenke klikket`, `alert vist`.
  Fnr (`\d{11}`) og NAV-ident (`[A-Za-z]\d{6}`) maskeres før sending (`src/umami/umami.ts:29-55`).
- **Grafana Faro** (`telemetry.nav.no`): kun web vitals, ingen persondata funnet.
- **Amplitude**: dependency i `package.json`, men ikke i bruk i koden.
- **Sentry/Hotjar/Google Analytics**: ikke funnet.
- **CSP** `connectSrc`: `'self'`, `wss:`, `*.nav.no`, `*.adeo.no`.

### 🟢 veilarbfilter — begrenset persondata
Lagrer veilederes egne filterdefinisjoner. `PortefoljeFilter` *kan* inneholde søkeparametre
(`navnEllerFnrQuery`, `veiledere`), men er primært veileders eget oppsett, ikke et personregister.
Tilgang til enhetsfiltre sjekkes mot poao-tilgang. Ingen Kafka. Ingen oppslagslogg (trolig korrekt,
men bør bekreftes for `navnEllerFnrQuery`).

---

## 4. Positive funn (verifisert i kode)

- **Autentisering:** Azure AD OIDC i alle tre tjenestene (`FilterConfig.java`). Frontend har
  Azure-sidecar med `autoLogin: true`, kun **intern** ingress i prod.
- **Autorisasjon:** poao-tilgang brukes konsekvent. `AuthService` sensurerer på diskresjonskode
  (`STRENGT_FORTROLIG`→kode 6, `FORTROLIG`→kode 7), egen ansatt og barn under 18 (`AuthService.java:84-151`).
- **Tilgangskontroll til skjermede/kode 6-7** finnes i `PoaoTilgangWrapper.java:76-92`.
- **Universell utforming:** Frontend bruker `@navikt/ds-react 8.11.0` (Aksel) med konkrete UU-tiltak:
  `role="search"`, `aria-live`, `role="listbox/option"`, fokusadministrasjon, tastaturnavigasjon.
- **Kafka over SSL**, secrets via NAIS, Cloud SQL (GCP).
- **Databehandleravtaler** dokumentert i K190.2 SK2 (GCP, Aiven) — matcher de 2 databehandlerne i B555.

---

## 5. Kvalitetsvurdering av eksisterende begrunnelser

Legende: ✅ OK · ⚠️ Forbedringsforslag · 🔴 Utdatert/feil

### Krav som krever endring

**🔴 K102.3 SK1** (formål) — Status OPPFYLT, men **tom begrunnelse**.
> Forslag: «Oversikten har ett klart, definert formål: oppfølging mot arbeid (behandling B555,
> rettsgrunnlag NAV-loven § 14 a). Verktøyet gir veiledere oversikt over brukere under arbeidsrettet
> oppfølging slik at de kan prioritere og følge opp. Formålet er fastsatt i Behandlingskatalogen (B555).»

**🔴 K107.2 SK1** (behandlingsgrunnlag) — Status OPPFYLT, men **tom begrunnelse**.
> Forslag: «Behandlingsgrunnlaget er GDPR art. 6(1)(e) (utøvelse av offentlig myndighet), og for
> særlige kategorier art. 9(2)(b) (arbeids-, trygde- og sosialrett). Nasjonalt rettsgrunnlag:
> NAV-loven §§ 4, 14, 14 a; folketrygdloven kap. 4, 8, 11, 11a, 15; arbeidsmarkedsloven §§ 10, 12, 13.
> Registrert i B555.»

**🔴 K107.2 SK3** (dokumentert vurdering) — Begrunnelse «Det er ikke tolkningstvil.»
Dette svarer på feil spørsmål. SK3 spør om vurderingen er *dokumentert*, ikke om den er åpenbar.
> Forslag: «Valg av behandlingsgrunnlag er dokumentert i Behandlingskatalogen (B555) med konkrete
> rettsgrunnlag pr. GDPR-artikkel og nasjonal lov (se SK1). Behandlingen utøver offentlig myndighet
> etter NAV-loven § 14 a.»

**🔴 K191.1 SK3** (kassasjon) — Begrunnelse «Det lagres ikke noe informasjon i Oversikten…»
**Faktuelt misvisende.** Backend lagrer egne persondata (huskelapp, fargekategori, tiltakshendelse,
enslige forsørgere). SK1/SK2 er OPPFYLT med tom begrunnelse.
> Forslag SK1/SK2: «Oversikten lagrer egne persondata (huskelapp med fritekst, fargekategori,
> tiltakshendelser). Autoritativ lagringstid for behandlingen er 120 måneder med påfølgende kassasjon
> (B555, websak 2018000730). Speilet data fra kildesystemer (PDL, Arena, vedtak) eies ikke av Oversikten.»
> SK3: Beskriv faktisk kassasjonsmekanisme. Inaktive brukere fjernes fra porteføljen hendelsesdrevet;
> [Teamet må dokumentere: tidsbasert sletting/kassasjon av huskelapp/fargekategori].

**🔴 K190.2 SK1** (tredjepartsrelasjoner) — Status OPPFYLT, **tom begrunnelse**. SK3 under arbeid
(behandlingsansvar NAV-loven §14a).
> Forslag SK1: «Tredjepartsrelasjoner er kartlagt: databehandlere GCP og Aiven (se SK2). Datakilder
> (PDL, Arena, arbeidssøkerregisteret, vedtaksløsninger) er behandlingsansvarliges egne kilder, ikke
> databehandlere.»

**⚠️ K115.1 / K188.1** (automatisering/profilering) — IKKE_RELEVANT med «benyttes ikke i løsningen».
For kategorisk.
> Forslag: «Oversikten utfører ikke selv profilering eller automatiserte avgjørelser. Løsningen
> konsumerer og viser profileringsresultat fra arbeidssøkerregisteret (PAW, `paw.arbeidssoker-profilering-v1`).
> Ansvar for profileringslogikk, informasjon til den registrerte og vurdering av automatisering ligger
> i kildesystemet. [Teamet må bekrefte: at profileringsresultatet kun vises og ikke brukes til
> automatisert beslutning i Oversikten.]»

**⚠️ K108.2** (informasjon til registrerte) — UNDER_REDIGERING, SK2/SK3 tomme (under arbeid).
SK1 IKKE_RELEVANT-begrunnelse er rimelig (bruker informeres i registreringsløsningen). Fullfør SK2/SK3.

**⚠️ K196.6** (WCAG) — SK3/SK5 IKKE_OPPFYLT («ikke prioritert nå»). Ærlig egenvurdering, men frontend
har faktisk god UU-støtte (Aksel 8.11.0). SK6 IKKE_RELEVANT («interne flater») er korrekt — a11y-statement
gjelder eksterne flater.
> Anbefaling: Kjør en UU-gjennomgang; status kan trolig heves til OPPFYLT for SK3.

### Krav vurdert som korrekte (utvalg)

- **✅ K115/K233-236 (økonomi/ØSA), K120/K121/K122 (sporing/vedtak/økonomi)** — IKKE_RELEVANT er korrekt.
  Dokumentet er irrelevant for OKONOMISYSTEM og VEDTAKSBEHANDLING; Oversikten fatter ikke vedtak.
- **✅ K109.1** (fødselsnummer), **K111.1** (nødvendighet, grundig 2582 tegn), **K130.2** (lagret i Norge),
  **K154.1** (taushetsplikt) — godt dokumentert.
- **✅ K101.2** (overføring utland), **K157.1** (eksterne tjenester) — OK.

---

## 6. Foreslåtte begrunnelser for de 16 manglende kravene

### Migreres fra gammel versjon

**K103.2 — Personopplysninger skal kunne rettes** (kilde: K103.1, var IKKE_RELEVANT)
> SK1/SK3 IKKE_RELEVANT: «Personopplysningene i Oversikten hentes fra kilder Nav ikke korrigerer her
> (PDL/Folkeregisteret, Arena, arbeidssøkerregisteret). Retting skjer i kildesystemene. Opplysninger
> kan ikke rettes i Oversikten.» SK2 IKKE_RELEVANT (varsling om retting skjer i kildesystem).
> [Nytt i v2: vurdér SK3 om tiltak for å sikre korrekte/oppdaterte opplysninger — data oppdateres
> løpende via Kafka fra kildene.]

**K205.2 — Utenforstående får ikke uberettiget innsyn** (kilde: K205.1)
> SK1 IKKE_RELEVANT: «Oversikten produserer ikke enkeltvedtak/forhåndsvarsel, og den det gjelder kan
> ikke logge inn (intern veilederflate).» SK2 OPPFYLT: «Tilgangsstyring for medarbeidere via Azure AD +
> poao-tilgang; kun veiledere med tjenstlig behov. Sensurering på kode 6/7 og skjerming.»
> SK3 OPPFYLT: «NAIS/GCP, Azure AD i intern sone, løpende patching av avhengigheter.»

**K230.2 — Dokumentasjonen skal kunne avleveres og slettes til rett tid** (kilde: K230.1, var OPPFYLT)
> Gjennomgå mot nynorsk-versjonens nye SK-struktur (SK1 bevaringsverdi, SK2 lagringstid, SK3 kassasjon,
> SK4 avlevering, SK5 begrenset tilgang historisk). Lagringstid 120 mnd (B555). [Teamet må avklare:
> om Oversikten har bevaringsverdig dokumentasjon, eller om dette dekkes av arkivkjernen (Joark/Gosys).]

### Helt nye krav

**K245.1 — Risikovurdering** — [Teamet må dokumentere: ROS/TryggNok-vurdering for systemet, risikoeier].
Hvis ROS finnes, legg ROS-lenke i `risikovurderinger`-feltet.

**K251.1 — Barnets beste** — SK0: «Oversikten behandler opplysninger om brukeres barn under 18 år
(`barnUnder18AarData`, `enslige_forsorgere_barn`) som del av veileders oppfølging. Barn berøres
indirekte.» SK1: [Teamet må dokumentere konkret barnets-beste-vurdering.]

**K253.1 — Oppslagslogg (Arcsight)** — 🔴 Se §3.
> SK0 **IKKE_OPPFYLT / UNDER_ARBEID**: «Kodegjennomgang fant ingen oppslagslogging til Arcsight i
> backend. Enkeltpersonrettede funksjoner (huskelapp, fargekategori) bør oppslagslogges.»
> SK3 OPPFYLT: «Ordinær listevisning av porteføljen logges ikke per bruker, jf. unntaket for liste-/
> sammenstilte visninger.» SK4/SK5 UNDER_ARBEID: «Må avklares med Team Auditlogging Arcsight.»
> Settes IKKE som IKKE_RELEVANT.

**K255.1 — Adressebeskyttelse** — SK0 OPPFYLT: «poao-tilgang sjekker rolle/tilgang før visning;
`AuthService` sensurerer kode 6/7.» SK5/SK10 (sammenstilte oversikter): [Teamet må verifisere at
brukere med adressebeskyttelse beskyttes også i listevisningen og ved geolokaliserende felt
(`geografiskBosted`, `bostedsadresse`).]

**K262.1 — Rett til å protestere** — SK0: «Behandlingen har rettsgrunnlag i art. 6(1)(e). Retten til å
protestere er aktuell ved offentlig myndighetsutøvelse.» SK1/SK2: [Teamet må dokumentere rutine for å
håndtere protest — håndteres trolig i oppfølgingsvedtaket, ikke i Oversikten.]

**K267.1 — Forsvarlig sikkerhetsnivå** — Mye er på plass:
> SK1 (oppdatert): Dependabot/NAIS-bygg. SK3 (secrets): NAIS secrets, ikke i kode. SK6 (tilgangskontroll
> på endepunkter): Azure AD OIDC-filter på alle `/api/*`. SK4 (logging): secureLog finnes.
> [Teamet må bekrefte: sårbarhetshåndtering (SK0), input/output-validering (SK2), backup (SK5).]

**K268.1 — EØS** — SK0: [Teamet må dokumentere at visningen ikke forskjellsbehandler EØS-borgere —
Oversikten viser data nøytralt fra kildene.]

**K269.1–K274.1 — Arkivkrav (nynorsk)** — Oversikten er et **visningsverktøy** som speiler data fra
andre systemer i sanntid, ikke et arkivsystem.
> Foreløpig vurdering (må bekreftes av teamet/arkivressurs):
> - **K269** (forvaltes som arekiv?): [Avklar — trolig nei for Oversiktens egen data; huskelapp er
    >   arbeidsverktøy, ikke arkivverdig dokumentasjon.]
> - **K270–K274** (arkivkrav, journalføring): Sannsynligvis **IKKE_RELEVANT** dersom Oversikten ikke
    >   produserer journalpliktig/bevaringsverdig dokumentasjon. Journalføring skjer i Joark/Gosys.
    >   [Teamet/arkivressurs må bekrefte.]

---

## 7. Prioritert kravliste (forslag til `prioritertKravNummer`)

`253, 255, 191, 107, 102, 190, 245, 267, 115, 188, 251, 262, 196`

(Kritiske personvern-/sikkerhetskrav først, deretter høy/medium. Arkivkrav K269-274 holdes utenfor til
relevans er avklart.)

---

## 8. Systemarkitektur

```
                          Veileder (intern, Azure AD)
                                    │
                                    ▼
                 ┌──────────────────────────────────┐
                 │  veilarbportefoljeflatefs (React) │  Aksel 8.11.0
                 │  - intern ingress (prod)          │  Umami (maskert), Faro
                 │  - Azure sidecar, autoLogin       │
                 └──────────────┬───────────────────-┘
                                │ OBO-token
            ┌───────────────────┼────────────────────┐
            ▼                   ▼                     ▼
   ┌─────────────────┐  ┌──────────────┐   ┌────────────────────┐
   │ veilarbportefolje│  │ veilarbfilter│   │ poao-tilgang       │
   │ (backend)        │  │ (filtervalg) │   │ (kode 6/7, skjerm) │
   │ Azure AD OIDC    │  │ Azure AD     │   └────────────────────┘
   │ poao-tilgang     │  └──────┬───────┘
   └───┬─────────┬────┘         │
       │         │              ▼
       │         │         Cloud SQL (filtre)
       ▼         ▼
  Cloud SQL   Kafka-topics: pdl, vedtak-14a, oppfolgingsbruker,
  (huskelapp,  arbeidssoker-profilering, skjermede-personer,
   fargekat.,  ytelser, tiltakshendelse, enslig-forsorger …
   tiltaks-    │
   hendelse)   ▼
            Kildesystemer (PDL, Arena, PAW, vedtaksløsninger)

  ⚠️ Mangler: oppslagslogg (Arcsight/CEF) på enkeltpersonrettede oppslag
```

---

## 9. Manglende data / forbehold

- **PVK:** Ingen PVK koblet i etterlevelsesløsningen. `dpia.refToDpia` peker til ekstern P360 «under arbeid».
- **Databehandlerdetaljer:** `/processor/{id}`-oppslag ga 404 for begge databehandler-IDer i B555;
  navn (GCP/Aiven) er hentet fra K190.2 SK2-begrunnelsen.
- **ROS/TryggNok:** Ikke innhentet — [teamet må oppgi ROS-ID hvis den finnes].
- **Oppslagslogg:** Bekreft at logging ikke skjer via felles filter/sidecar/bibliotek før K253 konkluderes.