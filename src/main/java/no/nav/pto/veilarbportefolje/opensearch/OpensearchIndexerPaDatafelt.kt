package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoekerEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ProfileringEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.mapTilUtdanning
import no.nav.pto.veilarbportefolje.dialog.DialogdataDto
import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse.HendelseInnhold
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringDTO
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.opensearch.OpenSearchException
import org.opensearch.action.delete.DeleteRequest
import org.opensearch.action.update.UpdateRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.common.xcontent.XContentFactory
import org.opensearch.core.rest.RestStatus
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.script.Script
import org.opensearch.script.ScriptType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class OpensearchIndexerPaDatafelt(
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val restHighLevelClient: RestHighLevelClient
) {
    fun updateOpplysningerOmArbeidssoeker(
        aktoerId: AktorId,
        opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerEntity
    ) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(
                FeltNavn.ARBEIDSSOEKER_BRUKERS_SITUASJONER,
                opplysningerOmArbeidssoeker.opplysningerOmJobbsituasjon.jobbsituasjon
            )
            .field(FeltNavn.ARBEIDSSOEKER_UTDANNING, mapTilUtdanning(opplysningerOmArbeidssoeker.utdanningNusKode))
            .field(FeltNavn.ARBEIDSSOEKER_UTDANNING_BESTATT, opplysningerOmArbeidssoeker.utdanningBestatt)
            .field(FeltNavn.ARBEIDSSOEKER_UTDANNING_GODKJENT, opplysningerOmArbeidssoeker.utdanningGodkjent)
            .field(
                FeltNavn.ARBEIDSSOEKER_UTDANNING_OG_SITUASJON_SIST_ENDRET,
                DateUtils.toLocalDateOrNull(opplysningerOmArbeidssoeker.sendtInnTidspunkt)
            )
            .endObject()

        update(aktoerId, content, "Oppdater opplysninger om arbeidssøker")
    }

    fun updateProfilering(aktoerId: AktorId, profileringEntity: ProfileringEntity) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.ARBEIDSSOEKER_PROFILERING_RESULTAT, profileringEntity.profileringsresultat)
            .endObject()

        update(aktoerId, content, "Oppdater profileringsresultat")
    }

    fun updateHuskelapp(aktoerId: AktorId, huskelapp: HuskelappForBruker) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(FeltNavn.HUSKELAPP)
            .field(FeltNavn.HUSKELAPP_FRIST, huskelapp.frist)
            .field(FeltNavn.HUSKELAPP_KOMMENTAR, huskelapp.kommentar)
            .field(FeltNavn.HUSKELAPP_ENDRET_AV, huskelapp.endretAv)
            .field(FeltNavn.HUSKELAPP_ENDRET_DATO, huskelapp.endretDato)
            .field(FeltNavn.HUSKELAPP_HUSKELAPP_ID, huskelapp.huskelappId)
            .field(FeltNavn.HUSKELAPP_ENHET_ID, huskelapp.enhetId)
            .endObject()
            .endObject()

        update(aktoerId, content, "Oppretter/redigerer huskelapp")
    }

    fun slettHuskelapp(aktoerId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .nullField(FeltNavn.HUSKELAPP)
            .endObject()

        update(aktoerId, content, "Sletter huskelapp")
    }

    fun updateFargekategori(aktoerId: AktorId, fargekategori: String?, enhetId: String?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.FARGEKATEGORI, fargekategori)
            .field(FeltNavn.FARGEKATEGORI_ENHET_ID, enhetId)
            .endObject()

        update(aktoerId, content, "Oppretter/redigerer fargekategori")
    }

    fun slettFargekategori(aktoerId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .nullField(FeltNavn.FARGEKATEGORI)
            .nullField(FeltNavn.FARGEKATEGORI_ENHET_ID)
            .endObject()

        update(aktoerId, content, "Sletter fargekategori")
    }

    fun updateSisteEndring(dto: SisteEndringDTO) {
        val kategori = dto.kategori.name
        val tidspunkt = DateUtils.toIsoUTC(dto.tidspunkt)

        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(FeltNavn.AKTIVITETER_SISTE_ENDRINGER)
            .startObject(kategori)
            .field(FeltNavn.AKTIVITETER_SISTE_ENDRINGER_TIDSPUNKT, tidspunkt)
            .field(FeltNavn.AKTIVITETER_SISTE_ENDRINGER_AKTIVTETID, dto.aktivtetId)
            .field(FeltNavn.AKTIVITETER_SISTE_ENDRINGER_ER_SETT, "N")
            .endObject()
            .endObject()
            .endObject()
        update(
            dto.aktoerId,
            content,
            "Oppdaterte siste endring med tidspunkt: $tidspunkt"
        )
    }

    fun updateSisteEndring(aktorId: AktorId, kategori: SisteEndringsKategori) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(FeltNavn.AKTIVITETER_SISTE_ENDRINGER)
            .startObject(kategori.name)
            .field(FeltNavn.AKTIVITETER_SISTE_ENDRINGER_ER_SETT, "J")
            .endObject()
            .endObject()
            .endObject()
        update(aktorId, content, "Oppdaterte siste endring, kategori ${kategori.name} er nå sett")
    }

    fun updateHarDeltCv(aktoerId: AktorId, harDeltCv: Boolean) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.CV_HAR_DELT_CV, harDeltCv)
            .endObject()

        update(aktoerId, content, "Har delt cv: $harDeltCv")
    }

    fun updateCvEksistere(aktoerId: AktorId, cvEksistere: Boolean) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.CV_CV_EKSISTERE, cvEksistere)
            .endObject()

        update(aktoerId, content, "CV eksistere: $cvEksistere")
    }

    fun settManuellStatus(aktoerId: AktorId, manuellStatus: String?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.OPPFOLGING_MANUELL_BRUKER, manuellStatus)
            .endObject()

        update(aktoerId, content, "Satt til manuell bruker: $manuellStatus")
    }

    fun oppdaterNyForVeileder(aktoerId: AktorId, nyForVeileder: Boolean) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.OPPFOLGING_NY_FOR_VEILEDER, nyForVeileder)
            .endObject()

        update(aktoerId, content, "Oppdatert ny for veileder: $nyForVeileder")
    }

    fun oppdaterVeileder(aktoerId: AktorId, veilederId: VeilederId, tildeltTidspunkt: ZonedDateTime?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.OPPFOLGING_VEILEDER_ID, veilederId.toString())
            .field(FeltNavn.OPPFOLGING_NY_FOR_VEILEDER, true)
            .field(FeltNavn.OPPFOLGING_TILDELT_TIDSPUNKT, DateUtils.toIsoUTC(tildeltTidspunkt))
            .endObject()

        update(aktoerId, content, "Oppdatert veileder")
    }

    fun updateDialog(melding: DialogdataDto) {
        val venterPaaSvarFraBruker = DateUtils.toIsoUTC(melding.tidspunktEldsteVentende)
        val venterPaaSvarFraNav = DateUtils.toIsoUTC(melding.tidspunktEldsteUbehandlede)

        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.DIALOG_VENTER_PA_SVAR_FRA_BRUKER, venterPaaSvarFraBruker)
            .field(FeltNavn.DIALOG_VENTER_PA_SVAR_FRA_NAV, venterPaaSvarFraNav)
            .endObject()

        update(
            AktorId.of(melding.aktorId),
            content,
            "Oppdatert dialog med venter på svar fra nav: $venterPaaSvarFraNav og venter på svar fra bruker: $venterPaaSvarFraBruker"
        )
    }

    fun updateErSkjermet(aktorId: AktorId, erSkjermet: Boolean) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.EGEN_ANSATT, erSkjermet)
            .endObject()

        update(
            aktorId,
            content,
            "Oppdatert ${FeltNavn.EGEN_ANSATT} $erSkjermet for bruker: $aktorId"
        )
    }

    fun updateSkjermetTil(aktorId: AktorId, skjermetTil: LocalDateTime?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.SKJERMET_TIL, skjermetTil)
            .endObject()

        update(
            aktorId,
            content,
            "Oppdatert ${FeltNavn.SKJERMET_TIL} $skjermetTil for bruker: $aktorId"
        )
    }

    fun updateOvergangsstonad(
        aktorId: AktorId,
        ensligeForsorgerOvergangsstønadTiltakDto: EnsligeForsorgerOvergangsstønadTiltakDto
    ) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(FeltNavn.ENSLIGE_FORSORGERE_OVERGANGSSTONAD)
            .field(
                FeltNavn.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_VEDTAKSPERIODETYPE,
                ensligeForsorgerOvergangsstønadTiltakDto.vedtaksPeriodetypeBeskrivelse
            )
            .field(
                FeltNavn.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_HARAKTIVITETSPLIKT,
                ensligeForsorgerOvergangsstønadTiltakDto.aktivitsplikt
            )
            .field(
                FeltNavn.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO,
                ensligeForsorgerOvergangsstønadTiltakDto.utløpsDato
            )
            .field(
                FeltNavn.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTEBARNSFØDSELSDATO,
                ensligeForsorgerOvergangsstønadTiltakDto.yngsteBarnsFødselsdato
            )
            .endObject()
            .endObject()

        update(aktorId, content, "Update overgangsstønad")
    }

    fun deleteOvergansstonad(aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.ENSLIGE_FORSORGERE_OVERGANGSSTONAD, null as String?)
            .endObject()

        update(aktorId, content, "Fjern overgangsstønad")
    }

    fun updateTiltakshendelse(aktorId: AktorId, tiltakshendelse: Tiltakshendelse) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(FeltNavn.TILTAKSHENDELSE)
            .field(FeltNavn.TILTAKSHENDELSE_ID, tiltakshendelse.id.toString())
            .field(FeltNavn.TILTAKSHENDELSE_LENKE, tiltakshendelse.lenke)
            .field(FeltNavn.TILTAKSHENDELSE_OPPRETTET, tiltakshendelse.opprettet)
            .field(FeltNavn.TILTAKSHENDELSE_TEKST, tiltakshendelse.tekst)
            .field(FeltNavn.TILTAKSHENDELSE_TILTAKSTYPE, tiltakshendelse.tiltakstype)
            .endObject()
            .endObject()

        update(aktorId, content, "Oppdatert tiltakshendelse med id: ${tiltakshendelse.id}")
    }

    fun slettTiltakshendelse(aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .nullField(FeltNavn.TILTAKSHENDELSE)
            .endObject()

        update(aktorId, content, "Slettet tiltakshendelse for aktorId: $aktorId")
    }

    fun updateGjeldendeVedtak14a(gjeldendeVedtak14a: GjeldendeVedtak14a, aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(FeltNavn.GJELDENDE_VEDTAK_14A)
            .field(FeltNavn.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE, gjeldendeVedtak14a.innsatsgruppe)
            .field(FeltNavn.GJELDENDE_VEDTAK_14A_HOVEDMAL, gjeldendeVedtak14a.hovedmal)
            .field(FeltNavn.GJELDENDE_VEDTAK_14A_FATTET_DATO, gjeldendeVedtak14a.fattetDato)
            .endObject()
            .endObject()

        update(aktorId, content, "Oppdaterte gjeldendeVedtak14a for aktorId: $aktorId")
    }

    fun oppdaterHendelse(hendelse: Hendelse, aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(FeltNavn.HENDELSER)
            .startObject(hendelse.kategori.name)
            .field(FeltNavn.HENDELSER_BESKRIVELSE, hendelse.hendelse.beskrivelse)
            .field(FeltNavn.HENDELSER_DATO, hendelse.hendelse.dato)
            .field(FeltNavn.HENDELSER_LENKE, hendelse.hendelse.lenke.toString())
            .field(FeltNavn.HENDELSER_DETALJER, hendelse.hendelse.detaljer)
            .endObject()
            .endObject()
            .endObject()

        update(
            aktorId,
            content,
            "Oppdaterte hendelse med kategori ${hendelse.kategori.name} for aktorId: $aktorId"
        )
    }

    fun slettHendelse(kategori: Kategori, aktorId: AktorId) {
        val kategoriKey = "kategori"
        val params = mapOf(kategoriKey to kategori.name)

        val updateScript = Script(
            ScriptType.INLINE,
            "painless",
            "ctx._source.${FeltNavn.HENDELSER}.remove(params.$kategoriKey)",
            params
        )

        updateWithScript(
            aktorId,
            updateScript,
            "Slettet hendelse med kategori ${kategori.name} for aktorId: $aktorId"
        )
    }

    fun oppdaterAapKelvin(
        aktorId: AktorId,
        harAapKelvin: Boolean,
        tomVedtaksdato: LocalDate?,
        rettighetstype: AapRettighetstype?
    ) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.AAP_KELVIN, harAapKelvin)
            .field(FeltNavn.AAP_KELVIN_TOM_VEDTAKSDATO, tomVedtaksdato)
            .field(FeltNavn.AAP_KELVIN_RETTIGHETSTYPE, rettighetstype)
            .endObject()

        update(aktorId, content, "Oppdatert aap kelvin for aktorId: $aktorId")
    }

    fun slettAapKelvin(aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.AAP_KELVIN, false)
            .nullField(FeltNavn.AAP_KELVIN_TOM_VEDTAKSDATO)
            .nullField(FeltNavn.AAP_KELVIN_RETTIGHETSTYPE)
            .endObject()

        update(aktorId, content, "Slettet aap kelvin for aktorId: $aktorId")
    }

    fun oppdaterTiltakspenger(
        aktorId: AktorId,
        harTiltakspenger: Boolean,
        vedtaksdatoTom: LocalDate?,
        rettighet: TiltakspengerRettighet?
    ) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(FeltNavn.TILTAKSPENGER, harTiltakspenger)
            .field(FeltNavn.TILTAKSPENGER_VEDTAKSDATO_TOM, vedtaksdatoTom)
            .field(FeltNavn.TILTAKSPENGER_RETTIGHET, rettighet)
            .endObject()

        update(aktorId, content, "Oppdatert tiltakspenger for aktorId: $aktorId")
    }

    private fun updateWithScript(aktoerId: AktorId, script: Script?, logInfo: String?) {
        val request = UpdateRequest().script(script)
        executeUpdate(aktoerId, request, logInfo)
    }

    private fun update(aktoerId: AktorId, docContent: XContentBuilder?, logInfo: String?) {
        val request = UpdateRequest().doc(docContent)
        executeUpdate(aktoerId, request, logInfo)
    }

    private fun executeUpdate(aktoerId: AktorId, updateRequest: UpdateRequest, logInfo: String?) {
        if (!oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktoerId)) {
            secureLog.info(
                "Oppdaterte ikke OS for brukere som ikke er under oppfolging, heller ikke for historiske identer: $aktoerId, med info $logInfo"
            )
            return
        }

        updateRequest
            .index(BRUKERINDEKS_ALIAS)
            .id(aktoerId.get())
            .retryOnConflict(6)

        try {
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT)
            secureLog.info("Oppdaterte dokument for bruker $aktoerId med info $logInfo")
        } catch (e: OpenSearchException) {
            if (e.status() == RestStatus.NOT_FOUND) {
                secureLog.warn("Kunne ikke finne dokument for bruker $aktoerId ved oppdatering av indeks")
            } else {
                secureLog.error("Det skjedde en feil ved oppdatering av opensearch for bruker $aktoerId", e)
            }
        }
    }

    private fun delete(aktoerId: AktorId) {
        val deleteRequest = DeleteRequest()
        deleteRequest.index(BRUKERINDEKS_ALIAS)
        deleteRequest.id(aktoerId.get())

        try {
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT)
            secureLog.info("Slettet dokument for $aktoerId")
        } catch (e: OpenSearchException) {
            if (e.status() == RestStatus.NOT_FOUND) {
                secureLog.info("Kunne ikke finne dokument for bruker $aktoerId ved sletting av indeks")
            } else {
                secureLog.error(
                    "Det skjedde en feil ved sletting i opensearch for bruker $aktoerId",
                    e
                )
            }
        }
    }

}

