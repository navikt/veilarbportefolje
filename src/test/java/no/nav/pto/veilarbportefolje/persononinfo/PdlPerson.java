package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson.kontrollerResponseOgHentNavn;
import static org.assertj.core.api.Assertions.assertThat;

public class PdlPerson {

    @Test
    public void prioriteringAvUlikeKilderForNavn() {
        String pdlNavn = "pdl_F";
        String fregNavn = "freg_F";
        String annetNavn = "annet_F";
        PdlPersonResponse.PdlPersonResponseData.Metadata pdlMeta = new PdlPersonResponse.PdlPersonResponseData.Metadata()
                .setHistorisk(false)
                .setMaster(PdlPersonResponse.PdlMaster.PDL);
        PdlPersonResponse.PdlPersonResponseData.Metadata fregMeta = new PdlPersonResponse.PdlPersonResponseData.Metadata()
                .setHistorisk(false)
                .setMaster(PdlPersonResponse.PdlMaster.FREG);
        PdlPersonResponse.PdlPersonResponseData.Metadata annetMeta = new PdlPersonResponse.PdlPersonResponseData.Metadata()
                .setHistorisk(false)
                .setMaster(PdlPersonResponse.PdlMaster.UVIST);

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
    public void prioriteringAvSammeKildeForNavn_skalVelgeForsteIListen() {
        String pdlNavn1 = "pdl_1";
        String pdlNavn2 = "pdl_2";

        PdlPersonResponse.PdlPersonResponseData.Metadata pdlMeta = new PdlPersonResponse.PdlPersonResponseData.Metadata()
                .setHistorisk(false)
                .setMaster(PdlPersonResponse.PdlMaster.PDL);
        List<PdlPersonResponse.PdlPersonResponseData.Navn> navn = List.of(
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(pdlNavn1).setMetadata(pdlMeta),
                new PdlPersonResponse.PdlPersonResponseData.Navn().setFornavn(pdlNavn2).setMetadata(pdlMeta)
        );

        assertThat(kontrollerResponseOgHentNavn(navn).getFornavn()).isEqualTo(pdlNavn1);
    }
}
