package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringDTO;
import no.nav.pto.veilarbportefolje.sistelest.SistLestKafkaMelding;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
public class ElasticServiceV2 {

    private final IndexName indexName;
    private final RestHighLevelClient restHighLevelClient;

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
        String kategori = dto.getKategori().name().toLowerCase();
        final String tidspunkt = toIsoUTC(dto.getTidspunkt());

        final XContentBuilder content = jsonBuilder()
                .startObject()
                    .startObject("siste_endringer")
                        .startObject(kategori)
                            .field("tidspunkt", tidspunkt)
                            .field("aktivtetId", dto.getAktivtetId())
                        .endObject()
                    .endObject()
                .endObject();
        update(dto.getAktoerId(), content, format("Oppdaterte siste endring med tidspunkt: %s", tidspunkt));
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
    public void markerBrukerSomSlettet(AktorId aktoerId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("oppfolging", false)
                .endObject();

        update(aktoerId, content, "Slettet bruker");
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
                .field("arbeidsliste_aktiv",true)
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
                .field("arbeidsliste_sist_endret_av_veilederid", (String)null)
                .field("arbeidsliste_endringstidspunkt", (String)null)
                .field("arbeidsliste_kommentar", (String)null)
                .field("arbeidsliste_overskrift", (String)null)
                .field("arbeidsliste_frist", (String)null)
                .field("arbeidsliste_kategori", (String)null)
                .endObject();

        update(aktoerId, content, "Sletter arbeidsliste");
    }

    @SneakyThrows
    public void updateSistLest(SistLestKafkaMelding melding) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("sist_lest_aktivitetsplanen", toIsoUTC(melding.getHarLestTidspunkt()))
                .endObject();

        update(melding.getAktorId(), content, "Oppdaterer sist lest");
    }

    private void update(AktorId aktoerId, XContentBuilder content, String logInfo) throws IOException {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indexName.getValue());
        updateRequest.type("_doc");
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

}
