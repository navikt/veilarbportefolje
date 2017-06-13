package no.nav.fo.domene;


import no.nav.fo.service.PepClient;
import no.nav.fo.util.PortefoljeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SensurerBrukerTest {

    @Mock
    private PepClient pepClient;

    @Test
    public void skalIkkeSeKode6Bruker() {
        when(pepClient.isSubjectAuthorizedToSeeKode6(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.sensurerBrukere(kode6Bruker(),"X123456",pepClient);
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere.get(0));
    }

    @Test
    public void skalIkkeSeKode7Bruker() {
        when(pepClient.isSubjectAuthorizedToSeeKode7(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.sensurerBrukere(kode7Bruker(),"X123456",pepClient);
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere.get(0));
    }

    @Test
    public void skalIkkeSeEgenAnsatt() {
        when(pepClient.isSubjectAuthorizedToSeeEgenAnsatt(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.sensurerBrukere(egenAnsatt(),"X123456",pepClient);
        sjekkAtKonfidensiellDataErVasket(filtrerteBrukere.get(0));
    }

    @Test
    public void skalSeKode6Bruker() {
        when(pepClient.isSubjectAuthorizedToSeeKode6(eq("X123456"))).thenReturn(true);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.sensurerBrukere(kode6Bruker(),"X123456",pepClient);
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("fornavnKode6");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("etternanvKode6");
    }

    @Test
    public void skalSeKode7Bruker() {
        when(pepClient.isSubjectAuthorizedToSeeKode7(eq("X123456"))).thenReturn(true);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.sensurerBrukere(kode7Bruker(),"X123456",pepClient);
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("fornavnKode7");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("etternanvKode7");
    }

    @Test
    public void skalSeEgenAnsatt() {
        when(pepClient.isSubjectAuthorizedToSeeEgenAnsatt(eq("X123456"))).thenReturn(true);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.sensurerBrukere(egenAnsatt(),"X123456",pepClient);
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("11111111111");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("fornavnKodeEgenAnsatt");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("etternanvEgenAnsatt");
    }

    @Test
    public void skalSeIkkeKonfidensiellBruker() {
        when(pepClient.isSubjectAuthorizedToSeeKode7(eq("X123456"))).thenReturn(false);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.sensurerBrukere(ikkeKonfidensiellBruker(),"X123456",pepClient);
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
