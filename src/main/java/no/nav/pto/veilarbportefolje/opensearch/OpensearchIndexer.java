package no.nav.pto.veilarbportefolje.opensearch;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV1;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.BrukertiltakV2;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepository;
import org.opensearch.action.ActionListener;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.pto.veilarbportefolje.opensearch.IndekseringUtils.finnBruker;
import static no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;

@Slf4j
@Service
public class OpensearchIndexer {

    public static final int BATCH_SIZE = 1000;
    public static final int ORACLE_BATCH_SIZE_LIMIT = 1000;
    private final RestHighLevelClient restHighLevelClient;
    private final AktivitetDAO aktivitetDAO;
    private final BrukerRepository brukerRepository;
    private final IndexName alias;
    private final SisteEndringRepository sisteEndringRepository;
    private final TiltakRepositoryV1 tiltakRepositoryV1;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    @Autowired
    public OpensearchIndexer(
            AktivitetDAO aktivitetDAO,
            BrukerRepository brukerRepository,
            RestHighLevelClient restHighLevelClient,
            SisteEndringRepository sisteEndringRepository,
            IndexName opensearchIndex,
            TiltakRepositoryV1 tiltakRepositoryV1,
            OpensearchIndexerV2 opensearchIndexerV2) {

        this.aktivitetDAO = aktivitetDAO;
        this.brukerRepository = brukerRepository;
        this.restHighLevelClient = restHighLevelClient;
        this.sisteEndringRepository = sisteEndringRepository;
        this.tiltakRepositoryV1 = tiltakRepositoryV1;
        this.alias = opensearchIndex;
        this.opensearchIndexerV2 = opensearchIndexerV2;
    }

    static int utregnTil(int from, int batchSize) {
        if (from < 0 || batchSize < 0) {
            throw new IllegalArgumentException("Negative numbers are not allowed");
        }

        if (from == 0) {
            return batchSize;
        }

        return from + BATCH_SIZE;
    }

    public void indekser(AktorId aktoerId) {
        brukerRepository.hentBrukerFraView(aktoerId).ifPresent(this::indekserBruker);
    }

    private void indekserBruker(OppfolgingsBruker bruker) {
        if (erUnderOppfolging(bruker)) {
            leggTilAktiviteter(bruker);
            leggTilTiltak(bruker);
            leggTilSisteEndring(bruker);
            skrivTilIndeks(alias.getValue(), bruker);
        } else {
            opensearchIndexerV2.slettDokumenter(List.of(AktorId.of(bruker.getAktoer_id())));
        }
    }

