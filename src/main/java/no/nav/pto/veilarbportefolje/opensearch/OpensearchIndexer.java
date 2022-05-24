package no.nav.pto.veilarbportefolje.opensearch;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.BrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.postgres.PostgresOpensearchMapper;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.opensearch.action.ActionListener;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpensearchIndexer {
    public static final int BATCH_SIZE = 1000;
    public static final int ORACLE_BATCH_SIZE_LIMIT = 1000;

    private final RestHighLevelClient restHighLevelClient;
    private final BrukerRepositoryV2 brukerRepositoryV2;
    private final IndexName alias;
    private final UnleashService unleashService;
    private final PostgresOpensearchMapper postgresOpensearchMapper;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    public void indekser(AktorId aktoerId) {
        Optional<OppfolgingsBruker> bruker;
        bruker = brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktoerId)).stream().findAny();
        bruker.ifPresent(this::indekserBruker);
    }

    private void indekserBruker(OppfolgingsBruker bruker) {
        if (erUnderOppfolging(bruker)) {
            postgresOpensearchMapper.flettInnAktivitetsData(List.of(bruker));
            postgresOpensearchMapper.flettInnSisteEndringerData(List.of(bruker));

            syncronIndekseringsRequest(bruker);
        } else {
            opensearchIndexerV2.slettDokumenter(List.of(AktorId.of(bruker.getAktoer_id())));
        }
    }

    @SneakyThrows
    public void syncronIndekseringsRequest(OppfolgingsBruker bruker) {
        IndexRequest indexRequest = new IndexRequest(alias.getValue()).id(bruker.getAktoer_id());
        indexRequest.source(toJson(bruker), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
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

    private void validateBatchSize(List<?> brukere) {
        if (brukere.size() > ORACLE_BATCH_SIZE_LIMIT) {
            throw new IllegalStateException(format("Kan ikke prossessere flere enn %s brukere av gangen pga begrensninger i oracle db", ORACLE_BATCH_SIZE_LIMIT));
        }
    }

    @SneakyThrows
    public void oppdaterAlleBrukereIOpensearch(List<AktorId> brukere) {
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
    public boolean indexerInParallel(List<AktorId> alleBrukere) {
        List<List<AktorId>> brukerePartition = Lists.partition(alleBrukere, (alleBrukere.size() / getNumberOfThreads()) + 1);
        ExecutorService executor = Executors.newFixedThreadPool(getNumberOfThreads());
        executor.execute(() ->
                brukerePartition.parallelStream().forEach(bolk -> {
                            try {
                                partition(bolk, BATCH_SIZE).forEach(this::indekserBolk);
                            } catch (Exception e) {
                                log.error("error under hovedindeksering", e);
                            }
                        }
                )
        );

        executor.shutdown();
        return executor.awaitTermination(7, TimeUnit.HOURS);
    }

    public void indekserBolk(List<AktorId> aktorIds) {
        indekserBolk(aktorIds, this.alias);
    }

    public void indekserBolk(List<AktorId> aktorIds, IndexName index) {
        validateBatchSize(aktorIds);

        List<OppfolgingsBruker> brukere;
        brukere = brukerRepositoryV2.hentOppfolgingsBrukere(aktorIds);
        postgresOpensearchMapper.flettInnAktivitetsData(brukere);
        postgresOpensearchMapper.flettInnSisteEndringerData(brukere);

        this.skrivTilIndeks(index.getValue(), brukere);
    }

    private int getNumberOfThreads() {
        if (isDevelopment().orElse(false)) {
            return 1;
        }
        return 6;
    }

    public void dryrunAvPostgresTilOpensearchMapping(List<AktorId> brukereUnderOppfolging) {
        partition(brukereUnderOppfolging, BATCH_SIZE).forEach(bolk -> {
            List<OppfolgingsBruker> brukere = brukerRepositoryV2.hentOppfolgingsBrukere(bolk, true);
        });
    }
}