object FeltNavn {
    val ARBEIDSSOEKER_PROFILERING_RESULTAT = PortefoljebrukerOpensearchModell::profilering_resultat.name
    val ARBEIDSSOEKER_BRUKERS_SITUASJONER = PortefoljebrukerOpensearchModell::brukers_situasjoner.name
    val ARBEIDSSOEKER_UTDANNING = PortefoljebrukerOpensearchModell::utdanning.name
    val ARBEIDSSOEKER_UTDANNING_BESTATT = PortefoljebrukerOpensearchModell::utdanning_bestatt.name
    val ARBEIDSSOEKER_UTDANNING_GODKJENT = PortefoljebrukerOpensearchModell::utdanning_godkjent.name
    val ARBEIDSSOEKER_UTDANNING_OG_SITUASJON_SIST_ENDRET =
        PortefoljebrukerOpensearchModell::utdanning_og_situasjon_sist_endret.name

    val HUSKELAPP = PortefoljebrukerOpensearchModell::huskelapp.name
    val HUSKELAPP_FRIST = HuskelappForBruker::frist.name
    val HUSKELAPP_KOMMENTAR = HuskelappForBruker::kommentar.name
    val HUSKELAPP_ENDRET_AV = HuskelappForBruker::endretAv.name
    val HUSKELAPP_ENDRET_DATO = HuskelappForBruker::endretDato.name
    val HUSKELAPP_HUSKELAPP_ID = HuskelappForBruker::huskelappId.name
    val HUSKELAPP_ENHET_ID = HuskelappForBruker::enhetId.name
    val FARGEKATEGORI = PortefoljebrukerOpensearchModell::fargekategori.name
    val FARGEKATEGORI_ENHET_ID = PortefoljebrukerOpensearchModell::fargekategori_enhetId.name

