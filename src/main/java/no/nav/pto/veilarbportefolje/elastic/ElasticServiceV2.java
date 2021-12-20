package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringDTO;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.BRUKERINDEKS_ALIAS;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;

@Slf4j
@Service
public class ElasticServiceV2 {

    private final IndexName indexName;
    private final RestHighLevelClient restHighLevelClient;

    @Autowired
    public ElasticServiceV2(RestHighLevelClient restHighLevelClient, IndexName indexName) {
        this.restHighLevelClient = restHighLevelClient;
        this.indexName = indexName;
    }

    @SneakyThrows
    public void updateRegistering(AktorId aktoerId, ArbeidssokerRegistrertEvent utdanningEvent) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("brukers_situasjon", utdanningEvent.getBrukersSituasjon())
                .field("utdanning", utdanningEvent.getUtdanning())
                .field("utdanning_bestatt", utdanningEvent.getUtdanningBestatt())
                .field("utdanning_godkjent", utdanningEvent.getUtdanningGodkjent())
                .endObject();

        update(aktoerId, content, "Oppdater registrering");
    }

    @SneakyThrows
    public void updateSisteEndring(SisteEndringDTO dto) {
        String kategori = dto.getKategori().name();
        final String tidspunkt = toIsoUTC(dto.getTidspunkt());

        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("siste_endringer")
                .startObject(kategori)
                .field("tidspunkt", tidspunkt)
                .field("aktivtetId", dto.getAktivtetId())
                .field("er_sett", "N")
                .endObject()
                .endObject()
                .endObject();
        update(dto.getAktoerId(), content, format("Oppdaterte siste endring med tidspunkt: %s", tidspunkt));
    }


    @SneakyThrows
    public void updateSisteEndring(AktorId aktorId, SisteEndringsKategori kategori) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("siste_endringer")
                .startObject(kategori.name())
                .field("er_sett", "J")
                .endObject()
                .endObject()
                .endObject();
        update(aktorId, content, format("Oppdaterte siste endring, kategori %s er nå sett", kategori.name()));
    }

    @SneakyThrows
    public void updateHarDeltCv(AktorId aktoerId, boolean harDeltCv) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("har_delt_cv", harDeltCv)
                .endObject();

        update(aktoerId, content, format("Har delt cv: %s", harDeltCv));
    }

    @SneakyThrows
    public void updateCvEksistere(AktorId aktoerId, boolean cvEksistere) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("cv_eksistere", cvEksistere)
                .endObject();

        update(aktoerId, content, format("CV eksistere: %s", cvEksistere));
    }

    @SneakyThrows
    public void settManuellStatus(AktorId aktoerId, String manuellStatus) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("manuell_bruker", manuellStatus)
                .endObject();

        update(aktoerId, content, format("Satt til manuell bruker: %s", manuellStatus));
    }

    @SneakyThrows
    public void oppdaterNyForVeileder(AktorId aktoerId, boolean nyForVeileder) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("ny_for_veileder", nyForVeileder)
                .endObject();

        update(aktoerId, content, format("Oppdatert ny for veileder: %s", nyForVeileder));
    }

    @SneakyThrows
    public void oppdaterVeileder(AktorId aktoerId, VeilederId veilederId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("veileder_id", veilederId.toString())
                .field("ny_for_enhet", false)
                .field("ny_for_veileder", true)
                .endObject();

        update(aktoerId, content, "Oppdatert veileder");
    }

    @SneakyThrows
    public void updateDialog(Dialogdata melding) {
        final String venterPaaSvarFraBruker = toIsoUTC(melding.getTidspunktEldsteVentende());
        final String venterPaaSvarFraNav = toIsoUTC(melding.getTidspunktEldsteUbehandlede());

        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("venterpasvarfrabruker", venterPaaSvarFraBruker)
                .field("venterpasvarfranav", venterPaaSvarFraNav)
                .endObject();

        update(
                AktorId.of(melding.getAktorId()),
                content,
                format("Oppdatert dialog med venter på svar fra nav: %s og venter på svar fra bruker: %s", venterPaaSvarFraNav, venterPaaSvarFraBruker)
        );
    }

    @SneakyThrows
    public void updateArbeidsliste(ArbeidslisteDTO arbeidslisteDTO) {
        log.info("Oppdater arbeidsliste for {} med frist {}", arbeidslisteDTO.getAktorId(), arbeidslisteDTO.getFrist());
        final String frist = toIsoUTC(arbeidslisteDTO.getFrist());
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("arbeidsliste_aktiv", true)
                .field("arbeidsliste_overskrift", arbeidslisteDTO.getOverskrift())
                .field("arbeidsliste_kommentar", arbeidslisteDTO.getKommentar())
                .field("arbeidsliste_frist", Optional.ofNullable(frist).orElse(getFarInTheFutureDate()))
                .field("arbeidsliste_sist_endret_av_veilederid", arbeidslisteDTO.getVeilederId().getValue())
                .field("arbeidsliste_endringstidspunkt", toIsoUTC(arbeidslisteDTO.getEndringstidspunkt()))
                .field("arbeidsliste_kategori", arbeidslisteDTO.getKategori().name())
                .endObject();

        update(arbeidslisteDTO.getAktorId(), content, format("Oppdatert arbeidsliste med frist: %s", frist));
    }

    @SneakyThrows
    public void slettArbeidsliste(AktorId aktoerId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("arbeidsliste_aktiv", false)
                .field("arbeidsliste_sist_endret_av_veilederid", (String) null)
                .field("arbeidsliste_endringstidspunkt", (String) null)
                .field("arbeidsliste_kommentar", (String) null)
                .field("arbeidsliste_overskrift", (String) null)
                .field("arbeidsliste_frist", (String) null)
                .field("arbeidsliste_kategori", (String) null)
                .endObject();

        update(aktoerId, content, "Sletter arbeidsliste");
    }

    @SneakyThrows
    public void slettDokumenter(List<AktorId> aktorIds) {
        log.info("Sletter gamle aktorIder {}", aktorIds);
        for (AktorId aktorId : aktorIds) {
            if (aktorId != null) {
                delete(aktorId);
            }
        }
    }

    private void update(AktorId aktoerId, XContentBuilder content, String logInfo) throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indexName.getValue());
        updateRequest.id(aktoerId.get());
        updateRequest.doc(content);
        updateRequest.retryOnConflict(6);

        try {
            restHighLevelClient.update(updateRequest, DEFAULT);
            log.info("Oppdaterte dokument for bruker {} med info {}", aktoerId, logInfo);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.warn("Kunne ikke finne dokument for bruker {} ved oppdatering av indeks", aktoerId.toString());
            } else {
                final String message = format("Det skjedde en feil ved oppdatering av elastic for bruker %s", aktoerId.toString());
                log.error(message, e);
            }
        }
    }

    @SneakyThrows
    private void delete(AktorId aktoerId) {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index(indexName.getValue());
        deleteRequest.id(aktoerId.get());

        try {
            restHighLevelClient.delete(deleteRequest, DEFAULT);
            log.info("Slettet dokument for {} ", aktoerId);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                log.info("Kunne ikke finne dokument for bruker {} ved sletting av indeks", aktoerId.get());
            } else {
                final String message = format("Det skjedde en feil ved sletting i elastic for bruker %s", aktoerId.get());
                log.error(message, e);
            }
        }
    }

    @SneakyThrows
    private GetResponse fetchDocument(AktorId aktoerId) {
        GetRequest getRequest = new GetRequest();
        getRequest.index(indexName.getValue());
        getRequest.id(aktoerId.toString());
        return restHighLevelClient.get(getRequest, DEFAULT);
    }

    @SneakyThrows
    public void opprettNyIndeks(String indeksNavn) {
        InputStream resourceAsStream = getClass().getResourceAsStream("/elastic_settings.json");
        String json = IOUtils.toString(resourceAsStream, "UTF-8");

        CreateIndexRequest request = new CreateIndexRequest(indeksNavn)
                .source(json, XContentType.JSON);

        CreateIndexResponse response = restHighLevelClient.indices().create(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke opprette ny indeks {}", indeksNavn);
            throw new RuntimeException();
        }
    }

    private static String createIndexName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("%s_%s", BRUKERINDEKS_ALIAS, timestamp);
    }

    @SneakyThrows
    public void opprettAliasForIndeks(String indeks) {
        IndicesAliasesRequest.AliasActions addAliasAction = new IndicesAliasesRequest.AliasActions(ADD)
                .index(indeks)
                .alias(BRUKERINDEKS_ALIAS);

        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(addAliasAction);
        AcknowledgedResponse response = restHighLevelClient.indices().updateAliases(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke legge til alias {}", BRUKERINDEKS_ALIAS);
            throw new RuntimeException();
        }
    }
}
