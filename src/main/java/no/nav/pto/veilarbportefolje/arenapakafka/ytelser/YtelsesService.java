package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.ArenaHendelseRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.*;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class YtelsesService {
    @NonNull
    @Qualifier("systemClient")
    private final AktorClient aktorClient;
    private final BrukerService brukerService;
    private final BrukerDataService brukerDataService;
    private final YtelsesRepository ytelsesRepository;
    private final YtelsesServicePostgres ytelsesServicePostgres;
    private final ArenaHendelseRepository arenaHendelseRepository;
    private final ElasticIndexer elasticIndexer;
    private final OppfolgingRepository oppfolgingRepository;

    public void behandleKafkaRecord(ConsumerRecord<String, YtelsesDTO> kafkaMelding, TypeKafkaYtelse ytelse) {
        YtelsesDTO melding = kafkaMelding.value();
        log.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );

        YtelsesInnhold innhold = getInnhold(melding);
        if (innhold == null || erGammelMelding(melding, innhold)) {
            return;
        }

        behandleKafkaMeldingOracle(melding, ytelse);
        behandleKafkaMeldingPostgres(melding, ytelse);

        arenaHendelseRepository.upsertYtelsesHendelse(innhold.getVedtakId(), innhold.getHendelseId());
    }

    public void behandleKafkaMeldingPostgres(YtelsesDTO melding, TypeKafkaYtelse ytelse) {
        ytelsesServicePostgres.behandleKafkaMeldingPostgres(melding, ytelse);
    }

    public void behandleKafkaMeldingOracle(YtelsesDTO kafkaMelding, TypeKafkaYtelse ytelse) {
        YtelsesInnhold innhold = getInnhold(kafkaMelding);

        AktorId aktorId = getAktorId(aktorClient, innhold.getFnr());
        if (skalSlettesGoldenGate(kafkaMelding)) {
            log.info("Sletter ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepository.slettYtelse(innhold.getVedtakId());
            oppdaterYtelsesInformasjonMedUnntaksLogikkForSletting(aktorId, innhold);
        } else {
            log.info("Lagrer ytelse: {}, pa aktorId: {}", innhold.getVedtakId(), aktorId);
            ytelsesRepository.upsertYtelse(aktorId, ytelse, innhold);
            oppdaterYtelsesInformasjonOracle(aktorId, PersonId.of(innhold.getPersonId()));
        }


        elasticIndexer.indekser(aktorId);
    }

    public Optional<YtelseDAO> oppdaterYtelsesInformasjonOracle(AktorId aktorId, PersonId personId) {
        Optional<YtelseDAO> lopendeYtelse = finnLopendeYtelseOracle(aktorId);
        log.info("AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, lopendeYtelse.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
        brukerDataService.oppdaterYtelserOracle(aktorId, personId, lopendeYtelse);

        return lopendeYtelse;
    }

    /**
     * NB: I tilfeller der arena sletter et løpende vedtak.
     * Det må da sjekkes om det finnes andre vedtak i samme sak, hvis dette er tilfellet så skal ytelsen på brukeren fortsette.
     * Dette gjelder uavhengig av startdatoen på vedtaket som tar over som løpende.
     * Det neste løpende vedtaket kan med andre ord ha en startdato satt i fremtiden.
     */
    public Optional<YtelseDAO> oppdaterYtelsesInformasjonMedUnntaksLogikkForSletting(AktorId aktorId, YtelsesInnhold innhold) {
        LocalDate iDag = LocalDate.now();

        Timestamp startDato = Timestamp.valueOf(innhold.getFraOgMedDato().getLocalDateTime());
        Timestamp utlopsDato = innhold.getTilOgMedDato() == null ? null : Timestamp.valueOf(innhold.getTilOgMedDato().getLocalDateTime());

        boolean erLopendeVedtak = harLopendeStartDato(startDato, iDag) && harLopendeUtlopsDato(utlopsDato, iDag);

        if (erLopendeVedtak) {
            Optional<YtelseDAO> sisteYtelsePaSakId = finnSisteYtelsePaSakIdSomIkkeErUtloptOracle(aktorId, innhold.getSaksId());
            if (sisteYtelsePaSakId.isPresent()) {
                log.info("AktoerId: {} har en løpende ytelse med saksId: {}", aktorId, sisteYtelsePaSakId.map(YtelseDAO::getSaksId).orElse("ingen løpende vedtak"));
                brukerDataService.oppdaterYtelserOracle(aktorId, PersonId.of(innhold.getPersonId()), sisteYtelsePaSakId);

                return sisteYtelsePaSakId;
            }
        }

        return oppdaterYtelsesInformasjonOracle(aktorId, PersonId.of(innhold.getPersonId()));
    }

    public Optional<YtelseDAO> finnLopendeYtelseOracle(AktorId aktorId) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveYtelser = ytelsesRepository.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> harLopendeUtlopsDato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveYtelser.isEmpty()) {
            return Optional.empty();
        }

        YtelseDAO tidligsteYtelse = aktiveYtelser.stream()
                .min(Comparator.comparing(YtelseDAO::getStartDato)).get();

        if (!harLopendeStartDato(tidligsteYtelse.getStartDato(), iDag)) {
            return Optional.empty();
        }
        if (TypeKafkaYtelse.DAGPENGER.equals(tidligsteYtelse.getType())) {
            // Dagpenger skal aldri ha en utløpsdato
            // Hvis det finnes en utløpsdato er det mest sannynlig et annet dagpenge vedtak som skal ta over for det løpende vedatekt, eller en bug
            return Optional.of(tidligsteYtelse.setUtlopsDato(null));
        }

        if (tidligsteYtelse.getUtlopsDato() == null) {
            return Optional.of(tidligsteYtelse);
        }
        return finnVedtakMedSisteUtlopsDatoPaSak(aktiveYtelser, tidligsteYtelse);
    }

    public Optional<YtelseDAO> finnSisteYtelsePaSakIdSomIkkeErUtloptOracle(AktorId aktorId, String sakID) {
        LocalDate iDag = LocalDate.now();
        List<YtelseDAO> aktiveYtelserPaSakID = ytelsesRepository.getYtelser(aktorId).stream()
                .filter(Objects::nonNull)
                .filter(ytelse -> sakID.equals(ytelse.getSaksId()))
                .filter(ytelse -> harLopendeUtlopsDato(ytelse.getUtlopsDato(), iDag))
                .collect(Collectors.toList());

        if (aktiveYtelserPaSakID.isEmpty()) {
            return Optional.empty();
        }
        Optional<YtelseDAO> ytelseMedSluttDatoEllerNull = aktiveYtelserPaSakID.stream()
                .filter(ytelseDAO -> ytelseDAO.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));

        return Optional.of(ytelseMedSluttDatoEllerNull.orElse(aktiveYtelserPaSakID.get(0)));
    }

    public void oppdaterBrukereMedYtelserSomStarterIDagOracle() {
        List<AktorId> brukere = ytelsesRepository.hentBrukereMedYtelserSomStarterIDag();
        log.info("Oppdaterer ytelser for: " + brukere.size() + " antall brukere");

        brukere.forEach(aktorId -> {
            log.info("Oppdaterer ytelse for aktorId: " + aktorId);
            PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
            if (personId == null) {
                log.warn("Avbryter ytelse oppdatering pga. manglende personId for aktorId: " + aktorId);
                return;
            }
            oppdaterYtelsesInformasjonOracle(aktorId, personId);
            elasticIndexer.indekser(aktorId);
        });

        log.info("Oppdatering av ytelser fullført");
    }

    public void syncYtelserForAlleBrukere() {
        log.info("Starter jobb: oppdater Ytelser");
        List<AktorId> brukereSomMaOppdateres = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        log.info("Oppdaterer ytelser for alle brukere under oppfolging: {}", brukereSomMaOppdateres.size());

        ForkJoinPool pool = new ForkJoinPool(5);
        try {
            pool.submit(() ->
                    brukereSomMaOppdateres.parallelStream().forEach(aktorId -> {
                                log.info("Oppdater ytelser for aktorId: {}", aktorId);
                                if (aktorId != null) {
                                    try {
                                        PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
                                        oppdaterYtelsesInformasjonOracle(aktorId, personId);
                                        elasticIndexer.indekser(aktorId);
                                    } catch (Exception e) {
                                        log.warn("Fikk error under sync jobb, men fortsetter. Aktoer: {}, exception: {}", aktorId, e);
                                    }
                                }
                            }
                    )).get(10, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error i sync jobben.", e);
        }
    }

    private Optional<YtelseDAO> finnVedtakMedSisteUtlopsDatoPaSak(List<YtelseDAO> ytelser, YtelseDAO tidligsteYtelse) {
        return ytelser.stream()
                .filter(ytelsesDAO -> tidligsteYtelse.getSaksId().equals(ytelsesDAO.getSaksId()))
                .filter(ytelsesDAO -> ytelsesDAO.getUtlopsDato() != null)
                .max(Comparator.comparing(YtelseDAO::getUtlopsDato));
    }

    private boolean harLopendeStartDato(Timestamp startDato, LocalDate iDag) {
        // startDato er en 'fra og med' dato.
        return startDato.toLocalDateTime().toLocalDate().isBefore(iDag.plusDays(1));
    }

    private boolean harLopendeUtlopsDato(Timestamp utlopsDato, LocalDate iDag) {
        // Utløpsdato er en 'til og med' dato.
        return utlopsDato == null || utlopsDato.toLocalDateTime().toLocalDate().isAfter(iDag.minusDays(1));
    }

    private boolean erGammelMelding(YtelsesDTO kafkaMelding, YtelsesInnhold innhold) {
        Long hendelseIDB = arenaHendelseRepository.retrieveYtelsesHendelse(innhold.getVedtakId());

        if (erGammelHendelseBasertPaOperasjon(hendelseIDB, innhold.getHendelseId(), skalSlettesGoldenGate(kafkaMelding))) {
            log.info("Fikk tilsendt gammel ytelsesmelding, vedtak: {}, personId: {}", innhold.getVedtakId(), innhold.getPersonId());
            return true;
        }
        return false;
    }
}
