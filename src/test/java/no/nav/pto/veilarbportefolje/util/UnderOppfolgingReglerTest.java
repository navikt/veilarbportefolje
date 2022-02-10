package no.nav.pto.veilarbportefolje.util;

import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import org.junit.Test;

import static no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler.erUnderOppfolging;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UnderOppfolgingReglerTest {

    @Test
    public void skal_vaere_under_oppfolging() {
        var bruker = new OppfolgingsBruker()
                .setFnr("00000000000")
                .setFormidlingsgruppekode("foo")
                .setKvalifiseringsgruppekode("bar")
                .setOppfolging(true);

        var result = erUnderOppfolging(bruker);
        assertThat(result).isTrue();
    }

    @Test
    public void skal_ikke_vaere_under_oppfolging() {
        var bruker = new OppfolgingsBruker()
                .setFnr("00000000000")
                .setFormidlingsgruppekode("foo")
                .setKvalifiseringsgruppekode("bar")
                .setOppfolging(false);

        var result = erUnderOppfolging(bruker);
        assertThat(result).isFalse();
    }
}
