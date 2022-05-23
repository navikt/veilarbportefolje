package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepositoryV2;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.enums.arena.SikkerhetstiltakType;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukOppfolgingsbrukerPaPostgres;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerServiceV2 extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final OpensearchIndexer opensearchIndexer;
    private final VedtakStatusRepositoryV2 vedtakStatusRepositoryV2;
    private final UnleashService unleashService;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        ZonedDateTime iservDato = Optional.ofNullable(kafkaMelding.getIservFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);

        OppfolgingsbrukerEntity oppfolgingsbruker = new OppfolgingsbrukerEntity(
                kafkaMelding.getFodselsnummer(), kafkaMelding.getFormidlingsgruppe().name(),
                iservDato, kafkaMelding.getEtternavn(), kafkaMelding.getFornavn(),
                kafkaMelding.getOppfolgingsenhet(), Optional.ofNullable(kafkaMelding.getKvalifiseringsgruppe()).map(Kvalifiseringsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getRettighetsgruppe()).map(Rettighetsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getHovedmaal()).map(Hovedmaal::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getSikkerhetstiltakType()).map(SikkerhetstiltakType::name).orElse(null), kafkaMelding.getDiskresjonskode(),
                Optional.ofNullable(kafkaMelding.getSperretAnsatt()).orElse(false), Optional.ofNullable(kafkaMelding.getErDoed()).orElse(false),
                kafkaMelding.getSistEndretDato());
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbruker);

        brukerServiceV2.hentAktorId(Fnr.of(kafkaMelding.getFodselsnummer()))
                .ifPresent(id -> {
                    log.info("Fikk endring pa oppfolgingsbruker (V2): {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v2", id);
                    if (brukOppfolgingsbrukerPaPostgres(unleashService)) {
                        opensearchIndexer.indekser(id);
                    } else {
                        oppdaterOpensearch(id, oppfolgingsbruker);
                    }
                });
    }

    private void oppdaterOpensearch(AktorId aktorId, OppfolgingsbrukerEntity oppfolgingsbruker) {
        String vedtak14aStatus = vedtakStatusRepositoryV2.hent14aVedtak(aktorId.get())
                .map(KafkaVedtakStatusEndring::getVedtakStatusEndring)
                .map(KafkaVedtakStatusEndring.VedtakStatusEndring::toString)
                .orElse(null);
        opensearchIndexerV2.updateOppfolgingsbruker(aktorId, oppfolgingsbruker, vedtak14aStatus);
    }
}



