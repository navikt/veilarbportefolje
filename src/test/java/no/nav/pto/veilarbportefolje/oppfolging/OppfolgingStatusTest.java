package no.nav.pto.veilarbportefolje.oppfolging;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingStatusTest {

    @Test
    public void skal_deserialisere_kafka_payload() {
        String json = "{\"aktoerid\":\"testAktoerId\","
                      + "\"veileder\":\"testVeilederId\""
                      + ",\"oppfolging\":true"
                      + ",\"nyForVeileder\":true"
                      + ",\"manuell\":false"
                      + ",\"endretTimestamp\":\"2020-05-12T16:40:20.586+02:00\""
                      + ",\"startDato\":\"2019-11-01T13:45:54.788+01:00\"}";

        OppfolgingStatus status = OppfolgingStatus.fromJson(json);
        assertThat(status.getAktoerId()).isNotNull();
        assertThat(status.getVeilederId()).isNotNull();
        assertThat(status.isOppfolging()).isTrue();
        assertThat(status.isManuell()).isFalse();
        assertThat(status.isNyForVeileder()).isTrue();
        assertThat(status.getEndretTimestamp()).isNotNull();
        assertThat(status.getStartDato()).isNotNull();
    }
}