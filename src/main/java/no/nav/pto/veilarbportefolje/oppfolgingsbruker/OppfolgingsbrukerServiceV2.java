package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtak;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakRepository;
import no.nav.pto.veilarbportefolje.vedtakstotte.Kafka14aStatusendring;
import no.nav.pto.veilarbportefolje.vedtakstotte.Utkast14aStatusRepository;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukOppfolgingsbrukerPaPostgres;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerServiceV2 extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final OpensearchIndexer opensearchIndexer;
    private final Utkast14aStatusRepository utkast14aStatusRepository;
    private final DefaultUnleash defaultUnleash;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        ZonedDateTime iservDato = Optional.ofNullable(kafkaMelding.getIservFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);

        OppfolgingsbrukerEntity oppfolgingsbruker = new OppfolgingsbrukerEntity(
                kafkaMelding.getFodselsnummer(), kafkaMelding.getFormidlingsgruppe().name(),
                iservDato,
                kafkaMelding.getOppfolgingsenhet(), Optional.ofNullable(kafkaMelding.getKvalifiseringsgruppe()).map(Kvalifiseringsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getRettighetsgruppe()).map(Rettighetsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getHovedmaal()).map(Hovedmaal::name).orElse(null),//TODO dersom siste14avedtak fraArena=false bruk den i steden for denne inkommende
                kafkaMelding.getSistEndretDato());
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbruker);

        brukerServiceV2.hentAktorId(Fnr.of(kafkaMelding.getFodselsnummer()))
                .ifPresent(id -> {
                    secureLog.info("Fikk endring pa oppfolgingsbruker (V2): {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v2", id);
                    if (brukOppfolgingsbrukerPaPostgres(defaultUnleash)) {
                        opensearchIndexer.indekser(id);
                    } else {
                        oppdaterOpensearch(id, oppfolgingsbruker);
                    }
                });
    }

    private void oppdaterOpensearch(AktorId aktorId, OppfolgingsbrukerEntity oppfolgingsbruker) {
        String utkast14aStatus = utkast14aStatusRepository.hentStatusEndringForBruker(aktorId.get())
                .map(Kafka14aStatusendring::getVedtakStatusEndring)
                .map(Kafka14aStatusendring.Status::toString)
                .orElse(null);
        if(oppfolgingsbruker.hovedmaalkode().isEmpty()) {
          //  Optional<Siste14aVedtak> siste14aVedtakForBruker = siste14aVedtakRepository.hentSiste14aVedtak(new IdenterForBruker(List.of(aktorId.get())));
          //  Boolean vedtakFattetIArena = siste14aVedtakForBruker.map(Siste14aVedtak::isFraArena).orElse();
        }
        opensearchIndexerV2.updateOppfolgingsbruker(aktorId, oppfolgingsbruker, utkast14aStatus);
    }
}



