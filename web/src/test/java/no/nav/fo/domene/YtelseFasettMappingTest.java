package no.nav.fo.domene;

import no.nav.melding.virksomhet.loependeytelser.v1.AAPtellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Optional;

import static no.nav.fo.domene.YtelseMapping.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class YtelseFasettMappingTest {
    @Test
    public void skalKlassifisereOrdinareDagpenger() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "DAGO")))
                .isEqualTo(Optional.of(ORDINARE_DAGPENGER));
    }

    @Test
    public void skalKlassifisereDagpengerMedPermittering() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "PERM")))
                .isEqualTo(Optional.of(DAGPENGER_MED_PERMITTERING));
    }

    @Test
    public void skalKlassifisereDagpengerMedPermitteringFiskeindustri() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "FISK")))
                .isEqualTo(Optional.of(DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI));
    }

    @Test
    public void skalKlassifisereLonnsgarantimidlerDagpenger() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "LONN")))
                .isEqualTo(Optional.of(LONNSGARANTIMIDLER_DAGPENGER));
    }

    @Test
    public void skalKlassifisereAAPMaxTid() throws Exception {
        LoependeVedtak vedtak = lagVedtak("AA", "AAP");
        vedtak.setAaptellere(new AAPtellere());
        assertThat(YtelseMapping.of(vedtak)).isEqualTo(Optional.of(AAP_MAXTID));
    }

    @Test
    public void skalKlassifisereAAPUnntak() throws Exception {
        LoependeVedtak vedtak = lagVedtak("AA", "AAP");
        AAPtellere teller = new AAPtellere();
        teller.setAntallDagerIgjenUnntak(BigInteger.ONE);
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