    val AKTIVITETER_SISTE_ENDRINGER = PortefoljebrukerOpensearchModell::siste_endringer.name
    val AKTIVITETER_SISTE_ENDRINGER_ER_SETT =
        "erSett" // Denne har ingen korresponderende property i PortefoljebrukerOpensearchModell
    val AKTIVITETER_SISTE_ENDRINGER_TIDSPUNKT = Endring::tidspunkt.name
    val AKTIVITETER_SISTE_ENDRINGER_AKTIVTETID = Endring::aktivtetId.name

    val CV_HAR_DELT_CV = PortefoljebrukerOpensearchModell::har_delt_cv.name
    val CV_CV_EKSISTERE = PortefoljebrukerOpensearchModell::cv_eksistere.name

    val OPPFOLGING_MANUELL_BRUKER = PortefoljebrukerOpensearchModell::manuell_bruker.name
    val OPPFOLGING_VEILEDER_ID = PortefoljebrukerOpensearchModell::veileder_id.name
    val OPPFOLGING_NY_FOR_VEILEDER = PortefoljebrukerOpensearchModell::ny_for_veileder.name
    val OPPFOLGING_TILDELT_TIDSPUNKT = PortefoljebrukerOpensearchModell::tildelt_tidspunkt.name

    val DIALOG_VENTER_PA_SVAR_FRA_BRUKER = PortefoljebrukerOpensearchModell::venterpasvarfrabruker.name
    val DIALOG_VENTER_PA_SVAR_FRA_NAV = PortefoljebrukerOpensearchModell::venterpasvarfranav.name

