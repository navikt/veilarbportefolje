package no.nav.fo.domene;

import no.nav.melding.virksomhet.loependeytelser.v1.AAPtellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Optional;

import static no.nav.fo.domene.YtelseMapping.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class YtelseMappingTest {
    @Test
    public void skalKlassifisereOrdinareDagpenger() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "DAGO"))).isEqualTo(Optional.of(ORDINARE_DAGPENGER));
    }

    @Test
    public void skalKlassifisereDagpengerMedPermittering() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "PERM"))).isEqualTo(Optional.of(DAGPENGER_MED_PERMITTERING));
    }

    @Test
    public void skalKlassifisereOvrigeDagpenger() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "NOEANNET"))).isEqualTo(Optional.of(DAGPENGER_OVRIGE));
    }

    @Test
    public void skalKlassifisereAAPMaxTid() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("AA", "AAP"))).isEqualTo(Optional.of(AAP_MAXTID));
    }

    @Test
    public void skalKlassifisereAAPUnntak() throws Exception {
        LoependeVedtak vedtak = lagVedtak("AA", "AAP");
        AAPtellere teller = new AAPtellere();
        teller.setAntallDagerUnntak(BigInteger.ONE);
        vedtak.setAaptellere(teller);

        assertThat(YtelseMapping.of(vedtak)).isEqualTo(Optional.of(AAP_UNNTAK));
    }

    @Test
    public void skalKlassifisereTiltaksPenger() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("INDIV", "BASI"))).isEqualTo(Optional.of(TILTAKSPENGER));
    }

    @Test
    public void skalKlassifisereFungereMedHeltFeilData() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("TULL", "BALL"))).isEqualTo(Optional.empty());
    }

    private LoependeVedtak lagVedtak(String sakstypekode, String rettighetstypekode) {
        LoependeVedtak vedtak = new LoependeVedtak();
        vedtak.setSakstypeKode(sakstypekode);
        vedtak.setRettighetstypeKode(rettighetstypekode);
        return vedtak;
    }
}