package no.nav.fo.domene;


import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.service.PepClient;
import no.nav.fo.util.PortefoljeUtils;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ApplicationConfigTest.class})
public class FiltrerBrukerTest {

    @Inject
    private PepClient pepClient;

    @Test
    public void skalFjerneNavnOgFNrPaaBrukerSomVeilederIkkeHarTilgangTil() {
        when(pepClient.isServiceCallAllowed(eq("11111111111"),eq("X123456"))).thenReturn(false);
        when(pepClient.isServiceCallAllowed(eq("22222222222"),eq("X123456"))).thenReturn(true);
        List<Bruker> filtrerteBrukere = PortefoljeUtils.filtrerBrukere(genererLittBrukere(),"X123456",pepClient);
        assertThat(filtrerteBrukere.get(0).getFnr()).isEqualTo("");
        assertThat(filtrerteBrukere.get(0).getEtternavn()).isEqualTo("");
        assertThat(filtrerteBrukere.get(0).getFornavn()).isEqualTo("");

        assertThat(filtrerteBrukere.get(1).getFnr()).isEqualTo("22222222222");
        assertThat(filtrerteBrukere.get(1).getEtternavn()).isEqualTo("etternavnSomSkalVises");
        assertThat(filtrerteBrukere.get(1).getFornavn()).isEqualTo("fornavnSomSkalVises");

    }

    private List<Bruker> genererLittBrukere() {
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(new Bruker()
                .setFnr("11111111111")
                .setEgenAnsatt(true)
                .setEtternavn("etternavnSomIkkeSkalVises")
                .setFornavn("fornavnSomIkkeSkalVises"));
        brukere.add(new Bruker()
                .setFnr("22222222222")
                .setEgenAnsatt(true)
                .setEtternavn("etternavnSomSkalVises")
                .setFornavn("fornavnSomSkalVises"));

        return brukere;
    }

}
