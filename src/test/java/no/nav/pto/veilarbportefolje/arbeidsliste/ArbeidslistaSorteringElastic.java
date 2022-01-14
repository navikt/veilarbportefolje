package no.nav.pto.veilarbportefolje.arbeidsliste;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Brukerstatus;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste.Kategori.BLA;
import static no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste.Kategori.GRONN;
import static no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste.Kategori.LILLA;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ArbeidslistaSorteringElastic extends EndToEndTest {
    private final ElasticService elasticService;
    private final ElasticServiceV2 elasticServiceV2;

    @Autowired
    public ArbeidslistaSorteringElastic(ElasticService elasticService, ElasticServiceV2 elasticServiceV2) {
        this.elasticService = elasticService;
        this.elasticServiceV2 = elasticServiceV2;
    }

    @Test
    public void sisteendring_sortering() {
        EnhetId enhetId = EnhetId.of("123");
        VeilederId veilederId = new VeilederId("V1");
        final AktorId aktoerId_1 = randomAktorId();
        final AktorId aktoerId_2 = randomAktorId();
        final AktorId aktoerId_3 = randomAktorId();
        populateElastic(enhetId, veilederId, aktoerId_1.get(), aktoerId_2.get(), aktoerId_3.get());
        Arbeidsliste.Kategori arbeidsliste1_kategori = GRONN;
        Arbeidsliste.Kategori arbeidsliste2_kategori = BLA;
        Arbeidsliste.Kategori arbeidsliste3_kategori = LILLA;
        ArbeidslisteDTO arbeidsliste1 = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
                .setAktorId(aktoerId_1)
                .setVeilederId(veilederId)
                .setKategori(arbeidsliste1_kategori)
                .setOverskrift("Arbeid er en overskrift");

        ArbeidslisteDTO arbeidsliste2 = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
                .setAktorId(aktoerId_2)
                .setVeilederId(veilederId)
                .setKategori(arbeidsliste2_kategori)
                .setOverskrift("Arbeid skal først");

        ArbeidslisteDTO arbeidsliste3 = new ArbeidslisteDTO(Fnr.ofValidFnr("01010101010"))
                .setAktorId(aktoerId_3)
                .setVeilederId(veilederId)
                .setKategori(arbeidsliste3_kategori)
                .setOverskrift("Nav skal sist");


        elasticServiceV2.updateArbeidsliste(arbeidsliste1);
        elasticServiceV2.updateArbeidsliste(arbeidsliste3);
        elasticServiceV2.updateArbeidsliste(arbeidsliste2);

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    enhetId.get(),
                    empty(),
                    "ascending",
                    "ikke_satt",
                    getArbeidslisteFilter(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 3;
        });

        var sortertResponsAscending = elasticService.hentBrukere(
                enhetId.get(),
                empty(),
                "ascending",
                "arbeidsliste_overskrift",
                getArbeidslisteFilter(),
                null,
                null);

        var sortertResponsDescending = elasticService.hentBrukere(
                enhetId.get(),
                empty(),
                "desc",
                "arbeidsliste_overskrift",
                getArbeidslisteFilter(),
                null,
                null);

        assertThat(sortertResponsAscending.getBrukere().get(0).getArbeidsliste().getKategori())
                .isEqualTo(arbeidsliste2_kategori);
        assertThat(sortertResponsAscending.getBrukere().get(1).getArbeidsliste().getKategori())
                .isEqualTo(arbeidsliste1_kategori);
        assertThat(sortertResponsAscending.getBrukere().get(2).getArbeidsliste().getKategori())
                .isEqualTo(arbeidsliste3_kategori);

        assertThat(sortertResponsDescending.getBrukere().get(2).getArbeidsliste().getKategori())
                .isEqualTo(arbeidsliste2_kategori);
        assertThat(sortertResponsDescending.getBrukere().get(1).getArbeidsliste().getKategori())
                .isEqualTo(arbeidsliste1_kategori);
        assertThat(sortertResponsDescending.getBrukere().get(0).getArbeidsliste().getKategori())
                .isEqualTo(arbeidsliste3_kategori);

    }

    private static Filtervalg getArbeidslisteFilter() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(List.of(Brukerstatus.MIN_ARBEIDSLISTE));
        return filtervalg;
    }
}
