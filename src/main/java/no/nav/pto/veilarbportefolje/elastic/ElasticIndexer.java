package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.CollectionUtils;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.metrikker.MetricsUtils;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.createIndexName;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAlias;
import static no.nav.pto.veilarbportefolje.elastic.IndekseringUtils.finnBruker;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.filtrerBrukertiltak;
import static no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.REMOVE;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
public class ElasticIndexer {

    final static int BATCH_SIZE = 1000;
    final static int BATCH_SIZE_LIMIT = 1000;
    private final ElasticService elasticService;
    private final RestHighLevelClient restHighLevelClient;
    private final AktivitetDAO aktivitetDAO;
    private final BrukerRepository brukerRepository;
    private final UnleashService unleashService;
    private final MetricsClient metricsClient;

    private final CvService cvService;

    private String indexName;

    public ElasticIndexer(
            AktivitetDAO aktivitetDAO,
            BrukerRepository brukerRepository,
            RestHighLevelClient restHighLevelClient,
            ElasticService elasticService,
            UnleashService unleashService,
            MetricsClient metricsClient,
            CvService cvService,
            String indexName
    ) {

        this.aktivitetDAO = aktivitetDAO;
        this.brukerRepository = brukerRepository;
        this.restHighLevelClient = restHighLevelClient;
        this.elasticService = elasticService;
        this.unleashService = unleashService;
        this.cvService = cvService;
        this.metricsClient = metricsClient;
        this.indexName = indexName;
    }

    @SneakyThrows
    public void startIndeksering() {
        if (unleashService.isEnabled(FeatureToggle.HOVEDINDEKSERING_MED_PAGING)) {
            nyHovedIndekseringMedPaging();
        } else {
            gammelHovedIndeksering();
        }
    }

    private void gammelHovedIndeksering() {
        log.info("Hovedindeksering: Starter hovedindeksering i Elasticsearch");
        long t0 = System.currentTimeMillis();
        Timestamp tidsstempel = Timestamp.valueOf(LocalDateTime.now());

        String nyIndeks = opprettNyIndeks(createIndexName(getAlias()));
        log.info("Hovedindeksering: Opprettet ny index {}", nyIndeks);


        List<OppfolgingsBruker> brukere = brukerRepository.hentAlleBrukereUnderOppfolging();
        log.info("Hovedindeksering: Hentet {} oppfølgingsbrukere fra databasen", brukere.size());

        log.info("Hovedindeksering: Batcher opp uthenting av aktiviteter og tiltak samt skriveoperasjon til indeks (BATCH_SIZE={})", BATCH_SIZE);
        CollectionUtils.partition(brukere, BATCH_SIZE).forEach(brukerBatch -> {
            leggTilAktiviteter(brukerBatch);
            leggTilTiltak(brukerBatch);
            skrivTilIndeks(nyIndeks, brukerBatch);
        });

        Optional<String> gammelIndeks = hentGammeltIndeksNavn();
        if (gammelIndeks.isPresent()) {
            log.info("Hovedindeksering: Peker alias mot ny indeks og sletter den gamle");
            flyttAliasTilNyIndeks(gammelIndeks.get(), nyIndeks);
            slettGammelIndeks(gammelIndeks.get());
        } else {
            log.info("Hovedindeksering: Lager alias til ny indeks");
            opprettAliasForIndeks(nyIndeks);
        }

        long t1 = System.currentTimeMillis();
        long time = t1 - t0;

        brukerRepository.oppdaterSistIndeksertElastic(tidsstempel);
        log.info("Hovedindeksering: Hovedindeksering for {} brukere fullførte på {}ms", brukere.size(), time);
    }

