package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
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
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukOppfolgingsbrukerPaPostgres;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerServiceV2 extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final OpensearchIndexer opensearchIndexer;
    private final Utkast14aStatusRepository utkast14aStatusRepository;
    private final DefaultUnleash defaultUnleash;
    private final VeilarbarenaClient veilarbarenaClient;
    private final PdlIdentRepository pdlIdentRepository;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        String fodselsnummer = kafkaMelding.getFodselsnummer();

        if (!pdlIdentRepository.erBrukerUnderOppfolging(fodselsnummer)) {
            log.info("Bruker er ikke under oppfølging, ignorerer melding.");
            secureLog.info("Bruker er ikke under oppfølging, ignorerer endring på bruker med fnr: {}", fodselsnummer);
            return;
        }

        ZonedDateTime iservDato = Optional.ofNullable(kafkaMelding.getIservFraDato())
                .map(dato -> ZonedDateTime.of(dato.atStartOfDay(), ZoneId.systemDefault()))
                .orElse(null);

        OppfolgingsbrukerEntity oppfolgingsbruker = new OppfolgingsbrukerEntity(
                fodselsnummer,
                kafkaMelding.getFormidlingsgruppe().name(),
                iservDato,
                kafkaMelding.getOppfolgingsenhet(),
                Optional.ofNullable(kafkaMelding.getKvalifiseringsgruppe()).map(Kvalifiseringsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getRettighetsgruppe()).map(Rettighetsgruppe::name).orElse(null),
                Optional.ofNullable(kafkaMelding.getHovedmaal()).map(Hovedmaal::name).orElse(null),
                kafkaMelding.getSistEndretDato());
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbruker);

        brukerServiceV2.hentAktorId(Fnr.of(fodselsnummer))
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
        opensearchIndexerV2.updateOppfolgingsbruker(aktorId, oppfolgingsbruker, utkast14aStatus);
    }

    public void hentOgLagreOppfolgingsbruker(AktorId aktorId) {
        Fnr fnr = pdlIdentRepository.hentFnrForAktivBruker(aktorId);

        Optional<OppfolgingsbrukerDTO> oppfolgingsbrukerDTO = veilarbarenaClient.hentOppfolgingsbruker(fnr);

        if(oppfolgingsbrukerDTO.isEmpty()) {
            secureLog.error("Fant ingen oppfølgingsbrukerdata for bruker med fnr: {}.", fnr);
            throw new RuntimeException("Fant ingen oppfølgingsbrukerdata for brukeren.");
        }

        OppfolgingsbrukerEntity oppfolgingsbrukerEntity = new OppfolgingsbrukerEntity(
                oppfolgingsbrukerDTO.get().getFodselsnr(),
                oppfolgingsbrukerDTO.get().getFormidlingsgruppekode(),
                oppfolgingsbrukerDTO.get().getIservFraDato(),
                oppfolgingsbrukerDTO.get().getNavKontor(),
                oppfolgingsbrukerDTO.get().getKvalifiseringsgruppekode(),
                oppfolgingsbrukerDTO.get().getRettighetsgruppekode(),
                oppfolgingsbrukerDTO.get().getHovedmaalkode(),
                oppfolgingsbrukerDTO.get().getSistEndretDato()
        );

        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbrukerEntity);
        log.info("Oppfolgingsbruker hentet og lagret.");
        secureLog.info("Oppfolgingsbruker hentet og lagret for aktorId: {} / fnr: {}.", aktorId, fnr);
    }

    public void slettOppfolgingsbruker(AktorId aktorId, Optional<Fnr> maybeFnr) {
        if (maybeFnr.isEmpty()) {
            secureLog.warn("Kunne ikke slette oppfolgingsbruker med Aktør-ID {}. Årsak fødselsnummer-parameter var tom.", aktorId.get());
            return;
        }

        try {
            int raderSlettet = oppfolgingsbrukerRepositoryV3.slettOppfolgingsbruker(maybeFnr.get());

            if(raderSlettet != 0) {
                log.info("Oppfolgingsbruker slettet.");
                secureLog.info("Oppfolgingsbruker slettet for fnr: {} / Aktør-ID: {}.", maybeFnr.get(), aktorId.get());
            } else {
                log.info("Fant ingen oppfolgingsbruker å slette.");
                secureLog.info("Fant ingen oppfolgingsbruker å slette for fnr: {} / Aktør-ID: {}.", maybeFnr.get(), aktorId.get());
            }
        } catch (Exception e) {
            secureLog.error(
                    String.format("Kunne ikke slette oppfolgingsbruker for fnr: %s / Aktør-ID: %s.", maybeFnr.get(), aktorId.get()),
                    e
            );
            throw new RuntimeException("Kunne ikke slette oppfolgingsbruker");
        }
    }
}



