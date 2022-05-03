package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.enums.arena.SikkerhetstiltakType;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerServiceV2 extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final AktorClient aktorClient;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        hentOgLoggAktoerId(Fnr.of(kafkaMelding.getFodselsnummer()));

        ZonedDateTime iservDato = Optional.ofNullable(kafkaMelding.getIservFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);
        ZonedDateTime dodFraDato = Optional.ofNullable(kafkaMelding.getDoedFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);
        OppfolgingsbrukerEntity oppfolgingsbruker =  new OppfolgingsbrukerEntity(
                null, kafkaMelding.getFodselsnummer(), kafkaMelding.getFormidlingsgruppe().name(),
                iservDato, kafkaMelding.getEtternavn(), kafkaMelding.getFornavn(),
                kafkaMelding.getOppfolgingsenhet(), Optional.ofNullable(kafkaMelding.getKvalifiseringsgruppe()).map(Kvalifiseringsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getRettighetsgruppe()).map(Rettighetsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getHovedmaal()).map(Hovedmaal::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getSikkerhetstiltakType()).map(SikkerhetstiltakType::name).orElse(null), kafkaMelding.getDiskresjonskode(),
                Optional.ofNullable(kafkaMelding.getHarOppfolgingssak()).orElse(false),
                Optional.ofNullable(kafkaMelding.getSperretAnsatt()).orElse(false), Optional.ofNullable(kafkaMelding.getErDoed()).orElse(false),
                dodFraDato, kafkaMelding.getSistEndretDato());

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbruker);
    }

    private void hentOgLoggAktoerId(Fnr fodselsnummer) {
        try {
            AktorId aktorId = aktorClient.hentAktorId(fodselsnummer);
            log.info("Fikk endring pa oppfolgingsbruker (V2): {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v2", aktorId);
        } catch (Exception ignored) {
        }
    }
}