    private void nyHovedIndekseringMedPaging() {
        log.info("Starter hovedindeksering");

        String nyIndeks = opprettNyIndeks(createIndexName(getAlias()));
        log.info("Opprettet ny indeks {}", nyIndeks);

        int antallBrukere = brukerRepository.hentAntallBrukereUnderOppfolging().orElseThrow(IllegalStateException::new);

        log.info("Starter oppdatering av {} brukere i indeks med aktiviteter, tiltak og ytelser fra arena (BATCH_SIZE={})", antallBrukere, BATCH_SIZE);

        int currentPage = 0;
        for (int fra = 0; fra < antallBrukere; fra = utregnTil(fra, BATCH_SIZE)) {

            int til = utregnTil(fra, BATCH_SIZE);

            int numberOfPages = antallBrukere / BATCH_SIZE - 1;
            currentPage = currentPage + 1;

            log.info("{}/{} Indekserer brukere fra {} til {} av {}", currentPage, numberOfPages, fra, til, antallBrukere);

            List<OppfolgingsBruker> brukere = brukerRepository.hentAlleBrukereUnderOppfolging(fra, til);
            log.info("Hentet ut {} brukere fra databasen", brukere.size());

            leggTilAktiviteter(brukere);
            leggTilTiltak(brukere);

            skrivTilIndeks(nyIndeks, brukere);
        }

        Optional<String> gammelIndeks = hentGammeltIndeksNavn();
        if (gammelIndeks.isPresent()) {
            log.info("Peker alias mot ny indeks {} og sletter gammel indeks {}", nyIndeks, gammelIndeks);
            flyttAliasTilNyIndeks(gammelIndeks.get(), nyIndeks);
            slettGammelIndeks(gammelIndeks.get());
        } else {
            log.info("Oppretter alias til ny indeks: {}", nyIndeks);
            opprettAliasForIndeks(nyIndeks);
        }

        brukerRepository.oppdaterSistIndeksertElastic(Timestamp.valueOf(now()));
        log.info("Ferdig! Hovedindeksering for {} brukere er gjennomført!", antallBrukere);
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

    public void deltaindeksering() {
        if (indeksenIkkeFinnes()) {
            String message = format("Deltaindeksering: finner ingen indeks med alias %s", getAlias());
            throw new IllegalStateException(message);
        }

        Event event = new Event("portefolje.antall.brukere");
        event.addFieldToReport("antall_brukere", ElasticUtils.getCount());
        metricsClient.report(event);

        log.info("Deltaindeksering: Starter deltaindeksering i Elasticsearch");

        List<OppfolgingsBruker> brukere = brukerRepository.hentOppdaterteBrukere();

        List<String> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).collect(toList());

        log.info("Deltaindeksering: hentet ut {} oppdaterte brukere {}", brukere.size(), aktoerIder);

        if (brukere.isEmpty()) {
            log.info("Deltaindeksering: Ingen oppdaterte brukere ble funnet. Avslutter.");
            return;
        }

        Timestamp timestamp = Timestamp.valueOf(now());

        CollectionUtils.partition(brukere, BATCH_SIZE).forEach(brukerBatch -> {

            List<OppfolgingsBruker> brukereFortsattUnderOppfolging = brukerBatch.stream()
                    .filter(UnderOppfolgingRegler::erUnderOppfolging)
                    .collect(toList());

            if (!brukereFortsattUnderOppfolging.isEmpty()) {
                log.info("Deltaindeksering: Legger til aktiviteter");
                leggTilAktiviteter(brukereFortsattUnderOppfolging);
                log.info("Deltaindeksering: Legger til tiltak");
                leggTilTiltak(brukereFortsattUnderOppfolging);
                log.info("Deltaindeksering: Skriver til indeks");
                skrivTilIndeks(getAlias(), brukereFortsattUnderOppfolging);
            }

            log.info("Deltaindeksering: Sletter brukere som ikke lenger ligger under oppfolging");
            slettBrukereIkkeLengerUnderOppfolging(brukerBatch);
        });

        log.info("Deltaindeksering: Indeks oppdatert for {} brukere {}", brukere.size(), aktoerIder);

        brukerRepository.oppdaterSistIndeksertElastic(timestamp);

        int antall = brukere.size();

        metricsClient.report(new Event("es.deltaindeksering.fullfort").addFieldToReport("es.antall.oppdateringer", antall));
    }

    @SneakyThrows
    private boolean indeksenIkkeFinnes() {
        GetIndexRequest request = new GetIndexRequest();
        request.indices(getAlias());

        boolean exists = restHighLevelClient.indices().exists(request, DEFAULT);
        return !exists;
    }

    private void slettBrukereIkkeLengerUnderOppfolging(List<OppfolgingsBruker> brukerBatch) {
        brukerBatch.stream()
                .filter(bruker -> !erUnderOppfolging(bruker))
                .forEach(this::slettBruker);
    }

