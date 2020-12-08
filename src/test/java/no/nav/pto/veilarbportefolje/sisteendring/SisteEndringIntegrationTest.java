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
    public void siste_endring_ulike_typer_aktivteter() {
        final AktoerId aktoerId = randomAktoerId();
        elasticTestClient.createUserInElastic(aktoerId);

        String nyAktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\""+aktoerId.getValue()+"\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                "\"aktivitetType\":\"IJOBB\"," +
                "\"aktivitetStatus\":\"FULLFORT\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";

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
        aktivitetService.behandleKafkaMelding(nyAktivitetKafkaMelding);
        aktivitetService.behandleKafkaMelding(aktivitetKafkaMelding);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);

        assertThat(getResponse.isExists()).isTrue();

        String endring_tidspunkt = (String) getResponse.getSourceAsMap().get("siste_endring_endret_aktivitet");
        String ny_tidspunkt = (String) getResponse.getSourceAsMap().get("siste_endring_ny_aktivitet");

        assertThat(endring_tidspunkt).isEqualTo("2020-05-28 09:47:42.48");
        assertThat(ny_tidspunkt).isNotNull();
        assertThat(!ny_tidspunkt.equals(endring_tidspunkt)).isTrue();
    }

}
