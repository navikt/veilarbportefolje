package no.nav.pto.veilarbportefolje.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoekerEntity;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ProfileringEntity;
import no.nav.pto.veilarbportefolje.dialog.DialogdataDto;
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.output.EnsligeForsorgerOvergangsstønadTiltakDto;
import no.nav.pto.veilarbportefolje.hendelsesfilter.Hendelse;
import no.nav.pto.veilarbportefolje.hendelsesfilter.Kategori;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringDTO;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet;
import org.opensearch.OpenSearchException;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

import static java.lang.String.format;
import static no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerMapperKt.mapTilUtdanning;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;
import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpensearchIndexerPaDatafelt {

    private final IndexName indexName;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final RestHighLevelClient restHighLevelClient;

    @SneakyThrows
    public void updateOpplysningerOmArbeidssoeker(AktorId aktoerId, OpplysningerOmArbeidssoekerEntity opplysningerOmArbeidssoeker) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("brukers_situasjoner", opplysningerOmArbeidssoeker.getOpplysningerOmJobbsituasjon().getJobbsituasjon())
                .field("utdanning", mapTilUtdanning(opplysningerOmArbeidssoeker.getUtdanningNusKode()))
                .field("utdanning_bestatt", opplysningerOmArbeidssoeker.getUtdanningBestatt())
                .field("utdanning_godkjent", opplysningerOmArbeidssoeker.getUtdanningGodkjent())
                .field("utdanning_og_situasjon_sist_endret", toLocalDateOrNull(opplysningerOmArbeidssoeker.getSendtInnTidspunkt()))
                .endObject();

        update(aktoerId, content, "Oppdater opplysninger om arbeidssøker");
    }

    @SneakyThrows
    public void updateProfilering(AktorId aktoerId, ProfileringEntity profileringEntity) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("profilering_resultat", profileringEntity.getProfileringsresultat())
                .endObject();

        update(aktoerId, content, "Oppdater profileringsresultat");
    }

    @SneakyThrows
    public void updateHuskelapp(AktorId aktoerId, HuskelappForBruker huskelapp) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("huskelapp")
                .field("frist", huskelapp.frist())
                .field("kommentar", huskelapp.kommentar())
                .field("endretAv", huskelapp.endretAv())
                .field("endretDato", huskelapp.endretDato())
                .field("huskelappId", huskelapp.huskelappId())
                .field("enhetId", huskelapp.enhetId())
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
    public void updateFargekategori(AktorId aktoerId, String fargekategori, String enhetId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("fargekategori", fargekategori)
                .field("fargekategori_enhetId", enhetId)
                .endObject();

        update(aktoerId, content, "Oppretter/redigerer fargekategori");
    }


    @SneakyThrows
    public void slettFargekategori(AktorId aktoerId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .nullField("fargekategori")
                .nullField("fargekategori_enhetId")
                .endObject();

        update(aktoerId, content, "Sletter fargekategori");
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
                .field("erSett", "N")
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
                .field("erSett", "J")
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
    public void oppdaterVeileder(AktorId aktoerId, VeilederId veilederId, ZonedDateTime tildeltTidspunkt) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("veileder_id", veilederId.toString())
                .field("ny_for_veileder", true)
                .field("tildelt_tidspunkt", toIsoUTC(tildeltTidspunkt))
                .endObject();

        update(aktoerId, content, "Oppdatert veileder");
    }

    @SneakyThrows
    public void updateDialog(DialogdataDto melding) {
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
    public void updateTiltakshendelse(AktorId aktorId, Tiltakshendelse tiltakshendelse) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("tiltakshendelse")
                .field("id", tiltakshendelse.id().toString())
                .field("lenke", tiltakshendelse.lenke())
                .field("opprettet", tiltakshendelse.opprettet())
                .field("tekst", tiltakshendelse.tekst())
                .field("tiltakstype", tiltakshendelse.tiltakstype())
                .endObject()
                .endObject();

        update(aktorId, content, format("Oppdatert tiltakshendelse med id: %s", tiltakshendelse.id()));
    }

    @SneakyThrows
    public void slettTiltakshendelse(AktorId aktorId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .nullField("tiltakshendelse")
                .endObject();

        update(aktorId, content, format("Slettet tiltakshendelse for aktorId: %s", aktorId.get()));
    }

    @SneakyThrows
    public void updateGjeldendeVedtak14a(GjeldendeVedtak14a gjeldendeVedtak14a, AktorId aktorId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("gjeldendeVedtak14a")
                .field("innsatsgruppe", gjeldendeVedtak14a.innsatsgruppe())
                .field("hovedmal", gjeldendeVedtak14a.hovedmal())
                .field("fattetDato", gjeldendeVedtak14a.fattetDato())
                .endObject()
                .endObject();

        update(aktorId, content, format("Oppdaterte gjeldendeVedtak14a for aktorId: %s", aktorId.get()));
    }

    @SneakyThrows
    public void oppdaterHendelse(Hendelse hendelse, AktorId aktorId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .startObject("hendelser")
                .startObject(hendelse.getKategori().name())
                .field("beskrivelse", hendelse.getHendelse().getBeskrivelse())
                .field("dato", hendelse.getHendelse().getDato())
                .field("lenke", hendelse.getHendelse().getLenke().toString())
                .field("detaljer", hendelse.getHendelse().getDetaljer())
                .endObject()
                .endObject()
                .endObject();

        update(aktorId, content, format("Oppdaterte hendelse med kategori %s for aktorId: %s", hendelse.getKategori().name(), aktorId.get()));
    }

    @SneakyThrows
    public void slettHendelse(Kategori kategori, AktorId aktorId) {
        Map<String, Object> params = Map.of("kategori", kategori.name());

        Script updateScript = new Script(
                ScriptType.INLINE,
                "painless",
                "ctx._source.hendelser.remove(params.kategori)",
                params);

        updateWithScript(aktorId, updateScript, format("Slettet hendelse med kategori %s for aktorId: %s", kategori.name(), aktorId.get()));
    }

    @SneakyThrows
    public void oppdaterAapKelvin(AktorId aktorId, boolean harAapKelvin, LocalDate tomVedtaksdato, AapRettighetstype rettighetstype) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("aap_kelvin", harAapKelvin)
                .field("aap_kelvin_tom_vedtaksdato", tomVedtaksdato)
                .field("aap_kelvin_rettighetstype", rettighetstype)
                .endObject();

        update(aktorId, content, format("Oppdatert aap kelvin for aktorId: %s", aktorId.get()));

    }

    @SneakyThrows
    public void slettAapKelvin(AktorId aktorId) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("aap_kelvin", false)
                .nullField("aap_kelvin_tom_vedtaksdato")
                .nullField("aap_kelvin_rettighetstype")
                .endObject();

        update(aktorId, content, format("Slettet aap kelvin for aktorId: %s", aktorId.get()));
    }

    @SneakyThrows
    public void oppdaterTiltakspenger(AktorId aktorId, boolean harTiltakspenger, LocalDate vedtaksdatoTom, TiltakspengerRettighet rettighet) {
        final XContentBuilder content = jsonBuilder()
                .startObject()
                .field("tiltakspenger", harTiltakspenger)
                .field("tiltakspenger_vedtaksdato_tom", vedtaksdatoTom)
                .field("tiltakspenger_rettighet", rettighet)
                .endObject();

        update(aktorId, content, format("Oppdatert tiltakspenger for aktorId: %s", aktorId.get()));

    }

    public void updateWithScript(AktorId aktoerId, Script script, String logInfo) throws IOException {
        UpdateRequest request = new UpdateRequest().script(script);
        executeUpdate(aktoerId, request, logInfo);
    }

    public void update(AktorId aktoerId, XContentBuilder docContent, String logInfo) throws IOException {
        UpdateRequest request = new UpdateRequest().doc(docContent);
        executeUpdate(aktoerId, request, logInfo);
    }

    private void executeUpdate(AktorId aktoerId, UpdateRequest updateRequest, String logInfo) throws IOException {
        if (!oppfolgingRepositoryV2.erUnderOppfolgingOgErAktivIdent(aktoerId)) {
            secureLog.info("Oppdaterte ikke OS for brukere som ikke er under oppfolging, heller ikke for historiske identer: {}, med info {}", aktoerId, logInfo);
            return;
        }

        updateRequest
                .index(indexName.getValue())
                .id(aktoerId.get())
                .retryOnConflict(6);

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
}