    @SneakyThrows
    public void slettBruker(OppfolgingsBruker bruker) {

        if (unleashService.isEnabled(FeatureToggle.MARKER_SOM_SLETTET)) {
            updateOppfolgingTilFalse(bruker);
        } else {
            List<Integer> backoffs = Arrays.asList(1000, 3000, 6000, 0);

            for (Integer backoff : backoffs) {
                DeleteByQueryRequest deleteQuery = new DeleteByQueryRequest(getAlias())
                        .setQuery(new TermQueryBuilder("fnr", bruker.getFnr()));

                try {
                    BulkByScrollResponse response = restHighLevelClient.deleteByQuery(deleteQuery, DEFAULT);
                    if (response.getDeleted() == 1) {
                        log.info("Sletting: slettet bruker {} (personId {})", bruker.getAktoer_id(), bruker.getPerson_id());
                    }
                } catch (ElasticsearchStatusException e) {
                    String mld = format("ElasticsearchStatusException for bruker %s (personId %s)", bruker.getAktoer_id(), bruker.getPerson_id());
                    log.warn(mld, e);
                    Thread.sleep(backoff);
                } catch (Exception e) {
                    String mld = format("Sletting feilet for bruker %s (personId %s)", bruker.getAktoer_id(), bruker.getPerson_id());
                    log.error(mld, e);
                }
            }
        }
    }

