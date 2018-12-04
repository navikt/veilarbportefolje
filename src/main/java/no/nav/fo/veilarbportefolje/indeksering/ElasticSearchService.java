package no.nav.fo.veilarbportefolje.indeksering;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.util.Utils;
import no.nav.json.JsonUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.ES_DELTAINDEKSERING;
import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.ES_TOTALINDEKSERING;
import static no.nav.fo.veilarbportefolje.indeksering.ElasticSearchUtils.finnBruker;
import static no.nav.fo.veilarbportefolje.indeksering.IndekseringConfig.ALIAS;
import static no.nav.fo.veilarbportefolje.indeksering.IndekseringConfig.BATCH_SIZE;
import static no.nav.fo.veilarbportefolje.util.AktivitetUtils.filtrerBrukertiltak;
import static no.nav.fo.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.REMOVE;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

@Slf4j
public class ElasticSearchService implements IndekseringService {

    private RestHighLevelClient client;

    private AktivitetDAO aktivitetDAO;

    private BrukerRepository brukerRepository;

    private LockingTaskExecutor shedlock;

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
            "    }\n" +
            "  }\n" +
            "}";

    @Inject
    public ElasticSearchService(RestHighLevelClient client, AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, LockingTaskExecutor shedlock) {
        this.client = client;
        this.aktivitetDAO = aktivitetDAO;
        this.brukerRepository = brukerRepository;
        this.shedlock = shedlock;
    }

    @Override
    public void hovedindeksering() {

        shedlock.executeWithLock(() -> {
                    log.info("Hovedindeksering: Starter hovedindeksering i Elastic Search");
                    long t0 = System.currentTimeMillis();

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

                    log.info("Hovedindeksering: Peker alias mot ny indeks og sletter den gamle");
                    Optional<String> gammelIndeks = hentGammeltIndeksNavn();
                    gammelIndeks.ifPresent(navn -> {
                        fjernAliasFraGammelIndeks(navn);
                        slettGammelIndeks(navn);
                    });

                    opprettNyIndeks(nyIndeks);

                    long t1 = System.currentTimeMillis();
                    long time = t1 - t0;

                    log.info("Hovedindeksering: Hovedindeksering for {} brukere fullførte på {}ms", brukere.size(), time);
                },
                new LockConfiguration(ES_TOTALINDEKSERING, Instant.now().plusSeconds(60 * 60 * 3)));
    }

    @Override
    public void deltaindeksering() {
        shedlock.executeWithLock(() -> {

            log.info("Deltaindeksering: Starter deltaindeksering i Elastic Search");
            List<BrukerDTO> brukere = brukerRepository.hentOppdaterteBrukereUnderOppfolging();

            log.info("Deltaindeksering: Hentet ut {} oppdaterte brukere i Elastic Search", brukere.size());

            Utils.splittOppListe(brukere, BATCH_SIZE).forEach(brukerBatch -> {
                leggTilAktiviteter(brukere);
                leggTilTiltak(brukere);
                skrivTilIndeks(ALIAS, brukere);
            });

            log.info("Deltaindeksering: Deltaindeksering for {} brukere er utført", brukere.size());

        }, new LockConfiguration(ES_DELTAINDEKSERING, Instant.now().plusSeconds(50)));
    }

    @Override
    @SneakyThrows
    public void slettBruker(String fnr) {

        DeleteByQueryRequest deleteQuery = new DeleteByQueryRequest()
                .setQuery(new TermQueryBuilder("fnr", fnr));

        BulkByScrollResponse response = client.deleteByQuery(deleteQuery, DEFAULT);
        if (response.getDeleted() != 1) {
            throw new RuntimeException("Feil under sletting av bruker i indeks");
        }
    }

    @Override
    public void slettBruker(PersonId personid) {
        throw new IllegalStateException();
    }

    @Override
    public void indekserBrukerdata(PersonId personId) {
        throw new IllegalStateException();
    }

