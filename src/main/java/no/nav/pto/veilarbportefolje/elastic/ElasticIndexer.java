package no.nav.pto.veilarbportefolje.elastic;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.utils.IdUtils;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.BrukertiltakV2;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepository;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.MDC;

import java.util.*;
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
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
public class ElasticIndexer {

    static final int BATCH_SIZE = 1000;
    static final int BATCH_SIZE_LIMIT = 1000;
    private final RestHighLevelClient restHighLevelClient;
    private final AktivitetDAO aktivitetDAO;
    private final BrukerRepository brukerRepository;
    private final IndexName alias;
    private final SisteEndringRepository sisteEndringRepository;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final UnleashService unleashService;

    public ElasticIndexer(
            AktivitetDAO aktivitetDAO,
            BrukerRepository brukerRepository,
            RestHighLevelClient restHighLevelClient,
            SisteEndringRepository sisteEndringRepository,
            IndexName alias,
            TiltakRepositoryV2 tiltakRepositoryV2,
            UnleashService unleashService) {

        this.aktivitetDAO = aktivitetDAO;
        this.brukerRepository = brukerRepository;
        this.restHighLevelClient = restHighLevelClient;
        this.sisteEndringRepository = sisteEndringRepository;
        this.tiltakRepositoryV2 = tiltakRepositoryV2;
        this.unleashService = unleashService;
        this.alias = alias;
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

    @SneakyThrows
    public void markerBrukerSomSlettet(OppfolgingsBruker bruker) {
        log.info("Markerer bruker {} som slettet", bruker.getAktoer_id());
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(alias.getValue());
        updateRequest.type("_doc");
        updateRequest.id(bruker.getAktoer_id());
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("oppfolging", false)
                .endObject()
        );

        restHighLevelClient.updateAsync(updateRequest, DEFAULT, new ActionListener<>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {
                log.info("Satte under oppfolging til false i elasticsearch");
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ResponseException) {
                    final int statusCode = ((ResponseException) e).getResponse().getStatusLine().getStatusCode();
                    if (statusCode != 404) {
                        log.error(format("Feil ved markering av bruker %s som slettet", bruker.getAktoer_id()), e);
                    }
                } else {
                    log.info("Elastic update feilet, ", e);
                }
            }
        });
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
            markerBrukerSomSlettet(bruker);
        }
    }

    public void skrivTilIndeks(String indeksNavn, List<OppfolgingsBruker> oppfolgingsBrukere) {

        BulkRequest bulk = new BulkRequest();
        oppfolgingsBrukere.stream()
                .map(bruker -> new IndexRequest(indeksNavn, "_doc", bruker.getAktoer_id()).source(toJson(bruker), XContentType.JSON))
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

    @SneakyThrows
    public String opprettNyIndeks(String navn) {

        String json = IOUtils.toString(Objects.requireNonNull(getClass().getResource("/elastic_settings.json")));
        CreateIndexRequest request = new CreateIndexRequest(navn)
                .source(json, XContentType.JSON);

        CreateIndexResponse response = restHighLevelClient.indices().create(request, DEFAULT);
        if (!response.isAcknowledged()) {
            log.error("Kunne ikke opprette ny indeks {}", navn);
            throw new RuntimeException();
        }

        return navn;
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

        Map<AktorId, Set<BrukertiltakV2>> alleTiltakForBrukere = tiltakRepositoryV2.hentBrukertiltak(aktorIder);

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
