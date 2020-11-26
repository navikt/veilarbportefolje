package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticIndex;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticSearchResponse;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Slf4j
@Service
public class ElasticServiceV2 {
    private DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss.")
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
            .toFormatter();
    private final String indeks;
    private final Supplier<RestHighLevelClient> restHighLevelClientSupplier;

    @Autowired
    public ElasticServiceV2(Supplier<RestHighLevelClient> restHighLevelClientSupplier, ElasticIndex elasticIndex) {
        this.restHighLevelClientSupplier = restHighLevelClientSupplier;
        this.indeks = elasticIndex.getIndex();
    }

    public Optional<AktoerId> hentAktoerId(Fnr fnr){
        GetRequest getRequest = new GetRequest();
        getRequest.index(indeks);
        getRequest.type("_doc");
        getRequest.id(fnr.getFnr());

        try {
            GetResponse a = restHighLevelClientSupplier.get().get(getRequest, DEFAULT);
            String id = (String) a.getSource().get("aktoer_id");
            return Optional.of(AktoerId.of(id));
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.info("Kunne ikke finne dokument i oppersajon hent aktoer_id fra fnr.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Fnr> hentFnr(AktoerId aktoerId) {
        SearchSourceBuilder request =
                new SearchSourceBuilder().query(
                        boolQuery()
                                .must(termQuery("aktoer_id", aktoerId.aktoerId))
                );

        ElasticSearchResponse response = search(request, ElasticSearchResponse.class);
        return response.getHits().getHits().stream()
                .map(hit -> Fnr.of(hit.get_source().getFnr()))
                .collect(toList());
    }

    @SneakyThrows
    public void updateHarDeltCv(Fnr fnr, boolean harDeltCv) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indeks);
        updateRequest.type("_doc");
        updateRequest.id(fnr.getFnr());
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("har_delt_cv", harDeltCv)
                .endObject()
        );

        try {
            restHighLevelClientSupplier.get().update(updateRequest, DEFAULT);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.info("Kunne ikke finne dokument ved oppdatering av cv");
            }
        }
    }

    @SneakyThrows
    public void updateRegistering(Fnr fnr, ArbeidssokerRegistrertEvent utdanningEvent) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indeks);
        updateRequest.type("_doc");
        updateRequest.id(fnr.getFnr());
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("brukers_situasjon", utdanningEvent.getBrukersSituasjon())
                .field("utdanning", utdanningEvent.getUtdanning())
                .field("utdanning_bestatt", utdanningEvent.getUtdanningBestatt())
                .field("utdanning_godkjent", utdanningEvent.getUtdanningGodkjent())
                .endObject()
        );

        try {
            restHighLevelClientSupplier.get().update(updateRequest, DEFAULT);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.info("Kunne ikke finne dokument ved oppdatering av registering");
            }
        }
    }

    @SneakyThrows
    public void updateArbeidsliste(ArbeidslisteDTO arbeidslisteDTO) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indeks);
        updateRequest.type("_doc");
        updateRequest.id(arbeidslisteDTO.getFnr().getFnr());
        String frist = arbeidslisteDTO.getFrist() == null ? null : arbeidslisteDTO.getFrist().toString();
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("arbeidsliste_aktiv",true)
                .field("arbeidsliste_overskrift", arbeidslisteDTO.getOverskrift())
                .field("arbeidsliste_kommentar", arbeidslisteDTO.getKommentar())
                .field("arbeidsliste_frist", frist)
                .field("arbeidsliste_kategori", arbeidslisteDTO.getKategori().name())
                .field("arbeidsliste_endringstidspunkt", arbeidslisteDTO.getEndringstidspunkt().toString())
                .field("arbeidsliste_sist_endret_av_veilederid", arbeidslisteDTO.getVeilederId().veilederId)
                //.field("NAV_KONTOR_FOR_ARBEIDSLISTE", arbeidslisteDTO.getNavKontorForArbeidsliste())
                .endObject()
        );
        try {
            restHighLevelClientSupplier.get().update(updateRequest, DEFAULT);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.info("Kunne ikke finne dokument ved oppdatering av arbeidsliste");
            }
        }
    }

    @SneakyThrows
    public AktoerId slettArbeidsliste(Fnr fnr, AktoerId aktoerId) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indeks);
        updateRequest.type("_doc");
        updateRequest.id(fnr.getFnr());
        updateRequest.doc(jsonBuilder()
                .startObject()
                .field("arbeidsliste_aktiv",false)
                .nullField("arbeidsliste_overskrift")
                .nullField("arbeidsliste_kommentar")
                .nullField("arbeidsliste_frist")
                .nullField("arbeidsliste_kategori")
                .nullField("arbeidsliste_endringstidspunkt")
                .nullField("arbeidsliste_sist_endret_av_veilederid")
                .endObject()
        );
        try {
            restHighLevelClientSupplier.get().update(updateRequest, DEFAULT);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.info("Kunne ikke finne dokument ved oppdatering av arbeidsliste");
            }
        }
        return aktoerId;
    }

    public Optional<Arbeidsliste> hentArbeidsListe(Fnr fnr){
        GetRequest getRequest = new GetRequest();
        getRequest.index(indeks);
        getRequest.type("_doc");
        getRequest.id(fnr.getFnr());

        try {
            GetResponse a = restHighLevelClientSupplier.get().get(getRequest, DEFAULT);
            return Optional.ofNullable(arbeidslisteMapper(a.getSource()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @SneakyThrows
    private <T> T search(SearchSourceBuilder searchSourceBuilder, Class<T> clazz) {
        SearchRequest request = new SearchRequest()
                .indices(indeks)
                .source(searchSourceBuilder);

        SearchResponse response = restHighLevelClientSupplier.get().search(request, RequestOptions.DEFAULT);
        return JsonUtils.fromJson(response.toString(), clazz);
    }

    @SneakyThrows
    private Arbeidsliste arbeidslisteMapper(Map<String,Object> rs) {
        if(rs.get("arbeidsliste_aktiv") != null && (boolean)rs.get("arbeidsliste_aktiv")){
            ZonedDateTime frist = rs.get("arbeidsliste_frist") == null ? null : ZonedDateTime.of(LocalDateTime.parse((String)rs.get("arbeidsliste_frist"),formatter), ZoneId.of("UTC"));
            ZonedDateTime endringstidspunkt = rs.get("arbeidsliste_endringstidspunkt") == null ? null : ZonedDateTime.of(LocalDateTime.parse((String)rs.get("arbeidsliste_endringstidspunkt"),formatter), ZoneId.of("UTC"));
            Arbeidsliste.Kategori kategori = rs.get("arbeidsliste_kategori") == null ? null : Arbeidsliste.Kategori.valueOf((String) rs.get("arbeidsliste_kategori"));

            return new Arbeidsliste(
                    VeilederId.of((String) rs.get("arbeidsliste_sist_endret_av_veilederid")),
                    endringstidspunkt,
                    (String) rs.get("arbeidsliste_overskrift"),
                    (String) rs.get("arbeidsliste_kommentar"),
                    frist,
                    kategori
                    )
                    .setArbeidslisteAktiv(true);
            }
        return null;
    }

}
