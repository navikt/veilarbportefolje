package no.nav.pto.veilarbportefolje.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;

import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.BrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.postgres.PostgresOpensearchMapper;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;
import static no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpensearchIndexer {
    public static final int BATCH_SIZE = 1000;
    public static final int BATCH_SIZE_LIMIT = 1000;

    private final RestHighLevelClient restHighLevelClient;
    private final BrukerRepositoryV2 brukerRepositoryV2;
    private final IndexName alias;
    private final PostgresOpensearchMapper postgresOpensearchMapper;
    private final OpensearchIndexerV2 opensearchIndexerV2;

    public void indekser(AktorId aktoerId) {
        Optional<OppfolgingsBruker> bruker;
        bruker = brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktoerId)).stream().findAny();
        bruker.ifPresentOrElse(this::indekserBruker, () -> opensearchIndexerV2.slettDokumenter(List.of(aktoerId)));
    }

    private void indekserBruker(OppfolgingsBruker bruker) {
        if (erUnderOppfolging(bruker)) {
            flettInnNodvendigData(List.of(bruker));
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

    @SneakyThrows
    public void skrivBulkTilIndeks(String indeksNavn, List<OppfolgingsBruker> oppfolgingsBrukere) {
        BulkRequest bulk = new BulkRequest();
        List<String> aktoerIds = oppfolgingsBrukere.stream().map(OppfolgingsBruker::getAktoer_id).toList();
        oppfolgingsBrukere.stream()
                .map(bruker -> {
                    IndexRequest indexRequest = new IndexRequest(indeksNavn).id(bruker.getAktoer_id());
                    return indexRequest.source(toJson(bruker), XContentType.JSON);
                })
                .forEach(bulk::add);

        try {
            restHighLevelClient.bulk(bulk, RequestOptions.DEFAULT);
            secureLog.info("Skrev {} brukere til indeks: {}", oppfolgingsBrukere.size(), aktoerIds);
        } catch (IOException e) {
            secureLog.error(String.format("Klart ikke å skrive til indeks: %s", aktoerIds), e);
            throw e;
        }
    }

    private void validateBatchSize(List<?> brukere) {
        if (brukere.size() > BATCH_SIZE_LIMIT) {
            throw new IllegalStateException(format("Kan ikke prossessere flere enn %s brukere av gangen pga begrensninger i Postgres db", BATCH_SIZE_LIMIT));
        }
    }

    public void oppdaterAlleBrukereIOpensearch(List<AktorId> brukere) {
        long tidsStempel0 = System.currentTimeMillis();
        log.info("Hovedindeksering: Indekserer {} brukere", brukere.size());

        batchIndeksering(brukere);
        long tid = System.currentTimeMillis() - tidsStempel0;
        log.info("Hovedindeksering: Ferdig på {} ms, indekserte {} brukere", tid, brukere.size());
    }

    public void batchIndeksering(List<AktorId> alleBrukere) {
        try {
            partition(alleBrukere, BATCH_SIZE).forEach(this::indekserBolk);
        } catch (Exception e) {
            log.error("Hovedindeksering: ble ikke fullført", e);
            throw e;
        }
    }

    public void indekserBolk(List<AktorId> aktorIds) {
        validateBatchSize(aktorIds);

        List<OppfolgingsBruker> brukere = brukerRepositoryV2.hentOppfolgingsBrukere(aktorIds);

        if (brukere != null && !brukere.isEmpty()) {
            flettInnNodvendigData(brukere);
            this.skrivBulkTilIndeks(alias.getValue(), brukere);
        }
    }

    private void flettInnNodvendigData(List<OppfolgingsBruker> brukere) {
        postgresOpensearchMapper.flettInnAvvik14aVedtak(brukere);
        postgresOpensearchMapper.flettInnAktivitetsData(brukere);
        postgresOpensearchMapper.flettInnSisteEndringerData(brukere);
        postgresOpensearchMapper.flettInnStatsborgerskapData(brukere);
        postgresOpensearchMapper.flettInnEnsligeForsorgereData(brukere);
        postgresOpensearchMapper.flettInnBarnUnder18Aar(brukere);
        postgresOpensearchMapper.flettInnTiltakshendelser(brukere);
        postgresOpensearchMapper.flettInnSiste14aVedtak(brukere);
        postgresOpensearchMapper.flettInnEldsteUtgattVarsel(brukere);

        postgresOpensearchMapper.flettInnOpplysningerOmArbeidssoekerData(brukere);

        if (brukere.isEmpty()) {
            log.warn("Skriver ikke til index da alle brukere i batchen er ugyldige");
        }
    }

    public void dryrunAvPostgresTilOpensearchMapping(List<AktorId> brukereUnderOppfolging) {
        partition(brukereUnderOppfolging, BATCH_SIZE).forEach(bolk -> {
            List<OppfolgingsBruker> brukere = brukerRepositoryV2.hentOppfolgingsBrukere(bolk, true);
        });
    }
}
