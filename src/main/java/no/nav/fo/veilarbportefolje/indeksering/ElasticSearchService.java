package no.nav.fo.veilarbportefolje.indeksering;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.util.UnderOppfolgingRegler;
import no.nav.fo.veilarbportefolje.util.Utils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
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
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.ES_DELTAINDEKSERING;
import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.ES_TOTALINDEKSERING;
import static no.nav.fo.veilarbportefolje.indeksering.IndekseringConfig.*;
import static no.nav.fo.veilarbportefolje.indeksering.IndekseringUtils.finnBruker;
import static no.nav.fo.veilarbportefolje.util.AktivitetUtils.filtrerBrukertiltak;
import static no.nav.fo.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.REMOVE;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Slf4j
public class ElasticSearchService implements IndekseringService {

    private AktivitetDAO aktivitetDAO;

    private BrukerRepository brukerRepository;

    private LockingTaskExecutor shedlock;

    private Exception indekseringFeilet;

    static String mappingJson = "{\n" +
            "  \"properties\": {\n" +
            "    \"veileder_id\": {\n" +
            "      \"type\": \"keyword\"\n" +
            "    },\n" +
            "    \"enhet_id\": {\n" +
            "      \"type\": \"keyword\"\n" +
            "    },\n" +
            "    \"person_id\": {\n" +
            "      \"type\": \"keyword\"\n" +
            "    },\n" +
            "    \"aktoer_id\": {\n" +
            "      \"type\": \"keyword\"\n" +
            "    },\n" +
            "    \"etternavn\": {\n" +
            "      \"type\" : \"keyword\"\n" +
            "    },\n" +
            "    \"fnr\": {\n" +
            "      \"type\" : \"keyword\"\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    @Inject
    public ElasticSearchService(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, LockingTaskExecutor shedlock) {
        this.aktivitetDAO = aktivitetDAO;
        this.brukerRepository = brukerRepository;
        this.shedlock = shedlock;
    }

    public Exception hentIndekseringFeiletStatus() {
        return indekseringFeilet;
    }

    @Override
    public void hovedindeksering() {
        shedlock.executeWithLock(() -> {
                    try {
                        startIndeksering();
                        indekseringFeilet = null;
                    } catch (Exception e) {
                        log.error("Hovedindeksering: indeksering feilet {}", e.getMessage());
                        indekseringFeilet = e;
                    }
                },
                new LockConfiguration(ES_TOTALINDEKSERING, Instant.now().plusSeconds(60 * 60 * 3))
        );
    }

