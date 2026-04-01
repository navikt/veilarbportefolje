package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Endringer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Metadata;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.PdlMaster;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PdlPersonValideringException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson.kontrollerResponseOgHentKjonn;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson.kontrollerResponseOgHentNavn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PDLPersonTest {

    @Test
    void prioriteringAvUlikeKilderForNavn() {
        String pdlNavn = "pdl_F";
        String fregNavn = "freg_F";
        String annetNavn = "annet_F";
        Metadata pdlMeta = new Metadata()
                .setHistorisk(false)
                .setMaster(PdlMaster.PDL);
        Metadata fregMeta = new Metadata()
                .setHistorisk(false)
                .setMaster(PdlMaster.FREG);
        Metadata annetMeta = new Metadata()
                .setHistorisk(false)
                .setMaster(PdlMaster.UVIST);

        List<PdlPersonResponse.PdlPersonResponseData.Navn> pdl_freg_og_annet = List.of(
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(pdlNavn).setMetadata(pdlMeta),
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(fregNavn).setMetadata(fregMeta),
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(annetNavn).setMetadata(annetMeta)
        );

        List<PdlPersonResponse.PdlPersonResponseData.Navn> freg_og_annet = List.of(
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(fregNavn).setMetadata(fregMeta),
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(annetNavn).setMetadata(annetMeta)
        );

        List<PdlPersonResponse.PdlPersonResponseData.Navn> kun_annet = List.of(
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(annetNavn).setMetadata(annetMeta)
        );

        assertThat(kontrollerResponseOgHentNavn(pdl_freg_og_annet).getFornavn()).isEqualTo(pdlNavn);
        assertThat(kontrollerResponseOgHentNavn(freg_og_annet).getFornavn()).isEqualTo(fregNavn);
        assertThat(kontrollerResponseOgHentNavn(kun_annet).getFornavn()).isEqualTo(annetNavn);
    }

    @Test
    void prioriteringAvSammeKildeForNavn_skalVelgeForsteIListen() {
        String pdlNavn1 = "pdl_1";
        String pdlNavn2 = "pdl_2";

        Metadata pdlMeta = new Metadata()
                .setHistorisk(false)
                .setMaster(PdlMaster.PDL);
        List<PdlPersonResponse.PdlPersonResponseData.Navn> navn = List.of(
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(pdlNavn1).setMetadata(pdlMeta),
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(pdlNavn2).setMetadata(pdlMeta)
        );

        assertThat(kontrollerResponseOgHentNavn(navn).getFornavn()).isEqualTo(pdlNavn1);
    }

    @Test
    void enkelKjonnFraPdl_returnererRiktigKjonn() {
        var kjoenn = lagKjoenn("MANN", PdlMaster.PDL, "2024-01-01");
        assertThat(kontrollerResponseOgHentKjonn(List.of(kjoenn))).isEqualTo(Kjonn.M);
    }

    @Test
    void enkelKjonnFraFreg_returnererRiktigKjonn() {
        var kjoenn = lagKjoenn("KVINNE", PdlMaster.FREG, "2024-01-01");
        assertThat(kontrollerResponseOgHentKjonn(List.of(kjoenn))).isEqualTo(Kjonn.K);
    }

    @Test
    void flerKjonn_pdlOgFreg_velgerPdlNaarPdlErNyest() {
        var pdlKjoenn = lagKjoenn("MANN", PdlMaster.PDL, "2024-06-01");
        var fregKjoenn = lagKjoenn("KVINNE", PdlMaster.FREG, "2024-01-01");
        assertThat(kontrollerResponseOgHentKjonn(List.of(pdlKjoenn, fregKjoenn))).isEqualTo(Kjonn.M);
    }

    @Test
    void flerKjonn_pdlOgFreg_velgerFregNaarFregErNyere() {
        var pdlKjoenn = lagKjoenn("MANN", PdlMaster.PDL, "2024-01-01");
        var fregKjoenn = lagKjoenn("KVINNE", PdlMaster.FREG, "2024-06-01");
        assertThat(kontrollerResponseOgHentKjonn(List.of(pdlKjoenn, fregKjoenn))).isEqualTo(Kjonn.K);
    }

    @Test
    void flerKjonn_sammeDato_velgerPdl() {
        var pdlKjoenn = lagKjoenn("MANN", PdlMaster.PDL, "2024-01-01");
        var fregKjoenn = lagKjoenn("KVINNE", PdlMaster.FREG, "2024-01-01");
        assertThat(kontrollerResponseOgHentKjonn(List.of(pdlKjoenn, fregKjoenn))).isEqualTo(Kjonn.M);
    }

    @Test
    void ingenAktiveKjonn_kasterException() {
        var historisk = lagKjoenn("MANN", PdlMaster.PDL, "2024-01-01");
        historisk.getMetadata().setHistorisk(true);
        var input = List.of(historisk);
        assertThatThrownBy(() -> kontrollerResponseOgHentKjonn(input))
                .isInstanceOf(PdlPersonValideringException.class);
    }

    @Test
    void ukjentKjonn_kasterException() {
        var kjoenn = lagKjoenn("ANNET", PdlMaster.PDL, "2024-01-01");
        var input = List.of(kjoenn);
        assertThatThrownBy(() -> kontrollerResponseOgHentKjonn(input))
                .isInstanceOf(PdlPersonValideringException.class);
    }

    private PdlPersonResponse.PdlPersonResponseData.Kjoenn lagKjoenn(String kjonn, PdlMaster master, String registrert) {
        var endring = new Endringer();
        endring.setRegistrert(registrert);
        var metadata = new Metadata()
                .setHistorisk(false)
                .setMaster(master)
                .setEndringer(List.of(endring));
        var kjoenn = new PdlPersonResponse.PdlPersonResponseData.Kjoenn();
        kjoenn.setKjoenn(kjonn);
        kjoenn.setMetadata(metadata);
        return kjoenn;
    }


}
