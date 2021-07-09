package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.BrukertiltakV2;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepository;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.nio.charset.Charset;
import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.filtrerBrukertiltak;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erGR202PaKafka;
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
                final int statusCode = ((ResponseException) e).getResponse().getStatusLine().getStatusCode();
                if (statusCode != 404) {
                    log.error(format("Feil ved markering av bruker %s som slettet", bruker.getAktoer_id()), e);
                }
            }
        });
    }

    public void indekser(AktorId aktoerId) {
        brukerRepository.hentBrukerFraView(aktoerId).ifPresent(this::indekserBruker);
    }

    public void indekser(List<PersonId> personIds) {
        partition(personIds, BATCH_SIZE).forEach(partition -> {
            final List<OppfolgingsBruker> oppfolgingsBrukers = brukerRepository.hentBrukereFraView(partition);
            oppfolgingsBrukers.forEach(this::indekserBruker);
        });
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

    @SneakyThrows
    public Optional<String> hentGammeltIndeksNavn() {
        GetAliasesRequest getAliasRequest = new GetAliasesRequest(alias.getValue());
        GetAliasesResponse response = restHighLevelClient.indices().getAlias(getAliasRequest, DEFAULT);
        return response.getAliases().keySet().stream().findFirst();
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

                List<String> aktoerIds = oppfolgingsBrukere.stream().map(bruker -> bruker.getAktoer_id()).collect(toList());
                log.info("Skrev {} brukere til indeks: {}", oppfolgingsBrukere.size(), aktoerIds);

            }

            @Override
            public void onFailure(Exception e) {
                log.warn("Feil under asynkron indeksering av brukerere ", e);
            }
        });


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

    private void leggTilTiltakV2(List<OppfolgingsBruker> brukere) {
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
        if (erGR202PaKafka(unleashService)) {
            leggTilTiltakV2(Collections.singletonList(bruker));
        } else {
            leggTilTiltak(Collections.singletonList(bruker));
        }
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

}
