package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.val;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import org.junit.Test;

import static no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService.brukerenIkkeLengerErUnderOppfolging;
import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingServiceTest {

    @Test
    public void skal_sjekk_om_bruker_ikke_lenger_er_under_oppfolging() {
        val dto = OppfolgingDTO.builder()
                .aktoerId(AktoerId.of("testId"))
                .oppfolging(false)
                .build();

        boolean result = brukerenIkkeLengerErUnderOppfolging(dto);
        assertThat(result).isTrue();
    }

}