    private void updateOppfolgingTilFalse(OppfolgingsBruker bruker) throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(getAlias());
        updateRequest.type("_doc");
        updateRequest.id(bruker.getFnr());
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("oppfolging", false)
                .endObject()
        );

        restHighLevelClient.update(updateRequest, DEFAULT);
    }

    public CompletableFuture<Void> indekserAsynkront(AktoerId aktoerId) {

        CompletableFuture<Void> future = runAsync(() -> indekser(aktoerId));

        future.exceptionally(e -> {
            RuntimeException wrappedException = new RuntimeException(e);
            log.warn("Feil under asynkron indeksering av bruker " + aktoerId, wrappedException);
            return null;
        });

        return future;
    }

    public Result<OppfolgingsBruker> indekser(AktoerId aktoerId) {

        Result<OppfolgingsBruker> result = MetricsUtils.timed(
                "portefolje.indeks.hentBruker",
                () -> brukerRepository.hentBruker(aktoerId),
                metricsClient
        );

        if (result.isErr()) {
            log.error("Kunne ikke hente bruker {} ", aktoerId);
            return result;
        }

        OppfolgingsBruker bruker = result.orElseThrowException();

        if (erUnderOppfolging(bruker)) {
            MetricsUtils.timed(
                    "portefolje.indeks.leggTilAktiviteter",
                    () -> leggTilAktiviteter(bruker),
                    metricsClient
            );

            MetricsUtils.timed(
                    "portefolje.indeks.leggTilTiltak",
                    () -> leggTilTiltak(bruker),
                    metricsClient
            );

            MetricsUtils.timed(
                    "portefolje.indeks.skrivTilIndeks",
                    () -> skrivTilIndeks(getAlias(), bruker),
                    metricsClient
            );

        } else {
            slettBruker(bruker);
        }

        return result;
    }

    public Result<OppfolgingsBruker> indekser(Fnr fnr) {
        Result<OppfolgingsBruker> result = brukerRepository.hentBruker(fnr);
        if (result.isErr()) {
            log.error("Kunne ikke hente bruker {} ", fnr.toString());
            return result;
        }

        OppfolgingsBruker bruker = result.orElseThrowException();

        if (erUnderOppfolging(bruker)) {
            leggTilAktiviteter(bruker);
            leggTilTiltak(bruker);
            skrivTilIndeks(getAlias(), bruker);
        } else {
            slettBruker(bruker);
        }

        return result;
    }

    public void indekserBrukere(List<PersonId> personIds) {
        CollectionUtils.partition(personIds, BATCH_SIZE).forEach(batch -> {
            List<OppfolgingsBruker> brukere = brukerRepository.hentBrukere(batch);
            leggTilAktiviteter(brukere);
            leggTilTiltak(brukere);
            skrivTilIndeks(indexName, brukere);
        });
    }

    @SneakyThrows
    public Optional<String> hentGammeltIndeksNavn() {
        GetAliasesRequest getAliasRequest = new GetAliasesRequest(getAlias());
        GetAliasesResponse response = restHighLevelClient.indices().getAlias(getAliasRequest, DEFAULT);
        return response.getAliases().keySet().stream().findFirst();
    }

    @SneakyThrows
    private void opprettAliasForIndeks(String indeks) {
        AliasActions addAliasAction = new AliasActions(ADD)
                .index(indeks)
                .alias(getAlias());

        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(addAliasAction);
        AcknowledgedResponse response = restHighLevelClient.indices().updateAliases(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke legge til alias {}", getAlias());
            throw new RuntimeException();
        }
    }

    @SneakyThrows
    private void flyttAliasTilNyIndeks(String gammelIndeks, String nyIndeks) {

        AliasActions addAliasAction = new AliasActions(ADD)
                .index(nyIndeks)
                .alias(getAlias());

        AliasActions removeAliasAction = new AliasActions(REMOVE)
                .index(gammelIndeks)
                .alias(getAlias());

        IndicesAliasesRequest request = new IndicesAliasesRequest()
                .addAliasAction(removeAliasAction)
                .addAliasAction(addAliasAction);

        AcknowledgedResponse response = restHighLevelClient.indices().updateAliases(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke oppdatere alias {}", getAlias());
        }
    }

    @SneakyThrows
    public void slettGammelIndeks(String gammelIndeks) {
        AcknowledgedResponse response = restHighLevelClient.indices().delete(new DeleteIndexRequest(gammelIndeks), DEFAULT);
        if (!response.isAcknowledged()) {
            log.warn("Kunne ikke slette gammel indeks {}", gammelIndeks);
        }
    }

    @SneakyThrows
    public BulkResponse skrivTilIndeks(String indeksNavn, List<OppfolgingsBruker> oppfolgingsBrukere) {

        BulkRequest bulk = new BulkRequest();
        oppfolgingsBrukere.stream()
                .map(bruker -> new IndexRequest(indeksNavn, "_doc", bruker.getFnr()).source(toJson(bruker), XContentType.JSON))
                .forEach(bulk::add);

        BulkResponse response = restHighLevelClient.bulk(bulk, DEFAULT);

        if (response.hasFailures()) {
            log.warn("Klart ikke å skrive til indeks: {}", response.buildFailureMessage());
        }

        if (response.getItems().length != oppfolgingsBrukere.size()) {
            log.warn("Antall faktiske adds og antall brukere som skulle oppdateres er ulike");
        }

        List<String> aktoerIds = oppfolgingsBrukere.stream().map(bruker -> bruker.getAktoer_id()).collect(toList());
        log.info("Skrev {} brukere til indeks: {}", oppfolgingsBrukere.size(), aktoerIds);

        return response;
    }

    public void skrivTilIndeks(String indeksNavn, OppfolgingsBruker oppfolgingsBruker) {
        skrivTilIndeks(indeksNavn, Collections.singletonList(oppfolgingsBruker));
    }

    @SneakyThrows
    public String opprettNyIndeks(String navn) {

        String json = IOUtils.toString(getClass().getResource("/elastic_settings.json"), Charset.forName("UTF-8"));
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

        List<Fnr> fodselsnummere = brukere.stream()
                .map(OppfolgingsBruker::getFnr)
                .map(Fnr::of)
                .collect(toList());

        Map<Fnr, Set<Brukertiltak>> alleTiltakForBrukere = filtrerBrukertiltak(aktivitetDAO.hentBrukertiltak(fodselsnummere));

        alleTiltakForBrukere.forEach((fnr, brukerMedTiltak) -> {
            Set<String> tiltak = brukerMedTiltak.stream()
                    .map(Brukertiltak::getTiltak)
                    .collect(toSet());

            OppfolgingsBruker bruker = finnBruker(brukere, fnr);
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

    private void leggTilAktiviteter(OppfolgingsBruker bruker) {
        leggTilAktiviteter(Collections.singletonList(bruker));
    }


    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        return elasticService.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg, fra, antall, getAlias());
    }

    public StatusTall hentStatusTallForPortefolje(String enhet) {
        return elasticService.hentStatusTallForEnhet(enhet, getAlias());
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {
        return elasticService.hentPortefoljestorrelser(enhetId, getAlias());
    }

    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        return elasticService.hentStatusTallForVeileder(veilederIdent, enhet, getAlias());
    }

    public List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        return elasticService.hentBrukereMedArbeidsliste(veilederId.getVeilederId(), enhet, getAlias());
    }
}