    public void skrivTilIndeks(String indeksNavn, List<OppfolgingsBruker> oppfolgingsBrukere) {
        BulkRequest bulk = new BulkRequest();
        oppfolgingsBrukere.stream()
                .map(bruker -> {
                    IndexRequest indexRequest = new IndexRequest(indeksNavn).id(bruker.getAktoer_id());
                    return indexRequest.source(toJson(bruker), XContentType.JSON);
                })
                .forEach(bulk::add);

        restHighLevelClient.bulkAsync(bulk, RequestOptions.DEFAULT, new ActionListener<>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                if (bulkItemResponses.hasFailures()) {
                    log.warn("Klart ikke å skrive til indeks: {}", bulkItemResponses.buildFailureMessage());
                }

                if (bulkItemResponses.getItems().length != oppfolgingsBrukere.size()) {
                    log.warn("Antall faktiske adds og antall brukere som skulle oppdateres er ulike");
                }

                List<String> aktoerIds = oppfolgingsBrukere.stream().map(OppfolgingsBruker::getAktoer_id).collect(toList());
                log.info("Skrev {} brukere til indeks: {}", oppfolgingsBrukere.size(), aktoerIds);

            }

            @Override
            public void onFailure(Exception e) {
                log.warn("Feil under asynkron indeksering av brukerere ", e);
            }
        });


    }

    public void skrivTilIndeks(String indeksNavn, OppfolgingsBruker oppfolgingsBruker) {
        this.skrivTilIndeks(indeksNavn, Collections.singletonList(oppfolgingsBruker));
    }


    private void validateBatchSize(List<?> brukere) {
        if (brukere.size() > ORACLE_BATCH_SIZE_LIMIT) {
            throw new IllegalStateException(format("Kan ikke prossessere flere enn %s brukere av gangen pga begrensninger i oracle db", ORACLE_BATCH_SIZE_LIMIT));
        }
    }

    private void leggTilTiltak(List<OppfolgingsBruker> brukere) {
        validateBatchSize(brukere);

        List<AktorId> aktorIder = brukere.stream()
                .map(OppfolgingsBruker::getAktoer_id)
                .map(AktorId::of)
                .collect(toList());

        Map<AktorId, Set<BrukertiltakV2>> alleTiltakForBrukere = tiltakRepositoryV1.hentBrukertiltak(aktorIder);

        alleTiltakForBrukere.forEach((aktorId, brukerMedTiltak) -> {
            Set<String> tiltak = brukerMedTiltak.stream()
                    .map(BrukertiltakV2::getTiltak)
                    .collect(toSet());

            OppfolgingsBruker bruker = finnBruker(brukere, aktorId);
            bruker.setTiltak(tiltak);
        });
    }


    private void leggTilTiltak(OppfolgingsBruker bruker) {
        leggTilTiltak(Collections.singletonList(bruker));
    }

    private void leggTilAktiviteter(List<OppfolgingsBruker> brukere) {
        if (brukere == null || brukere.isEmpty()) {
            throw new IllegalArgumentException();
        }

        validateBatchSize(brukere);

        List<PersonId> personIder = brukere.stream()
                .map(OppfolgingsBruker::getPerson_id)
                .map(PersonId::of)
                .collect(toList());

        Map<PersonId, Set<AktivitetStatus>> alleAktiviteterForBrukere = aktivitetDAO.getAktivitetstatusForBrukere(personIder);

        alleAktiviteterForBrukere.forEach((personId, statuserForBruker) -> {

            OppfolgingsBruker bruker = finnBruker(brukere, personId);

            statuserForBruker.forEach(status -> {
                IndekseringUtils.leggTilUtlopsDato(bruker, status);
                IndekseringUtils.leggTilStartDato(bruker, status);
            });

            Set<String> aktiviteterSomErAktive = statuserForBruker.stream()
                    .filter(AktivitetStatus::isAktiv)
                    .map(AktivitetStatus::getAktivitetType)
                    .collect(toSet());

            bruker.setAktiviteter(aktiviteterSomErAktive);
        });
    }

    private void leggTilSisteEndring(List<OppfolgingsBruker> brukere) {
        if (brukere == null || brukere.isEmpty()) {
            throw new IllegalArgumentException();
        }

        validateBatchSize(brukere);
        sisteEndringRepository.setAlleSisteEndringTidspunkter(brukere);
    }

    private void leggTilAktiviteter(OppfolgingsBruker bruker) {
        leggTilAktiviteter(Collections.singletonList(bruker));
    }

    private void leggTilSisteEndring(OppfolgingsBruker bruker) {
        leggTilSisteEndring(Collections.singletonList(bruker));
    }

    @SneakyThrows
    public void nyHovedIndeksering(List<AktorId> brukere) {
        long tidsStempel0 = System.currentTimeMillis();
        log.info("Hovedindeksering: Indekserer {} brukere", brukere.size());

        boolean success = indexerInParallel(brukere);
        if (success) {
            long tid = System.currentTimeMillis() - tidsStempel0;
            log.info("Hovedindeksering: Ferdig på {} ms, indekserte {} brukere", tid, brukere.size());
        } else {
            log.error("Hovedindeksering: ble ikke fullført");
        }
    }

    @SneakyThrows
    private boolean indexerInParallel(List<AktorId> alleBrukere) {
        List<List<AktorId>> brukerePartition = Lists.partition(alleBrukere, (alleBrukere.size() / getNumberOfThreads()) + 1);
        ExecutorService executor = Executors.newFixedThreadPool(getNumberOfThreads());
        executor.execute(() ->
                brukerePartition.parallelStream().forEach(bolk ->
                        partition(bolk, BATCH_SIZE).forEach(this::indekserBolk)
                )
        );

        return executor.awaitTermination(7, TimeUnit.HOURS);
    }

    public void indekserBolk(List<AktorId> aktorIds) {
        indekserBolk(aktorIds, this.alias);
    }

    public void indekserBolk(List<AktorId> aktorIds, IndexName index) {
        validateBatchSize(aktorIds);
        List<OppfolgingsBruker> brukere = brukerRepository.hentBrukereFraView(aktorIds)
                .stream().filter(bruker -> bruker.getAktoer_id() != null).toList();

        leggTilAktiviteter(brukere);
        leggTilTiltak(brukere);
        leggTilSisteEndring(brukere);

        this.skrivTilIndeks(index.getValue(), brukere);
    }

    private int getNumberOfThreads() {
        if (isDevelopment().orElse(false)) {
            return 2;
        }
        return 8;
    }

}
