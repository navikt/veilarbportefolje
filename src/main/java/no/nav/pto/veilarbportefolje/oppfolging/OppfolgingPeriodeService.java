package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingPeriodeService extends KafkaCommonConsumerService<SisteOppfolgingsperiodeV1> {
    private final OppfolgingStartetService oppfolgingStartetService;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;

    @Override
    public void behandleKafkaMeldingLogikk(SisteOppfolgingsperiodeV1 sisteOppfolgingsperiod) {
        if (sisteOppfolgingsperiod.getAktorId().isEmpty() || sisteOppfolgingsperiod.getStartDato() == null) {
            log.warn("Ugyldig data for siste oppfolging periode på bruker: " + sisteOppfolgingsperiod.getAktorId());
            return;
        }
        if (sisteOppfolgingsperiod.getSluttDato() != null && sisteOppfolgingsperiod.getStartDato().isAfter(sisteOppfolgingsperiod.getSluttDato())) {
            log.error("Ugyldig start/slutt dato for siste oppfolging periode på bruker: " + sisteOppfolgingsperiod.getAktorId());
            return;
        }

        if (sisteOppfolgingsperiod.getSluttDato() == null) {
            secureLog.info("Starter oppfolging for: " + sisteOppfolgingsperiod.getAktorId());
            oppfolgingStartetService.startOppfolging(AktorId.of(sisteOppfolgingsperiod.getAktorId()), sisteOppfolgingsperiod.getStartDato());
        } else {
            secureLog.info("Avslutter oppfolging for: " + sisteOppfolgingsperiod.getAktorId());
            oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(sisteOppfolgingsperiod.getAktorId()));
        }
    }
}
