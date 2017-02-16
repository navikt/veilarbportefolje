package no.nav.fo.util;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Portefolje;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class PortefoljeUtilsTest {
    private List<Bruker> brukere;

    @Before
    public void setUp() throws Exception {
        brukere = createBrukere();
    }

    @Test
    public void skalGiKorrektSublisteAvBrukere() {
        List<Bruker> subListe = PortefoljeUtils.getSublist(brukere, 2, 2);
        assertThat(subListe.size()).isEqualTo(2);
        assertThat(subListe.get(0).getFornavn()).isEqualTo("Peder");

        subListe = PortefoljeUtils.getSublist(brukere, 0, 3);
        assertThat(subListe.size()).isEqualTo(3);
        assertThat(subListe.get(1).getFornavn()).isEqualTo("Christian");
    }

    @Test
    public void portefoljeSkalHaRiktigInnhold() {
        int fra = 1;
        List<Bruker> subListe = PortefoljeUtils.getSublist(brukere, fra, 3);
        Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukere, subListe,"0106", fra );

        assertThat(portefolje.getBrukere().size()).isEqualTo(subListe.size());
        assertThat(portefolje.getAntallReturnert()).isEqualTo(subListe.size());
        assertThat(portefolje.getAntallTotalt()).isEqualTo(5);
        assertThat(portefolje.getBrukere().get(0).getFornavn()).isEqualTo("Christian");
    }

    List<Bruker> createBrukere() {
        List<Bruker> brukere = new ArrayList<>();
        Bruker bruker1 = createBruker("Johan", "Rusvik");
        Bruker bruker2 = createBruker("Christian", "Finstad");
        Bruker bruker3 =createBruker("Peder", "Korsveien");
        Bruker bruker4 =createBruker("Mathilde", "Kirkhus");
        Bruker bruker5 =createBruker("Ingrid", "Guren");
        brukere.add(bruker1);
        brukere.add(bruker2);
        brukere.add(bruker3);
        brukere.add(bruker4);
        brukere.add(bruker5);

        return brukere;
    }

    Bruker createBruker(String fornavn, String etternavn) {
        return new Bruker()
                .setFnr("123456")
                .setFornavn(fornavn)
                .setEtternavn(etternavn)
                .setVeilderId("112233")
                .setDiskresjonskode("")
                .setEgenAnsatt(false)
                .setErDoed(false)
                .setSikkerhetstiltak(null);
    }
}