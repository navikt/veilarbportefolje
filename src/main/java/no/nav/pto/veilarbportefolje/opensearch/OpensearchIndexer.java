package no.nav.pto.veilarbportefolje.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell;
import no.nav.pto.veilarbportefolje.postgres.BrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.postgres.PostgresOpensearchMapper;
import org.opensearch.OpenSearchException;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS;
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
    private final PostgresOpensearchMapper postgresOpensearchMapper;

    public void indekser(AktorId aktoerId) {
        Optional<PortefoljebrukerOpensearchModell> brukerOpensearchModell;
        brukerOpensearchModell = brukerRepositoryV2.hentPortefoljeBrukereTilOpensearchModell(List.of(aktoerId)).stream().findAny();
        brukerOpensearchModell.ifPresentOrElse(this::indekserBruker, () -> slettDokumenter(List.of(aktoerId)));
    }

    @SneakyThrows
    public void syncronIndekseringsRequest(PortefoljebrukerOpensearchModell brukerOpensearchModell) {
        IndexRequest indexRequest = new IndexRequest(BRUKERINDEKS_ALIAS).id(brukerOpensearchModell.getAktoer_id());
        indexRequest.source(toJson(brukerOpensearchModell), XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    @SneakyThrows
    public void skrivBulkTilIndeks(String indeksNavn, List<PortefoljebrukerOpensearchModell> brukerOpensearchModellList) {
        BulkRequest bulk = new BulkRequest();
        List<String> aktoerIds = brukerOpensearchModellList.stream().map(PortefoljebrukerOpensearchModell::getAktoer_id).toList();
        brukerOpensearchModellList.stream()
                .map(bruker -> {
                    IndexRequest indexRequest = new IndexRequest(indeksNavn).id(bruker.getAktoer_id());
                    return indexRequest.source(toJson(bruker), XContentType.JSON);
                })
                .forEach(bulk::add);

        try {
            restHighLevelClient.bulk(bulk, RequestOptions.DEFAULT);
            secureLog.info("Skrev {} brukere til indeks: {}", brukerOpensearchModellList.size(), aktoerIds);
        } catch (IOException e) {
            secureLog.error(String.format("Klart ikke å skrive til indeks: %s", aktoerIds), e);
            throw e;
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

        List<PortefoljebrukerOpensearchModell> brukere = brukerRepositoryV2.hentPortefoljeBrukereTilOpensearchModell(aktorIds);

        if (brukere != null && !brukere.isEmpty()) {
            flettInnNodvendigData(brukere);
            this.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, brukere);
        }
    }

    @SneakyThrows
    public void slettDokumenter(List<AktorId> aktorIds) {
        secureLog.info("Sletter gamle aktorIder {}", aktorIds);
        for (AktorId aktorId : aktorIds) {
            if (aktorId != null) {
                delete(aktorId);
            }
        }
    }

    @SneakyThrows
    private void delete(AktorId aktoerId) {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index(BRUKERINDEKS_ALIAS);
        deleteRequest.id(aktoerId.get());

        try {
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
            secureLog.info("Slettet dokument for {} ", aktoerId);
        } catch (OpenSearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                secureLog.info("Kunne ikke finne dokument for bruker {} ved sletting av indeks", aktoerId.get());
            } else {
                final String message = format("Det skjedde en feil ved sletting i opensearch for bruker %s", aktoerId.get());
                secureLog.error(message, e);
            }
        }
    }

    private void indekserBruker(PortefoljebrukerOpensearchModell brukerOpensearchModell) {
        if (erUnderOppfolging(brukerOpensearchModell)) {
            flettInnNodvendigData(List.of(brukerOpensearchModell));
            syncronIndekseringsRequest(brukerOpensearchModell);
        } else {
            slettDokumenter(List.of(AktorId.of(brukerOpensearchModell.getAktoer_id())));
        }
    }

    private void flettInnNodvendigData(List<PortefoljebrukerOpensearchModell> brukerOpensearchModell) {
        postgresOpensearchMapper.flettInnAktivitetsData(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnSisteEndringerData(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnStatsborgerskapData(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnEnsligeForsorgereData(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnBarnUnder18Aar(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnTiltakshendelser(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnEldsteHendelsePerKategori(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnOpplysningerOmArbeidssoekerData(brukerOpensearchModell);
        postgresOpensearchMapper.flettInnGjeldende14aVedtak(brukerOpensearchModell);

        if (brukerOpensearchModell.isEmpty()) {
            log.warn("Skriver ikke til index da alle brukere i batchen er ugyldige");
        }
    }

    private void validateBatchSize(List<?> brukere) {
        if (brukere.size() > BATCH_SIZE_LIMIT) {
            throw new IllegalStateException(format("Kan ikke prossessere flere enn %s brukere av gangen pga begrensninger i Postgres db", BATCH_SIZE_LIMIT));
        }
    }
}
