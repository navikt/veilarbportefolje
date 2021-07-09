package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.UtdanningsAktivitetDTO;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.UtdanningsAktivitetInnhold;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class DtoParsing {
    @Test
    public void skal_bygge_korrekt_UtdanningsAktivitetInnhold_json() {
        String goldenGateDtoString = readFileAsJsonString("/goldenGateUtdanningsAktivitet.json", getClass());

        UtdanningsAktivitetDTO goldenGateDTO = fromJson(goldenGateDtoString, UtdanningsAktivitetDTO.class);
        assertThat(goldenGateDTO.getCurrentTimestamp()).isEqualTo("2021-06-23T09:03:55.677014");
        assertThat(goldenGateDTO.getAfter().getClass()).isEqualTo(UtdanningsAktivitetInnhold.class);
        assertThat(goldenGateDTO.getAfter().getEndretDato().getDato().toString()).isEqualTo("2021-06-18T00:00+02:00[Europe/Oslo]");
        assertThat(goldenGateDTO.getAfter()).isNotNull();
        assertThat(goldenGateDTO.getBefore()).isNull();
    }

    @Test
    public void skal_bygge_korrekt_GruppeAktivitetInnhold_json() {
        String goldenGateDtoString = readFileAsJsonString("/goldenGateUtdanningsAktivitet.json", getClass());

        GruppeAktivitetDTO goldenGateDTO = fromJson(goldenGateDtoString, GruppeAktivitetDTO.class);
        assertThat(goldenGateDTO.getCurrentTimestamp()).isEqualTo("2021-06-23T09:03:55.677014");
        assertThat(goldenGateDTO.getAfter().getClass()).isEqualTo(GruppeAktivitetInnhold.class);
        assertThat(goldenGateDTO.getAfter()).isNotNull();
        assertThat(goldenGateDTO.getBefore()).isNull();
    }
}
