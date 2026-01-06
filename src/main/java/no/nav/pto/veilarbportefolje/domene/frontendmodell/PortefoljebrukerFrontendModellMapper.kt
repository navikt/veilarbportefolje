package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.DateUtils.*
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils.vurderingsBehov
import java.sql.Timestamp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object PortefoljebrukerFrontendModellMapper {

    fun toPortefoljebrukerFrontendModell(
        opensearchBruker: PortefoljebrukerOpensearchModell,
        ufordelt: Boolean,
        filtervalg: Filtervalg?
    ): PortefoljebrukerFrontendModell {

        val kvalifiseringsgruppekode = opensearchBruker.kvalifiseringsgruppekode
        val profileringResultat = opensearchBruker.profilering_resultat
        val innsatsgruppe = if (OppfolgingUtils.INNSATSGRUPPEKODER.contains(opensearchBruker.kvalifiseringsgruppekode))
            opensearchBruker.kvalifiseringsgruppekode else null
        val vurderingsBehov = if (opensearchBruker.trenger_vurdering)
            vurderingsBehov(kvalifiseringsgruppekode, profileringResultat)
        else null
        val harBehovForArbeidsevneVurdering = vurderingsBehov == VurderingsBehov.ARBEIDSEVNE_VURDERING

        val trengerOppfolgingsvedtak = opensearchBruker.gjeldendeVedtak14a == null
        val harUtenlandskAdresse = opensearchBruker.utenlandskAdresse != null
        val diskresjonskodeFortrolig =
            if (Adressebeskyttelse.FORTROLIG.diskresjonskode == opensearchBruker.diskresjonskode
                || Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode == opensearchBruker.diskresjonskode
            ) opensearchBruker.diskresjonskode else null

        val bostedKommuneUkjentEllerUtland = if (harUtenlandskAdresse) {
            "Utland"
        } else if (opensearchBruker.harUkjentBosted) {
            "Ukjent"
        } else {
            "-"
        }


        val frontendbruker = PortefoljebrukerFrontendModell(
            aktoerid = opensearchBruker.aktoer_id,
            etiketter = Etiketter(
                erDoed = opensearchBruker.er_doed,
                erSykmeldtMedArbeidsgiver = opensearchBruker.er_sykmeldt_med_arbeidsgiver,
                trengerOppfolgingsvedtak = trengerOppfolgingsvedtak,
                nyForVeileder = opensearchBruker.ny_for_veileder,
                nyForEnhet = ufordelt,
                harBehovForArbeidsevneVurdering = harBehovForArbeidsevneVurdering,
                harSikkerhetstiltak = opensearchBruker.sikkerhetstiltak != null,
                diskresjonskodeFortrolig = diskresjonskodeFortrolig,
                profileringResultat = profileringResultat
            ),
            fnr = opensearchBruker.fnr,
            fornavn = opensearchBruker.fornavn,
            etternavn = opensearchBruker.etternavn,
            hovedStatsborgerskap = opensearchBruker.hovedStatsborgerskap?.let {
                StatsborgerskapForBruker(
                    statsborgerskap = it.statsborgerskap,
                    gyldigFra = it.gyldigFra
                )
            },
            foedeland = opensearchBruker.foedelandFulltNavn,
            geografiskBosted = GeografiskBostedForBruker(
                bostedKommune = opensearchBruker.kommunenummer,
                bostedBydel = opensearchBruker.bydelsnummer,
                bostedKommuneUkjentEllerUtland = bostedKommuneUkjentEllerUtland,
                bostedSistOppdatert = opensearchBruker.bostedSistOppdatert
            ),
            tolkebehov = Tolkebehov.of(
                opensearchBruker.talespraaktolk,
                opensearchBruker.tegnspraaktolk,
                opensearchBruker.tolkBehovSistOppdatert
            ),
            barnUnder18AarData = opensearchBruker.barn_under_18_aar,
            oppfolgingStartdato = fromIsoUtcToLocalDateOrNull(opensearchBruker.oppfolging_startdato),
            tildeltTidspunkt = fromLocalDateTimeToLocalDateOrNull(opensearchBruker.tildelt_tidspunkt),
            veilederId = opensearchBruker.veileder_id,
            egenAnsatt = opensearchBruker.egen_ansatt,
            skjermetTil = fromLocalDateTimeToLocalDateOrNull(opensearchBruker.skjermet_til),
            tiltakshendelse = opensearchBruker.tiltakshendelse?.let {
                TiltakshendelseForBruker(
                    id = it.id,
                    tiltakstype = it.tiltakstype,
                    opprettet = fromLocalDateTimeToLocalDateOrNull(it.opprettet),
                    tekst = it.tekst,
                    lenke = it.lenke
                )
            },
            hendelse = mapHendelserBasertPåFiltervalg(opensearchBruker, filtervalg),
            meldingerVenterPaSvar = MeldingerVenterPaSvar(
                datoMeldingVenterPaNav = fromIsoUtcToLocalDateOrNull(opensearchBruker.venterpasvarfranav),
                datoMeldingVenterPaBruker = fromIsoUtcToLocalDateOrNull(opensearchBruker.venterpasvarfrabruker),
            ),
            aktiviteterAvtaltMedNav = AktiviteterAvtaltMedNav(
                nesteUtlopsdatoForAlleAktiviteter = mapNesteUtlopsdatoForAlleAktiviteter(opensearchBruker),
                nyesteUtlopteAktivitet = fromIsoUtcToLocalDateOrNull(opensearchBruker.nyesteutlopteaktivitet),
                nesteUtlopsdatoForFiltrerteAktiviteter = mapNesteUtlopsdatoForAktivitetBasertPåFiltervalg(
                    opensearchBruker,
                    filtervalg
                ),
                aktivitetStart = fromIsoUtcToLocalDateOrNull(opensearchBruker.aktivitet_start),
                nesteAktivitetStart = fromIsoUtcToLocalDateOrNull(opensearchBruker.neste_aktivitet_start),
                forrigeAktivitetStart = fromIsoUtcToLocalDateOrNull(opensearchBruker.forrige_aktivitet_start),
            ),
            moteMedNavIDag = mapMoteMedNavIDag(opensearchBruker),
            sisteEndringAvBruker = mapSisteEndringerAvBrukerBasertPåFiltervalg(opensearchBruker, filtervalg),
            utdanningOgSituasjonSistEndret = opensearchBruker.utdanning_og_situasjon_sist_endret,
            nesteSvarfristCvStillingFraNav = opensearchBruker.neste_svarfrist_stilling_fra_nav,
            vedtak14a = mapVedtak14a(opensearchBruker),
            ytelser = YtelserForBruker(
                ytelserArena = YtelserArena(
                    innsatsgruppe = innsatsgruppe,
                    ytelse = YtelseMapping.of(opensearchBruker.ytelse),
                    utlopsdato = toLocalDateTimeOrNull(opensearchBruker.utlopsdato),
                    dagputlopUke = opensearchBruker.dagputlopuke,
                    permutlopUke = opensearchBruker.permutlopuke,
                    aapmaxtidUke = opensearchBruker.aapmaxtiduke,
                    aapUnntakUkerIgjen = opensearchBruker.aapunntakukerigjen,
                    aapordinerutlopsdato = opensearchBruker.aapordinerutlopsdato
                ),
                aap = mapAapKelvin(opensearchBruker),
                tiltakspenger = mapTiltakspenger(opensearchBruker),
                ensligeForsorgereOvergangsstonad = opensearchBruker.enslige_forsorgere_overgangsstonad?.let {
                    EnsligForsorgerOvergangsstonad(
                        vedtaksPeriodetype = it.vedtaksPeriodetype,
                        harAktivitetsplikt = it.harAktivitetsplikt,
                        utlopsDato = it.utlopsDato,
                        yngsteBarnsFodselsdato = it.yngsteBarnsFødselsdato
                    )
                }
            ),
            huskelapp = opensearchBruker.huskelapp,
            fargekategori = opensearchBruker.fargekategori,
            fargekategoriEnhetId = opensearchBruker.fargekategori_enhetId
        )

        return frontendbruker
    }


    private fun mapTiltakspenger(opensearchBruker: PortefoljebrukerOpensearchModell): Tiltakspenger? {
        val vedtaksdato = opensearchBruker.tiltakspenger_vedtaksdato_tom
        val rettighet = opensearchBruker.tiltakspenger_rettighet
        if (vedtaksdato == null && rettighet == null) {
            return null
        }
        // TODO: 2026-01-05, Sondre
        //  Fjern bruk av "non-null assertion (!!) her". Dette er ei reserveløysing for å gjere kompilatoren
        //  glad etter at PortefoljebrukerOpensearchModell vart skriven om til Kotlin.
        //  PortefoljebrukerOpensearchModell er per dags dato meir "korrekt" mtp. nullability sidan den gjenspeglar
        //  databasen 1-til-1. For å kunne kvitte oss med non-null assertion må vi difor gjere ein av to ting:
        //    * endre PortefoljebrukerFrontendModell til å gjenspegle PortefoljebrukerOpensearchModell
        //    (veilarbportefoljeflatefs bør då også oppdaterast)
        //    * endre database-schema og sette dei relevante kolonnene til "not null" samt validere dataen som
        //    puttast i tabellen, og endre respektive felt i PortefoljebrukerOpensearchModell til å ikkje vere nullable
        val rettighetTekst = TiltakspengerRettighet.tilFrontendtekst(rettighet!!)

        return Tiltakspenger(
            vedtaksdatoTilOgMed = opensearchBruker.tiltakspenger_vedtaksdato_tom!!,
            rettighet = rettighetTekst
        )
    }

    private fun mapAapKelvin(opensearchBruker: PortefoljebrukerOpensearchModell): AapKelvin? {
        val vedtaksdato = opensearchBruker.aap_kelvin_tom_vedtaksdato
        val rettighet = opensearchBruker.aap_kelvin_rettighetstype
        if (vedtaksdato == null && rettighet == null) {
            return null
        }
        // TODO: 2026-01-05, Sondre
        //  Fjern bruk av "non-null assertion (!!) her". Dette er ei reserveløysing for å gjere kompilatoren
        //  glad etter at PortefoljebrukerOpensearchModell vart skriven om til Kotlin.
        //  PortefoljebrukerOpensearchModell er per dags dato meir "korrekt" mtp. nullability sidan den gjenspeglar
        //  databasen 1-til-1. For å kunne kvitte oss med non-null assertion må vi difor gjere ein av to ting:
        //    * endre PortefoljebrukerFrontendModell til å gjenspegle PortefoljebrukerOpensearchModell
        //    (veilarbportefoljeflatefs bør då også oppdaterast)
        //    * endre database-schema og sette dei relevante kolonnene til "not null" samt validere dataen som
        //    puttast i tabellen, og endre respektive felt i PortefoljebrukerOpensearchModell til å ikkje vere nullable
        val rettighetTekst = AapRettighetstype.tilFrontendtekst(rettighet!!)

        return AapKelvin(
            vedtaksdatoTilOgMed = vedtaksdato!!,
            rettighetstype = rettighetTekst
        )
    }

    private fun mapMoteMedNavIDag(opensearchBruker: PortefoljebrukerOpensearchModell): MoteMedNavIDag? {
        val harMoteIDag = opensearchBruker.alle_aktiviteter_mote_startdato.let {
            toLocalDateOrNull(it).isEqual(LocalDate.now())
        }

        if (!harMoteIDag) {
            return null
        }

        val erAvtaltMedNav = opensearchBruker.aktivitet_mote_startdato.let {
            toLocalDateOrNull(it).isEqual(LocalDate.now())
        }

        val moteStart = toLocalDateTimeOrNull(opensearchBruker.alle_aktiviteter_mote_startdato)
        val moteSlutt = toLocalDateTimeOrNull(opensearchBruker.alle_aktiviteter_mote_utlopsdato)
        val varighet = ChronoUnit.MINUTES.between(moteStart, moteSlutt).toInt()
        val klokkeslett = String.format("%02d:%02d", moteStart.hour, moteStart.minute)

        return MoteMedNavIDag(
            avtaltMedNav = erAvtaltMedNav,
            klokkeslett = klokkeslett,
            varighetMinutter = varighet
        )
    }

    private fun mapVedtak14a(opensearchBruker: PortefoljebrukerOpensearchModell): Vedtak14aForBruker {
        val vedtak14a = opensearchBruker.gjeldendeVedtak14a

        val gjeldendeVedtak14a = buildIfAnyNotNull(vedtak14a?.innsatsgruppe, vedtak14a?.fattetDato) {
            Vedtak14aForBruker.GjeldendeVedtak14a(
                // TODO: 2026-01-05, Sondre
                //  Fjern bruk av "non-null assertion (!!) her". Dette er ei reserveløysing for å gjere kompilatoren
                //  glad etter at PortefoljebrukerOpensearchModell vart skriven om til Kotlin.
                //  PortefoljebrukerOpensearchModell er per dags dato meir "korrekt" mtp. nullability sidan den gjenspeglar
                //  databasen 1-til-1. For å kunne kvitte oss med non-null assertion må vi difor gjere ein av to ting:
                //    * endre PortefoljebrukerFrontendModell til å gjenspegle PortefoljebrukerOpensearchModell
                //    (veilarbportefoljeflatefs bør då også oppdaterast)
                //    * endre database-schema og sette dei relevante kolonnene til "not null" samt validere dataen som
                //    puttast i tabellen, og endre respektive felt i PortefoljebrukerOpensearchModell til å ikkje vere nullable
                innsatsgruppe = opensearchBruker.gjeldendeVedtak14a?.innsatsgruppe!!,
                hovedmal = opensearchBruker.gjeldendeVedtak14a?.hovedmal,
                fattetDato = fromZonedDateTimeToLocalDateOrNull(opensearchBruker.gjeldendeVedtak14a?.fattetDato)
            )
        }

        val utkast14a = buildIfAnyNotNull(
            opensearchBruker.utkast_14a_status,
            opensearchBruker.utkast_14a_status_endret,
            opensearchBruker.utkast_14a_ansvarlig_veileder
        ) {
            Vedtak14aForBruker.Utkast14a(
                // TODO: 2026-01-05, Sondre
                //  Fjern bruk av "non-null assertion (!!) her". Dette er ei reserveløysing for å gjere kompilatoren
                //  glad etter at PortefoljebrukerOpensearchModell vart skriven om til Kotlin.
                //  PortefoljebrukerOpensearchModell er per dags dato meir "korrekt" mtp. nullability sidan den gjenspeglar
                //  databasen 1-til-1. For å kunne kvitte oss med non-null assertion må vi difor gjere ein av to ting:
                //    * endre PortefoljebrukerFrontendModell til å gjenspegle PortefoljebrukerOpensearchModell
                //    (veilarbportefoljeflatefs bør då også oppdaterast)
                //    * endre database-schema og sette dei relevante kolonnene til "not null" samt validere dataen som
                //    puttast i tabellen, og endre respektive felt i PortefoljebrukerOpensearchModell til å ikkje vere nullable
                status = opensearchBruker.utkast_14a_status!!,
                dagerSidenStatusEndretSeg = lagDagerSidenTekst(opensearchBruker.utkast_14a_status_endret!!),
                ansvarligVeileder = opensearchBruker.utkast_14a_ansvarlig_veileder!!
            )
        }

        return Vedtak14aForBruker(
            gjeldendeVedtak14a = gjeldendeVedtak14a,
            utkast14a = utkast14a
        )
    }

    inline fun <T> buildIfAnyNotNull(vararg fields: Any?, builder: () -> T): T? =
        if (fields.any { it != null }) builder() else null

    private fun lagDagerSidenTekst(utcDato: String): String {
        val parsed = fromIsoUtcToLocalDateOrNull(utcDato)
        val dager = ChronoUnit.DAYS.between(parsed, LocalDate.now())

        return when (dager) {
            0L -> "I dag"
            1L -> "1 dag siden"
            else -> "$dager dager siden"
        }
    }

    // Vi sender kun én hendelse til frontend så logikken der blir enklere, og fordi vi kun kan ha ett av hendelsesfilterne valgt av gangen
    private fun mapHendelserBasertPåFiltervalg(
        opensearchBruker: PortefoljebrukerOpensearchModell,
        filtervalg: Filtervalg?
    ): HendelseInnhold? {
        // TODO: 2026-01-05, Sondre
        //  Fjern bruk av "non-null assertion (!!) her". Dette er ei reserveløysing for å gjere kompilatoren
        //  glad etter at PortefoljebrukerOpensearchModell vart skriven om til Kotlin.
        //  PortefoljebrukerOpensearchModell er per dags dato meir "korrekt" mtp. nullability sidan den gjenspeglar
        //  databasen 1-til-1. For å kunne kvitte oss med non-null assertion må vi difor gjere ein av to ting:
        //    * endre PortefoljebrukerFrontendModell til å gjenspegle PortefoljebrukerOpensearchModell
        //    (veilarbportefoljeflatefs bør då også oppdaterast)
        //    * endre database-schema og sette dei relevante kolonnene til "not null" samt validere dataen som
        //    puttast i tabellen, og endre respektive felt i PortefoljebrukerOpensearchModell til å ikkje vere nullable
        if (filtervalg?.ferdigfilterListe == null) return null
        if (filtervalg.ferdigfilterListe.contains(Brukerstatus.UTGATTE_VARSEL)) {
            val innhold = opensearchBruker.hendelser!![Kategori.UTGATT_VARSEL]
            return mapHendelseTilFrontendModell(innhold)
        } else if (filtervalg.ferdigfilterListe.contains(Brukerstatus.UDELT_SAMTALEREFERAT)) {
            val innhold = opensearchBruker.hendelser!![Kategori.UDELT_SAMTALEREFERAT]
            return mapHendelseTilFrontendModell(innhold)
        } else {
            return null
        }
    }

    private fun mapHendelseTilFrontendModell(
        hendelseOpensearch: Hendelse.HendelseInnhold?
    ): HendelseInnhold {
        return HendelseInnhold(
            beskrivelse = hendelseOpensearch?.beskrivelse,
            dato = fromZonedDateTimeToLocalDateOrNull(hendelseOpensearch?.dato),
            lenke = hendelseOpensearch?.lenke
        )
    }

    private fun mapSisteEndringerAvBrukerBasertPåFiltervalg(
        opensearchBruker: PortefoljebrukerOpensearchModell,
        filtervalg: Filtervalg?
    ): SisteEndringAvBruker? {
        val opensearchSisteEndringer = opensearchBruker.siste_endringer
        if (filtervalg == null || !filtervalg.harSisteEndringFilter() || opensearchSisteEndringer.isNullOrEmpty()) return null

        //NB antar at her kan man kun få en, bør endre filteret til å være en enkel verdi istedenfor liste
        val valgtFilterSisteEndringKategori = filtervalg.sisteEndringKategori.first()
        val endringsdataForValgtFilter = opensearchSisteEndringer[valgtFilterSisteEndringKategori] ?: return null

        return SisteEndringAvBruker(
            kategori = valgtFilterSisteEndringKategori,
            tidspunkt = toLocalDateOrNull(endringsdataForValgtFilter.tidspunkt),
            aktivitetId = endringsdataForValgtFilter.aktivtetId
        )
    }

    private fun mapAktiviteter(opensearchBruker: PortefoljebrukerOpensearchModell): MutableMap<String, Timestamp> {
        val aktiviteter = mutableMapOf<String, Timestamp>()

        listOf(
            "tiltak" to opensearchBruker.aktivitet_tiltak_utlopsdato,
            "behandling" to opensearchBruker.aktivitet_behandling_utlopsdato,
            "sokeavtale" to opensearchBruker.aktivitet_sokeavtale_utlopsdato,
            "stilling" to opensearchBruker.aktivitet_stilling_utlopsdato,
            "ijobb" to opensearchBruker.aktivitet_ijobb_utlopsdato,
            "egen" to opensearchBruker.aktivitet_egen_utlopsdato,
            "gruppeaktivitet" to opensearchBruker.aktivitet_gruppeaktivitet_utlopsdato,
            "mote" to opensearchBruker.aktivitet_mote_utlopsdato,
            "utdanningaktivitet" to opensearchBruker.aktivitet_utdanningaktivitet_utlopsdato
        ).forEach { (aktivitetstype, dato) ->
            val utlopsdato = dateToTimestamp(dato)
            if (utlopsdato != null && !isFarInTheFutureDate(utlopsdato)) {
                aktiviteter[aktivitetstype] = utlopsdato
            }
        }
        return aktiviteter
    }

    private fun mapNesteUtlopsdatoForAlleAktiviteter(
        opensearchBruker: PortefoljebrukerOpensearchModell
    ): LocalDate? {
        val aktiviteter = mapAktiviteter(opensearchBruker)
        return aktiviteter.values.minOfOrNull { toLocalDateOrNull(it) }
    }

    private fun mapNesteUtlopsdatoForAktivitetBasertPåFiltervalg(
        opensearchBruker: PortefoljebrukerOpensearchModell,
        filtervalg: Filtervalg?
    ): LocalDate? {
        if (filtervalg == null) return null

        val aktiviteter = mapAktiviteter(opensearchBruker)
        var nesteUtopsdatoForenkletFilter: LocalDate? = null
        var nesteUlopsdatoAvansertFilter: LocalDate? = null

        when {
            filtervalg.harAktiviteterForenklet() -> {
                val aktivitetDatoerBasertPaFiltervalg =
                    filtervalg.aktiviteterForenklet?.mapNotNull { navn ->
                        aktiviteter[navn.lowercase()]
                    }

                nesteUtopsdatoForenkletFilter = aktivitetDatoerBasertPaFiltervalg?.minOfOrNull { toLocalDateOrNull(it) }
            }

            filtervalg.tiltakstyper.isNotEmpty() ->
                nesteUtopsdatoForenkletFilter = toLocalDateOrNull(aktiviteter["tiltak"])
        }

        if (filtervalg.harAktivitetFilter()) {
            val aktivitetDatoerBaserPaAvansertFiltervalg =
                filtervalg.aktiviteter?.mapNotNull { (navn, valg) ->
                    if (valg == AktivitetFiltervalg.JA) {
                        aktiviteter[navn.lowercase()]
                    } else {
                        null
                    }
                }

            nesteUlopsdatoAvansertFilter =
                aktivitetDatoerBaserPaAvansertFiltervalg?.minOfOrNull { toLocalDateOrNull(it) }
        }

        val nesteUlopsDato = listOfNotNull(nesteUtopsdatoForenkletFilter, nesteUlopsdatoAvansertFilter).minOrNull()
        return nesteUlopsDato
    }

}
