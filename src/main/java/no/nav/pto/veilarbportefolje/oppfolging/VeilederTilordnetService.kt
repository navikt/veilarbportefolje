package no.nav.pto.veilarbportefolje.oppfolging

import no.nav.common.job.JobRunner
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.VeilederId
import no.nav.pto.veilarbportefolje.domene.VeilederId.Companion.of
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriService
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.dto.VeilederTilordnetDTO
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.*

@Service
class VeilederTilordnetService(
    private val oppfolgingClient: OppfolgingClient,
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    private val huskelappService: HuskelappService,
    private val fargekategoriService: FargekategoriService,
    private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
    private val pdlIdentRepository: PdlIdentRepository
) : KafkaCommonNonKeyedConsumerService<VeilederTilordnetDTO?>() {

    public override fun behandleKafkaMeldingLogikk(dto: VeilederTilordnetDTO?) {
        checkNotNull(dto) { "Ulovlig tilstand: VeilederTilordnetDTO var null. Klarer ikke behandle melding." }

        val aktoerId = dto.aktorId
        val veilederId = dto.veilederId
        val tildeltTidspunkt = dto.tilordnetTidspunkt

        tilordneVeileder(aktoerId, veilederId, tildeltTidspunkt)
    }

    // midlertidig kode for å kjøre batch-jobb
    private val log = org.slf4j.LoggerFactory.getLogger(VeilederTilordnetService::class.java)

    fun lastInnTildelingstidspunktForVeileder() {
        log.info("Startet: Innlastning av tildelingstidspunkt for veileder")

        JobRunner.runAsync("OppfolgingSyncTildelingstidspunkt") {
            val oppfolgingsBrukere =
                oppfolgingRepositoryV2.hentAlleBrukerUnderOppfolgingMedTildeltVeileder()
            oppfolgingsBrukere.forEach(::oppdaterTildelingstidspunkt)

            log.info(
                "OppfolgingsJobb: oppdaterte tildelingstidspunkt pa: {} brukere",
                oppfolgingsBrukere.size
            )
        }

        log.info("Ferdig: Innlastning av tildelingstidspunkt for veileder")
    }

    fun oppdaterTildelingstidspunkt(aktorId: AktorId) {
        try {
            val veilarbInfo = oppfolgingClient.hentVeilarbData(aktorId)

            if (veilarbInfo.isErUnderOppfolging &&
                veilarbInfo.tilordnetTidspunkt != null
            ) {
                oppfolgingRepositoryV2.settTildeltTidspunkt(
                    aktorId,
                    veilarbInfo.tilordnetTidspunkt
                )
            }
        } catch (e: RuntimeException) {
            secureLog.error(
                "RuntimeException i OppfolgingsJobb tildelingstidspunkt for bruker $aktorId",
                e
            )
        } catch (e: Exception) {
            secureLog.error(
                "Exception i OppfolgingsJobb tildelingstidspunkt for bruker $aktorId",
                e
            )
        }
    }

    fun tilordneVeileder(aktoerId: AktorId, veilederId: VeilederId?, tildeltTidspunkt: ZonedDateTime?) {
        oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId)
        oppfolgingRepositoryV2.settTildeltTidspunkt(aktoerId, tildeltTidspunkt)

        kastErrorHvisBrukerSkalVaereUnderOppfolging(aktoerId, veilederId)
        opensearchIndexerPaDatafelt.oppdaterVeileder(aktoerId, veilederId, tildeltTidspunkt)
        secureLog.info("Oppdatert bruker: $aktoerId, til veileder med id: $veilederId, med tildelt tidspunkt: $tildeltTidspunkt")

        val maybeFnr: Fnr? = pdlIdentRepository.hentFnrForAktivBruker(aktoerId)

        val brukerHarByttetNavkontorHuskelapp =
            huskelappService.brukerHarHuskelappPaForrigeNavkontor(aktoerId, Optional.ofNullable(maybeFnr))
        if (brukerHarByttetNavkontorHuskelapp) {
            huskelappService.deaktivereAlleHuskelapperPaaBruker(aktoerId, Optional.ofNullable(maybeFnr))
        }

        val brukerHarByttetNavkontorFargekategori =
            fargekategoriService.brukerHarFargekategoriPaForrigeNavkontor(aktoerId, Optional.ofNullable(maybeFnr))
        if (brukerHarByttetNavkontorFargekategori) {
            fargekategoriService.slettFargekategoriPaaBruker(aktoerId, Optional.ofNullable(maybeFnr))
        }
    }

    private fun kastErrorHvisBrukerSkalVaereUnderOppfolging(aktorId: AktorId?, veilederId: VeilederId?) {
        if (hentVeileder(aktorId) == veilederId) {
            return
        }
        val erUnderOppfolgingIVeilarboppfolging = oppfolgingClient.hentUnderOppfolging(aktorId)
        check(!erUnderOppfolgingIVeilarboppfolging) { "Fikk 'veileder melding' på bruker som enda ikke er under oppfølging i veilarbportefolje" }
    }

    private fun hentVeileder(aktoerId: AktorId?): VeilederId? {
        return oppfolgingRepositoryV2.hentVeilederForBruker(aktoerId)
            .orElse(of(null))
    }
}
