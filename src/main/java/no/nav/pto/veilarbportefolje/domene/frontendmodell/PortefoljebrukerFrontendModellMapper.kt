package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.filtervalg.Brukerstatus
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import no.nav.pto.veilarbportefolje.util.DateUtils.*
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils.vurderingsBehov
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
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


        var frontendbruker = PortefoljebrukerFrontendModell(
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
            aktoerid = opensearchBruker.aktoer_id,
            fornavn = opensearchBruker.fornavn,
            etternavn = opensearchBruker.etternavn,
            barnUnder18AarData = opensearchBruker.barn_under_18_aar,
            tolkebehov = Tolkebehov.of(
                opensearchBruker.talespraaktolk,
                opensearchBruker.tegnspraaktolk,
                opensearchBruker.tolkBehovSistOppdatert
            ),
            foedeland = opensearchBruker.foedelandFulltNavn,
            hovedStatsborgerskap = opensearchBruker.hovedStatsborgerskap?.let {
                StatsborgerskapForBruker(
                    statsborgerskap = it.statsborgerskap,
                    gyldigFra = it.gyldigFra
                )
            },
            geografiskBosted = GeografiskBostedForBruker(
                bostedKommune = opensearchBruker.kommunenummer,
                bostedBydel = opensearchBruker.bydelsnummer,
                bostedKommuneUkjentEllerUtland = bostedKommuneUkjentEllerUtland,
                bostedSistOppdatert = opensearchBruker.bostedSistOppdatert
            ),
            avvik14aVedtak = opensearchBruker.avvik14aVedtak,

            vedtak14a = mapVedtak14a(opensearchBruker),
            gjeldendeVedtak14a = opensearchBruker.gjeldendeVedtak14a,
            utkast14a = Utkast14a(
                opensearchBruker.utkast_14a_status,
                toLocalDateTimeOrNull(opensearchBruker.utkast_14a_status_endret),
                opensearchBruker.utkast_14a_ansvarlig_veileder
            ),

            oppfolgingStartdato = fromIsoUtcToLocalDateOrNull(opensearchBruker.oppfolging_startdato),

            veilederId = opensearchBruker.veileder_id,
            tildeltTidspunkt = fromLocalDateTimeToLocalDateOrNull(opensearchBruker.tildelt_tidspunkt),
            utdanningOgSituasjonSistEndret = opensearchBruker.utdanning_og_situasjon_sist_endret,
            nyesteUtlopteAktivitet = fromIsoUtcToLocalDateOrNull(opensearchBruker.nyesteutlopteaktivitet),
            aktivitetStart = fromIsoUtcToLocalDateOrNull(opensearchBruker.aktivitet_start),
            nesteAktivitetStart = fromIsoUtcToLocalDateOrNull(opensearchBruker.neste_aktivitet_start),
            forrigeAktivitetStart = fromIsoUtcToLocalDateOrNull(opensearchBruker.forrige_aktivitet_start),
            moteStartTid = toLocalDateTimeOrNull(opensearchBruker.aktivitet_mote_startdato),
            alleMoterStartTid = toLocalDateTimeOrNull(opensearchBruker.alle_aktiviteter_mote_startdato),
            alleMoterSluttTid = toLocalDateTimeOrNull(opensearchBruker.alle_aktiviteter_mote_utlopsdato),

            aktiviteter = mutableMapOf(),
            nesteUtlopsdatoAktivitet = null,
            sisteEndringKategori = "",
            sisteEndringTidspunkt = null,
            sisteEndringAktivitetId = null,

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
                aap = AapKelvinForBruker.of(
                    opensearchBruker.aap_kelvin_tom_vedtaksdato,
                    opensearchBruker.aap_kelvin_rettighetstype
                ),
                tiltakspenger = TiltakspengerForBruker.of(
                    opensearchBruker.tiltakspenger_vedtaksdato_tom,
                    opensearchBruker.tiltakspenger_rettighet
                ),
                ensligeForsorgereOvergangsstonad = EnsligeForsorgereOvergangsstonadFrontend.of(
                    opensearchBruker.enslige_forsorgere_overgangsstonad
                ),
            ),
            meldingerVenterPaSvar = MeldingerVenterPaSvar(
                datoMeldingVenterPaNav = fromIsoUtcToLocalDateOrNull(opensearchBruker.venterpasvarfranav),
                datoMeldingVenterPaBruker = fromIsoUtcToLocalDateOrNull(opensearchBruker.venterpasvarfrabruker),
            ),

            egenAnsatt = opensearchBruker.egen_ansatt,
            skjermetTil = fromLocalDateTimeToLocalDateOrNull(opensearchBruker.skjermet_til),
            nesteSvarfristCvStillingFraNav = opensearchBruker.neste_svarfrist_stilling_fra_nav,
            huskelapp = opensearchBruker.huskelapp,
            fargekategori = opensearchBruker.fargekategori,
            fargekategoriEnhetId = opensearchBruker.fargekategori_enhetId,
            tiltakshendelse = TiltakshendelseForBruker.of(opensearchBruker.tiltakshendelse),
            hendelse = mapHendelserBasertPåFiltervalg(opensearchBruker, filtervalg),

            ).apply {
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
            ).forEach { (type, dato) ->
                addAvtaltAktivitetUtlopsdato(
                    this.aktiviteter,
                    type,
                    dateToTimestamp(dato)
                )
            }
            if (filtervalg != null) {
                mapFelterBasertPåFiltervalg(this, opensearchBruker, filtervalg)
            }

        }

        return frontendbruker
    }

    private fun mapVedtak14a(opensearchBruker: PortefoljebrukerOpensearchModell): Vedtak14aForBruker {
        val vedtak14a = opensearchBruker.gjeldendeVedtak14a

        val gjeldendeVedtak14a = buildIfAnyNotNull(vedtak14a?.innsatsgruppe, vedtak14a?.fattetDato) {
            Vedtak14aForBruker.GjeldendeVedtak14a(
                innsatsgruppe = opensearchBruker.gjeldendeVedtak14a.innsatsgruppe,
                hovedmal = opensearchBruker.gjeldendeVedtak14a.hovedmal,
                fattetDato = fromZonedDateTimeToLocalDateOrNull(opensearchBruker.gjeldendeVedtak14a.fattetDato)
            )
        }

        val utkast14a = buildIfAnyNotNull(
            opensearchBruker.utkast_14a_status,
            opensearchBruker.utkast_14a_status_endret,
            opensearchBruker.utkast_14a_ansvarlig_veileder
        ) {
            Vedtak14aForBruker.Utkast14a(
                status = opensearchBruker.utkast_14a_status,
                dagerSidenStatusEndretSeg = lagDagerSidenTekst(opensearchBruker.utkast_14a_status_endret),
                ansvarligVeileder = opensearchBruker.utkast_14a_ansvarlig_veileder
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
        if (filtervalg?.ferdigfilterListe == null) return null
        if (filtervalg.ferdigfilterListe.contains(Brukerstatus.UTGATTE_VARSEL)) {
            val innhold = opensearchBruker.hendelser[Kategori.UTGATT_VARSEL]
            return mapHendelseTilFrontendModell(innhold)
        } else if (filtervalg.ferdigfilterListe.contains(Brukerstatus.UDELT_SAMTALEREFERAT)) {
            val innhold = opensearchBruker.hendelser[Kategori.UDELT_SAMTALEREFERAT]
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

    private fun mapFelterBasertPåFiltervalg(
        frontendBruker: PortefoljebrukerFrontendModell,
        opensearchBruker: PortefoljebrukerOpensearchModell,
        filtervalg: Filtervalg
    ) {
        when {
            filtervalg.harAktiviteterForenklet() ->
                kalkulerNesteUtlopsdatoAvValgtAktivitetForenklet(frontendBruker, filtervalg.aktiviteterForenklet)

            filtervalg.tiltakstyper.isNotEmpty() ->
                kalkulerNesteUtlopsdatoAvValgtTiltakstype(frontendBruker)
        }

        if (filtervalg.harAktivitetFilter()) {
            kalkulerNesteUtlopsdatoAvValgtAktivitetAvansert(frontendBruker, filtervalg.aktiviteter)
        }

        if (filtervalg.harSisteEndringFilter()) {
            kalkulerSisteEndring(
                frontendBruker,
                opensearchBruker.siste_endringer,
                filtervalg.sisteEndringKategori
            )
        }
    }

    private fun kalkulerNesteUtlopsdatoAvValgtAktivitetForenklet(
        frontendbruker: PortefoljebrukerFrontendModell,
        filtervalgAktiviteterForenklet: List<String>?
    ) {
        filtervalgAktiviteterForenklet?.forEach { navn ->
            frontendbruker.nesteUtlopsdatoAktivitet = nesteUtlopsdatoAktivitet(
                frontendbruker.aktiviteter[navn.lowercase()],
                frontendbruker.nesteUtlopsdatoAktivitet
            )
        }
    }

    private fun kalkulerNesteUtlopsdatoAvValgtTiltakstype(frontendbruker: PortefoljebrukerFrontendModell) {
        frontendbruker.nesteUtlopsdatoAktivitet = nesteUtlopsdatoAktivitet(
            frontendbruker.aktiviteter["tiltak"],
            frontendbruker.nesteUtlopsdatoAktivitet
        )
    }

    private fun kalkulerNesteUtlopsdatoAvValgtAktivitetAvansert(
        frontendbruker: PortefoljebrukerFrontendModell,
        aktiviteterAvansert: Map<String, AktivitetFiltervalg>
    ) {
        aktiviteterAvansert.forEach { (navn, valg) ->
            if (valg == AktivitetFiltervalg.JA) {
                frontendbruker.nesteUtlopsdatoAktivitet = nesteUtlopsdatoAktivitet(
                    frontendbruker.aktiviteter[navn.lowercase()],
                    frontendbruker.nesteUtlopsdatoAktivitet
                )
            }
        }
    }

    private fun kalkulerSisteEndring(
        frontendbruker: PortefoljebrukerFrontendModell,
        sisteEndringer: Map<String, Endring>?,
        kategorier: List<String>
    ) {
        if (sisteEndringer == null) return

        kategorier.forEach { kategori ->
            if (erNyesteKategori(sisteEndringer, kategori, frontendbruker.sisteEndringTidspunkt)) {
                val endring = sisteEndringer[kategori]
                frontendbruker.sisteEndringKategori = kategori
                frontendbruker.sisteEndringTidspunkt = toLocalDateTimeOrNull(endring?.tidspunkt)
                frontendbruker.sisteEndringAktivitetId = endring?.aktivtetId
            }
        }
    }

    private fun erNyesteKategori(
        sisteEndringer: Map<String, Endring>,
        kategori: String,
        sisteEndringTidspunkt: LocalDateTime?
    ): Boolean {
        val endring = sisteEndringer[kategori] ?: return false
        val tidspunkt = toLocalDateTimeOrNull(endring.tidspunkt)
        return sisteEndringTidspunkt == null || (tidspunkt != null && tidspunkt.isAfter(sisteEndringTidspunkt))
    }

    private fun addAvtaltAktivitetUtlopsdato(
        aktiviteter: MutableMap<String, Timestamp>,
        type: String,
        utlopsdato: Timestamp?
    ) {
        if (utlopsdato == null || isFarInTheFutureDate(utlopsdato)) return
        aktiviteter[type] = utlopsdato
    }

    private fun nesteUtlopsdatoAktivitet(
        aktivitetUtlopsdato: Timestamp?,
        comp: LocalDateTime?
    ): LocalDateTime? {
        val aktivitetDato = aktivitetUtlopsdato?.toLocalDateTime() ?: return comp
        return if (comp == null || comp.isAfter(aktivitetDato)) aktivitetDato else comp
    }

}
