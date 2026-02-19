package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoekerEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ProfileringEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.mapTilUtdanning
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.dialog.DialogdataDto
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori
import no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS
import no.nav.pto.veilarbportefolje.opensearch.domene.DatafeltKeys
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringDTO
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.opensearch.OpenSearchException
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
                DatafeltKeys.Arbeidssoeker.BRUKERS_SITUASJONER,
                opplysningerOmArbeidssoeker.opplysningerOmJobbsituasjon.jobbsituasjon
            )
            .field(
                DatafeltKeys.Arbeidssoeker.UTDANNING,
                mapTilUtdanning(opplysningerOmArbeidssoeker.utdanningNusKode)
            )
            .field(
                DatafeltKeys.Arbeidssoeker.UTDANNING_BESTATT,
                opplysningerOmArbeidssoeker.utdanningBestatt
            )
            .field(
                DatafeltKeys.Arbeidssoeker.UTDANNING_GODKJENT,
                opplysningerOmArbeidssoeker.utdanningGodkjent
            )
            .field(
                DatafeltKeys.Arbeidssoeker.UTDANNING_OG_SITUASJON_SIST_ENDRET,
                DateUtils.toLocalDateOrNull(opplysningerOmArbeidssoeker.sendtInnTidspunkt)
            )
            .endObject()

        update(aktoerId, content, "Oppdater opplysninger om arbeidssøker")
    }

    fun updateProfilering(aktoerId: AktorId, profileringEntity: ProfileringEntity) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(
                DatafeltKeys.Arbeidssoeker.PROFILERING_RESULTAT,
                profileringEntity.profileringsresultat
            )
            .endObject()

        update(aktoerId, content, "Oppdater profileringsresultat")
    }

    fun updateHuskelapp(aktoerId: AktorId, huskelapp: HuskelappForBruker) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(DatafeltKeys.Annet.HUSKELAPP)
            .field(DatafeltKeys.Annet.HUSKELAPP_FRIST, huskelapp.frist)
            .field(DatafeltKeys.Annet.HUSKELAPP_KOMMENTAR, huskelapp.kommentar)
            .field(DatafeltKeys.Annet.HUSKELAPP_ENDRET_AV, huskelapp.endretAv)
            .field(DatafeltKeys.Annet.HUSKELAPP_ENDRET_DATO, huskelapp.endretDato)
            .field(DatafeltKeys.Annet.HUSKELAPP_HUSKELAPP_ID, huskelapp.huskelappId)
            .field(DatafeltKeys.Annet.HUSKELAPP_ENHET_ID, huskelapp.enhetId)
            .endObject()
            .endObject()

        update(aktoerId, content, "Oppretter/redigerer huskelapp")
    }

    fun slettHuskelapp(aktoerId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .nullField(DatafeltKeys.Annet.HUSKELAPP)
            .endObject()

        update(aktoerId, content, "Sletter huskelapp")
    }

    fun slettCV(aktoerId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .nullField(DatafeltKeys.CV.CV_EKSISTERE)
            .endObject()

        update(aktoerId, content, "Sletter CV eksisterer")
    }

    fun updateFargekategori(aktoerId: AktorId, fargekategori: String?, enhetId: String?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.Annet.FARGEKATEGORI, fargekategori)
            .field(DatafeltKeys.Annet.FARGEKATEGORI_ENHET_ID, enhetId)
            .endObject()

        update(aktoerId, content, "Oppretter/redigerer fargekategori")
    }

    fun slettFargekategori(aktoerId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .nullField(DatafeltKeys.Annet.FARGEKATEGORI)
            .nullField(DatafeltKeys.Annet.FARGEKATEGORI_ENHET_ID)
            .endObject()

        update(aktoerId, content, "Sletter fargekategori")
    }

    fun updateSisteEndring(dto: SisteEndringDTO) {
        val kategori = dto.kategori.name
        val tidspunkt = DateUtils.toIsoUTC(dto.tidspunkt)

        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(DatafeltKeys.Aktiviteter.SISTE_ENDRINGER)
            .startObject(kategori)
            .field(DatafeltKeys.Aktiviteter.SISTE_ENDRINGER_TIDSPUNKT, tidspunkt)
            .field(DatafeltKeys.Aktiviteter.SISTE_ENDRINGER_AKTIVITET_ID, dto.aktivtetId)
            .field(DatafeltKeys.Aktiviteter.SISTE_ENDRINGER_ER_SETT, "N")
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
            .startObject(DatafeltKeys.Aktiviteter.SISTE_ENDRINGER)
            .startObject(kategori.name)
            .field(DatafeltKeys.Aktiviteter.SISTE_ENDRINGER_ER_SETT, "J")
            .endObject()
            .endObject()
            .endObject()
        update(aktorId, content, "Oppdaterte siste endring, kategori ${kategori.name} er nå sett")
    }

    fun updateCvEksistere(aktoerId: AktorId, cvEksistere: Boolean) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.CV.CV_EKSISTERE, cvEksistere)
            .endObject()

        update(aktoerId, content, "CV eksistere: $cvEksistere")
    }

    fun settManuellStatus(aktoerId: AktorId, manuellStatus: String?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.Oppfolging.MANUELL_BRUKER, manuellStatus)
            .endObject()

        update(aktoerId, content, "Satt til manuell bruker: $manuellStatus")
    }

    fun oppdaterNyForVeileder(aktoerId: AktorId, nyForVeileder: Boolean) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.Oppfolging.NY_FOR_VEILEDER, nyForVeileder)
            .endObject()

        update(aktoerId, content, "Oppdatert ny for veileder: $nyForVeileder")
    }

    fun oppdaterVeileder(aktoerId: AktorId, veilederId: VeilederId, tildeltTidspunkt: ZonedDateTime?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.Oppfolging.VEILEDER_ID, veilederId.toString())
            .field(DatafeltKeys.Oppfolging.NY_FOR_VEILEDER, true)
            .field(DatafeltKeys.Oppfolging.TILDELT_TIDSPUNKT, DateUtils.toIsoUTC(tildeltTidspunkt))
            .endObject()

        update(aktoerId, content, "Oppdatert veileder")
    }

    fun updateDialog(melding: DialogdataDto) {
        val venterPaaSvarFraBruker = DateUtils.toIsoUTC(melding.tidspunktEldsteVentende)
        val venterPaaSvarFraNav = DateUtils.toIsoUTC(melding.tidspunktEldsteUbehandlede)

        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.Dialog.VENTER_PA_SVAR_FRA_BRUKER, venterPaaSvarFraBruker)
            .field(DatafeltKeys.Dialog.VENTER_PA_SVAR_FRA_NAV, venterPaaSvarFraNav)
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
            .field(DatafeltKeys.NavAnsatt.EGEN_ANSATT, erSkjermet)
            .endObject()

        update(
            aktorId,
            content,
            "Oppdatert ${DatafeltKeys.NavAnsatt.EGEN_ANSATT} $erSkjermet for bruker: $aktorId"
        )
    }

    fun updateSkjermetTil(aktorId: AktorId, skjermetTil: LocalDateTime?) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.NavAnsatt.SKJERMET_TIL, skjermetTil)
            .endObject()

        update(
            aktorId,
            content,
            "Oppdatert ${DatafeltKeys.NavAnsatt.SKJERMET_TIL} $skjermetTil for bruker: $aktorId"
        )
    }

    fun updateOvergangsstonad(
        aktorId: AktorId,
        ensligeForsorgerOvergangsstønadTiltakDto: EnsligeForsorgerOvergangsstønadTiltakDto
    ) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD)
            .field(
                DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_VEDTAKSPERIODETYPE,
                ensligeForsorgerOvergangsstønadTiltakDto.vedtaksPeriodetypeBeskrivelse
            )
            .field(
                DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_HAR_AKTIVITETSPLIKT,
                ensligeForsorgerOvergangsstønadTiltakDto.aktivitsplikt
            )
            .field(
                DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD_UTLOPSDATO,
                ensligeForsorgerOvergangsstønadTiltakDto.utløpsDato
            )
            .field(
                DatafeltKeys.Ytelser.`ENSLIGE_FORSORGERE_OVERGANGSSTONAD_YNGSTE_BARNS_FØDSELSDATO`,
                ensligeForsorgerOvergangsstønadTiltakDto.yngsteBarnsFødselsdato
            )
            .endObject()
            .endObject()

        update(aktorId, content, "Update overgangsstønad")
    }

    fun deleteOvergansstonad(aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.Ytelser.ENSLIGE_FORSORGERE_OVERGANGSSTONAD, null as String?)
            .endObject()

        update(aktorId, content, "Fjern overgangsstønad")
    }

    fun updateTiltakshendelse(aktorId: AktorId, tiltakshendelse: Tiltakshendelse) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(DatafeltKeys.Annet.TILTAKSHENDELSE)
            .field(DatafeltKeys.Annet.TILTAKSHENDELSE_ID, tiltakshendelse.id.toString())
            .field(DatafeltKeys.Annet.TILTAKSHENDELSE_LENKE, tiltakshendelse.lenke)
            .field(DatafeltKeys.Annet.TILTAKSHENDELSE_OPPRETTET, tiltakshendelse.opprettet)
            .field(DatafeltKeys.Annet.TILTAKSHENDELSE_TEKST, tiltakshendelse.tekst)
            .field(DatafeltKeys.Annet.TILTAKSHENDELSE_TILTAKSTYPE, tiltakshendelse.tiltakstype)
            .endObject()
            .endObject()

        update(aktorId, content, "Oppdatert tiltakshendelse med id: ${tiltakshendelse.id}")
    }

    fun slettTiltakshendelse(aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .nullField(DatafeltKeys.Annet.TILTAKSHENDELSE)
            .endObject()

        update(aktorId, content, "Slettet tiltakshendelse for aktorId: $aktorId")
    }

    fun updateGjeldendeVedtak14a(gjeldendeVedtak14a: GjeldendeVedtak14a, aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A)
            .field(DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_INNSATSGRUPPE, gjeldendeVedtak14a.innsatsgruppe)
            .field(DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_HOVEDMAL, gjeldendeVedtak14a.hovedmal)
            .field(DatafeltKeys.Oppfolging.GJELDENDE_VEDTAK_14A_FATTET_DATO, gjeldendeVedtak14a.fattetDato)
            .endObject()
            .endObject()

        update(aktorId, content, "Oppdaterte gjeldendeVedtak14a for aktorId: $aktorId")
    }

    fun oppdaterHendelse(hendelse: Hendelse, aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(DatafeltKeys.Annet.HENDELSER)
            .startObject(hendelse.kategori.name)
            .field(DatafeltKeys.Annet.HENDELSER_BESKRIVELSE, hendelse.hendelse.beskrivelse)
            .field(DatafeltKeys.Annet.HENDELSER_DATO, hendelse.hendelse.dato)
            .field(DatafeltKeys.Annet.HENDELSER_LENKE, hendelse.hendelse.lenke.toString())
            .field(DatafeltKeys.Annet.HENDELSER_DETALJER, hendelse.hendelse.detaljer)
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
            "ctx._source.${DatafeltKeys.Annet.HENDELSER}.remove(params.$kategoriKey)",
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
            .field(DatafeltKeys.Ytelser.AAP_KELVIN, harAapKelvin)
            .field(DatafeltKeys.Ytelser.AAP_KELVIN_TOM_VEDTAKSDATO, tomVedtaksdato)
            .field(DatafeltKeys.Ytelser.AAP_KELVIN_RETTIGHETSTYPE, rettighetstype)
            .endObject()

        update(aktorId, content, "Oppdatert aap kelvin for aktorId: $aktorId")
    }

    fun slettAapKelvin(aktorId: AktorId) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .field(DatafeltKeys.Ytelser.AAP_KELVIN, false)
            .nullField(DatafeltKeys.Ytelser.AAP_KELVIN_TOM_VEDTAKSDATO)
            .nullField(DatafeltKeys.Ytelser.AAP_KELVIN_RETTIGHETSTYPE)
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
            .field(DatafeltKeys.Ytelser.TILTAKSPENGER, harTiltakspenger)
            .field(DatafeltKeys.Ytelser.TILTAKSPENGER_VEDTAKSDATO_TOM, vedtaksdatoTom)
            .field(DatafeltKeys.Ytelser.TILTAKSPENGER_RETTIGHET, rettighet)
            .endObject()

        update(aktorId, content, "Oppdatert tiltakspenger for aktorId: $aktorId")
    }

    fun oppdaterDagpenger(
        aktorId: AktorId,
        harDagpenger: Boolean,
        rettighetstype: DagpengerRettighetstype,
        antallResterendeDager: Int?,
        datoAntallDagerBleBeregnet: LocalDate?,
        datoStans: LocalDate?
    ) {
        val content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject(DatafeltKeys.Ytelser.DAGPENGER)
            .field(DatafeltKeys.Ytelser.DAGPENGER_HAR_DAGPENGER, harDagpenger)
            .field(DatafeltKeys.Ytelser.DAGPENGER_RETTIGHETSTYPE, rettighetstype)
            .field(DatafeltKeys.Ytelser.DAGPENGER_DATO_STANS, datoStans)
            .field(DatafeltKeys.Ytelser.DAGPENGER_ANTALL_RESTERENDE_DAGER, antallResterendeDager)
            .field(DatafeltKeys.Ytelser.DAGPENGER_DATO_ANTALL_DAGER_BLE_BEREGNET, datoAntallDagerBleBeregnet)
            .endObject()
            .endObject()

        update(aktorId, content, "Oppdatert dagpenger for aktorId: $aktorId")
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
}
