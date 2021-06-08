package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.val;
import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.UtdanningsAktivitet;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO.UtdanningsAktivitetInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class DtoParsing {


    @Test
    public void skal_bygge_korrekt_objekt_fra_json() {
        val goldenGateDto = readFileAsJsonString("/goldenGateUtdanningsAktivitet.json", getClass());
        UtdanningsAktivitet goldenGateDTO = fromJson(goldenGateDto, new TypeReference<>() {
        });
        assertThat(goldenGateDTO.getCurrentTimestamp()).isEqualTo("2021-05-21T14:57:16.390000");
        assertThat(goldenGateDTO.getAfter().getClass()).isEqualTo(UtdanningsAktivitetInnhold.class);
        assertThat(goldenGateDTO.getAfter()).isNotNull();
        assertThat(goldenGateDTO.getBefore()).isNull();
    }
}
