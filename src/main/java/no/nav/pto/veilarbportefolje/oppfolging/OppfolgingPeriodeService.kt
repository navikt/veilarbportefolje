package no.nav.pto.veilarbportefolje.oppfolging

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.domene.NavKontor
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.AvsluttetOppfolgingsperiodeV2
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.GjeldendeOppfolgingsperiodeV2Dto
import no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto.SisteOppfolgingsperiodeV2Dto
import no.nav.pto.veilarbportefolje.util.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Slf4j
@Service
@RequiredArgsConstructor
class OppfolgingPeriodeService(
    val oppfolgingStartetService: OppfolgingStartetService,
    val oppfolgingAvsluttetService: OppfolgingAvsluttetService
) : KafkaCommonNonKeyedConsumerService<SisteOppfolgingsperiodeV2Dto>() {
    private val log = LoggerFactory.getLogger(OppfolgingPeriodeService::class.java)

    public override fun behandleKafkaMeldingLogikk(sisteOppfolgingsperiode: SisteOppfolgingsperiodeV2Dto) {
        if (sisteOppfolgingsperiode.aktorId.isEmpty()) {
            log.warn("Ugyldig data for siste oppfolging periode på bruker med periode: " + sisteOppfolgingsperiode.oppfolgingsperiodeUuid.toString())
            return
        }

        when (sisteOppfolgingsperiode) {
           is GjeldendeOppfolgingsperiodeV2Dto -> {
               SecureLog.secureLog.info("Starter oppfolging for: " + sisteOppfolgingsperiode.aktorId)
               oppfolgingStartetService.behandleOppfølgingStartetEllerKontorEndret(
                   Fnr.of(sisteOppfolgingsperiode.ident),
                   AktorId.of(sisteOppfolgingsperiode.aktorId),
                   sisteOppfolgingsperiode.startTidspunkt,
                   NavKontor(sisteOppfolgingsperiode.kontorId)
               )
           }
           is AvsluttetOppfolgingsperiodeV2 -> {
               if (sisteOppfolgingsperiode.startTidspunkt.isAfter(sisteOppfolgingsperiode.sluttTidspunkt)) {
                   log.error("Ugyldig start/slutt dato for siste oppfolging periode på bruker: " + sisteOppfolgingsperiode.aktorId)
                   return
               }
               SecureLog.secureLog.info("Avslutter oppfolging for: " + sisteOppfolgingsperiode.aktorId)
               oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(sisteOppfolgingsperiode.aktorId))
           }
        }
    }
}
