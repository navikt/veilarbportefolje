package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OppfolginsbrukerService extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolginsbrukerRepositoryV2 OppfolginsbrukerRepositoryV2;
    private final AktorClient aktorClient;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        AktorId aktorId = aktorClient.hentAktorId(Fnr.of(kafkaMelding.getFodselsnummer()));

        if (aktorId == null) {
            throw new IllegalStateException("Fnr -> AktoerId var null på topic endringPaaOppfoelgingsBruker");
        }
        log.info("Fikk endring pa oppfolgingsbruker: {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v2", aktorId);
        int rader = OppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktorId.get(), kafkaMelding.getFodselsnummer(), kafkaMelding.getFormidlingsgruppe().name(),
                        ZonedDateTime.of(kafkaMelding.getIservFraDato().atStartOfDay(), ZoneId.systemDefault()), kafkaMelding.getEtternavn(), kafkaMelding.getFornavn(),
                        kafkaMelding.getOppfolgingsenhet(), kafkaMelding.getKvalifiseringsgruppe().name(), kafkaMelding.getRettighetsgruppe().name(),
                        kafkaMelding.getHovedmaal().name(), kafkaMelding.getSikkerhetstiltakType().name(), kafkaMelding.getDiskresjonskode(),
                        kafkaMelding.getHarOppfolgingssak(), kafkaMelding.getSperretAnsatt(), kafkaMelding.getErDoed(),
                        ZonedDateTime.of(kafkaMelding.getDoedFraDato().atStartOfDay(), ZoneId.systemDefault()), kafkaMelding.getSistEndretDato()));
        log.info("Oppdatert oppfolgingsbrukerinfo for bruker: {} i postgres, rader pavirket: {}", aktorId, rader);
    }
}



