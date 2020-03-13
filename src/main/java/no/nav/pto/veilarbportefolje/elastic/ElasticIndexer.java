package no.nav.pto.veilarbportefolje.elastic;

import io.micrometer.core.instrument.Counter;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.CollectionUtils;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetStatus;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.io.IOUtils;
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
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.BATCH_SIZE;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.BATCH_SIZE_LIMIT;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.createIndexName;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAlias;
import static no.nav.pto.veilarbportefolje.elastic.IndekseringUtils.finnBruker;
import static no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetUtils.filtrerBrukertiltak;
import static no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.REMOVE;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
public class ElasticIndexer {

    private final ElasticService elasticService;
    private final Counter writeCounter;

    private RestHighLevelClient client;

    private AktivitetDAO aktivitetDAO;

    private BrukerRepository brukerRepository;

    @Inject
    public ElasticIndexer(
            AktivitetDAO aktivitetDAO,
            BrukerRepository brukerRepository,
            RestHighLevelClient client,
            ElasticService elasticService
    ) {
        this.aktivitetDAO = aktivitetDAO;
        this.brukerRepository = brukerRepository;
        this.client = client;
        this.elasticService = elasticService;
        writeCounter = Counter.builder("portefolje_elastic_writes").register(getMeterRegistry());
    }

    @SneakyThrows
    public void startIndeksering() {
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
            leggTilAliasTilIndeks(nyIndeks);
        }

        long t1 = System.currentTimeMillis();
        long time = t1 - t0;

