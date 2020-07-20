package no.nav.pto.veilarbportefolje.domene;


import no.nav.common.abac.Pep;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SensurerBrukerTest {

    @Mock
    private Pep pep;

    @InjectMocks
    private AuthService authService;

    @Test
    public void skalIkkeSeKode6Bruker() {
        when(pep.harVeilederTilgangTilKode6(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = authService.sensurerBrukere(kode6Bruker());
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere.get(0));
    }

    @Test
    public void skalIkkeSeKode7Bruker() {
        when(pep.harVeilederTilgangTilKode7(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = authService.sensurerBrukere(kode7Bruker());
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere.get(0));
    }

    @Test
    public void skalIkkeSeEgenAnsatt() {
        when(pep.harVeilederTilgangTilEgenAnsatt(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = authService.sensurerBrukere(egenAnsatt());
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere.get(0));
    }

    @Test
    public void skalSeKode6Bruker() {
        when(pep.harVeilederTilgangTilKode6(eq("X123456"))).thenReturn(true);
        List<Bruker> filtrerteBrukere = authService.sensurerBrukere(kode6Bruker());
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("fornavnKode6");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("etternanvKode6");
    }

    @Test
    public void skalSeKode7Bruker() {
        when(pep.harVeilederTilgangTilKode7(eq("X123456"))).thenReturn(true);
        List<Bruker> filtrerteBrukere = authService.sensurerBrukere(kode7Bruker());
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("fornavnKode7");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("etternanvKode7");
    }

    @Test
    public void skalSeEgenAnsatt() {
        when(pep.harVeilederTilgangTilEgenAnsatt(eq("X123456"))).thenReturn(true);
        List<Bruker> filtrerteBrukere = authService.sensurerBrukere(egenAnsatt());
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("fornavnKodeEgenAnsatt");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("etternanvEgenAnsatt");
    }

    @Test
    public void skalSeIkkeKonfidensiellBruker() {
        when(pep.harVeilederTilgangTilKode7(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = authService.sensurerBrukere(ikkeKonfidensiellBruker());
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("fornavnIkkeKonfidensiellBruker");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("etternanvIkkeKonfidensiellBruker");
    }


    private void sjekkAtKonfidensiellDataErVasket(Bruker bruker) {
        assertThat(bruker.getFnr()).isEqualTo("");
        assertThat(bruker.getEtternavn()).isEqualTo("");
        assertThat(bruker.getFornavn()).isEqualTo("");
    }

    private List<Bruker> kode6Bruker() {
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvKode6")
                .setFornavn("fornavnKode6")
                .setDiskresjonskode("6"));
        return brukere;
    }

    private List<Bruker> kode7Bruker() {
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvKode7")
                .setFornavn("fornavnKode7")
                .setDiskresjonskode("7"));
        return brukere;
    }

    private List<Bruker> egenAnsatt() {
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvEgenAnsatt")
                .setFornavn("fornavnKodeEgenAnsatt")
                .setEgenAnsatt(true));
        return brukere;
    }

    private List<Bruker> ikkeKonfidensiellBruker() {
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvIkkeKonfidensiellBruker")
                .setFornavn("fornavnIkkeKonfidensiellBruker"));
        return brukere;
    }


}
