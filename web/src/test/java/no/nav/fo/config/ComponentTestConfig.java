package no.nav.fo.config;

import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.mock.AktoerServiceMock;
import no.nav.fo.mock.EnhetMock;
import no.nav.fo.mock.SolrServiceMock;
import no.nav.fo.service.*;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.virksomhet.tjenester.enhet.meldinger.v1.WSHentEnhetListeResponse;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import static io.vavr.control.Try.success;
import static no.nav.apiapp.ApiApplication.Sone.FSS;
import static no.nav.fo.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.mock.EnhetMock.NAV_SANDE_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Import({
        DatabaseConfigTest.class,
        ServiceConfig.class,
        RestConfig.class
})

public class ComponentTestConfig implements ApiApplication {

    @Bean
    public AktoerService aktoerService() {
        return new AktoerServiceMock();
    }

    @Bean
    public AktorService aktorService() {
        return mock(AktorService.class);
    }

    @Bean
    public VirksomhetEnhetService virksomhetEnhetService() {
        VirksomhetEnhetService virksomhetEnhetService = mock(VirksomhetEnhetService.class);
        WSHentEnhetListeResponse wsHentEnhetListeResponse = new WSHentEnhetListeResponse();
        wsHentEnhetListeResponse.getEnhetListe().add(new no.nav.virksomhet.organisering.enhetogressurs.v1.Enhet().withEnhetId(NAV_SANDE_ID).withNavn(NAV_SANDE_ID));
        when(virksomhetEnhetService.hentEnheter(any())).thenReturn(success(wsHentEnhetListeResponse));
        return virksomhetEnhetService;
    }

    @Bean
    public BrukerRepository brukerRepository(JdbcTemplate jdbcTemplate, DataSource ds, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new BrukerRepository(jdbcTemplate, ds, namedParameterJdbcTemplate);
    }

    @Bean
    public SolrService solrService() {
        return new SolrServiceMock();
    }

    @Bean
    public Pep pep() {
        return mock(Pep.class);
    }

    @Bean
    public PepClient pepClient() {
        return new PepClientMock();
    }

    @Bean
    public Enhet enhet() {
        return new EnhetMock();
    }

    @Bean
    public DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1() {
        return mock(DigitalKontaktinformasjonV1.class);
    }
}