    @SneakyThrows
    private void startIndeksering() {
        log.info("Hovedindeksering: Starter hovedindeksering i Elasticsearch");
        long t0 = System.currentTimeMillis();
        Timestamp tidsstempel = Timestamp.valueOf(LocalDateTime.now());

        String nyIndeks = opprettNyIndeks();
        log.info("Hovedindeksering: Opprettet ny index {}", nyIndeks);


        List<BrukerDTO> brukere = brukerRepository.hentAlleBrukereUnderOppfolging();
        log.info("Hovedindeksering: Hentet {} oppfølgingsbrukere fra databasen", brukere.size());

        log.info("Hovedindeksering: Batcher opp uthenting av aktiviteter og tiltak samt skriveoperasjon til indeks (BATCH_SIZE={})", BATCH_SIZE);
        Utils.splittOppListe(brukere, BATCH_SIZE).forEach(brukerBatch -> {
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

    @Override
    public void deltaindeksering() {
        shedlock.executeWithLock(() -> {

            if (indeksenIkkeFinnes()) {
                log.error("Deltaindeksering: finner ingen indeks med alias {}", getAlias());
                return;
            }

            log.info("Deltaindeksering: Starter deltaindeksering i Elasticsearch");

            List<BrukerDTO> brukere = brukerRepository.hentOppdaterteBrukere();

            if (brukere.isEmpty()) {
                log.info("Deltaindeksering: Fullført (Ingen oppdaterte brukere ble funnet)");
                return;
            }

            Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

            Utils.splittOppListe(brukere, BATCH_SIZE).forEach(brukerBatch -> {

                List<BrukerDTO> brukereFortsattUnderOppfolging = brukerBatch.stream()
                        .filter(UnderOppfolgingRegler::erUnderOppfolging)
                        .collect(Collectors.toList());

                leggTilAktiviteter(brukereFortsattUnderOppfolging);
                leggTilTiltak(brukereFortsattUnderOppfolging);
                skrivTilIndeks(IndekseringConfig.getAlias(), brukereFortsattUnderOppfolging);

                slettBrukereIkkeLengerUnderOppfolging(brukerBatch);

            });

            List<String> aktoerIder = brukere.stream().map(BrukerDTO::getAktoer_id).collect(Collectors.toList());
            log.info("Deltaindeksering: Fullført ( {} brukere med aktoerId {} ble oppdatert)", brukere.size(), aktoerIder);

            brukerRepository.oppdaterSistIndeksertElastic(timestamp);

            int antall = brukere.size();
            Event event = MetricsFactory.createEvent("es.deltaindeksering.fullfort");
            event.addFieldToReport("es.antall.oppdateringer", antall);
            event.report();

        }, new LockConfiguration(ES_DELTAINDEKSERING, Instant.now().plusSeconds(50)));
    }

    private boolean indeksenIkkeFinnes() {
        GetIndexRequest request = new GetIndexRequest();
        request.indices(getAlias());

        Boolean exists = ElasticUtils.withClient(client -> {
            return client.indices().exists(request, DEFAULT);
        });

        return !exists;
    }

    private void slettBrukereIkkeLengerUnderOppfolging(List<BrukerDTO> brukerBatch) {
        brukerBatch.stream()
                .filter(bruker -> !erUnderOppfolging(bruker))
                .forEach(this::slettBruker);
    }

    public void slettBruker(BrukerDTO bruker) {
        slettBruker(bruker.fnr);
        log.info("Slettet bruker med aktørId {}", bruker.aktoer_id);
    }

    @Override
    @SneakyThrows
    public void slettBruker(String fnr) {

        BulkByScrollResponse response = ElasticUtils.withClient(client -> {

            DeleteByQueryRequest deleteQuery = new DeleteByQueryRequest(getAlias())
                    .setQuery(new TermQueryBuilder("fnr", fnr));

            return client.deleteByQuery(deleteQuery, DEFAULT);

        });

        if (response.getDeleted() != 1) {
            log.warn("Feil ved sletting av bruker i indeks {}", response.toString());
        }
    }


    @Override
    public void indekserAsynkront(AktoerId aktoerId) {
        runAsync(() -> {
            BrukerDTO bruker = brukerRepository.hentBruker(aktoerId);

            if (erUnderOppfolging(bruker)) {
                leggTilAktiviteter(bruker);
                leggTilTiltak(bruker);
                skrivTilIndeks(IndekseringConfig.getAlias(), bruker);
            } else {
                slettBruker(bruker.fnr);
            }

        });
    }

    @Override
    public void indekserBrukere(List<PersonId> personIds) {
        Utils.splittOppListe(personIds, BATCH_SIZE).forEach(batch -> {
            List<BrukerDTO> brukere = brukerRepository.hentBrukere(batch);
            leggTilAktiviteter(brukere);
            leggTilTiltak(brukere);
            skrivTilIndeks(IndekseringConfig.getAlias(), brukere);
        });
    }

    @SneakyThrows
    private Optional<String> hentGammeltIndeksNavn() {
        GetAliasesResponse response = ElasticUtils.withClient(client -> {
            GetAliasesRequest getAliasRequest = new GetAliasesRequest(IndekseringConfig.getAlias());
            return client.indices().getAlias(getAliasRequest, DEFAULT);
        });
        return response.getAliases().keySet().stream().findFirst();
    }

    @SneakyThrows
    private void leggTilAliasTilIndeks(String indeks) {
        AliasActions addAliasAction = new AliasActions(ADD)
                .index(indeks)
                .alias(IndekseringConfig.getAlias());

        AcknowledgedResponse response = ElasticUtils.withClient(client -> {
            IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(addAliasAction);
            return client.indices().updateAliases(request, DEFAULT);
        });

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke legge til alias {}", IndekseringConfig.getAlias());
            throw new RuntimeException();
        }
    }

    @SneakyThrows
    private void flyttAliasTilNyIndeks(String gammelIndeks, String nyIndeks) {

        AcknowledgedResponse response = ElasticUtils.withClient(client -> {

            AliasActions addAliasAction = new AliasActions(ADD)
                    .index(nyIndeks)
                    .alias(IndekseringConfig.getAlias());

            AliasActions removeAliasAction = new AliasActions(REMOVE)
                    .index(gammelIndeks)
                    .alias(IndekseringConfig.getAlias());

            IndicesAliasesRequest request = new IndicesAliasesRequest()
                    .addAliasAction(removeAliasAction)
                    .addAliasAction(addAliasAction);

            return client.indices().updateAliases(request, DEFAULT);
        });

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke oppdatere alias {}", IndekseringConfig.getAlias());
        }
    }

    @SneakyThrows
    private void slettGammelIndeks(String gammelIndeks) {

        AcknowledgedResponse response = ElasticUtils.withClient(client -> {
            return client
                    .indices()
                    .delete(new DeleteIndexRequest(gammelIndeks), DEFAULT);
        });

        if (!response.isAcknowledged()) {
            log.warn("Kunne ikke slette gammel indeks {}", gammelIndeks);
        }
    }

    @SneakyThrows
    private void skrivTilIndeks(String indeksNavn, List<BrukerDTO> oppfolgingsBrukere) {

        BulkResponse response = ElasticUtils.withClient(client -> {

            BulkRequest bulk = new BulkRequest();
            oppfolgingsBrukere.stream()
                    .map(DokumentDTO::new)
                    .map(dokument -> new IndexRequest(indeksNavn, "_doc", dokument.getId()).source(dokument.getJson(), XContentType.JSON))
                    .forEach(bulk::add);

            return client.bulk(bulk, DEFAULT);
        });

        if (response.hasFailures()) {
            throw new RuntimeException(response.buildFailureMessage());
        }
    }

    private void skrivTilIndeks(String indeksNavn, BrukerDTO brukerDTO) {
        skrivTilIndeks(indeksNavn, Collections.singletonList(brukerDTO));
    }

    @SneakyThrows
    private String opprettNyIndeks() {

        String indexName = IndekseringUtils.createIndexName(IndekseringConfig.getAlias());
        CreateIndexResponse response = ElasticUtils.withClient(client -> {
            CreateIndexRequest request = new CreateIndexRequest(indexName)
                    .mapping("_doc", mappingJson, XContentType.JSON);
            return client.indices().create(request, DEFAULT);
        });

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke opprette ny indeks {}", indexName);
            throw new RuntimeException();
        }

        return indexName;
    }