    val EGEN_ANSATT = PortefoljebrukerOpensearchModell::egen_ansatt.name
    val SKJERMET_TIL = PortefoljebrukerOpensearchModell::skjermet_til.name

    val ENSLIGE_FORSORGERE_OVERGANGSSTONAD = PortefoljebrukerOpensearchModell::enslige_forsorgere_overgangsstonad.name
    val ENSLIGE_FORSORGERE_OVERGANGSSTONAD_VEDTAKSPERIODETYPE =
        EnsligeForsorgereOvergangsstonad::vedtaksPeriodetype.name
    val ENSLIGE_FORSORGERE_OVERGANGSSTONAD_HARAKTIVITETSPLIKT =
        EnsligeForsorgereOvergangsstonad::harAktivitetsplikt.name
    val ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO = EnsligeForsorgereOvergangsstonad::utlopsDato.name
    val ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTEBARNSFØDSELSDATO =
        EnsligeForsorgereOvergangsstonad::yngsteBarnsFødselsdato.name

    val TILTAKSHENDELSE = PortefoljebrukerOpensearchModell::tiltakshendelse.name
    val TILTAKSHENDELSE_ID = Tiltakshendelse::id.name
    val TILTAKSHENDELSE_LENKE = Tiltakshendelse::lenke.name
    val TILTAKSHENDELSE_OPPRETTET = Tiltakshendelse::opprettet.name
    val TILTAKSHENDELSE_TEKST = Tiltakshendelse::tekst.name
    val TILTAKSHENDELSE_TILTAKSTYPE = Tiltakshendelse::tiltakstype.name

