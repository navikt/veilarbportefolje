package no.nav.fo.service;

import no.nav.fo.config.CacheConfig;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CacheConfig.class, TestPepConfig.class})
public class PepClientTest {
    @Inject
    Pep pep;
    @Inject
    PepClient pepClient;

    @Before
    public void before() throws Exception {
        Mockito.reset(pep);

        when(pep.isSubjectAuthorizedToSeeEgenAnsatt(anyString(), anyString())).thenReturn(new BiasedDecisionResponse(Decision.Permit, null));
        when(pep.isSubjectAuthorizedToSeeKode6(anyString(), anyString())).thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
        when(pep.isSubjectAuthorizedToSeeKode7(anyString(), anyString())).thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
        when(pep.isSubjectMemberOfModiaOppfolging(anyString(), anyString())).thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
        when(pep.harInnloggetBrukerTilgangTilPerson(anyString(), anyString()))
                .thenReturn(new BiasedDecisionResponse(Decision.Deny, null));
    }

    /**
     * Denne testen MÅ kjøre OK slik at vi kan garantere at sikkerheten rundt kode6/7 er på plass.
     * <p>
     * Feil på denne testen indikerer feil i cache-oppsett, og kan medføre at brukere med kode6/7 blir vist til
     * saksbehandlere som ikke har tilgang.
     */
    @Test
    public void cacheSikkerhet() {
        assertThat(pepClient.isSubjectAuthorizedToSeeEgenAnsatt("cacheSikkerhet")).isTrue();
        assertThat(pepClient.isSubjectAuthorizedToSeeKode6("cacheSikkerhet")).isFalse();
        assertThat(pepClient.isSubjectAuthorizedToSeeKode7("cacheSikkerhet")).isFalse();
    }

    @Test
    public void cacheOppsett() throws Exception {
        pepClient.isSubjectAuthorizedToSeeKode7("cacheOppsett");
        pepClient.isSubjectAuthorizedToSeeKode7("cacheOppsett2");
        pepClient.isSubjectAuthorizedToSeeKode7("cacheOppsett");
        pepClient.isSubjectAuthorizedToSeeKode7("cacheOppsett2");
        verify(pep, times(2)).isSubjectAuthorizedToSeeKode7(anyString(), anyString());

        pepClient.isSubjectAuthorizedToSeeKode6("cacheOppsett");
        pepClient.isSubjectAuthorizedToSeeKode6("cacheOppsett2");
        pepClient.isSubjectAuthorizedToSeeKode6("cacheOppsett");
        pepClient.isSubjectAuthorizedToSeeKode6("cacheOppsett2");
        verify(pep, times(2)).isSubjectAuthorizedToSeeKode6(anyString(), anyString());

        pepClient.isSubjectAuthorizedToSeeEgenAnsatt("cacheOppsett");
        pepClient.isSubjectAuthorizedToSeeEgenAnsatt("cacheOppsett2");
        pepClient.isSubjectAuthorizedToSeeEgenAnsatt("cacheOppsett");
        pepClient.isSubjectAuthorizedToSeeEgenAnsatt("cacheOppsett2");
        verify(pep, times(2)).isSubjectAuthorizedToSeeEgenAnsatt(anyString(), anyString());

        pepClient.isSubjectMemberOfModiaOppfolging("cacheOppsett", "cacheOppsett");
        pepClient.isSubjectMemberOfModiaOppfolging("cacheOppsett2", "cacheOppsett2");
        pepClient.isSubjectMemberOfModiaOppfolging("cacheOppsett", "cacheOppsett");
        pepClient.isSubjectMemberOfModiaOppfolging("cacheOppsett2", "cacheOppsett2");
        verify(pep, times(2)).isSubjectMemberOfModiaOppfolging(anyString(), anyString());

        pepClient.tilgangTilBruker("cacheOppsett", "cacheOppsett");
        pepClient.tilgangTilBruker("cacheOppsett2", "cacheOppsett2");
        pepClient.tilgangTilBruker("cacheOppsett", "cacheOppsett");
        pepClient.tilgangTilBruker("cacheOppsett2", "cacheOppsett2");
        verify(pep, times(2)).harInnloggetBrukerTilgangTilPerson(anyString(), anyString());
    }
}