    @Override
    public void indekserAsynkront(AktoerId aktoerId) {
        runAsync(() -> {
            BrukerDTO bruker = brukerRepository.hentBruker(aktoerId);

            if (erUnderOppfolging(bruker)) {
                leggTilAktiviteter(bruker);
                leggTilTiltak(bruker);
                skrivTilIndeks(ALIAS, bruker);
            } else {
                slettBruker(bruker.fnr);
            }

        });
    }

    @Override
    public void indekserBrukere(List<PersonId> personIds) {
        throw new IllegalStateException();
    }

    @SneakyThrows
    private Optional<String> hentGammeltIndeksNavn() {
        GetAliasesRequest getAliasRequest = new GetAliasesRequest(ALIAS);
        GetAliasesResponse aliasResponse = client.indices().getAlias(getAliasRequest, DEFAULT);
        return aliasResponse.getAliases().keySet().stream().findFirst();
    }

    @SneakyThrows
    private void opprettNyIndeks(String nyIndeks) {
        AliasActions action = new AliasActions(ADD)
                .index(nyIndeks)
                .alias(ALIAS);

        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(action);

        AcknowledgedResponse addAliasResponse = client.indices().updateAliases(request);
        if (!addAliasResponse.isAcknowledged()) {
            throw new RuntimeException(String.format("Kunne ikke oppdatere ALIAS: %s", ALIAS));
        }

    }

    @SneakyThrows
    private void fjernAliasFraGammelIndeks(String gammelIndeks) {
        AliasActions removeAliasAction = new AliasActions(REMOVE)
                .index(gammelIndeks)
                .alias(ALIAS);

        IndicesAliasesRequest request = new IndicesAliasesRequest()
                .addAliasAction(removeAliasAction);

        AcknowledgedResponse addAliasResponse = client.indices().updateAliases(request);
        if (!addAliasResponse.isAcknowledged()) {
            throw new RuntimeException(String.format("Kunne ikke oppdatere ALIAS: %s", ALIAS));
        }
    }

    @SneakyThrows
    private void slettGammelIndeks(String gammelIndeks) {
        AcknowledgedResponse response = client
                .indices()
                .delete(new DeleteIndexRequest(gammelIndeks));

        if (!response.isAcknowledged()) {
            throw new RuntimeException(String.format("Kunne ikke slette gammel indeks %s", gammelIndeks));
        }
    }

    @SneakyThrows
    private void skrivTilIndeks(String indeksNavn, List<BrukerDTO> oppfolgingsBrukere) {
        BulkRequest bulk = new BulkRequest();
        oppfolgingsBrukere.stream()
                .map(JsonUtils::toJson)
                .map(json -> new IndexRequest(indeksNavn, "_doc").source(json, XContentType.JSON))
                .forEach(bulk::add);

        BulkResponse response = client.bulk(bulk);
        if (response.hasFailures()) {
            throw new RuntimeException(response.buildFailureMessage());
        }
    }

    private void skrivTilIndeks(String indeksNavn, BrukerDTO brukerDTO) {
        skrivTilIndeks(indeksNavn, Collections.singletonList(brukerDTO));
    }

    @SneakyThrows
    private String opprettNyIndeks() {
        String indexName = ElasticSearchUtils.createIndexName(ALIAS);
        CreateIndexRequest request = new CreateIndexRequest(indexName)
                .mapping("_doc", mappingJson, XContentType.JSON);
        client.indices().create(request);
        return indexName;
    }

    private void validateBatchSize(List<BrukerDTO> brukere) {
        if (brukere.size() > BATCH_SIZE) {
            throw new IllegalStateException("Kan ikke prossessere flere enn 1000 brukere av gangen pga begrensninger i Oracle DB samt minnebruk!");
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

            statuserForBruker.forEach(status -> ElasticSearchUtils.leggTilUtlopsDato(bruker, status));

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
    public void commit() {
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
