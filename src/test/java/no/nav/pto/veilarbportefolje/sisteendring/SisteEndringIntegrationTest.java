package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktoerId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SisteEndringIntegrationTest extends EndToEndTest {

    private final AktivitetService aktivitetService;

    @Autowired
    public SisteEndringIntegrationTest(AktivitetService aktivitetService) {
        this.aktivitetService = aktivitetService;
    }

    @Test
    public void siste_endring_full_integration() {
        final AktoerId aktoerId = randomAktoerId();
        elasticTestClient.createUserInElastic(aktoerId);
        String aktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\""+aktoerId.getValue()+"\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                "\"endretDato\":\"2020-05-28T09:47:42.48+02:00\"," +
                "\"aktivitetType\":\"IJOBB\"," +
                "\"aktivitetStatus\":\"FULLFORT\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";
        aktivitetService.behandleKafkaMelding(aktivitetKafkaMelding);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);

        assertThat(getResponse.isExists()).isTrue();

        String endring_kategori = (String) getResponse.getSourceAsMap().get("siste_endring_kategori");
        String endring_tidspunkt = (String) getResponse.getSourceAsMap().get("siste_endring_tidspunkt");

        assertThat(endring_kategori).isEqualTo(SisteEndringsKategorier.ENDRET_AKTIVITET.toString());
        System.out.println(endring_tidspunkt);
    }

}
