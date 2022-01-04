package no.nav.pto.veilarbportefolje.elastic;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.utils.IdUtils;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV1;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.BrukertiltakV2;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepository;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.pto.veilarbportefolje.elastic.IndekseringUtils.finnBruker;
import static no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Slf4j
@Service
public class ElasticIndexer {

    static final int BATCH_SIZE = 250;
    static final int BATCH_SIZE_LIMIT = 250;
    private final RestHighLevelClient restHighLevelClient;
    private final AktivitetDAO aktivitetDAO;
    private final BrukerRepository brukerRepository;
    private final IndexName alias;
    private final SisteEndringRepository sisteEndringRepository;
    private final TiltakRepositoryV1 tiltakRepositoryV1;
    private final ElasticServiceV2 elasticServiceV2;

    @Autowired
    public ElasticIndexer(
            AktivitetDAO aktivitetDAO,
            BrukerRepository brukerRepository,
            RestHighLevelClient restHighLevelClient,
            SisteEndringRepository sisteEndringRepository,
            IndexName alias,
            TiltakRepositoryV1 tiltakRepositoryV1, ElasticServiceV2 elasticServiceV2) {

        this.aktivitetDAO = aktivitetDAO;
        this.brukerRepository = brukerRepository;
        this.restHighLevelClient = restHighLevelClient;
        this.sisteEndringRepository = sisteEndringRepository;
        this.tiltakRepositoryV1 = tiltakRepositoryV1;
        this.alias = alias;
        this.elasticServiceV2 = elasticServiceV2;
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
            elasticServiceV2.slettDokumenter(List.of(AktorId.of(bruker.getAktoer_id())));
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

        restHighLevelClient.bulkAsync(bulk, DEFAULT, new ActionListener<>() {
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


    private void validateBatchSize(List<OppfolgingsBruker> brukere) {
        if (brukere.size() > BATCH_SIZE_LIMIT) {
            throw new IllegalStateException(format("Kan ikke prossessere flere enn %s brukere av gangen pga begrensninger i oracle db", BATCH_SIZE_LIMIT));
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
    public void nyHovedIndeksering(List<AktorId> aktorIds) {
        long tidsStempel0 = System.currentTimeMillis();

        log.info("Hovedindeksering: Starter 'ny' hovedindeksering i Elasticsearch");
        log.info("Hovedindeksering: Indekserer {} brukere", aktorIds.size());
        List<List<AktorId>> brukerePartition = Lists.partition(aktorIds, aktorIds.size() / 5);

        int antallTraader = brukerePartition.size();
        log.info("Hovedindeksering: Bruker {} tråder ", antallTraader);

        ExecutorService executor = Executors.newFixedThreadPool(antallTraader);
        CountDownLatch ferdigSignal = new CountDownLatch(antallTraader);

        brukerePartition.forEach(brukerePart -> executor.execute(() -> startAsyncPartition(brukerePart, ferdigSignal)));
        executor.shutdown();

        boolean hovedindekseringFullfort = ferdigSignal.await(8, TimeUnit.HOURS);
        long tidsStempel1 = System.currentTimeMillis();
        long tid = tidsStempel1 - tidsStempel0;
        if (hovedindekseringFullfort) {
            log.info("Hovedindeksering: Ferdig på {} ms, indekserte {} brukere, brukte {} tråder", tid, aktorIds.size(), antallTraader);
        } else {
            log.info("Hovedindeksering: Ble ikke ferdig, den timet ut på {} ms", tid);
            executor.shutdownNow();
        }
    }

    private void startAsyncPartition(List<AktorId> brukere, CountDownLatch ferdigSignal) {
        String hashID = IdUtils.generateId();
        log.info("Hovedindeksering: Startet for hash {} med {} brukere", hashID, brukere.size());
        MDC.put("jobId", hashID);

        partition(brukere, BATCH_SIZE).forEach(this::indekserBolk);
        log.info("Hovedindeksering: Avsluttet trådnummer {}", hashID);
        ferdigSignal.countDown();
    }

    public void indekserBolk(List<AktorId> aktorIds) {
        partition(aktorIds, BATCH_SIZE).forEach(partition -> {
            List<OppfolgingsBruker> brukere = brukerRepository.hentBrukereFraView(partition).stream().filter(bruker -> bruker.getAktoer_id() != null).collect(toList());
            leggTilAktiviteter(brukere);
            leggTilTiltak(brukere);
            leggTilSisteEndring(brukere);
            this.skrivTilIndeks(alias.getValue(), brukere);
        });
    }

}
