package no.nav.pto.veilarbportefolje.opensearch.domene

import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse

/**
 * Data sendes til OpenSearch på JSON-format hvor hver enkelt key i JSON-strukturen vil mappes til et felt i OpenSearch.
 *
 * Når vi indekserer hele brukere (eksempelvis i [no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer]) populerer
 * vi først en instans av [PortefoljebrukerOpensearchModell] før denne deserialiseres og sendes til OpenSearch i sin helhet.
 * Hver enkelt property i [PortefoljebrukerOpensearchModell] vil med andre ord mappes til et korresponderende felt i OpenSearch.
 *
 * Når vi derimot indekserer deler av brukere (eksempelvis i [no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt])
 * skreddersyr vi JSON-strukturen vha. [org.opensearch.common.xcontent.XContentFactory.jsonBuilder]. Med andre ord sender
 * vi ikke instanser av [PortefoljebrukerOpensearchModell].
 *
 * [DatafeltKeys] har som hensikt å sikre at vi har en definert kobling fra properties i [PortefoljebrukerOpensearchModell]
 * til streng-verdier som brukes for å konstruere JSON. Koblingen er her gjort ved å referere til navnet på property-en i
 * [PortefoljebrukerOpensearchModell] som aksesseres kjøretid vha. reflection.
 *
 * Dersom man gjør endringer, eksempelvis renaming av properties i [PortefoljebrukerOpensearchModell], vil dette reflekteres
 * i [no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt], slik at vi slipper å manuelt oppdatere hardkodede
 * verdier.
 */
object DatafeltKeys {
    object Personalia {}

    object Oppfolging {
        val GJELDENDE_VEDTAK_14A = PortefoljebrukerOpensearchModell::gjeldendeVedtak14a.name
        val GJELDENDE_VEDTAK_14A_INNSATSGRUPPE = GjeldendeVedtak14a::innsatsgruppe.name
        val GJELDENDE_VEDTAK_14A_HOVEDMAL = GjeldendeVedtak14a::hovedmal.name
        val GJELDENDE_VEDTAK_14A_FATTET_DATO = GjeldendeVedtak14a::fattetDato.name
        val MANUELL_BRUKER = PortefoljebrukerOpensearchModell::manuell_bruker.name
        val NY_FOR_VEILEDER = PortefoljebrukerOpensearchModell::ny_for_veileder.name
        val TILDELT_TIDSPUNKT = PortefoljebrukerOpensearchModell::tildelt_tidspunkt.name
        val VEILEDER_ID = PortefoljebrukerOpensearchModell::veileder_id.name
    }

    object Arbeidssoeker {
        val PROFILERING_RESULTAT = PortefoljebrukerOpensearchModell::profilering_resultat.name
        val BRUKERS_SITUASJONER = PortefoljebrukerOpensearchModell::brukers_situasjoner.name
        val UTDANNING = PortefoljebrukerOpensearchModell::utdanning.name
        val UTDANNING_BESTATT = PortefoljebrukerOpensearchModell::utdanning_bestatt.name
        val UTDANNING_GODKJENT = PortefoljebrukerOpensearchModell::utdanning_godkjent.name
        val UTDANNING_OG_SITUASJON_SIST_ENDRET =
            PortefoljebrukerOpensearchModell::utdanning_og_situasjon_sist_endret.name
    }

    object Arbeidsforhold {}

    object Aktiviteter {
        val SISTE_ENDRINGER = PortefoljebrukerOpensearchModell::siste_endringer.name
        val SISTE_ENDRINGER_ER_SETT =
            "erSett" // Denne har ingen korresponderende property i PortefoljebrukerOpensearchModell
        val SISTE_ENDRINGER_TIDSPUNKT = Endring::tidspunkt.name
        val SISTE_ENDRINGER_AKTIVTETID = Endring::aktivtetId.name
    }