    private void validateBatchSize(List<BrukerDTO> brukere) {
        if (brukere.size() > BATCH_SIZE_LIMIT) {
            throw new IllegalStateException(format("Kan ikke prossessere flere enn %s brukere av gangen pga begrensninger i oracle db", BATCH_SIZE_LIMIT));
        }
    }

    private void leggTilTiltak(List<BrukerDTO> brukere) {

        validateBatchSize(brukere);

        List<Fnr> fodselsnummere = brukere.stream()
                .map(BrukerDTO::getFnr)
                .map(Fnr::of)
                .collect(toList());

        Map<Fnr, Set<Brukertiltak>> alleTiltakForBrukere = filtrerBrukertiltak(aktivitetDAO.hentBrukertiltak(fodselsnummere));

        alleTiltakForBrukere.forEach((fnr, brukerMedTiltak) -> {
            Set<String> tiltak = brukerMedTiltak.stream()
                    .map(Brukertiltak::getTiltak)
                    .collect(toSet());

            BrukerDTO bruker = finnBruker(brukere, fnr);
            bruker.setTiltak(tiltak);
        });
    }

    private void leggTilTiltak(BrukerDTO bruker) {
        leggTilAktiviteter(Collections.singletonList(bruker));
    }

    private void leggTilAktiviteter(List<BrukerDTO> brukere) {

        validateBatchSize(brukere);

        List<PersonId> personIder = brukere.stream()
                .map(BrukerDTO::getPerson_id)
                .map(PersonId::of)
                .collect(toList());

        Map<PersonId, Set<AktivitetStatus>> alleAktiviteterForBrukere = aktivitetDAO.getAktivitetstatusForBrukere(personIder);

        alleAktiviteterForBrukere.forEach((personId, statuserForBruker) -> {

            BrukerDTO bruker = finnBruker(brukere, personId);

            statuserForBruker.forEach(status -> IndekseringUtils.leggTilUtlopsDato(bruker, status));

            Set<String> aktiviteterSomErAktive = statuserForBruker.stream()
                    .filter(AktivitetStatus::isAktiv)
                    .map(AktivitetStatus::getAktivitetType)
                    .collect(toSet());

            bruker.setAktiviteter(aktiviteterSomErAktive);
        });
    }

    private void leggTilAktiviteter(BrukerDTO bruker) {
        leggTilAktiviteter(Collections.singletonList(bruker));
    }


    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        throw new IllegalStateException();
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        throw new IllegalStateException();
    }

    @Override
    public StatusTall hentStatusTallForPortefolje(String enhet) {
        throw new IllegalStateException();
    }

    @Override
    public FacetResults hentPortefoljestorrelser(String enhetId) {
        throw new IllegalStateException();
    }

    @Override
    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        throw new IllegalStateException();
    }

    @Override
    public List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        throw new IllegalStateException();
    }
}
