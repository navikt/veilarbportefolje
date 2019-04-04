package no.nav.fo.veilarbportefolje.indeksering;

import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;

import static no.nav.fo.veilarbportefolje.indeksering.ElasticUtils.onDevillo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ElasticUtilsTest {

    @Rule
    public SystemPropertiesRule rule = new SystemPropertiesRule().setProperty("FASIT_ENVIRONMENT_NAME", "test");

    @Test
    public void skal_returnere_riktig_elastic_url() {
        String url = ElasticUtils.getAbsoluteUrl();

        if (onDevillo()) {
            assertThat(url).isEqualTo("https://tpa-veilarbelastic-elasticsearch.nais.preprod.local:443/brukerindeks_test/");
        }
    }

}