    object Ytelser {
        val AAP_KELVIN = PortefoljebrukerOpensearchModell::aap_kelvin.name
        val AAP_KELVIN_TOM_VEDTAKSDATO = PortefoljebrukerOpensearchModell::aap_kelvin_tom_vedtaksdato.name
        val AAP_KELVIN_RETTIGHETSTYPE = PortefoljebrukerOpensearchModell::aap_kelvin_rettighetstype.name
        val ENSLIGE_FORSORGERE_OVERGANGSSTONAD = PortefoljebrukerOpensearchModell::enslige_forsorgere_overgangsstonad.name
        val ENSLIGE_FORSORGERE_OVERGANGSSTONAD_VEDTAKSPERIODETYPE =
            EnsligeForsorgereOvergangsstonad::vedtaksPeriodetype.name
        val ENSLIGE_FORSORGERE_OVERGANGSSTONAD_HAR_AKTIVITETSPLIKT =
            EnsligeForsorgereOvergangsstonad::harAktivitetsplikt.name
        val ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO = EnsligeForsorgereOvergangsstonad::utlopsDato.name
        val `ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTE_BARNS_FØDSELSDATO` =
            EnsligeForsorgereOvergangsstonad::yngsteBarnsFødselsdato.name
        val TILTAKSPENGER = PortefoljebrukerOpensearchModell::tiltakspenger.name
        val TILTAKSPENGER_VEDTAKSDATO_TOM = PortefoljebrukerOpensearchModell::tiltakspenger_vedtaksdato_tom.name
        val TILTAKSPENGER_RETTIGHET = PortefoljebrukerOpensearchModell::tiltakspenger_rettighet.name
    }

    object Dialog {
        val VENTER_PA_SVAR_FRA_BRUKER = PortefoljebrukerOpensearchModell::venterpasvarfrabruker.name
        val VENTER_PA_SVAR_FRA_NAV = PortefoljebrukerOpensearchModell::venterpasvarfranav.name
    }

    object NavAnsatt {
        val EGEN_ANSATT = PortefoljebrukerOpensearchModell::egen_ansatt.name
        val SKJERMET_TIL = PortefoljebrukerOpensearchModell::skjermet_til.name
    }

    object CV {
        val HAR_DELT_CV = PortefoljebrukerOpensearchModell::har_delt_cv.name
        val CV_EKSISTERE = PortefoljebrukerOpensearchModell::cv_eksistere.name
    }

    object Annet {
        val FARGEKATEGORI = PortefoljebrukerOpensearchModell::fargekategori.name
        val FARGEKATEGORI_ENHET_ID = PortefoljebrukerOpensearchModell::fargekategori_enhetId.name
        val HUSKELAPP = PortefoljebrukerOpensearchModell::huskelapp.name
        val HUSKELAPP_FRIST = HuskelappForBruker::frist.name
        val HUSKELAPP_KOMMENTAR = HuskelappForBruker::kommentar.name
        val HUSKELAPP_ENDRET_AV = HuskelappForBruker::endretAv.name
        val HUSKELAPP_ENDRET_DATO = HuskelappForBruker::endretDato.name
        val HUSKELAPP_HUSKELAPP_ID = HuskelappForBruker::huskelappId.name
        val HUSKELAPP_ENHET_ID = HuskelappForBruker::enhetId.name
        val TILTAKSHENDELSE = PortefoljebrukerOpensearchModell::tiltakshendelse.name
        val TILTAKSHENDELSE_ID = Tiltakshendelse::id.name
        val TILTAKSHENDELSE_LENKE = Tiltakshendelse::lenke.name
        val TILTAKSHENDELSE_OPPRETTET = Tiltakshendelse::opprettet.name
        val TILTAKSHENDELSE_TEKST = Tiltakshendelse::tekst.name
        val TILTAKSHENDELSE_TILTAKSTYPE = Tiltakshendelse::tiltakstype.name
        val HENDELSER = PortefoljebrukerOpensearchModell::hendelser.name
        val HENDELSER_BESKRIVELSE = Hendelse.HendelseInnhold::beskrivelse.name
        val HENDELSER_DATO = Hendelse.HendelseInnhold::dato.name
        val HENDELSER_LENKE = Hendelse.HendelseInnhold::lenke.name
        val HENDELSER_DETALJER = Hendelse.HendelseInnhold::detaljer.name
    }
}