        brukerRepository.oppdaterSistIndeksertElastic(tidsstempel);
        log.info("Hovedindeksering: Hovedindeksering for {} brukere fullførte på {}ms", brukere.size(), time);
    }

    public void deltaindeksering() {
        if (indeksenIkkeFinnes()) {
            String message = format("Deltaindeksering: finner ingen indeks med alias %s", getAlias());
            throw new IllegalStateException(message);
        }

        log.info("Deltaindeksering: Starter deltaindeksering i Elasticsearch");

        List<OppfolgingsBruker> brukere = brukerRepository.hentOppdaterteBrukere();

        if (brukere.isEmpty()) {
            log.info("Deltaindeksering: Fullført (Ingen oppdaterte brukere ble funnet)");
            return;
        }

        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        CollectionUtils.partition(brukere, BATCH_SIZE).forEach(brukerBatch -> {

            List<OppfolgingsBruker> brukereFortsattUnderOppfolging = brukerBatch.stream()
                    .filter(UnderOppfolgingRegler::erUnderOppfolging)
                    .collect(Collectors.toList());

            if (!brukereFortsattUnderOppfolging.isEmpty()) {
                leggTilAktiviteter(brukereFortsattUnderOppfolging);
                leggTilTiltak(brukereFortsattUnderOppfolging);
                skrivTilIndeks(getAlias(), brukereFortsattUnderOppfolging);
            }

            slettBrukereIkkeLengerUnderOppfolging(brukerBatch);

        });

        List<String> aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).collect(Collectors.toList());
        List<String> personIder = brukere.stream().map(OppfolgingsBruker::getPerson_id).collect(Collectors.toList());
        log.info("DELTAINDEKSERING: Indeks oppdatert for {} brukere med aktoerId {} og personId {})", brukere.size(), aktoerIder, personIder);

        brukerRepository.oppdaterSistIndeksertElastic(timestamp);

        int antall = brukere.size();

        Event event = MetricsFactory.createEvent("es.deltaindeksering.fullfort");
        event.addFieldToReport("es.antall.oppdateringer", antall);
        event.report();
    }

    @SneakyThrows
    private boolean indeksenIkkeFinnes() {
        GetIndexRequest request = new GetIndexRequest();
        request.indices(getAlias());

        boolean exists = client.indices().exists(request, DEFAULT);
        return !exists;
    }

    private void slettBrukereIkkeLengerUnderOppfolging(List<OppfolgingsBruker> brukerBatch) {
        brukerBatch.stream()
                .filter(bruker -> !erUnderOppfolging(bruker))
                .forEach(this::slettBruker);
    }

    @SneakyThrows
    public void slettBruker(OppfolgingsBruker bruker) {

        DeleteByQueryRequest deleteQuery = new DeleteByQueryRequest(getAlias())
                .setQuery(new TermQueryBuilder("fnr", bruker.getFnr()));

        BulkByScrollResponse response = client.deleteByQuery(deleteQuery, DEFAULT);
        if (response.getDeleted() == 1) {
            log.info("Slettet bruker med aktorId {} og personId {} fra indeks {}", bruker.getAktoer_id(), bruker.getPerson_id(), getAlias());
        } else {
            String message = String.format("Feil ved sletting av bruker med aktoerId %s og personId %s i indeks %s", bruker.getAktoer_id(), bruker.getPerson_id(), getAlias());
            throw new RuntimeException(message);
        }
    }

    public CompletableFuture<Void> indekserAsynkront(AktoerId aktoerId) {

        CompletableFuture<Void> future = runAsync(() -> indekser(aktoerId));

        future.exceptionally(e -> {
            throw new RuntimeException(e);
        });
        return future;
    }

    public void indekser(AktoerId aktoerId) {
        OppfolgingsBruker bruker = brukerRepository.hentBruker(aktoerId);

        if (erUnderOppfolging(bruker)) {
            leggTilAktiviteter(bruker);
            leggTilTiltak(bruker);
            skrivTilIndeks(getAlias(), bruker);
        } else {
            slettBruker(bruker);
        }
    }

    public void indekserBrukere(List<PersonId> personIds) {
        CollectionUtils.partition(personIds, BATCH_SIZE).forEach(batch -> {
            List<OppfolgingsBruker> brukere = brukerRepository.hentBrukere(batch);
            leggTilAktiviteter(brukere);
            leggTilTiltak(brukere);
            skrivTilIndeks(getAlias(), brukere);
        });
    }

    @SneakyThrows
    public Optional<String> hentGammeltIndeksNavn() {
        GetAliasesRequest getAliasRequest = new GetAliasesRequest(getAlias());
        GetAliasesResponse response = client.indices().getAlias(getAliasRequest, DEFAULT);
        return response.getAliases().keySet().stream().findFirst();
    }

    @SneakyThrows
    private void leggTilAliasTilIndeks(String indeks) {
        AliasActions addAliasAction = new AliasActions(ADD)
                .index(indeks)
                .alias(getAlias());

        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(addAliasAction);
        AcknowledgedResponse response = client.indices().updateAliases(request, DEFAULT);

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

        AcknowledgedResponse response = client.indices().updateAliases(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke oppdatere alias {}", getAlias());
        }
    }

    @SneakyThrows
    public void slettGammelIndeks(String gammelIndeks) {
        AcknowledgedResponse response = client.indices().delete(new DeleteIndexRequest(gammelIndeks), DEFAULT);
        if (!response.isAcknowledged()) {
            log.warn("Kunne ikke slette gammel indeks {}", gammelIndeks);
        }
    }

    @SneakyThrows
    public void skrivTilIndeks(String indeksNavn, List<OppfolgingsBruker> oppfolgingsBrukere) {

        BulkRequest bulk = new BulkRequest();
        oppfolgingsBrukere.stream()
                .map(bruker -> new IndexRequest(indeksNavn, "_doc", bruker.getFnr()).source(toJson(bruker), XContentType.JSON))
                .forEach(bulk::add);

        BulkResponse response = client.bulk(bulk, DEFAULT);

        writeCounter.increment();

        if (response.hasFailures()) {
            throw new RuntimeException(response.buildFailureMessage());
        }
        log.info("Skrev {} brukere til indeks", oppfolgingsBrukere.size());
    }

    public Try<UpdateResponse> oppdaterBruker(Tuple2<Fnr, XContentBuilder> tupleAvFnrOgJson) {
        UpdateRequest updateRequest = new UpdateRequest()
                .index(getAlias())
                .type("_doc")
                .id(tupleAvFnrOgJson._1.toString())
                .doc(tupleAvFnrOgJson._2);

        return Try.of(() -> client.update(updateRequest, DEFAULT));
    }

    public void skrivTilIndeks(String indeksNavn, OppfolgingsBruker oppfolgingsBruker) {
        skrivTilIndeks(indeksNavn, Collections.singletonList(oppfolgingsBruker));
    }

    @SneakyThrows
    public String opprettNyIndeks(String navn) {

        String json = IOUtils.toString(getClass().getResource("/elastic_settings.json"), Charset.forName("UTF-8"));
        CreateIndexRequest request = new CreateIndexRequest(navn)
                .source(json, XContentType.JSON);

        CreateIndexResponse response = client.indices().create(request, DEFAULT);
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
        leggTilAktiviteter(Collections.singletonList(bruker));
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

    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        return elasticService.hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg, null, null, getAlias());
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
