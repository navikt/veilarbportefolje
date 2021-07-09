package no.nav.pto.veilarbportefolje.domene;

import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;
import org.junit.Test;

import java.util.Optional;

import static no.nav.pto.veilarbportefolje.domene.YtelseMapping.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    public void skalKlassifisereOvrigeDagpenger() throws Exception {
        assertThat(YtelseMapping.of(lagVedtak("DAGP", "NOEANNET"))).isEqualTo(Optional.of(DAGPENGER_OVRIGE));
    }

    @Test
    public void skalKlassifisereAAPMaxTid() throws Exception {
        YtelsesInnhold vedtak = lagVedtak("AA", "AAP");
        vedtak.setAaptellere(new YtelsesInnhold.Aaptellere());
        assertThat(YtelseMapping.of(vedtak)).isEqualTo(Optional.of(AAP_MAXTID));
    }

    @Test
    public void skalKlassifisereAAPUnntak() throws Exception {
        YtelsesInnhold vedtak = lagVedtak("AA", "AAP");
        YtelsesInnhold.Aaptellere teller =new YtelsesInnhold.Aaptellere();
        teller.setAntallDagerIgjenUnntak(1);
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

    private YtelsesInnhold lagVedtak(String sakstypekode, String rettighetstypekode) {
        YtelsesInnhold vedtak = new YtelsesInnhold();
        vedtak.setSakstypeKode(sakstypekode);
        vedtak.setRettighetstypeKode(rettighetstypekode);
        return vedtak;
    }
}
