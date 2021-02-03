package no.nav.pto.veilarbportefolje.domene;


import no.nav.common.abac.Pep;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
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
        Bruker filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker(),"X123456");
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere);
    }

    @Test
    public void skalIkkeSeKode7Bruker() {
        when(pep.harVeilederTilgangTilKode7(eq("X123456"))).thenReturn(false);
        Bruker filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker(),"X123456");
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere);
    }

    @Test
    public void skalIkkeSeEgenAnsatt() {
        when(pep.harVeilederTilgangTilEgenAnsatt(eq("X123456"))).thenReturn(false);
        Bruker filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt(),"X123456");
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere);
    }

    @Test
    public void skalSeKode6Bruker() {
        when(pep.harVeilederTilgangTilKode6(eq("X123456"))).thenReturn(true);
        Bruker filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode6Bruker(),"X123456");
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnKode6");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvKode6");
    }

    @Test
    public void skalSeKode7Bruker() {
        when(pep.harVeilederTilgangTilKode7(eq("X123456"))).thenReturn(true);
        Bruker filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(kode7Bruker(), "X123456");
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnKode7");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvKode7");
    }

    @Test
    public void skalSeEgenAnsatt() {
        when(pep.harVeilederTilgangTilEgenAnsatt(eq("X123456"))).thenReturn(true);
        Bruker filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(egenAnsatt(), "X123456");
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnKodeEgenAnsatt");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvEgenAnsatt");
    }

    @Test
    public void skalSeIkkeKonfidensiellBruker() {
        Bruker filtrerteBrukere = authService.fjernKonfidensiellInfoDersomIkkeTilgang(ikkeKonfidensiellBruker(), "X123456");
        assertThat(filtrerteBrukere.getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.getFornavn()).isEqualTo("fornavnIkkeKonfidensiellBruker");
        assertThat(filtrerteBrukere.getEtternavn()).isEqualTo("etternanvIkkeKonfidensiellBruker");
    }


    private void sjekkAtKonfidensiellDataErVasket(Bruker bruker) {
        assertThat(bruker.getFnr()).isEqualTo("");
        assertThat(bruker.getEtternavn()).isEqualTo("");
        assertThat(bruker.getFornavn()).isEqualTo("");
    }

    private Bruker kode6Bruker() {
        return new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvKode6")
                .setFornavn("fornavnKode6")
                .setDiskresjonskode("6");
    }

    private Bruker kode7Bruker() {
        return new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvKode7")
                .setFornavn("fornavnKode7")
                .setDiskresjonskode("7");
    }

    private Bruker egenAnsatt() {
        return new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvEgenAnsatt")
                .setFornavn("fornavnKodeEgenAnsatt")
                .setEgenAnsatt(true);
    }

    private Bruker ikkeKonfidensiellBruker() {
        return new Bruker()
                .setFnr("11111111111")
                .setEtternavn("etternanvIkkeKonfidensiellBruker")
                .setFornavn("fornavnIkkeKonfidensiellBruker");
    }


}
