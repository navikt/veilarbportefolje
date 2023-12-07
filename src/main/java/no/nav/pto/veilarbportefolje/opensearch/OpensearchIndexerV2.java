package no.nav.pto.veilarbportefolje.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringDTO;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;
import no.nav.pto.veilarbportefolje.util.FodselsnummerUtils;
import no.nav.pto.veilarbportefolje.util.OppfolgingUtils;
import org.opensearch.OpenSearchException;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.RestStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;
import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpensearchIndexerV2 {

    private final IndexName indexName;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final RestHighLevelClient restHighLevelClient;

    @SneakyThrows
    public void updateRegistering(AktorId aktoerId, ArbeidssokerRegistrertEvent arbeidssokerRegistrertEvent) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("brukers_situasjon", arbeidssokerRegistrertEvent.getBrukersSituasjon())
                .field("utdanning", arbeidssokerRegistrertEvent.getUtdanning())
                .field("utdanning_bestatt", arbeidssokerRegistrertEvent.getUtdanningBestatt())
                .field("utdanning_godkjent", arbeidssokerRegistrertEvent.getUtdanningGodkjent())
                .field("brukers_situasjon_sist_endret", toLocalDateOrNull(arbeidssokerRegistrertEvent.getRegistreringOpprettet()))
                .endObject();

        update(aktoerId, content, "Oppdater registrering");
    }

    @SneakyThrows
    public void updateEndringerIRegistering(AktorId aktoerId, ArbeidssokerBesvarelseEvent endringIRegistreringsdataEvent) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("brukers_situasjon", endringIRegistreringsdataEvent.getBesvarelse().getDinSituasjon().getVerdi())
                .field("brukers_situasjon_sist_endret", endringIRegistreringsdataEvent.getBesvarelse().getDinSituasjon().getEndretTidspunkt())
                .endObject();

        update(aktoerId, content, "Oppdater endring i registrering");
    }

    @SneakyThrows
    public void updateOppfolgingsbruker(AktorId aktoerId, OppfolgingsbrukerEntity oppfolgingsbruker, String utkast14aStatus) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("fnr", oppfolgingsbruker.fodselsnr())
                .field("formidlingsgruppekode", oppfolgingsbruker.formidlingsgruppekode())
                .field("iserv_fra_dato", toIsoUTC(oppfolgingsbruker.iserv_fra_dato()))
                .field("enhet_id", oppfolgingsbruker.nav_kontor())
                .field("kvalifiseringsgruppekode", oppfolgingsbruker.kvalifiseringsgruppekode())
                .field("rettighetsgruppekode", oppfolgingsbruker.rettighetsgruppekode())
                .field("hovedmaalkode", oppfolgingsbruker.hovedmaalkode())
                .field("fodselsdato", FodselsnummerUtils.lagFodselsdato(oppfolgingsbruker.fodselsnr()))
                .field("kjonn", FodselsnummerUtils.lagKjonn(oppfolgingsbruker.fodselsnr()))
                .field("fodselsdag_i_mnd", Integer.parseInt(FodselsnummerUtils.lagFodselsdagIMnd(oppfolgingsbruker.fodselsnr())))

                .field("trenger_revurdering", OppfolgingUtils.trengerRevurderingVedtakstotte(oppfolgingsbruker.formidlingsgruppekode(), oppfolgingsbruker.kvalifiseringsgruppekode(), utkast14aStatus))
                .field("trenger_vurdering", OppfolgingUtils.trengerVurdering(oppfolgingsbruker.rettighetsgruppekode(), oppfolgingsbruker.kvalifiseringsgruppekode()))
                .endObject();

        update(aktoerId, content, "Oppdater oppfolgingsbruker");
    }

    @SneakyThrows
    public void updateHuskelapp(AktorId aktoerId, HuskelappForBruker huskelapp) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("huskelapp")
                .field("frist", huskelapp.frist())
                .field("kommentar", huskelapp.kommentar())
                .endObject()
                .endObject();

        update(aktoerId, content, "Oppretter/redigerer huskelapp");
    }


    @SneakyThrows
    public void slettHuskelapp(AktorId aktoerId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .nullField("huskelapp")
                .endObject();

        update(aktoerId, content, "Sletter huskelapp");
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
    public void updateErSkjermet(AktorId aktorId, boolean erSkjermet) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("egen_ansatt", erSkjermet)
                .endObject();

        update(
                aktorId,
                content,
                format("Oppdatert er_skjermet %s for bruker: %s", erSkjermet, aktorId)
        );
    }

    @SneakyThrows
    public void updateSkjermetTil(AktorId aktorId, LocalDateTime skjermetTil) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("skjermet_til", skjermetTil)
                .endObject();

        update(
                aktorId,
                content,
                format("Oppdatert skjermet_til %s for bruker: %s", skjermetTil, aktorId)
        );
    }

    @SneakyThrows
    public void updateArbeidsliste(ArbeidslisteDTO arbeidslisteDTO) {
        secureLog.info("Oppdater arbeidsliste for {} med frist {}", arbeidslisteDTO.getAktorId(), arbeidslisteDTO.getFrist());
        final String frist = toIsoUTC(arbeidslisteDTO.getFrist());
        int arbeidsListeLengde = Optional.ofNullable(arbeidslisteDTO.getOverskrift())
                .map(String::length).orElse(0);
        String arbeidsListeSorteringsVerdi = Optional.ofNullable(arbeidslisteDTO.getOverskrift())
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0, Math.min(2, s.length())))
                .orElse("");

        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("arbeidsliste_aktiv", true)
                .field("arbeidsliste_tittel_sortering", arbeidsListeSorteringsVerdi)
                .field("arbeidsliste_tittel_lengde", arbeidsListeLengde)
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
                .field("arbeidsliste_tittel_sortering", (String) null)
                .field("arbeidsliste_tittel_lengde", 0)
                .field("arbeidsliste_sist_endret_av_veilederid", (String) null)
                .field("arbeidsliste_endringstidspunkt", (String) null)
                .field("arbeidsliste_frist", (String) null)
                .field("arbeidsliste_kategori", (String) null)
                .endObject();

        update(aktoerId, content, "Sletter arbeidsliste");
    }

    @SneakyThrows
    public void updateOvergangsstonad(AktorId aktorId, EnsligeForsorgerOvergangsstønadTiltakDto ensligeForsorgerOvergangsstønadTiltakDto) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("enslige_forsorgere_overgangsstonad")
                .field("vedtaksPeriodetype", ensligeForsorgerOvergangsstønadTiltakDto.vedtaksPeriodetypeBeskrivelse())
                .field("harAktivitetsplikt", ensligeForsorgerOvergangsstønadTiltakDto.aktivitsplikt())
                .field("utlopsDato", ensligeForsorgerOvergangsstønadTiltakDto.utløpsDato())
                .field("yngsteBarnsFødselsdato", ensligeForsorgerOvergangsstønadTiltakDto.yngsteBarnsFødselsdato())
                .endObject()
                .endObject();

        update(aktorId, content, "Update overgangsstønad");
    }

    @SneakyThrows
    public void deleteOvergansstonad(AktorId aktorId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("enslige_forsorgere_overgangsstonad", (String) null)
                .endObject();

        update(aktorId, content, "Fjern overgangsstønad");
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

    private void update(AktorId aktoerId, XContentBuilder content, String logInfo) throws IOException {
        if (!oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktoerId)) {
            secureLog.info("Oppdaterte ikke OS for brukere som ikke er under oppfolging, heller ikke for historiske identer: {}, med info {}", aktoerId, logInfo);
            return;
        }
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(indexName.getValue());
        updateRequest.id(aktoerId.get());
        updateRequest.doc(content);
        updateRequest.retryOnConflict(6);

        try {
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
            secureLog.info("Oppdaterte dokument for bruker {} med info {}", aktoerId, logInfo);
        } catch (OpenSearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                secureLog.warn("Kunne ikke finne dokument for bruker {} ved oppdatering av indeks", aktoerId);
            } else {
                final String message = format("Det skjedde en feil ved oppdatering av opensearch for bruker %s", aktoerId);
                secureLog.error(message, e);
            }
        }
    }

    @SneakyThrows
    private void delete(AktorId aktoerId) {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.index(indexName.getValue());
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


}
