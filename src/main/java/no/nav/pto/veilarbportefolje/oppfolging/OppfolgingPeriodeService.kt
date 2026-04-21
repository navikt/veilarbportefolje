package no.nav.pto.veilarbportefolje.oppfolging

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.NavKontor
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.AvsluttetOppfolgingsperiodeV3Dto
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.GjeldendeOppfolgingsperiodeV3Dto
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.SisteOppfolgingsperiodeV3Dto
import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.persononinfo.PdlHentBrukerDataException
import no.nav.pto.veilarbportefolje.util.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppfolgingPeriodeService(
    val oppfolgingStartetService: OppfolgingStartetService,
    val oppfolgingAvsluttetService: OppfolgingAvsluttetService
) : KafkaCommonNonKeyedConsumerService<SisteOppfolgingsperiodeV3Dto>() {
    private val log = LoggerFactory.getLogger(OppfolgingPeriodeService::class.java)

    public override fun behandleKafkaMeldingLogikk(sisteOppfolgingsperiode: SisteOppfolgingsperiodeV3Dto) {
        if (sisteOppfolgingsperiode.aktorId.isEmpty()) {
            log.warn("Ugyldig data for siste oppfolging periode på bruker med periode: " + sisteOppfolgingsperiode.oppfolgingsperiodeUuid.toString())
            return
        }

        when (sisteOppfolgingsperiode) {
           is GjeldendeOppfolgingsperiodeV3Dto -> {
               try {
                   oppfolgingStartetService.behandleOppfolgingStartetEllerKontorEndret(
                       Fnr.of(sisteOppfolgingsperiode.ident),
                       AktorId.of(sisteOppfolgingsperiode.aktorId),
                       sisteOppfolgingsperiode.startTidspunkt,
                       NavKontor(sisteOppfolgingsperiode.kontor.kontorId)
                   )
               } catch (e: PdlHentBrukerDataException) {
                   if (EnvironmentUtils.isDevelopment().orElse(false)) {
                       SecureLog.secureLog.error("Kunne ikke hente brukerdata fra PDL for bruker: ${sisteOppfolgingsperiode.aktorId}", e)
                   } else {
                       throw e
                   }
               }
           }
           is AvsluttetOppfolgingsperiodeV3Dto -> {
               if (sisteOppfolgingsperiode.startTidspunkt.isAfter(sisteOppfolgingsperiode.sluttTidspunkt)) {
                   log.error("Ugyldig start/slutt dato for siste oppfolging periode på bruker: " + sisteOppfolgingsperiode.aktorId)
                   return
               }
               SecureLog.secureLog.info("Avslutter oppfolging for: " + sisteOppfolgingsperiode.aktorId)
               oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(sisteOppfolgingsperiode.aktorId), sisteOppfolgingsperiode.sluttTidspunkt)
           }
        }
    }
}