    val GJELDENDE_VEDTAK_14A = PortefoljebrukerOpensearchModell::gjeldendeVedtak14a.name
    val GJELDENDE_VEDTAK_14A_INNSATSGRUPPE = GjeldendeVedtak14a::innsatsgruppe.name
    val GJELDENDE_VEDTAK_14A_HOVEDMAL = GjeldendeVedtak14a::hovedmal.name
    val GJELDENDE_VEDTAK_14A_FATTET_DATO = GjeldendeVedtak14a::fattetDato.name

    val HENDELSER = PortefoljebrukerOpensearchModell::hendelser.name
    val HENDELSER_BESKRIVELSE = HendelseInnhold::beskrivelse.name
    val HENDELSER_DATO = HendelseInnhold::dato.name
    val HENDELSER_LENKE = HendelseInnhold::lenke.name
    val HENDELSER_DETALJER = HendelseInnhold::detaljer.name

    val AAP_KELVIN = PortefoljebrukerOpensearchModell::aap_kelvin.name
    val AAP_KELVIN_TOM_VEDTAKSDATO = PortefoljebrukerOpensearchModell::aap_kelvin_tom_vedtaksdato.name
    val AAP_KELVIN_RETTIGHETSTYPE = PortefoljebrukerOpensearchModell::aap_kelvin_rettighetstype.name

    val TILTAKSPENGER = PortefoljebrukerOpensearchModell::tiltakspenger.name
    val TILTAKSPENGER_VEDTAKSDATO_TOM = PortefoljebrukerOpensearchModell::tiltakspenger_vedtaksdato_tom.name
    val TILTAKSPENGER_RETTIGHET = PortefoljebrukerOpensearchModell::tiltakspenger_rettighet.name
}
